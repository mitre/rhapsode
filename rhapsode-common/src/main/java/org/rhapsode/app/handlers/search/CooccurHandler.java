/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * NOTICE

 * This software was produced for the U.S. Government
 * under Basic Contract No. W15P7T-13-C-A802,
 * W15P7T-12-C-F600, and W15P7T-13-C-F600, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 *
 * (C) 2013-2017 The MITRE Corporation. All Rights Reserved.
 *
 */

package org.rhapsode.app.handlers.search;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.eclipse.jetty.server.Request;
import org.rhapsode.app.RhapsodeSearcherApp;
import org.rhapsode.app.contants.C;
import org.rhapsode.app.contants.CSS;
import org.rhapsode.app.contants.H;
import org.rhapsode.app.decorators.CCDecorator;
import org.rhapsode.app.decorators.RhapsodeDecorator;
import org.rhapsode.app.decorators.RhapsodeXHTMLHandler;
import org.rhapsode.app.session.DynamicParameters;
import org.rhapsode.lucene.search.cooccur.CooccurRequest;
import org.rhapsode.util.UserLogger;
import org.tallison.lucene.corpus.stats.IDFIndexCalc;
import org.tallison.lucene.corpus.stats.TermIDF;
import org.tallison.lucene.search.concordance.charoffsets.TargetTokenNotFoundException;
import org.tallison.lucene.search.concordance.classic.impl.IndexIdDocIdBuilder;
import org.tallison.lucene.search.concordance.util.EmptyTokenBlackList;
import org.tallison.lucene.search.concordance.util.IDFThresholdTokenBlackList;
import org.tallison.lucene.search.concordance.windowvisitor.ConcordanceArrayWindowSearcher;
import org.tallison.lucene.search.concordance.windowvisitor.CooccurVisitor;
import org.tallison.lucene.search.concordance.windowvisitor.WGrammer;
import org.xml.sax.SAXException;

public class CooccurHandler extends AbstractSearchHandler {
    private static final String TOOL_NAME = "Concordance Co-Occurrence Counter";
    private final RhapsodeSearcherApp searcherApp;
    private final NumberFormat intFormatter = new DecimalFormat("###,###,###,###,###");
    private final NumberFormat doubleFormatter = new DecimalFormat("###,###,###,###,##0.0");

    public CooccurHandler(RhapsodeSearcherApp searcherApp) {
        super(TOOL_NAME);
        this.searcherApp = searcherApp;
    }


    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest,
                       HttpServletResponse response) throws IOException, ServletException {
        init(request, response);
        RhapsodeXHTMLHandler xhtml = null;
        try {
            xhtml = initResponse(response, null);
        } catch (SAXException e) {
            throw new IOException(e);
        }

        if (!searcherApp.hasCollection()) {
            try {
                RhapsodeDecorator.writeNoCollection(xhtml);
                response.getOutputStream().flush();
            } catch (SAXException e) {
                e.printStackTrace();
            }
            return;
        }
        try {
            executeSearch(httpServletRequest, xhtml);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void executeSearch(HttpServletRequest httpServletRequest,
                               RhapsodeXHTMLHandler xhtml) throws IOException {

        CooccurRequest cooccurRequest = new CooccurRequest(
                searcherApp.getCooccurSearchConfig());
        String errorMsg = null;

        CooccurRequestBuilder requestBuilder = new CooccurRequestBuilder();
        try {
            requestBuilder.extract(searcherApp, httpServletRequest, cooccurRequest);
        } catch (ParseException e) {
            errorMsg = "Parse Exception: " + e.getMessage();
            e.printStackTrace();
            UserLogger.logException(TOOL_NAME, errorMsg, httpServletRequest);

        } catch (NullPointerException e) {
            errorMsg = "Parse Exception: didn't recognize field";
            e.printStackTrace();
            UserLogger.logException(TOOL_NAME, errorMsg, httpServletRequest);
        }
        float idfThreshold = cooccurRequest.getMinIDF();

        IndexReader reader = searcherApp.getRhapsodeCollection().
                getIndexManager().getSearcher().getIndexReader();


        IDFIndexCalc idfCalc = new IDFIndexCalc(reader);
        WGrammer wGrammer = (idfThreshold <= 0) ?
                new WGrammer(cooccurRequest.getMinXGram(), cooccurRequest.getMaxXGram(),
                        cooccurRequest.getContentField(),
                        new EmptyTokenBlackList(),
                        false) :
                new WGrammer(cooccurRequest.getMinXGram(), cooccurRequest.getMaxXGram(),
                        cooccurRequest.getContentField(),
                        new IDFThresholdTokenBlackList(idfCalc, idfThreshold),
                        false);
        CooccurVisitor visitor = new CooccurVisitor(
                cooccurRequest.getContentField(),
                cooccurRequest.getTokensBefore(), cooccurRequest.getTokensAfter(), wGrammer,
                idfCalc,
                cooccurRequest.getMaxStoredWindows(),
                !cooccurRequest.getIgnoreDuplicateWindows());
        visitor.setMinTermFreq(cooccurRequest.getMinTermFreq());
        visitor.setNumResults(cooccurRequest.getMaxNumResults());


        if (errorMsg == null &&
                Math.max(cooccurRequest.getMinXGram(), cooccurRequest.getMaxXGram()) >
                        Math.max(cooccurRequest.getTokensBefore(), cooccurRequest.getTokensAfter())) {
            errorMsg = "Your context size is smaller than your maximum ngram size.";
            UserLogger.logException(TOOL_NAME, errorMsg, httpServletRequest);
        }
        ConcordanceArrayWindowSearcher searcher = new ConcordanceArrayWindowSearcher();
        if (errorMsg == null && cooccurRequest.hasQuery()) {
            long startTime = System.currentTimeMillis();
            try {
                searcher.search(searcherApp.getRhapsodeCollection().getIndexManager().getSearcher(),
                        cooccurRequest.getContentField(),
                        cooccurRequest.getComplexQuery().getHighlightingQuery(),
                        cooccurRequest.getComplexQuery().getRetrievalQuery(),
                        searcherApp.getRhapsodeCollection().getIndexSchema().getOffsetAnalyzer(),
                        visitor,
                        new IndexIdDocIdBuilder());
                UserLogger.log(TOOL_NAME, cooccurRequest.getComplexQuery(), -1, (System.currentTimeMillis() - startTime));

            } catch (TargetTokenNotFoundException e) {
                e.printStackTrace();
                errorMsg = e.getMessage();
                UserLogger.logException(TOOL_NAME, errorMsg, httpServletRequest);
            }
        }
        List<TermIDF> results = ((CooccurVisitor) visitor).getResults();
        try {
            xhtml.startElement(H.FORM, H.METHOD, H.POST);

            addQueryWindow(searcherApp, cooccurRequest, xhtml);
            xhtml.br();
            CCDecorator.addWordsBeforeAfter(cooccurRequest, xhtml);
            xhtml.br();
            addMaxResults(cooccurRequest, xhtml);
            xhtml.br();
            addMinTermFreq(cooccurRequest, xhtml);
            xhtml.br();
            addMinMaxXGram(cooccurRequest, xhtml);
            xhtml.br();
            addMinIDF(cooccurRequest, xhtml);
            xhtml.br();
            CCDecorator.addMaxWindows(cooccurRequest.getMaxStoredWindows(), xhtml);
            xhtml.br();
            CCDecorator.includeDuplicateWindows(cooccurRequest, xhtml);
            RhapsodeDecorator.writeLanguageDirection(searcherApp.getSessionManager()
                            .getDynamicParameterConfig()
                            .getBoolean(
                                    DynamicParameters.SHOW_LANGUAGE_DIRECTION),
                    cooccurRequest.getLanguageDirection(), xhtml);
            xhtml.br();
            addHiddenInputAndButtons(xhtml);

            if (errorMsg == null && cooccurRequest.hasQuery() && results.size() > 0) {
                CCDecorator.writeHitMax(visitor.getHitMax(), visitor.getNumWindowsVisited(), xhtml);

                writeResults(results, xhtml);
            } else if (errorMsg != null) {
                RhapsodeDecorator.writeErrorMessage(errorMsg, xhtml);
            }
            xhtml.endElement(H.FORM);
            RhapsodeDecorator.addFooter(xhtml);
            xhtml.endDocument();
        } catch (SAXException e) {
            throw new IOException(e);
        }
    }

    private void addMinIDF(CooccurRequest cooccurRequest, RhapsodeXHTMLHandler xhtml) throws SAXException {
        xhtml.characters("Minimum IDF: ");
        String minIDF =
                (cooccurRequest.getMinIDF() < 0) ? " " :
                        Float.toString(cooccurRequest.getMinIDF());
        xhtml.startElement(H.INPUT,
                H.TYPE, H.TEXT,
                H.NAME, C.MIN_IDF,
                H.VALUE, minIDF,
                H.SIZE, "2");
        xhtml.endElement(H.INPUT);
    }

    private void addMinMaxXGram(CooccurRequest cooccurRequest, RhapsodeXHTMLHandler xhtml) throws SAXException {
        xhtml.characters("Minimum Phrase Length: ");
        xhtml.startElement(H.INPUT,
                H.TYPE, H.TEXT,
                H.NAME, C.MIN_NGRAM,
                H.VALUE, Integer.toString(cooccurRequest.getMinXGram()),
                H.SIZE, "2");
        xhtml.endElement(H.INPUT);

        xhtml.characters("Maximum Phrase Length: ");
        xhtml.startElement(H.INPUT,
                H.TYPE, H.TEXT,
                H.NAME, C.MAX_NGRAM,
                H.VALUE, Integer.toString(cooccurRequest.getMaxXGram()),
                H.SIZE, "2");
        xhtml.endElement(H.INPUT);
    }

    private void addMinTermFreq(CooccurRequest cooccurRequest, RhapsodeXHTMLHandler xhtml) throws SAXException {
        xhtml.characters("Minimum Term Frequency: ");
        xhtml.startElement(H.INPUT,
                H.TYPE, H.TEXT,
                H.NAME, C.MIN_TERM_FREQ,
                H.VALUE, Integer.toString(cooccurRequest.getMinTermFreq()),
                H.SIZE, "2");
        xhtml.endElement(H.INPUT);

    }

    private void addMaxResults(CooccurRequest cooccurRequest, RhapsodeXHTMLHandler xhtml) throws SAXException {
        xhtml.characters("Number of Results: ");
        xhtml.startElement(H.INPUT,
                H.TYPE, H.TEXT,
                H.NAME, C.NUM_RESULTS,
                H.VALUE, Integer.toString(cooccurRequest.getMaxNumResults()),
                H.SIZE, "2");
        xhtml.endElement(H.INPUT);
    }

    private void writeResults(List<TermIDF> results, RhapsodeXHTMLHandler xhtml) throws SAXException {
        if (results.size() == 0)
            return;

        xhtml.startElement(H.TABLE,
                H.CLASS, CSS.COOC_TABLE);
        xhtml.startElement(H.TR);
        xhtml.element(H.TH, "Term");
        xhtml.element(H.TH, "Term Freq");
        xhtml.element(H.TH, "IDF");
        xhtml.element(H.TH, "TFIDF");
        xhtml.endElement(H.TR);
        for (TermIDF r : results) {
            //TODO: add css at the cell/row level???
            xhtml.startElement(H.TR);

            xhtml.element(H.TD, r.getTerm());

            xhtml.element(H.TD,
                    intFormatter.format(r.getTermFreq()));
            xhtml.element(H.TD,
                    doubleFormatter.format(r.getIDF()));
            xhtml.element(H.TD,
                    doubleFormatter.format(r.getTFIDF()));

            xhtml.endElement(H.TR);
        }
        xhtml.endElement(H.TABLE);
    }

/*    private void writeHitMax(PrintWriter writer, IntermediateCooccurResults results){
        if (results != null && results.isHitMax()){
            writer.write("<font color=\"red\"><b>The search hit the maximum number of results. "+
                    "The results only include the first "+results.getTotalWindows() + " windows.</b></font><br/>");
        }
    }
*/
}

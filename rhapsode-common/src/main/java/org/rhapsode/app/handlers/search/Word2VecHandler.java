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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.eclipse.jetty.server.Request;
import org.rhapsode.app.RhapsodeSearcherApp;
import org.rhapsode.app.contants.C;
import org.rhapsode.app.contants.CSS;
import org.rhapsode.app.contants.H;
import org.rhapsode.app.decorators.RhapsodeDecorator;
import org.rhapsode.app.decorators.RhapsodeXHTMLHandler;
import org.rhapsode.util.LanguageDirection;
import org.rhapsode.util.UserLogger;
import org.xml.sax.SAXException;


public class Word2VecHandler extends AbstractSearchHandler {
    private static final String TOOL_NAME = "Word2Vec Handler";
    private static final String COMMA_SPACE = ", ";

    private final NumberFormat intFormatter = new DecimalFormat("###,###,###,###,###");
    private final NumberFormat doubleFormatter = new DecimalFormat("###,###,###,###,##0.00");

    private final RhapsodeSearcherApp app;

    public Word2VecHandler(RhapsodeSearcherApp app) {
        super(TOOL_NAME);
        this.app = app;
    }

    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest,
                       HttpServletResponse response) throws IOException, ServletException {
        RhapsodeXHTMLHandler xhtml = null;
        try {
            xhtml = initResponse(response, null);
        } catch (SAXException e) {
            throw new IOException(e);
        }
        if (!app.hasCollection()) {
            try {
                RhapsodeDecorator.writeNoCollection(xhtml);
                response.getOutputStream().flush();
            } catch (SAXException e) {
                e.printStackTrace();
            }
            return;
        }

        String errorMsg = null;
        Word2VecRequest w2vRequest = null;

        try {
            w2vRequest = Word2VecRequestBuilder.build(app, httpServletRequest);
        } catch (ParseException e) {
            e.printStackTrace();
            errorMsg = "Parse Exception: " + e.getMessage();
            UserLogger.logException(TOOL_NAME, errorMsg, httpServletRequest);
        } catch (Exception e) {
            e.printStackTrace();
            errorMsg = "unknown: " + e.getMessage();
            UserLogger.logException(TOOL_NAME, errorMsg, httpServletRequest);
        }

        try {
            xhtml.startElement(H.FORM, H.METHOD, H.POST);

            addW2VQueryParameters(w2vRequest, app, xhtml);
            addHiddenInputAndButtons(xhtml);
            Word2VecResults results = null;
            if (errorMsg == null && w2vRequest.hasQuery() && app.getRhapsodeCollection().hasWord2Vec()) {
                try {
                    long start = System.currentTimeMillis();
                    results = searchWord2Vec(w2vRequest, app);
                    //TODO turn this on
///                    UserLogger.log(TOOL_NAME, w2vRequest.getComplexQuery(), -1, System.currentTimeMillis()-start);

                } catch (Exception e) {
                    e.printStackTrace();
                    errorMsg = e.getMessage();
                    UserLogger.logException(TOOL_NAME, errorMsg, httpServletRequest);
                }
            }

            if (!app.getRhapsodeCollection().hasWord2Vec()) {
                errorMsg = "Couldn't find w2v model here: " + app.getRhapsodeCollection()
                        .getCollectionPath().resolve(app.getRhapsodeCollection().WORD_2_VEC_FILE);
            }

            if (errorMsg == null) {
                if (w2vRequest.hasQuery()) {
                    if (results != null && results.results.size() > 0) {
                        writeResults(w2vRequest, results, xhtml);
                    }
                }
            } else {
                xhtml.br();
                RhapsodeDecorator.writeErrorMessage(errorMsg, xhtml);
            }
            xhtml.endElement(H.FORM);
            RhapsodeDecorator.addFooter(xhtml);
            xhtml.endDocument();
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e);
        }
        response.getOutputStream().flush();
    }

    private Word2VecResults searchWord2Vec(Word2VecRequest searchRequest, RhapsodeSearcherApp searcherApp) throws IOException {
        org.deeplearning4j.models.word2vec.Word2Vec vec = app.getRhapsodeCollection().getWord2Vec();
        IndexReader reader = searcherApp.getRhapsodeCollection().getIndexManager().getSearcher().getIndexReader();

        Word2VecResults results = new Word2VecResults();
        if (searchRequest.isComplex()) {

            Collection<String> terms = vec.wordsNearest(searchRequest.getPositives(),
                    searchRequest.getNegatives(), searchRequest.getNumResults());
            for (String t : terms) {
                Term term = new Term(searcherApp.getRhapsodeCollection().getIndexSchema().getDefaultContentField(), t);
                results.add(new Word2VecResult(t, null, reader.docFreq(term), reader.totalTermFreq(term)));
            }
        } else {
            String target = searchRequest.getPositives().get(0);
            Collection<String> terms = vec.wordsNearest(searchRequest.getPositives().get(0), searchRequest.getNumResults());
            for (String t : terms) {
                Term term = new Term(searcherApp.getRhapsodeCollection().getIndexSchema().getDefaultContentField(), t);
                results.add(new Word2VecResult(t, vec.similarity(target, t), reader.docFreq(term), reader.totalTermFreq(term)));
            }
        }
        return results;
    }

    private void addW2VQueryParameters(Word2VecRequest searchRequest, RhapsodeSearcherApp searcherApp, RhapsodeXHTMLHandler xhtml) throws SAXException {
        String mainQueryString = searchRequest.getPositivesString();
        String negativeQueryString = searchRequest.getNegativesString();
        RhapsodeDecorator.writeQueryBox("Terms", C.W2V_POSITIVE, mainQueryString,
                getMainQueryBoxHeight(searcherApp), getMainQueryBoxWidth(searcherApp), LanguageDirection.LTR, xhtml);
        xhtml.br();
        RhapsodeDecorator.writeQueryBox("Negative Terms", C.W2V_NEGATIVE, negativeQueryString,
                getMainQueryBoxHeight(searcherApp), getMainQueryBoxWidth(searcherApp), LanguageDirection.LTR, xhtml);

        xhtml.br();
        xhtml.br();

        xhtml.characters("Number of Results:");
        xhtml.startElement(H.INPUT,
                H.TYPE, H.TEXT,
                H.NAME, C.NUM_RESULTS,
                H.VALUE, Integer.toString(searchRequest.getNumResults()),
                H.SIZE, "2");
        xhtml.endElement(H.INPUT);
        xhtml.br();
    }

    private void writeResults(Word2VecRequest request, Word2VecResults results,
                              RhapsodeXHTMLHandler xhtml) throws SAXException {

        List<Word2VecResult> list = results.results;
        if (list.size() == 0)
            return;
        xhtml.br();
        xhtml.startElement(H.TABLE,
                H.BORDER, "2",
                H.CLASS, CSS.VARIANT_RESULTS);

        DecimalFormat formatter = new DecimalFormat("0.####");
        if (request.isComplex()) {
            xhtml.startElement(H.TR);
            xhtml.element(H.TH, "Term");
            xhtml.element(H.TH, "Document Frequency");
//            xhtml.element(H.TH, "IDF");
            xhtml.element(H.TH, "Term Frequency");

            xhtml.endElement(H.TR);

            for (Word2VecResult result : results.results) {
                String term = result.getTerm();
                xhtml.startElement(H.TR);
                xhtml.startElement(H.TD,
                        //TODO -- for now
                        RhapsodeDecorator.getLangDirAttrs(LanguageDirection.LTR));
                xhtml.characters(term);
                xhtml.endElement(H.TD);
                xhtml.element(H.TD, Long.toString(result.getDocFreq()));
                xhtml.element(H.TD, Long.toString(result.getTermFreq()));
                xhtml.endElement(H.TR);
            }
        } else {
            xhtml.startElement(H.TR);
            xhtml.element(H.TH, "Term");
            xhtml.element(H.TH, "Similarity");
            xhtml.element(H.TH, "Document Frequency");
//            xhtml.element(H.TH, "IDF");
            xhtml.element(H.TH, "Term Frequency");
            xhtml.endElement(H.TR);
            for (Word2VecResult result : results.results) {
                String term = result.getTerm();
                xhtml.startElement(H.TR);
                xhtml.startElement(H.TD,
                        RhapsodeDecorator.getLangDirAttrs(LanguageDirection.LTR));
                xhtml.characters(term);
                xhtml.element(H.TD, doubleFormatter.format(result.getSimilarity()));
                xhtml.element(H.TD, Long.toString(result.getDocFreq()));
                xhtml.element(H.TD, Long.toString(result.getTermFreq()));
                xhtml.endElement(H.TR);
            }
        }
        xhtml.endElement(H.TABLE);
        xhtml.br();
        xhtml.br();

    }

    private static class Word2VecResult {
        private final String term;
        private final Double similarity;
        private final long docFreq;
        private final long termFreq;

        public Word2VecResult(String term, Double similarity, long docFreq, long termFreq) {
            this.term = term;
            this.similarity = similarity;
            this.docFreq = docFreq;
            this.termFreq = termFreq;
        }

        public String getTerm() {
            return term;
        }

        public Double getSimilarity() {
            return similarity;
        }

        public long getDocFreq() {
            return docFreq;
        }

        public long getTermFreq() {
            return termFreq;
        }
    }

    private class Word2VecResults {
        List<Word2VecResult> results = new ArrayList<>();

        void add(Word2VecResult result) {
            results.add(result);
        }
    }
}

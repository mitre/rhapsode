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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.spans.SpanQuery;
import org.eclipse.jetty.server.Request;
import org.rhapsode.app.RhapsodeSearcherApp;
import org.rhapsode.app.contants.C;
import org.rhapsode.app.contants.CSS;
import org.rhapsode.app.contants.H;
import org.rhapsode.app.decorators.RhapsodeDecorator;
import org.rhapsode.app.decorators.RhapsodeXHTMLHandler;
import org.rhapsode.lucene.search.variant.VariantTermRequest;
import org.rhapsode.text.StringToCodePoints;
import org.rhapsode.text.UnicodeNormalizer;
import org.rhapsode.util.UserLogger;
import org.tallison.lucene.corpus.stats.IDFCalc;
import org.tallison.lucene.corpus.stats.TermDFTF;
import org.tallison.lucene.search.concordance.charoffsets.SimpleAnalyzerUtil;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;


public class VariantTermHandler extends AbstractSearchHandler {
    private static final String TOOL_NAME = "Single Term Variant Counter";
    private static final String COMMA_SPACE = ", ";

    private final NumberFormat intFormatter = new DecimalFormat("###,###,###,###,###");
    private final NumberFormat doubleFormatter = new DecimalFormat("###,###,###,###,##0.00");

    private final RhapsodeSearcherApp searcherConfig;

    public VariantTermHandler(RhapsodeSearcherApp searcherConfig) {
        super(TOOL_NAME);
        this.searcherConfig = searcherConfig;
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
        if (!searcherConfig.hasCollection()) {
            try {
                RhapsodeDecorator.writeNoCollection(xhtml);
                response.getOutputStream().flush();
            } catch (SAXException e) {
                e.printStackTrace();
            }
            return;
        }

        String errorMsg = null;
        VariantTermRequestBuilder requestBuilder = new VariantTermRequestBuilder();
        VariantTermRequest searchRequest = new VariantTermRequest();

        try {
            requestBuilder.extract(searcherConfig, httpServletRequest, searchRequest);
            requestBuilder.parse(searcherConfig, searchRequest, MultiTermQuery.SCORING_BOOLEAN_REWRITE);
        } catch (ParseException e) {
            e.printStackTrace();
            errorMsg = "Parse Exception: " + e.getMessage();
            UserLogger.logException(TOOL_NAME, errorMsg, httpServletRequest);
        } catch (NullPointerException e) {
            errorMsg = "Parse Exception: didn't recognize field";
            UserLogger.logException(TOOL_NAME, errorMsg, httpServletRequest);
        } catch (Exception e) {
            e.printStackTrace();
            errorMsg = "unknown: " + e.getMessage();
            UserLogger.logException(TOOL_NAME, errorMsg, httpServletRequest);
        }

        try {
            xhtml.startElement(H.FORM, H.METHOD, H.POST);

            addMainQueryWindow(searcherConfig, searchRequest, xhtml);
            addVariantQueryParameters(searchRequest, xhtml);
            addHiddenInputAndButtons(xhtml);
            VariantResults results = null;
            if (errorMsg == null && searchRequest.hasQuery()) {
                try {
                    long start = System.currentTimeMillis();
                    results = simpleTermSearch(searchRequest);
                    UserLogger.log(TOOL_NAME, searchRequest.getComplexQuery(), -1, System.currentTimeMillis() - start);

                } catch (Exception e) {
                    e.printStackTrace();
                    errorMsg = e.getMessage();
                    UserLogger.logException(TOOL_NAME, errorMsg, httpServletRequest);

                }
            }


            if (errorMsg == null) {
                if (searchRequest.hasQuery()) {
                    writeCodePointRequestTable(searchRequest, xhtml);
                    if (results != null && results.results.size() > 0) {
                        writeResults(searchRequest, results, xhtml);
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

    private void addVariantQueryParameters(VariantTermRequest searchRequest, RhapsodeXHTMLHandler xhtml) throws SAXException {
        xhtml.characters("Number of Results:");
        xhtml.startElement(H.INPUT,
                H.TYPE, H.TEXT,
                H.NAME, C.NUM_RESULTS,
                H.VALUE, Integer.toString(searchRequest.getNumResults()),
                H.SIZE, "2");
        xhtml.endElement(H.INPUT);
        xhtml.br();

        //if (searcherConfig.getVariantConfig().isShowAdvanced()) {
        xhtml.characters("Show Code Points: ");
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", H.TYPE, H.TYPE, "", H.CHECKBOX);
        attrs.addAttribute("", H.NAME, H.NAME, "", C.SHOW_CODE_POINTS);
        if (searchRequest.isShowCodePoints()) {
            attrs.addAttribute("", H.CHECKED, H.CHECKED, "", H.CHECKED);
        }
        xhtml.startElement(H.INPUT, attrs);
        xhtml.endElement(H.INPUT);
        xhtml.br();


    }

    private VariantResults simpleTermSearch(VariantTermRequest searchRequest)
            throws IOException, ParseException {
        IndexReader reader = searcherConfig.getRhapsodeCollection().getIndexManager().getSearcher().getIndexReader();
        Query q = searchRequest.getComplexQuery().getHighlightingQuery();
        if (q instanceof BooleanQuery || q instanceof SpanQuery) {
            throw new ParseException("The single term variant counter is not meant for phrasal or boolean queries.\n" +
                    "Try using the Target Counter tool.");
        }
//        Query q = new PrefixQuery(new Term("content", "f"));
        //      ((PrefixQuery)q).setRewriteMethod(MultiTermQuery.CONSTANT_SCORE_BOOLEAN_REWRITE);

        q = q.rewrite(reader);
        Collector collector = new TotalHitCountCollector();
        searcherConfig.getRhapsodeCollection().
                getIndexManager().getSearcher().search(q, collector);
        int totalHits = ((TotalHitCountCollector) collector).getTotalHits();
        Set<Term> terms = new HashSet<>();
        Weight weight = q.createWeight(searcherConfig.getRhapsodeCollection().
                getIndexManager().getSearcher(), ScoreMode.COMPLETE_NO_SCORES, 1.0f);

        weight.extractTerms(terms);
        List<TermDFTF> results = new ArrayList<>();
        long totalOccurrences = 0;
        for (Term t : terms) {
            long totalTermFreq = reader.totalTermFreq(t);
            results.add(new TermDFTF(t.text(), reader.docFreq(t), totalTermFreq));
            totalOccurrences += totalTermFreq;
        }
        Collections.sort(results);
        int uniqTerms = results.size();
        while (results.size() > searchRequest.getNumResults()) {
            results.remove(results.size() - 1);
        }

        return new VariantResults(results, uniqTerms, totalOccurrences, totalHits,
                reader.numDocs());
    }

    private void writeCodePointRequestTable(VariantTermRequest searchRequest, RhapsodeXHTMLHandler xhtml) throws SAXException {
        if (!searchRequest.isShowCodePoints()) {
            return;
        }
        //TODO: need to refactor to get rewrittenmainquerystring
        String literalQueryString = searchRequest.getComplexQuery().getStoredQuery().getMainQueryString();
        String normalized = UnicodeNormalizer.normalize(literalQueryString);
        xhtml.br();
        xhtml.startElement(H.TABLE,
                H.CLASS, CSS.VARIANT_QUERY_CODEPOINT_TABLE,
                H.BORDER, "2"
        );
        xhtml.startElement(H.TR);
        xhtml.startElement(H.TD);
        xhtml.endElement(H.TD);
        xhtml.element(H.TD, "String");
        xhtml.element(H.TD, "Unicode Code Points (Left to Right)");
        xhtml.endElement(H.TR);

        xhtml.startElement(H.TR);
        xhtml.element(H.TD, "Literal Query String");
        xhtml.element(H.TD, literalQueryString);
        xhtml.element(H.TD, StringToCodePoints.toHex(literalQueryString, ", "));
        xhtml.endElement(H.TR);

        xhtml.startElement(H.TR);
        xhtml.element(H.TD, "Normalized Query String");
        xhtml.element(H.TD, normalized);
        xhtml.element(H.TD, StringToCodePoints.toHex(normalized, ", "));
        xhtml.endElement(H.TR);
        xhtml.endElement(H.TABLE);
    }

    private void writeResults(VariantTermRequest request, VariantResults results,
                              RhapsodeXHTMLHandler xhtml) throws SAXException {

        List<TermDFTF> list = results.results;
        if (list.size() == 0)
            return;
        xhtml.br();
        xhtml.startElement(H.TABLE,
                H.BORDER, "2",
                H.CLASS, CSS.VARIANT_RESULTS);
        List<String> terms = new ArrayList<>();

        IDFCalc calc = new IDFCalc((int) results.totalDocs);
        DecimalFormat formatter = new DecimalFormat("0.#");
        if (request.isShowCodePoints()) {
            xhtml.startElement(H.TR);
            xhtml.element(H.TH, "Term");
            xhtml.element(H.TH, "Unicode Code Points (Left to Right)");
            xhtml.element(H.TH, "Document Frequency");
            xhtml.element(H.TH, "IDF");
            xhtml.element(H.TH, "Term Frequency");

            xhtml.endElement(H.TR);

            for (TermDFTF w : results.results) {
                String term = w.getTerm();
                terms.add(term);
                xhtml.startElement(H.TR);
                xhtml.startElement(H.TD,
                        RhapsodeDecorator.getLangDirAttrs(request));
                xhtml.characters(term);
                xhtml.endElement(H.TD);

                xhtml.element(H.TD, StringToCodePoints.toHex(term, COMMA_SPACE));
                xhtml.element(H.TD, intFormatter.format(w.getDocFreq()));
                xhtml.element(H.TD, doubleFormatter.format(calc.getIDF(w.getDocFreq())));

                xhtml.element(H.TD, intFormatter.format(w.getTermFreq()));

                xhtml.endElement(H.TR);
            }
        } else {
            xhtml.startElement(H.TR);
            xhtml.element(H.TH, "Term");
            xhtml.element(H.TH, "Document Frequency");
            xhtml.element(H.TH, "IDF");
            xhtml.element(H.TH, "Term Frequency");
            xhtml.endElement(H.TR);
            for (TermDFTF w : results.results) {
                String term = w.getTerm();
                terms.add(term);
                xhtml.startElement(H.TR);
                xhtml.startElement(H.TD,
                        RhapsodeDecorator.getLangDirAttrs(request));
                xhtml.characters(term);
                xhtml.element(H.TD, intFormatter.format(w.getDocFreq()));
                xhtml.element(H.TD, doubleFormatter.format(calc.getIDF(w.getDocFreq())));
                xhtml.element(H.TD, intFormatter.format(w.getTermFreq()));

                xhtml.endElement(H.TR);
            }
        }
        xhtml.endElement(H.TABLE);
        xhtml.br();
        xhtml.br();
        if (terms.size() > 0) {
            Analyzer analyzer = searcherConfig.getRhapsodeCollection().
                    getIndexSchema().getOffsetAnalyzer();
            List<String> quoted = new ArrayList<>();
            List<String> termlets = new ArrayList<>();
            for (String t : terms) {
                try {
                    SimpleAnalyzerUtil.getTermStrings(t, request.getContentField(), analyzer, termlets);
                    if (termlets.size() > 1) {
                        t = "\"" + t + "\"";
                    }
                    quoted.add(t);
                } catch (IOException e) {
                    //swallow for now
                }
            }
            xhtml.characters("(" + StringUtils.join(quoted, COMMA_SPACE) + ")");
        }

    }

    private class VariantResults {
        private final List<TermDFTF> results;
        private final int uniqueTerms;
        private final long totalOccurrences;
        private final long docsHits;
        private final long totalDocs;
        //        return new VariantResults(results, uniqTerms, totalOccurrences, totalHits,
//                reader.numDocs());

        private VariantResults(List<TermDFTF> results, int uniqueTerms,
                               long totalOccurrences, long docHits,
                               long totalDocs) {
            this.results = results;
            this.uniqueTerms = uniqueTerms;
            this.totalOccurrences = totalOccurrences;
            this.docsHits = docHits;
            this.totalDocs = totalDocs;
        }
    }
}

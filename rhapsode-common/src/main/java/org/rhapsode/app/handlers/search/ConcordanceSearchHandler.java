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

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.queryparser.classic.ParseException;
import org.carrot2.clustering.kmeans.BisectingKMeansClusteringAlgorithm;
import org.carrot2.clustering.lingo.LingoClusteringAlgorithm;
import org.carrot2.clustering.stc.STCClusteringAlgorithm;
import org.carrot2.core.Cluster;
import org.carrot2.core.Document;
import org.eclipse.jetty.server.Request;
import org.rhapsode.app.config.RhapsodeSearcherApp;
import org.rhapsode.app.contants.C;
import org.rhapsode.app.contants.CSS;
import org.rhapsode.app.contants.H;
import org.rhapsode.app.decorators.CCDecorator;
import org.rhapsode.app.decorators.IndexedDocURLBuilder;
import org.rhapsode.app.decorators.RhapsodeDecorator;
import org.rhapsode.app.decorators.RhapsodeXHTMLHandler;
import org.rhapsode.app.session.DynamicParameters;
import org.rhapsode.lucene.search.BaseSearchRequest;
import org.rhapsode.lucene.search.BaseSearchResult;
import org.rhapsode.lucene.search.concordance.ConcordanceSearchRequest;
import org.rhapsode.util.LanguageDirection;
import org.tallison.lucene.search.concordance.charoffsets.TargetTokenNotFoundException;
import org.tallison.lucene.search.concordance.classic.AbstractConcordanceWindowCollector;
import org.tallison.lucene.search.concordance.classic.ConcordanceSearcher;
import org.tallison.lucene.search.concordance.classic.ConcordanceSortOrder;
import org.tallison.lucene.search.concordance.classic.ConcordanceSorter;
import org.tallison.lucene.search.concordance.classic.ConcordanceWindow;
import org.tallison.lucene.search.concordance.classic.WindowBuilder;
import org.tallison.lucene.search.concordance.classic.impl.ConcordanceWindowCollector;
import org.tallison.lucene.search.concordance.classic.impl.DedupingConcordanceWindowCollector;
import org.tallison.lucene.search.concordance.classic.impl.DefaultSortKeyBuilder;
import org.tallison.lucene.search.concordance.classic.impl.FieldBasedDocIdBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class ConcordanceSearchHandler extends AbstractSearchHandler {

    private final RhapsodeSearcherApp searcherApp;
    private final ConcordanceSearchRequestBuilder requestBuilder;
    private final IndexedDocURLBuilder indexedDocURLBuilder;


    public ConcordanceSearchHandler(RhapsodeSearcherApp config) {
        super("Concordance");
        this.searcherApp = config;
        requestBuilder = new ConcordanceSearchRequestBuilder();
        indexedDocURLBuilder = new IndexedDocURLBuilder(searcherApp);

    }

    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest,
                       HttpServletResponse response) throws IOException, ServletException {
        try {
            _handle(s, request, httpServletRequest, response);
        } catch (Throwable t) {
            t.printStackTrace();
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
        }
    }


    private void _handle(String s, Request request, HttpServletRequest httpServletRequest,
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


        ConcordanceSearchRequest searchRequest = new ConcordanceSearchRequest(
                searcherApp.getConcordanceSearchConfig());
        String errorMsg = null;
        /*searchRequest.addFields(searcherApp.
                getDocMetadataExtractor().getFieldSelector());*/
        try {
            requestBuilder.extract(searcherApp, httpServletRequest, searchRequest);
        } catch (ParseException e) {
            errorMsg = "Parse Exception: " + e.getMessage();
            e.printStackTrace();
        } catch (NullPointerException e) {
            errorMsg = "Parse Exception: didn't recognize field";
            e.printStackTrace();
        }
        ConcordanceSearcher searcher = buildSearcher(searchRequest);
        AbstractConcordanceWindowCollector windowCollector = null;

        if (searchRequest.getIgnoreDuplicateWindows()) {
            windowCollector = new DedupingConcordanceWindowCollector(searchRequest.getMaxStoredWindows());
        } else {
            windowCollector = new ConcordanceWindowCollector(searchRequest.getMaxStoredWindows());
        }
        long started = new Date().getTime();
        long elapsed = -1L;
        if (errorMsg == null && searchRequest.hasQuery()) {
            try {
                searcher.search(searcherApp.getRhapsodeCollection().getIndexManager().getSearcher(),
                        searchRequest.getContentField(),
                        searchRequest.getComplexQuery().getHighlightingQuery(),
                        searchRequest.getComplexQuery().getRetrievalQuery(),
                        searcherApp.getRhapsodeCollection().getIndexSchema().getOffsetAnalyzer(),
                        windowCollector);
                elapsed = new Date().getTime() - started;
            } catch (TargetTokenNotFoundException e) {
                e.printStackTrace();
                errorMsg += "\n" + e.getMessage();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            xhtml.startElement(H.FORM, H.METHOD, H.POST);

            addQueryWindow(searcherApp, searchRequest, xhtml);
            xhtml.br();
            CCDecorator.addWordsBeforeAfter(searchRequest, xhtml);
            xhtml.br();
            addSortOrder(searchRequest, xhtml);
            xhtml.br();
            CCDecorator.addMaxWindows(searchRequest.getMaxStoredWindows(), xhtml);
            xhtml.br();
            CCDecorator.includeDuplicateWindows(searchRequest, xhtml);
            RhapsodeDecorator.writeLanguageDirection(searcherApp.getSessionManager()
                            .getDynamicParameterConfig()
                            .getBoolean(
                                    DynamicParameters.SHOW_LANGUAGE_DIRECTION),
                    searchRequest.getLanguageDirection(), xhtml);
            if (searcherApp.getSessionManager()
                    .getDynamicParameterConfig()
                    .getBoolean(
                            DynamicParameters.CONC_ALLOW_CLUSTERING)) {
                try {
                    xhtml.br();
                    writeClusteringConfig(searchRequest, xhtml);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            xhtml.br();
            addHiddenInputAndButtons(searchRequest, xhtml);

            if (errorMsg == null && searchRequest.hasQuery() && windowCollector.size() > 0) {
                RhapsodeDecorator.writeElapsed(elapsed, xhtml);
                CCDecorator.writeResultCounts(searchRequest, windowCollector, searcherApp.getRhapsodeCollection().getIgnoredSize(), xhtml);
                CCDecorator.writeHitMax(windowCollector.getHitMax(), windowCollector.getNumWindows(), xhtml);
                writeResults(searchRequest, windowCollector, xhtml);
            } else if (errorMsg != null) {
                RhapsodeDecorator.writeErrorMessage(errorMsg, xhtml);
            }
            xhtml.endElement(H.FORM);
            RhapsodeDecorator.addFooter(xhtml);
            xhtml.endDocument();
        } catch (SAXException e) {
            throw new IOException(e);
        }
        response.getOutputStream().flush();
    }


    private void addSortOrder(ConcordanceSearchRequest searchRequest,
                              RhapsodeXHTMLHandler xhtml) throws SAXException {
        Map<String, String> m = new LinkedHashMap<>();

        m.put(C.CONC_SORT_ORDER_DOC, "Document Name");
        m.put(C.CONC_SORT_ORDER_PRE, "Pre");
        m.put(C.CONC_SORT_ORDER_POST, "Post");
        m.put(C.CONC_SORT_ORDER_TARGET_PRE, "Target->Pre");
        m.put(C.CONC_SORT_ORDER_TARGET_POST, "Target->Post");
        xhtml.characters("Sort Order: ");
        xhtml.startElement(H.SELECT,
                H.NAME, C.CONC_SORT_ORDER_SELECTOR);
        for (Map.Entry<String, String> entry : m.entrySet()) {
            AttributesImpl attrs = new AttributesImpl();
            attrs.addAttribute("", H.VALUE, H.VALUE, "", entry.getKey());
            if (entry.getKey().equals(C.CONC_SORT_ORDER_PRE) &&
                    searchRequest.getSortOrder() == ConcordanceSortOrder.PRE) {
                attrs.addAttribute("", H.SELECTED, H.SELECTED, "", H.SELECTED);
            } else if (entry.getKey().equals(C.CONC_SORT_ORDER_POST) &&
                    searchRequest.getSortOrder() == ConcordanceSortOrder.POST) {
                attrs.addAttribute("", H.SELECTED, H.SELECTED, "", H.SELECTED);
            } else if (entry.getKey().equals(C.CONC_SORT_ORDER_TARGET_PRE) &&
                    searchRequest.getSortOrder() == ConcordanceSortOrder.TARGET_PRE) {
                attrs.addAttribute("", H.SELECTED, H.SELECTED, "", H.SELECTED);
            } else if (entry.getKey().equals(C.CONC_SORT_ORDER_TARGET_POST) &&
                    searchRequest.getSortOrder() == ConcordanceSortOrder.TARGET_POST) {
                attrs.addAttribute("", H.SELECTED, H.SELECTED, "", H.SELECTED);
            } else if (entry.getKey().equals(C.CONC_SORT_ORDER_DOC) &&
                    searchRequest.getSortOrder() == ConcordanceSortOrder.DOC) {
                attrs.addAttribute("", H.SELECTED, H.SELECTED, "", H.SELECTED);
            }
            xhtml.startElement(H.OPTION, attrs);
            xhtml.characters(entry.getValue());
            xhtml.endElement(H.OPTION);
        }
        xhtml.endElement(H.SELECT);
    }


    private ConcordanceSearcher buildSearcher(ConcordanceSearchRequest searchRequest) {
        WindowBuilder windowBuilder = new WindowBuilder(
                searchRequest.getTokensBefore(),
                searchRequest.getTokensAfter(),
                100,//offset gap?
                new DefaultSortKeyBuilder(searchRequest.getSortOrder()),
                searchRequest.getDocMetadataExtractor(),
                new FieldBasedDocIdBuilder(searcherApp.getRhapsodeCollection().getIndexSchema().getUniqueDocField())
        );
        return new ConcordanceSearcher(windowBuilder);
    }


    private void writeResults(ConcordanceSearchRequest request,
                              AbstractConcordanceWindowCollector collector,
                              RhapsodeXHTMLHandler xhtml) throws SAXException {
        //write result summary
        boolean isDeduping = collector instanceof DedupingConcordanceWindowCollector;
        //if ! clustering
        if (request.getClusteringAlgo() == null || collector.getWindows().size() < 2) {
            _writeBasicResults(request, collector, isDeduping, xhtml);
        } else {
            List<ConcordanceWindow> windows = collector.getWindows();
            List<Cluster> clusters = null;
            try {
                clusters = ConcordanceClusterer.cluster(
                        request.getClusteringAlgo(), request.getClusteringAttrs(),
                        collector.getWindows());
            } catch (Exception e) {
                e.printStackTrace();
                RhapsodeDecorator.writeErrorMessage("There was an error during clustering: " + e.getMessage(), xhtml);
                RhapsodeDecorator.writeErrorMessage("Unclustered results: ", xhtml);
                _writeBasicResults(request, collector, isDeduping, xhtml);
                return;
            }

            xhtml.element("p", "There are " + clusters.size() + " clusters.");
            ConcordanceSorter sorter = new ConcordanceSorter();
            for (Cluster cluster : clusters) {
                xhtml.element("p", "LABEL: " + cluster.getLabel() +
                        " (" + cluster.getDocuments().size() + " windows)");

                xhtml.startElement(H.TABLE,
                        H.BORDER, "2");

                List<ConcordanceWindow> clusterWindows = new ArrayList<>();
                for (Document carrotDoc : cluster.getDocuments()) {
                    String idString = carrotDoc.getStringId();
                    Integer id = Integer.parseInt(idString);
                    clusterWindows.add(windows.get(id));
                }
                Collections.sort(clusterWindows, sorter);
                for (ConcordanceWindow window : clusterWindows) {
                    writeConcordanceRow(request, window, isDeduping, xhtml);
                }
                xhtml.endElement(H.TABLE);
            }
        }
    }

    private void _writeBasicResults(ConcordanceSearchRequest request,
                                    AbstractConcordanceWindowCollector collector,
                                    boolean isDeduping, RhapsodeXHTMLHandler xhtml) throws SAXException {
        xhtml.startElement(H.TABLE,
                H.BORDER, "2");
        for (ConcordanceWindow w : collector.getSortedWindows()) {
            writeConcordanceRow(request, w, isDeduping, xhtml);
        }
        xhtml.endElement(H.TABLE);

    }

    private void writeConcordanceRow(ConcordanceSearchRequest request,
                                     ConcordanceWindow w, boolean isDeduping,
                                     RhapsodeXHTMLHandler xhtml) throws SAXException {
        xhtml.startElement(H.TR);
        //TODO: clean up this abomination
        BaseSearchResult r = new BaseSearchResult();
        r.setLuceneDocId(-1);
        r.setMetadata(w.getMetadata());
        addDocLink(request, r, xhtml);
        if (isDeduping) {
            xhtml.element(H.TD, Integer.toString(w.getCount()));
        }
        //now add extra rows
        for (String rowResultColumn : searcherApp
                .getSessionManager()
                .getDynamicParameterConfig()
                .getStringList(DynamicParameters.ROW_VIEWER_DISPLAY_FIELDS)) {
            String val = r.getMetadata().get(rowResultColumn);
            if (StringUtils.isBlank(val)) {
                val = " ";
            }
            int maxColLen = searcherApp
                    .getSessionManager()
                    .getDynamicParameterConfig()
                    .getInt(DynamicParameters.MAX_EXTRA_COLUMNS_LEN);
            if (val.length() > maxColLen) {
                val = val.substring(0, maxColLen);
            }
            xhtml.element(H.TD, val);
        }

        //TODO: addDate(r.getMetadata(), xhtml);
        if (request.getLanguageDirection().equals(LanguageDirection.LTR)) {
            xhtml.startElement(H.TD,
                    H.CLASS, CSS.CONC_LEFT);
            xhtml.characters(w.getPre());
            xhtml.endElement(H.TD);
            xhtml.startElement(H.TD,
                    H.CLASS, CSS.CONC_TARGET);
            xhtml.characters(w.getTarget());
            xhtml.endElement(H.TD);
            xhtml.startElement(H.TD,
                    H.CLASS, CSS.CONC_RIGHT);
            xhtml.characters(w.getPost());
            xhtml.endElement(H.TD);
        } else {
            xhtml.startElement(H.TD,
                    H.CLASS, CSS.CONC_LEFT);
            xhtml.characters(w.getPost());
            xhtml.endElement(H.TD);
            xhtml.startElement(H.TD,
                    H.CLASS, CSS.CONC_TARGET);
            xhtml.characters(w.getTarget());
            xhtml.endElement(H.TD);
            xhtml.startElement(H.TD,
                    H.CLASS, CSS.CONC_RIGHT);
            xhtml.characters(w.getPre());
            xhtml.endElement(H.TD);
        }
        //xhtml.element(H.TD, r.getSnippet());
        xhtml.endElement(H.TR);
    }

    private void writeClusteringConfig(ConcordanceSearchRequest searchRequest,
                                       RhapsodeXHTMLHandler xhtml) throws SAXException {
        Map<String, String> m = new LinkedHashMap<>();
        m.put(C.NO_CLUSTERING, "");
        m.put(C.KMEANS, "K-Means");
        m.put(C.LINGO, "Lingo");
        m.put(C.STC, "STC");
        xhtml.characters("Clustering Algorithm: ");
        xhtml.startElement(H.SELECT,
                H.NAME, C.CLUSTER_ALGO_SELECTOR);
        for (Map.Entry<String, String> entry : m.entrySet()) {
            AttributesImpl attrs = new AttributesImpl();
            attrs.addAttribute("", H.VALUE, H.VALUE, "", entry.getKey());
            if (entry.getKey().equals(C.NO_CLUSTERING) &&
                    searchRequest.getClusteringAlgo() == null) {
                attrs.addAttribute("", H.SELECTED, H.SELECTED, "", H.SELECTED);
            } else if (entry.getKey().equals(C.KMEANS) &&
                    BisectingKMeansClusteringAlgorithm.class.equals(
                            searchRequest.getClusteringAlgo())) {
                attrs.addAttribute("", H.SELECTED, H.SELECTED, "", H.SELECTED);
            } else if (entry.getKey().equals(C.LINGO) &&
                    LingoClusteringAlgorithm.class.equals(
                            searchRequest.getClusteringAlgo())) {
                attrs.addAttribute("", H.SELECTED, H.SELECTED, "", H.SELECTED);
            } else if (entry.getKey().equals(C.STC) &&
                    STCClusteringAlgorithm.class.equals(
                            searchRequest.getClusteringAlgo())) {
                attrs.addAttribute("", H.SELECTED, H.SELECTED, "", H.SELECTED);
            }
            xhtml.startElement(H.OPTION, attrs);
            xhtml.characters(entry.getValue());
            xhtml.endElement(H.OPTION);
        }
        xhtml.endElement(H.SELECT);
        xhtml.characters("Number of Clusters (K-Means Only): ");
        Map<String, Object> clusteringAttrs = searchRequest.getClusteringAttrs();
        Integer numClusters = null;
        if (clusteringAttrs != null) {
            Object numClustersObj = clusteringAttrs.get(ConcordanceClusterer.CLUSTER_COUNT);
            if (numClustersObj != null) {
                numClusters = (Integer) numClustersObj;
            }
        }
        if (numClusters == null) {
            xhtml.startElement(H.INPUT,
                    H.TYPE, H.TEXT,
                    H.NAME, C.CLUSTERING_NUM_CLUSTERS,
                    H.SIZE, "2");
            xhtml.endElement(H.INPUT);
        } else {
            xhtml.startElement(H.INPUT,
                    H.TYPE, H.TEXT,
                    H.NAME, C.CLUSTERING_NUM_CLUSTERS,
                    H.SIZE, "2",
                    H.VALUE, Integer.toString(numClusters));
            xhtml.endElement(H.INPUT);
        }

    }


    private void addDocLink(BaseSearchRequest request,
                            BaseSearchResult result, RhapsodeXHTMLHandler xhtml) throws SAXException {
        String displayName = result.getMetadata().get(searcherApp.getRhapsodeCollection().getIndexSchema().getLinkDisplayField());
        int maxLen = searcherApp.getSessionManager().getDynamicParameterConfig().getInt(DynamicParameters.MAX_FILE_NAME_DISPLAY_LENGTH);
        if (maxLen > -1 && displayName != null && displayName.length() > maxLen) {
            displayName = displayName.substring(0, maxLen);
        }

        String docKey = result.getMetadata().get(searcherApp.getRhapsodeCollection().getIndexSchema().getUniqueDocField());
        xhtml.startElement(H.TD);
        String url = indexedDocURLBuilder.getURL(
                docKey,
                Integer.toString(result.getLuceneDocId()),
                null,
                request);
        xhtml.href(url, displayName);
        xhtml.endElement(H.TD);
    }

    @Override
    int getMainQueryBoxWidth(RhapsodeSearcherApp searcherApp) {
        return searcherApp.getSessionManager().getDynamicParameterConfig().getInt(DynamicParameters.CONC_MAIN_QUERY_WIDTH);

    }

    @Override
    int getMainQueryBoxHeight(RhapsodeSearcherApp searcherApp) {
        return searcherApp.getSessionManager().getDynamicParameterConfig().getInt(DynamicParameters.CONC_MAIN_QUERY_HEIGHT);

    }

    @Override
    int getFilterQueryBoxWidth(RhapsodeSearcherApp searcherApp) {
        return searcherApp.getSessionManager().getDynamicParameterConfig().getInt(DynamicParameters.CONC_FILTER_QUERY_WIDTH);
    }

    @Override
    int getFilterQueryBoxHeight(RhapsodeSearcherApp searcherApp) {
        return searcherApp.getSessionManager().getDynamicParameterConfig().getInt(DynamicParameters.CONC_FILTER_QUERY_HEIGHT);
    }


}

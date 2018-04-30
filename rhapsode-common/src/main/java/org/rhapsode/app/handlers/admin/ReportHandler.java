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

package org.rhapsode.app.handlers.admin;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.classic.ParseException;
import org.eclipse.jetty.server.Request;
import org.rhapsode.app.RhapsodeSearcherApp;
import org.rhapsode.app.contants.C;
import org.rhapsode.app.contants.CSS;
import org.rhapsode.app.contants.H;
import org.rhapsode.app.contants.Internal;
import org.rhapsode.app.decorators.RhapsodeDecorator;
import org.rhapsode.app.decorators.RhapsodeXHTMLHandler;
import org.rhapsode.app.session.DBStoredQueryManager;
import org.rhapsode.app.tagger.Tagger;
import org.rhapsode.app.tagger.TaggerRequest;
import org.rhapsode.lucene.search.ComplexQuery;
import org.rhapsode.lucene.search.ComplexQueryBuilder;
import org.rhapsode.lucene.search.MaxResultsQuery;
import org.rhapsode.lucene.search.StoredQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


public class ReportHandler extends AdminHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ReportHandler.class);

    private final RhapsodeSearcherApp searcherApp;

    public ReportHandler(RhapsodeSearcherApp searcherConfig) {
        super("Report Writer");
        this.searcherApp = searcherConfig;
    }


    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest,
                       HttpServletResponse response) throws IOException, ServletException {
        RhapsodeXHTMLHandler xhtml = null;
        ReportRequest rr = null;
        try {
            rr = ReportRequest.build(httpServletRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            xhtml = initResponse(response, null);
            if (!searcherApp.hasCollection()) {
                try {
                    RhapsodeDecorator.writeNoCollection(xhtml);
                    response.getOutputStream().flush();
                } catch (SAXException e) {
                    LOG.error("problem writing no collection", e);
                }
                return;
            }
            xhtml.startElement(H.FORM, H.METHOD, H.POST);
            String errorMessage = null;
            switch (rr.getActionType()) {
                case SELECT_ALL:
                    selectAll(rr);
                    writeReportForm(rr, xhtml);
                    break;
                case DESELECT_ALL:
                    rr.deselectAll();
                    writeReportForm(rr, xhtml);
                    break;
                case REPORT_DIALOGUE:
                    writeReportForm(rr, xhtml);
                    break;
                case WRITE_REPORT:
                    try {
                        writeReport(rr, xhtml);
                    } catch (ReportException e) {
                        e.printStackTrace();
                        errorMessage = e.getMessage();
                    }
                    writeReportForm(rr, xhtml);
                    break;
            }
            if (errorMessage != null) {
                xhtml.br();
                RhapsodeDecorator.writeErrorMessage(errorMessage, xhtml);
                xhtml.br();
            }

            xhtml.endElement(H.FORM);
            RhapsodeDecorator.addFooter(xhtml);
            xhtml.endDocument();

        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e);
        }

    }

    private void selectAll(ReportRequest rr) {
        for (Integer id : searcherApp.getSessionManager().getStoredQueryManager().getStoredQueryMap().keySet()) {
            rr.selectQuery(id);
        }
    }

    private void writeReport(ReportRequest rr, RhapsodeXHTMLHandler xhtml) throws ReportException, SAXException {
        if (StringUtils.isBlank(rr.getReportName())) {
            throw new ReportException("Must specify report name");
        }
        if (!rr.getIncludeFavorites() && (rr.getStoredQueryIds() == null || rr.getStoredQueryIds().size() == 0)) {
            throw new ReportException("Must select at least one query");
        }
        Path outputFile = searcherApp.getReportsDirectory().resolve(rr.getReportName() + ".xlsx");
        if (outputFile == null) {
            throw new ReportException("Must specify report name");
        }
        String warnMsg = null;
        Path outputDir = null;
        if (rr.getReportType().equals(ReportRequest.REPORT_TYPE.STATIC_LINKS)) {
            outputDir = searcherApp.getReportsDirectory().resolve(rr.getReportName() + "_docs");
            if (Files.exists(outputDir)) {
                warnMsg = "The output directory for the colorized files already exists.\n";
                warnMsg += "This report would overwrite the contents of that directory, and it would contain\n";
                warnMsg += "extra documents from the last time you ran a report.\n";
                warnMsg += "Please delete the report directory before writing a new report.\n";
                throw new ReportException(warnMsg);
            }
        }
        Map<Integer, MaxResultsQuery> queries = null;
        try {
            queries = buildQueries(rr.getStoredQueryIds(), rr.getIncludeFavorites(), rr.getAbsMaxHitsPerQuery());
        } catch (IOException e) {
            throw new ReportException(e);
        }
        if (queries.size() == 0) {
            throw new ReportException("Couldn't build a valid query?!");
        }

        TaggerRequest taggerRequest = null;
        try {
            taggerRequest = new TaggerRequest(queries,
                    searcherApp.getRhapsodeCollection().getIndexManager().getSearcher(),
                    searcherApp.getRhapsodeCollection().getIndexSchema().getRelPathField(),
                    searcherApp.getReportsDirectory(),
                    rr.getReportName(),
                    rr.getReportType(),
                    rr.getTopNCombinedReportResults(),
                    rr.getNormType());
        } catch (IOException e) {
            throw new ReportException(e);
        }
        Tagger t = new Tagger(taggerRequest, searcherApp);
        try {
            t.execute();
        } catch (Exception e) {
            throw new ReportException(e);
        }
        xhtml.br();
        xhtml.startElement(H.P);
        xhtml.characters("Successfully wrote report to: ");
        xhtml.href("/rhapsode/reports/" +
                searcherApp.getReportsDirectory().relativize(outputFile).toString(), outputFile.getFileName().toString());
        xhtml.endElement(H.P);
        xhtml.br();
    }

    private Map<Integer, MaxResultsQuery> buildQueries(Set<Integer> storedQueryIds, boolean includeFavorites,
                                                       int hardLimitMaxHits) throws IOException {
        DBStoredQueryManager sqm = searcherApp.getSessionManager().getStoredQueryManager();
        Map<Integer, StoredQuery> sqMap = sqm.getStoredQueryMap();
        Map<Integer, MaxResultsQuery> ret = new LinkedHashMap<>();

        int maxPriority = StoredQuery.DEFAULT_PRIORITY;
        for (StoredQuery sq : sqMap.values()) {
            if (sq.getPriority() > maxPriority) {
                maxPriority = sq.getPriority();
            }
        }

        if (includeFavorites) {
            ret.put(Internal.MANUALLY_SELECTED_FAVORITES_QUERY_NAME,
                    new MaxResultsQuery(Internal.FAVORITES_QUERY_NAME,
                            searcherApp.getRhapsodeCollection().getFavoritesQuery(), -1, 0));
        }

        for (Integer queryId : storedQueryIds) {
            StoredQuery sq = sqMap.get(queryId);
            if (sq == null) {
                LOG.warn("Couldn't find query for id: " + queryId);
                continue;
            }
            ComplexQuery cq = null;

            try {
                cq = ComplexQueryBuilder.buildQuery(
                        sq,
                        searcherApp.getParserPlugin(),
                        searcherApp.getSessionManager().getStoredConceptManager(),
                        searcherApp.getGeoConfig(),
                        searcherApp.getRhapsodeCollection().getIgnoredQuery()
                );
            } catch (ParseException e) {
                searcherApp.getSessionManager().getStoredQueryManager().updateQueryExceptions(sq, searcherApp);
                continue;
            }
            if (cq.getRetrievalQuery() == null) {
                continue;
            }
            int max = sq.getMaxHits();
            if (hardLimitMaxHits > StoredQuery.RETRIEVE_ALL_HITS
                    && (max == StoredQuery.RETRIEVE_ALL_HITS || max > hardLimitMaxHits)) {
                max = hardLimitMaxHits;
            }
            int priority = (sq.getPriority() == StoredQuery.DEFAULT_PRIORITY) ? maxPriority + 1 : sq.getPriority();
            ret.put(queryId, new MaxResultsQuery(sq.getQueryName(), cq.getRetrievalQuery(), max, priority));
        }
        return ret;
    }

    private void writeReportForm(ReportRequest rr, RhapsodeXHTMLHandler xhtml) throws SAXException {

        AttributesImpl fAttrs = new AttributesImpl();
        fAttrs.addAttribute("", H.TYPE, H.TYPE, "", H.TEXT);
        fAttrs.addAttribute("", H.NAME, H.NAME, "", C.REPORT_NAME);
        fAttrs.addAttribute("", H.SIZE, H.SIZE, "", Integer.toString(60));
        if (rr.getReportName() != null) {
            fAttrs.addAttribute("", H.VALUE, H.VALUE, "", rr.getReportName());
        }

        AttributesImpl topNAttrs = new AttributesImpl();
        topNAttrs.addAttribute("", H.TYPE, H.TYPE, "", H.TEXT);
        topNAttrs.addAttribute("", H.NAME, H.NAME, "", C.TOP_N_COMBINED_REPORT_RESULTS);
        topNAttrs.addAttribute("", H.SIZE, H.SIZE, "", Integer.toString(5));
        if (rr.getTopNCombinedReportResults() > -1) {
            topNAttrs.addAttribute("", H.VALUE, H.VALUE, "", Integer.toString(rr.getTopNCombinedReportResults()));
        }

        AttributesImpl amAttrs = new AttributesImpl();
        amAttrs.addAttribute("", H.TYPE, H.TYPE, "", H.TEXT);
        amAttrs.addAttribute("", H.NAME, H.NAME, "", C.ABS_MAX_REPORT_RESULTS);
        amAttrs.addAttribute("", H.SIZE, H.SIZE, "", Integer.toString(5));
        if (rr.getAbsMaxHitsPerQuery() > -1) {
            amAttrs.addAttribute("", H.VALUE, H.VALUE, "", Integer.toString(rr.getAbsMaxHitsPerQuery()));
        }


        xhtml.characters("Report Name: ");
        xhtml.startElement(H.INPUT, fAttrs);
        xhtml.endElement(H.INPUT);
        xhtml.br();
        xhtml.characters("Max Results Per Query: ");
        xhtml.startElement(H.INPUT, amAttrs);
        xhtml.endElement(H.INPUT);
        xhtml.br();
        xhtml.characters("Top N Combined Results: ");
        xhtml.startElement(H.INPUT, topNAttrs);
        xhtml.endElement(H.INPUT);


        xhtml.br();
        xhtml.characters("Live Links: ");
        if (rr.getReportType().equals(ReportRequest.REPORT_TYPE.LIVE_LINKS)) {
            xhtml.startElement(H.INPUT,
                    H.TYPE, H.RADIO,
                    H.NAME, C.REPORT_TYPE,
                    H.VALUE, C.REPORT_TYPE_LIVE,
                    H.CHECKED, H.CHECKED);
        } else {
            xhtml.startElement(H.INPUT,
                    H.TYPE, H.RADIO,
                    H.NAME, C.REPORT_TYPE,
                    H.VALUE, C.REPORT_TYPE_LIVE);

        }
        xhtml.characters(" Static Links: ");
        if (rr.getReportType().equals(ReportRequest.REPORT_TYPE.STATIC_LINKS)) {
            xhtml.startElement(H.INPUT,
                    H.TYPE, H.RADIO,
                    H.NAME, C.REPORT_TYPE,
                    H.VALUE, C.REPORT_TYPE_STATIC,
                    H.CHECKED, H.CHECKED);
        } else {
            xhtml.startElement(H.INPUT,
                    H.TYPE, H.RADIO,
                    H.NAME, C.REPORT_TYPE,
                    H.VALUE, C.REPORT_TYPE_STATIC);

        }
        xhtml.characters(" No Links: ");
        if (rr.getReportType().equals(ReportRequest.REPORT_TYPE.NO_LINKS)) {
            xhtml.startElement(H.INPUT,
                    H.TYPE, H.RADIO,
                    H.NAME, C.REPORT_TYPE,
                    H.VALUE, C.REPORT_TYPE_NO_LINKS,
                    H.CHECKED, H.CHECKED);
        } else {
            xhtml.startElement(H.INPUT,
                    H.TYPE, H.RADIO,
                    H.NAME, C.REPORT_TYPE,
                    H.VALUE, C.REPORT_TYPE_NO_LINKS);
        }


        xhtml.br();
        xhtml.characters("Score Normalization Options: ");
        xhtml.startElement(H.SELECT,
                H.NAME, C.REPORT_NORM_TYPE);
        for (TaggerRequest.NORM_TYPE t : TaggerRequest.NORM_TYPE.values()) {
            if (rr.getNormType() != null && rr.getNormType().equals(t)) {

                xhtml.startElement(H.OPTION,
                        H.VALUE, t.toString(),
                        H.SELECTED, H.SELECTED);
            } else {
                xhtml.startElement(H.OPTION,
                        H.VALUE, t.toString());
            }
            xhtml.characters(t.getDisplayString());
            xhtml.endElement(H.OPTION);
        }
        xhtml.endElement(H.SELECT);
        xhtml.br();

        if (searcherApp.getRhapsodeCollection().getFavoritesSize() > 0) {
            xhtml.characters("Add 'Favorites' ");
            xhtml.characters("Yes: ");
            if (rr.getIncludeFavorites()) {
                xhtml.startElement(H.INPUT,
                        H.TYPE, H.RADIO,
                        H.NAME, C.REPORT_INCLUDE_FAVORITES,
                        H.VALUE, C.TRUE,
                        H.CHECKED, H.CHECKED);
            } else {
                xhtml.startElement(H.INPUT,
                        H.TYPE, H.RADIO,
                        H.NAME, C.REPORT_INCLUDE_FAVORITES,
                        H.VALUE, C.TRUE);

            }
            xhtml.characters(" No: ");
            if (!rr.getIncludeFavorites()) {
                xhtml.startElement(H.INPUT,
                        H.TYPE, H.RADIO,
                        H.NAME, C.REPORT_INCLUDE_FAVORITES,
                        H.VALUE, C.FALSE,
                        H.CHECKED, H.CHECKED);
            } else {
                xhtml.startElement(H.INPUT,
                        H.TYPE, H.RADIO,
                        H.NAME, C.REPORT_INCLUDE_FAVORITES,
                        H.VALUE, C.FALSE);

            }
            xhtml.br();
        }


        xhtml.characters("Stored Queries:");
        xhtml.br();
        xhtml.startElement(H.TABLE);
        for (Map.Entry<Integer, StoredQuery> e :
                searcherApp.getSessionManager().getStoredQueryManager().getStoredQueryMap().entrySet()) {
            xhtml.startElement(H.TR);
            xhtml.startElement(H.TD);
            Integer id = e.getKey();
            StoredQuery sq = e.getValue();
            String qName = sq.getQueryName();
            boolean noParseException =
                    StringUtils.isBlank(sq.getMainQueryExcMsg()) &&
                            StringUtils.isBlank(sq.getFilterQueryExcMsg());
            if (!noParseException) {
                xhtml.startElement(H.SPAN, H.CLASS, CSS.ERROR_MSG);
            }

            if (noParseException) {
                if (rr.getStoredQueryIds().contains(id)) {
                    xhtml.startElement(H.INPUT,
                            H.TYPE, H.CHECKBOX,
                            H.NAME, C.QUERIES_FOR_REPORT,
                            H.VALUE, Integer.toString(id),
                            H.CHECKED, H.CHECKED);
                } else {
                    xhtml.startElement(H.INPUT,
                            H.TYPE, H.CHECKBOX,
                            H.NAME, C.QUERIES_FOR_REPORT,
                            H.VALUE, Integer.toString(id));
                }
            } else {
                xhtml.startElement(H.INPUT,
                        H.TYPE, H.CHECKBOX,
                        H.NAME, C.QUERIES_FOR_REPORT,
                        H.VALUE, Integer.toString(id),
                        H.DISABLED, H.DISABLED);
            }


            xhtml.characters(qName);
            xhtml.endElement(H.CHECKBOX);
            if (!noParseException) {
                xhtml.endElement(H.SPAN);
            }
            xhtml.endElement(H.TD);
            xhtml.endElement(H.TR);
        }
        xhtml.endElement(H.TABLE);
        xhtml.br();
        if (rr.getActionType() == ReportRequest.ACTION_TYPE.SELECT_ALL) {
            xhtml.startElement(H.INPUT,
                    H.TYPE, H.SUBMIT,
                    H.NAME, C.DESELECT_ALL,
                    H.VALUE, "Deselect All");
            xhtml.endElement(H.INPUT);
        } else {
            xhtml.startElement(H.INPUT,
                    H.TYPE, H.SUBMIT,
                    H.NAME, C.SELECT_ALL,
                    H.VALUE, "Select All");
            xhtml.endElement(H.INPUT);
        }

        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.WRITE_REPORT,
                H.VALUE, "Write Report",
                "default", "default");
        xhtml.endElement(H.INPUT);
    }

    private class ReportException extends Exception {
        public ReportException(String msg) {
            super(msg);
        }

        public ReportException(Throwable t) {
            super(t);
        }
    }

/*
    private void writeQueriesTable(Map<String, Integer> docCounts, RhapsodeXHTMLHandler xhtml)
            throws SQLException, SAXException {
        Map<String, StoredQuery> queries = searcherApp.getSessionManager().
                getStoredQueryManager().getStoredQueryMap();
        if (queries.size() == 0) {
            xhtml.characters("The are currently no stored queries.");
            xhtml.br();
            return;
        }
        if (queries.size() > 0) {
            xhtml.startElement(H.TABLE,
                    H.BORDER, "2");
        }
        xhtml.startElement(H.TR);
        xhtml.element(H.TH, "Name");
        xhtml.element(H.TH, "Default Field");
        xhtml.element(H.TH, "Main Query");
        xhtml.element(H.TH, "Filter Query");
        xhtml.element(H.TH, "Max Hits");
        xhtml.element(H.TH, "Document Counts");

        SortedSet<String> keys = new TreeSet<>(queries.keySet());
        for (String k : keys) {
            StoredQuery q = queries.get(k);
            String mq = q.getMainQueryString();
            String fq = q.getFilterQueryString();
            fq = (fq == null) ? " " : fq;
            xhtml.startElement(H.TR);
            xhtml.element(H.TD, k);
            xhtml.element(H.TD, q.getDefaultField());
            xhtml.element(H.TD, mq);
            xhtml.element(H.TD, fq);
            xhtml.element(H.TD, (q.getMaxHits() == StoredQuery.RETRIEVE_ALL_HITS) ? "ALL" :
                Integer.toString(q.getMaxHits()));
            if (docCounts != null) {
                Integer val = docCounts.get(k);
                String intVal = "n/a";
                if (val != null) {
                    intVal = Integer.toString(val);
                }
                xhtml.element(H.TD, intVal);
            }
            xhtml.endElement(H.TR);
        }
        xhtml.endElement(H.TABLE);

    }

    private void addStoredQuery(StoredQueryRequest sqr) throws ParseException, SQLException {
        String name = testBlank("name", sqr.getQueryName());
        String mainQuery = testBlank("main query", sqr.getQueryFields().getMainQueryString());
        name = name.trim();
        name = name.toLowerCase(Locale.ENGLISH);

        testAlphanumeric(name);
        try {
            searcherApp.getSessionManager().getStoredQueryManager().addQuery(name, sqr.getQueryFields());
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains(SQL.ALREADY_EXISTS)) {
                throw new SQLException("A stored query with that name already exists.");
            }
        }
    }
    */
}

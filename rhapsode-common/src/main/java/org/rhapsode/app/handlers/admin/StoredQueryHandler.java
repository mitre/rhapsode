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
import org.apache.lucene.search.Query;
import org.eclipse.jetty.server.Request;
import org.rhapsode.app.HitCounter;
import org.rhapsode.app.RhapsodeSearcherApp;
import org.rhapsode.app.contants.C;
import org.rhapsode.app.contants.CSS;
import org.rhapsode.app.contants.H;
import org.rhapsode.app.contants.SQL;
import org.rhapsode.app.decorators.RhapsodeDecorator;
import org.rhapsode.app.decorators.RhapsodeXHTMLHandler;
import org.rhapsode.app.session.DBStoredConceptManager;
import org.rhapsode.app.session.DynamicParameterConfig;
import org.rhapsode.app.session.DynamicParameters;
import org.rhapsode.app.session.StoredQueryReader;
import org.rhapsode.app.session.StoredQueryWriter;
import org.rhapsode.app.utils.StoredQCUtils;
import org.rhapsode.lucene.search.ComplexQuery;
import org.rhapsode.lucene.search.ComplexQueryBuilder;
import org.rhapsode.lucene.search.SQField;
import org.rhapsode.lucene.search.StoredQuery;
import org.rhapsode.util.LanguageDirection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class StoredQueryHandler extends AdminHandler {
    private static final Logger LOG = LoggerFactory.getLogger(StoredQueryHandler.class);

    private final long MAX_WAIT_FOR_COUNTS = 20000;//20 seconds
    private final RhapsodeSearcherApp searcherApp;

    public StoredQueryHandler(RhapsodeSearcherApp searcherApp) {
        super("Stored Queries");
        this.searcherApp = searcherApp;
    }


    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest,
                       HttpServletResponse response) throws IOException, ServletException {
        RhapsodeXHTMLHandler xhtml = null;
        StoredQueryRequest sqr = StoredQueryRequest.build(httpServletRequest);
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
            Map<Integer, Integer> docCounts = new HashMap<>();
            boolean addSelectCheckbox = false;
            switch (sqr.getActionType()) {
                case LOAD_STORED_QUERIES_DIALOGUE:
                    writeLoadQueriesDialogue(xhtml);
                    break;
                case LOAD_STORED_QUERIES:
                    errorMessage = loadStoredQueries(sqr);
                    break;
                case ADD_DIALOGUE:
                    addOrUpdateStoredQueryDialogue(LanguageDirection.LTR, xhtml, null, null, true);
                    xhtml.br();
                    break;
                case ADD_QUERY:
                    try {
                        addStoredQuery(sqr);
                    } catch (Exception e) {
                        addOrUpdateStoredQueryDialogue(LanguageDirection.LTR, xhtml, e.getMessage(), sqr, true);
                        e.printStackTrace();
                        errorMessage = e.getMessage();
                    }
                    break;
                case DELETE_ALL_STORED_QUERIES:
                    try {
                        deleteStoredQueries();
                    } catch (Exception e) {
                        errorMessage = e.getMessage();
                    }
                    break;
                case UPDATE_DOC_COUNTS:
                    if (searcherApp.getRhapsodeCollection() == null) {
                        errorMessage = "Need to load new collection";
                    } else {
                        try {
                            docCounts = updateCounts();
                        } catch (IOException e) {
                            e.printStackTrace();
                            errorMessage = "IOException during search: " + e.getMessage();
                        } catch (ParseException e) {
                            e.printStackTrace();
                            errorMessage = "Failed to parse: " + e.getMessage();
                        }
                    }
                    break;
                case SELECT_QUERIES_FOR_UPDATE:
                    addSelectCheckbox = true;
                    break;
                case UPDATE_SELECTED_QUERY_DIALOGUE:
                    if (sqr.getSelectedQueries().size() > 0) {
                        updateQueryParamsFromDB(sqr);
                        addOrUpdateStoredQueryDialogue(LanguageDirection.LTR, xhtml, null, sqr, false);
                        xhtml.br();
                    }
                    break;
                case UPDATE_SELECTED_QUERY:
                    try {
                        updateSelectedQuery(sqr, xhtml);
                    } catch (Exception e) {
                        e.printStackTrace();
                        addOrUpdateStoredQueryDialogue(LanguageDirection.LTR, xhtml, e.getMessage(), sqr, false);
                        errorMessage = e.getMessage();
                    }
                    break;
                case DELETE_SELECTED_QUERIES:
                    try {
                        int deleted = deleteSelectedQueries(sqr);
                        xhtml.br();
                        xhtml.element(H.P, "Successfully deleted " + Integer.toString(deleted) + " stored queries.");
                        xhtml.br();
                    } catch (SQLException e) {
                        errorMessage = (e.getMessage() == null) ?
                                "Unknown exception while trying to delete selected stored queries" :
                                e.getMessage();
                    }
                    break;
                case SAVE_DIALOGUE:
                    saveDialogue(sqr, xhtml);
                    break;
                case SAVE_QUERIES:
                    saveQueries(sqr, xhtml);
                    break;
            }
            if (errorMessage != null) {
                RhapsodeDecorator.writeErrorMessage(errorMessage, xhtml);
            }
            writeQueriesTable(docCounts, addSelectCheckbox, xhtml);
            if (addSelectCheckbox) {
                writeSelectFooter(LanguageDirection.LTR, xhtml);
            } else if (!sqr.getActionType().equals(StoredQueryRequest.ActionType.ADD_DIALOGUE)) {
                writeDefaultStoredQuery(LanguageDirection.LTR, xhtml);
            }
            xhtml.endElement(H.FORM);
            RhapsodeDecorator.addFooter(xhtml);
            xhtml.endDocument();

        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e);
        }

    }

    private void saveQueries(StoredQueryRequest sqr, RhapsodeXHTMLHandler xhtml) throws SAXException {
        Path p = null;
        if (StringUtils.isBlank(sqr.getFileToLoad())) {
            RhapsodeDecorator.writeErrorMessage("Must specify file to save to.", xhtml);
            return;
        }
        try {
            p = Paths.get(sqr.getFileToLoad());
        } catch (Exception e) {
            RhapsodeDecorator.writeErrorMessage("Couldn't create path for file:" + sqr.getFileToLoad(), xhtml);
            RhapsodeDecorator.writeErrorMessage("Error message: " + e.getMessage(), xhtml);
            return;

        }

        if (p == null) {
            RhapsodeDecorator.writeErrorMessage("Stored query output file is null ?!", xhtml);
            return;
        }

        try {
            if (p.getParent() != null) {
                Files.createDirectories(p.getParent());
            }
        } catch (IOException e) {
            RhapsodeDecorator.writeErrorMessage("Couldn't create parent directory for file: " + sqr.getFileToLoad(), xhtml);
            RhapsodeDecorator.writeErrorMessage("Error message: " + e.getMessage(), xhtml);
            return;
        }
        StoredQueryWriter writer = new StoredQueryWriter(searcherApp);
        try {
            writer.writeStoredQueries(p,
                    searcherApp.getSessionManager().getDynamicParameterConfig().getString(DynamicParameters.METADATA_CREATOR));
        } catch (IOException e) {
            RhapsodeDecorator.writeErrorMessage("Couldn't write file: " + p.toAbsolutePath(), xhtml);
            RhapsodeDecorator.writeErrorMessage("Error message: " + e.getMessage(), xhtml);
            return;
        }
        xhtml.element(H.P, "Successfully wrote queries to: " + p.toAbsolutePath());

    }

    private void saveDialogue(StoredQueryRequest sqr, RhapsodeXHTMLHandler xhtml) throws SAXException {
        xhtml.startElement(H.INPUT,
                H.TYPE, H.TEXT,
                H.NAME, C.STORED_QUERIES_PATH,
                H.SIZE, Integer.toString(80));
        xhtml.endElement(H.INPUT);

        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.SAVE_STORED_QUERIES,
                H.VALUE, "Save",
                "default", "");
        xhtml.endElement(H.INPUT);
        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.CANCEL,
                H.VALUE, "Cancel",
                "default", "");
        xhtml.endElement(H.INPUT);

    }

    private void updateQueryParamsFromDB(StoredQueryRequest sqr) throws IOException {
        List<Integer> qNames = sqr.getSelectedQueries();
        if (qNames != null && qNames.size() > 0) {
            Integer id = sqr.getSelectedQueries().get(0);
            StoredQuery sq = searcherApp.getSessionManager().getStoredQueryManager().getStoredQuery(id);
            Map<SQField, String> queryFields = new HashMap<>();
            for (SQField sqField : SQField.values()) {
                queryFields.put(sqField, sq.getString(sqField));
            }
            sqr.setQuery(queryFields);
        }
    }

    private int deleteSelectedQueries(StoredQueryRequest sqr) throws SQLException {
        //TODO: vulnerable to % in query name, but
        //this won't happen in regular use!
        int deleted = 0;
        for (Integer q : sqr.getSelectedQueries()) {
            deleted += searcherApp.getSessionManager().getStoredQueryManager().deleteQuery(q);
        }
        return deleted;
    }

    private void writeSelectFooter(LanguageDirection ltr, RhapsodeXHTMLHandler xhtml) throws SAXException {
        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.UPDATE_SELECTED_QUERY_DIALOGUE,
                H.VALUE, "Edit",
                "default", "");
        xhtml.endElement(H.INPUT);

        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.DELETE_SELECTED_QUERIES,
                H.VALUE, "Delete");
        xhtml.endElement(H.INPUT);

        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.CANCEL,
                H.VALUE, "Cancel");
        xhtml.endElement(H.INPUT);
    }

    private void updateSelectedQuery(StoredQueryRequest sqr, RhapsodeXHTMLHandler xhtml) throws ParseException, SAXException, SQLException {
        String idString = testBlank("id", sqr.getQuery().get(SQField.ID));
        idString = idString.trim();
        idString = idString.toLowerCase(Locale.ENGLISH);

        int id = -3;
        try {
            id = Integer.parseInt(idString);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("ID has to be an integer:" + idString);
        }
        sqr.getQuery().put(SQField.ID, Integer.toString(id));
        StoredQuery sq = StoredQCUtils.buildStoredQuery(sqr.getQuery());
        String name = sq.getQueryName();
        ComplexQueryBuilder.validateMain(sq,
                searcherApp.getParserPlugin(), searcherApp.getSessionManager().getStoredConceptManager());
        ComplexQueryBuilder.validateFilter(sq,
                searcherApp.getParserPlugin(), searcherApp.getSessionManager().getStoredConceptManager());
        int updated = searcherApp.getSessionManager().getStoredQueryManager().update(sq);
        if (updated == 1) {
            xhtml.br();
            xhtml.element(H.P, "Successfully updated query: " + name);
            xhtml.br();
        } else {
            xhtml.br();
            xhtml.element(H.P, "Failed to update query: " + name);
            xhtml.br();
        }
    }


    private String loadStoredQueries(StoredQueryRequest sqr) throws SQLException {
        searcherApp.getSessionManager().getStoredConceptManager().deleteConcepts();
        searcherApp.getSessionManager().getStoredQueryManager().deleteAllQueries();

        StoredQueryReader reader = new StoredQueryReader(
                (DBStoredConceptManager) searcherApp.getSessionManager().getStoredConceptManager(),
                searcherApp.getSessionManager().getStoredQueryManager(),
                searcherApp.getParserPlugin(),
                searcherApp.getRhapsodeCollection().getIndexSchema(),
                searcherApp.getSessionManager().getConnection());
        try (InputStream is = Files.newInputStream(Paths.get(sqr.getFileToLoad()))) {
            reader.loadBoth(is);
        } catch (Exception e) {
            return e.getMessage();
        }
        return "";
    }

    private Map<Integer, Integer> updateCounts() throws SQLException, IOException, ParseException {
        Map<Integer, StoredQuery> c = searcherApp.getSessionManager().
                getStoredQueryManager().getStoredQueryMap();
        Map<Integer, Query> queries = new HashMap<>();
        for (Map.Entry<Integer, StoredQuery> e : c.entrySet()) {

            try {
                ComplexQuery cq = ComplexQueryBuilder.buildQuery(
                        e.getValue(),
                        searcherApp.getParserPlugin(),
                        searcherApp.getSessionManager().getStoredConceptManager(),
                        searcherApp.getGeoConfig(),
                        searcherApp.getRhapsodeCollection().getIgnoredQuery()
                );
                if (cq.getRetrievalQuery() != null) {
                    //TODO throw exception for this one...this is bad
                    queries.put(e.getKey(), cq.getRetrievalQuery());
                }
            } catch (ParseException pe) {
                searcherApp.getSessionManager().getStoredQueryManager().updateQueryExceptions(e.getValue(), searcherApp);
            }
        }
        HitCounter hc = new HitCounter();
        return hc.count(queries, searcherApp.getRhapsodeCollection().getIndexManager().getSearcher(),
                searcherApp.getCommonSearchConfig().getNumThreadsForConcurrentSearches(), MAX_WAIT_FOR_COUNTS);
    }


    private void deleteStoredQueries() throws Exception {
        Path trashDir = Paths.get("resources/trash");
        Files.createDirectories(trashDir);
        DateFormat f = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
        String dString = f.format(new Date());
        Path outFile = trashDir.resolve("stored_queries_" + dString + ".xlsx");
        StoredQueryWriter writer = new StoredQueryWriter(searcherApp);
        writer.writeStoredQueries(outFile,
                searcherApp.getSessionManager()
                        .getDynamicParameterConfig()
                        .getString(DynamicParameters.METADATA_CREATOR));
        searcherApp.getSessionManager().getStoredQueryManager().deleteAllQueries();
    }

    private void writeQueriesTable(Map<Integer, Integer> docCounts, boolean includeSelection,
                                   RhapsodeXHTMLHandler xhtml)
            throws SQLException, SAXException {

        Map<Integer, StoredQuery> queries = searcherApp.getSessionManager().
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
        if (includeSelection) {
            xhtml.element(H.TH, " ");
        }
        ShowSettings s = getShowSettings();
        xhtml.element(H.TH, "Name");
        xhtml.element(H.TH, "Main Query");
        if (s.showMainQueryTranslation) {
            xhtml.element(H.TH, "Main Query Translation");
        }
        if (s.showFilterQuery) {
            xhtml.element(H.TH, "Filter Query");
        }
        if (s.showFilterQueryTranslation) {
            xhtml.element(H.TH, "Filter Query Translation");
        }
        if (s.showMaxHits) {
            xhtml.element(H.TH, "Max Hits");
        }
        if (s.showDefaultField) {
            xhtml.element(H.TH, "Default Field");
        }
        if (s.showStyles) {
            xhtml.element(H.TH, "Highlighting Style");
        }
        if (s.showPriority) {
            xhtml.element(H.TH, "Priority");
        }
        if (s.showNotes) {
            xhtml.element(H.TH, "Notes");
        }
        xhtml.element(H.TH, "Document Counts");

        for (Integer id : queries.keySet()) {
            StoredQuery q = queries.get(id);
            String mq = q.getMainQueryString();
            String fq = q.getFilterQueryString();
            fq = (fq == null) ? " " : fq;
            mq = (mq == null) ? " " : mq;
            xhtml.startElement(H.TR);
            String queryName = q.getQueryName();
            if (includeSelection) {
                xhtml.startElement(H.TD);
                xhtml.startElement(H.INPUT,
                        H.TYPE, H.CHECKBOX,
                        H.NAME, C.SELECTED_STORED_QUERY,
                        H.VALUE, Integer.toString(id));
                xhtml.endElement(H.INPUT);
                xhtml.endElement(H.TD);
            }
//for debugging            xhtml.element(H.TD, Integer.toString(q.getId()));
            xhtml.startElement(H.TD);
            writeStoredQueryLink(xhtml, id, queryName);
            xhtml.endElement(H.TD);
            if (!StringUtils.isBlank(q.getMainQueryExcMsg())) {
                xhtml.startElement(H.TD);
                xhtml.startElement(H.SPAN, H.CLASS, CSS.ERROR_MSG);
                xhtml.characters(mq);
                xhtml.endElement(H.SPAN);
                xhtml.endElement(H.TD);
            } else {
                xhtml.td(mq);
            }
            if (s.showMainQueryTranslation) {
                xhtml.td(q.getMainQueryTranslation());
            }
            if (s.showFilterQuery) {
                if (!StringUtils.isBlank(q.getFilterQueryExcMsg())) {
                    xhtml.startElement(H.TD);
                    xhtml.startElement(H.SPAN, H.CLASS, CSS.ERROR_MSG);
                    xhtml.characters(fq);
                    xhtml.endElement(H.SPAN);
                    xhtml.endElement(H.TD);
                } else {
                    xhtml.td(fq);
                }
            }
            if (s.showFilterQueryTranslation) {
                xhtml.td(q.getFilterQueryTranslation());
            }
            if (s.showMaxHits) {
                xhtml.element(H.TD, (q.getMaxHits() == StoredQuery.RETRIEVE_ALL_HITS) ? "ALL" :
                        Integer.toString(q.getMaxHits()));
            }
            if (s.showDefaultField) {
                xhtml.td(q.getDefaultField());
            }
            if (s.showStyles) {
                xhtml.td(q.getHighlightingStyle());
            }
            if (s.showPriority) {
                if (q.getPriority() == StoredQuery.DEFAULT_PRIORITY) {
                    xhtml.element(H.TD, "default");
                } else {
                    xhtml.element(H.TD, Integer.toString(q.getPriority()));
                }
            }

            if (s.showNotes) {
                xhtml.td(q.getNotes());
            }
            if (docCounts != null) {
                Integer val = docCounts.get(id);
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

    private void writeStoredQueryLink(RhapsodeXHTMLHandler xhtml, Integer id, String name) throws SAXException {
        xhtml.startElement(H.A,
                H.HREF, "/rhapsode/basic?&" + C.STORED_QUERY_ID + "=" + id);
        xhtml.characters(name);
        xhtml.endElement(H.A);
    }

    private void addStoredQuery(StoredQueryRequest sqr) throws IllegalArgumentException, ParseException, SQLException {
        String name = testBlank("name", sqr.getQuery().get(SQField.NAME));
        name = name.trim();
        name = name.toLowerCase(Locale.ENGLISH);

        testAlphanumeric(name);
        StoredQuery sq = StoredQCUtils.buildStoredQuery(sqr.getQuery());
        if (StringUtils.isBlank(sq.getMainQueryString()) && StringUtils.isBlank(sq.getFilterQueryString())) {
            throw new ParseException("Must specify either main query string or filter query string");
        }
        ComplexQueryBuilder.validateMain(sq,
                searcherApp.getParserPlugin(), searcherApp.getSessionManager().getStoredConceptManager());
        ComplexQueryBuilder.validateFilter(sq,
                searcherApp.getParserPlugin(), searcherApp.getSessionManager().getStoredConceptManager());
        try {
            searcherApp.getSessionManager().getStoredQueryManager().addQuery(sq);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains(SQL.ALREADY_EXISTS)) {
                throw new SQLException(
                        String.format(Locale.US, "A stored query named '%s' already exists. Please rename the query.",
                                name));
            }
        }
    }


    public static void writeDefaultStoredQuery(LanguageDirection ltr, RhapsodeXHTMLHandler xhtml) throws SAXException {
        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.ADD_STORED_QUERY_DIALOGUE,
                H.VALUE, "Add Query");
        xhtml.endElement(H.INPUT);

        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.SELECT_QUERIES_FOR_UPDATE,
                H.VALUE, "Edit Queries");
        xhtml.endElement(H.INPUT);

        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.LOAD_STORED_QUERIES_DIALOGUE,
                H.VALUE, "Load Queries");
        xhtml.endElement(H.INPUT);

        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.SAVE_STORED_QUERIES_DIALOGUE,
                H.VALUE, "Save Queries");
        xhtml.endElement(H.INPUT);

        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.DELETE_ALL_STORED_QUERIES,
                H.VALUE, "Delete All Queries");
        xhtml.endElement(H.INPUT);

        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.UPDATE_DOCUMENT_COUNTS,
                H.VALUE, "Update Document Counts");
        xhtml.endElement(H.INPUT);

    }


    public void addOrUpdateStoredQueryDialogue(LanguageDirection direction,
                                               RhapsodeXHTMLHandler xhtml, String errorMessage,
                                               StoredQueryRequest sqr, boolean isAdd) throws SAXException {
        String queryId = "";
        String queryName = "";

        if (!isAdd) {
            if (sqr == null) {
                throw new IllegalArgumentException("Must pass in non-null stored query request!");
            } else {
                queryId = sqr.getQuery().get(SQField.ID);
                queryName = sqr.getQuery().get(SQField.NAME);
                if (StringUtils.isBlank(queryId)) {
                    throw new IllegalArgumentException("Uh, query id can't be blank during an update!");
                }
            }
        }
        AttributesImpl nameAttrs = new AttributesImpl();
        if (isAdd) {
            nameAttrs.addAttribute("", H.TYPE, H.TYPE, "", H.TEXT);
            nameAttrs.addAttribute("", H.SIZE, H.SIZE, "", Integer.toString(40));
            nameAttrs.addAttribute("", H.NAME, H.NAME, "", C.STORED_QUERY_NAME);
            if (sqr != null) {
                nameAttrs.addAttribute("", H.VALUE, H.VALUE, "", sqr.getQuery().get(SQField.NAME));
            }
        } else if (sqr != null) {
            nameAttrs.addAttribute("", H.TYPE, H.TYPE, "", H.HIDDEN);
            nameAttrs.addAttribute("", H.NAME, H.NAME, "", C.STORED_QUERY_NAME);
            nameAttrs.addAttribute("", H.VALUE, H.VALUE, "", sqr.getQuery().get(SQField.NAME));
        }


        //maxhits
        AttributesImpl mhAttrs = new AttributesImpl();
        mhAttrs.addAttribute("", H.TYPE, H.TYPE, "", H.TEXT);
        mhAttrs.addAttribute("", H.NAME, H.NAME, "", C.MAX_SEARCH_RESULTS);
        mhAttrs.addAttribute("", H.SIZE, H.SIZE, "", Integer.toString(2));
        if (sqr != null) {
            addIntegerString(sqr.getQuery().get(SQField.MAX_HITS), mhAttrs);
        }

        AttributesImpl pAttrs = new AttributesImpl();
        pAttrs.addAttribute("", H.TYPE, H.TYPE, "", H.TEXT);
        pAttrs.addAttribute("", H.NAME, H.NAME, "", C.STORED_QUERY_PRIORITY);
        pAttrs.addAttribute("", H.SIZE, H.SIZE, "", Integer.toString(2));
        if (sqr != null) {
            addIntegerString(sqr.getQuery().get(SQField.PRIORITY), pAttrs);
        }

        if (sqr != null) {
            AttributesImpl idAttrs = new AttributesImpl();
            idAttrs.addAttribute("", H.TYPE, H.TYPE, "", H.HIDDEN);
            idAttrs.addAttribute("", H.NAME, H.NAME, "", C.STORED_QUERY_ID);
            idAttrs.addAttribute("", H.VALUE, H.VALUE, "", queryId);
            //add hidden id attribute
            xhtml.startElement(H.INPUT, idAttrs);
            xhtml.endElement(H.INPUT);

        }

        if (!StringUtils.isBlank(errorMessage)) {
            xhtml.startElement(H.SPAN, H.CLASS, CSS.ERROR_MSG);
            xhtml.characters(errorMessage);
            xhtml.endElement(H.SPAN);
            xhtml.br();
        }

        xhtml.startElement(H.TABLE, H.BORDER, "2");

        xhtml.startElement(H.TR);
        xhtml.element(H.TD, "Stored Query Name: ");

        xhtml.startElement(H.TD);
        xhtml.startElement(H.INPUT, nameAttrs);
        if (!isAdd && queryName != null) {
            xhtml.characters(queryName);
        }
        xhtml.endElement(H.INPUT);
        xhtml.endElement(H.TD);

        xhtml.endElement(H.TR);

        xhtml.startElement(H.TR);
        xhtml.element(H.TD, "Default search field: ");
        xhtml.startElement(H.TD);
        //TODO: add sqr's already selected field
        RhapsodeDecorator.writeFieldSelector(
                searcherApp.getRhapsodeCollection().getIndexSchema(),
                searcherApp.getStringParameter(DynamicParameters.DEFAULT_CONTENT_FIELD), xhtml);
        xhtml.endElement(H.TD);
        xhtml.endElement(H.TR);

        xhtml.startElement(H.TR);
        xhtml.element(H.TD, "Main Query: ");
        xhtml.startElement(H.TD);
        xhtml.startElement(H.DIV);
        xhtml.startElement(H.TEXT_AREA,
                H.NAME, C.MAIN_QUERY,
                H.ROWS, Integer.toString(5),
                H.COLS, Integer.toString(40));
        if (sqr != null) {
            xhtml.characters(sqr.getQuery().get(SQField.MAIN_QUERY));
        }
        xhtml.endElement(H.TEXT_AREA);
        xhtml.endElement(H.DIV);
        xhtml.endElement(H.TD);
        xhtml.endElement(H.TR);

        xhtml.startElement(H.TR);
        xhtml.element(H.TD, "Main Query Translation: ");
        xhtml.startElement(H.TD);
        xhtml.startElement(H.DIV);
        xhtml.startElement(H.TEXT_AREA,
                H.NAME, C.MAIN_QUERY_TRANSLATION,
                H.ROWS, Integer.toString(5),
                H.COLS, Integer.toString(40));
        if (sqr != null) {
            xhtml.characters(sqr.getQuery().get(SQField.MAIN_QUERY_TRANSLATION));
        }
        xhtml.endElement(H.TEXT_AREA);
        xhtml.endElement(H.DIV);
        xhtml.endElement(H.TD);
        xhtml.endElement(H.TR);

        xhtml.startElement(H.TR);
        xhtml.element(H.TD, "Filter Query: ");
        xhtml.startElement(H.TD);
        xhtml.startElement(H.DIV);
        xhtml.startElement(H.TEXT_AREA,
                H.NAME, C.FILTER_QUERY,
                H.ROWS, Integer.toString(5),
                H.COLS, Integer.toString(40));
        if (sqr != null) {
            xhtml.characters(sqr.getQuery().get(SQField.FILTER_QUERY));
        } else {
        }
        xhtml.endElement(H.TEXT_AREA);
        xhtml.endElement(H.DIV);
        xhtml.endElement(H.TD);
        xhtml.endElement(H.TR);
        xhtml.startElement(H.TR);

        xhtml.element(H.TD, "Filter Query Translation: ");
        xhtml.startElement(H.TD);
        xhtml.startElement(H.DIV);
        xhtml.startElement(H.TEXT_AREA,
                H.NAME, C.FILTER_QUERY_TRANSLATION,
                H.ROWS, Integer.toString(5),
                H.COLS, Integer.toString(40));
        if (sqr != null) {
            xhtml.characters(sqr.getQuery().get(SQField.FILTER_QUERY_TRANSLATION));
        }
        xhtml.endElement(H.TEXT_AREA);
        xhtml.endElement(H.DIV);
        xhtml.endElement(H.TD);
        xhtml.endElement(H.TR);


        xhtml.startElement(H.TR);
        xhtml.element(H.TD, "Max Number of Results: ");
        xhtml.startElement(H.TD);
        xhtml.startElement(H.INPUT, mhAttrs);

        xhtml.endElement(H.INPUT);
        xhtml.endElement(H.TD);
        xhtml.endElement(H.TR);

        xhtml.startElement(H.TR);
        xhtml.element(H.TD, "Highlighting Style: ");
        xhtml.startElement(H.TD);
        xhtml.startElement(H.SELECT,
                H.NAME, C.HIGHLIGHT_STYLE);
        //TODO: select appropriate highlighting style if sqr is non null
        for (String n : searcherApp.getCommonSearchConfig().getHighlightingStyles().keySet()) {
            xhtml.startElement(H.OPTION,
                    H.VALUE, n);
            xhtml.characters(n);
            xhtml.endElement(H.OPTION);

        }
        xhtml.endElement(H.SELECT);
        xhtml.endElement(H.TD);
        xhtml.endElement(H.TR);

        xhtml.startElement(H.TR);
        xhtml.element(H.TD, "Priority: ");
        xhtml.startElement(H.TD);
        xhtml.startElement(H.INPUT, pAttrs);
        xhtml.endElement(H.INPUT);
        xhtml.endElement(H.TD);
        xhtml.endElement(H.TR);
        xhtml.startElement(H.TR);

        xhtml.element(H.TD, "NOTES: ");
        xhtml.startElement(H.TD);
        xhtml.startElement(H.DIV);
        xhtml.startElement(H.TEXT_AREA,
                H.NAME, C.STORED_QUERY_NOTES,
                H.ROWS, Integer.toString(5),
                H.COLS, Integer.toString(40));
        if (sqr != null) {
            xhtml.characters(sqr.getQuery().get(SQField.NOTES));
        }
        xhtml.endElement(H.TEXT_AREA);
        xhtml.endElement(H.DIV);
        xhtml.endElement(H.TD);
        xhtml.endElement(H.TR);
        xhtml.endElement(H.TABLE);

        if (isAdd) {
            xhtml.startElement(H.INPUT,
                    H.TYPE, H.SUBMIT,
                    H.NAME, C.ADD_STORED_QUERY,
                    H.VALUE, "Add",
                    "default", "");
            xhtml.endElement(H.INPUT);
        } else {
            xhtml.startElement(H.INPUT,
                    H.TYPE, H.SUBMIT,
                    H.NAME, C.UPDATE_SELECTED_QUERY,
                    H.VALUE, "Save",
                    "default", "");
            xhtml.endElement(H.INPUT);
        }
        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.CANCEL,
                H.VALUE, "Cancel");
        xhtml.endElement(H.INPUT);

    }

    private void addIntegerString(String s, AttributesImpl mhAttrs) {
        if (StringUtils.isBlank(s)) {
            return;
        }
        try {
            int i = Integer.parseInt(s);
            if (i > -1) {
                mhAttrs.addAttribute("", H.VALUE,
                        H.VALUE, "", Integer.toString(i));
                return;
            }
        } catch (NumberFormatException e) {
        }
        mhAttrs.addAttribute("", H.VALUE,
                H.VALUE, "", s);
    }

    private void writeLoadQueriesDialogue(RhapsodeXHTMLHandler xhtml) throws SAXException {
        AttributesImpl sqAttrs = new AttributesImpl();
        sqAttrs.addAttribute("", H.TYPE, H.TYPE, "", H.TEXT);
        sqAttrs.addAttribute("", H.NAME, H.NAME, "", C.STORED_QUERIES_PATH);
        sqAttrs.addAttribute("", H.SIZE, H.SIZE, "", Integer.toString(60));

        xhtml.characters("WARNING! LOADING A STORED QUERIES FILE WILL DELETE CURRENT STORED CONCEPTS AND STORED QUERIES!!!");
        xhtml.startElement(H.TABLE);
        xhtml.startElement(H.TABLE, H.BORDER, "2");
        xhtml.startElement(H.TR);


        xhtml.startElement(H.TD);
        xhtml.characters("File to load: ");
        xhtml.startElement(H.INPUT, sqAttrs);
        xhtml.endElement(H.INPUT);
        xhtml.endElement(H.TD);

        xhtml.startElement(H.TD);
        xhtml.startElement(H.DIV);
        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.LOAD_STORED_QUERIES,
                H.VALUE, "Load",
                "default", "");
        xhtml.endElement(H.INPUT);
        xhtml.endElement(H.DIV);

        xhtml.startElement(H.DIV);
        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.CANCEL,
                H.VALUE, "Cancel");
        xhtml.endElement(H.INPUT);
        xhtml.endElement(H.DIV);
        xhtml.endElement(H.TD);

        xhtml.endElement(H.TR);
        xhtml.endElement(H.TABLE);
    }

    private ShowSettings getShowSettings() {
        DynamicParameterConfig config = searcherApp.getSessionManager().getDynamicParameterConfig();
        ShowSettings s = new ShowSettings();
        s.showMainQueryTranslation = config.getBoolean(DynamicParameters.SQ_SHOW_MAIN_QUERY_TRANSLATION);
        s.showFilterQuery = config.getBoolean(DynamicParameters.SQ_SHOW_FILTER_QUERY);
        s.showFilterQueryTranslation = config.getBoolean(DynamicParameters.SQ_SHOW_FILTER_QUERY_TRANSLATION);
        s.showDefaultField = config.getBoolean(DynamicParameters.SQ_SHOW_DEFAULT_FIELD);
        s.showStyles = config.getBoolean(DynamicParameters.SQ_SHOW_STYLES);
        s.showMaxHits = config.getBoolean(DynamicParameters.SQ_SHOW_MAX_HITS);
        s.showPriority = config.getBoolean(DynamicParameters.SQ_SHOW_PRIORITY);
        s.showNotes = config.getBoolean(DynamicParameters.SQ_SHOW_NOTES);
        return s;
    }

    private class ShowSettings {
        boolean showMainQueryTranslation;
        boolean showFilterQuery;
        boolean showFilterQueryTranslation;
        boolean showMaxHits;
        boolean showDefaultField;
        boolean showPriority;
        boolean showStyles;
        boolean showNotes;
    }
}

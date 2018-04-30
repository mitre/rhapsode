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
import org.rhapsode.app.session.DynamicParameters;
import org.rhapsode.app.session.StoredQueryWriter;
import org.rhapsode.app.utils.StoredQCUtils;
import org.rhapsode.lucene.search.ComplexQuery;
import org.rhapsode.lucene.search.ComplexQueryBuilder;
import org.rhapsode.lucene.search.SCField;
import org.rhapsode.lucene.search.SQField;
import org.rhapsode.lucene.search.StoredConcept;
import org.rhapsode.lucene.search.StoredQuery;
import org.rhapsode.lucene.search.StoredQueryBuilder;
import org.rhapsode.util.LanguageDirection;
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
import java.nio.file.Paths;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class ConceptHandler extends AdminHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ConceptHandler.class);

    private final long MAX_WAIT_FOR_COUNTS = 20000;//20 seconds
    private final RhapsodeSearcherApp searcherApp;

    public ConceptHandler(RhapsodeSearcherApp searcherApp) {
        super("Concepts");
        this.searcherApp = searcherApp;
    }


    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest,
                       HttpServletResponse response) throws IOException, ServletException {
        RhapsodeXHTMLHandler xhtml = null;
        ConceptRequest cr = ConceptRequest.build(httpServletRequest);
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
            Map<String, Integer> docCounts = new HashMap<>();
            switch (cr.getActionType()) {
                case ADD_DIALOGUE:
                    writeAddConceptDialogue(LanguageDirection.LTR, cr, errorMessage, xhtml);
                    break;
                case ADD_CONCEPT:
                    try {
                        addConcept(cr);
                    } catch (Exception e) {
                        errorMessage = e.getMessage();
                        if (errorMessage == null) {
                            errorMessage = "unknown error while trying to load concept";
                        }
                        writeAddConceptDialogue(LanguageDirection.LTR, cr,
                                "While parsing concept query: " + e.getMessage(), xhtml);
                        e.printStackTrace();
                    }
                    break;
                case DELETE:
                    try {
                        deleteConcepts();
                    } catch (Exception e) {
                        errorMessage = e.getMessage();
                    }
                case UPDATE_DOC_COUNTS:
                    if (searcherApp.getRhapsodeCollection() == null) {
                        errorMessage = "Need to load new collection";
                    } else {
                        try {
                            docCounts = updateCounts();
                        } catch (IOException e) {
                            errorMessage = "IOException during search ?!";
                        } catch (ParseException e) {
                            errorMessage = "Failed to parse: " + e.getMessage();
                        }
                    }
            }
            if (errorMessage != null) {
                RhapsodeDecorator.writeErrorMessage(errorMessage, xhtml);
            }
            writeConceptsTable(docCounts, xhtml);
            if (!cr.getActionType().equals(ConceptRequest.ActionType.ADD_DIALOGUE)) {
                writeDefaultConcept(LanguageDirection.LTR, xhtml);
            }
            xhtml.endElement(H.FORM);
            RhapsodeDecorator.addFooter(xhtml);
            xhtml.endDocument();

        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e);
        }

    }

    private Map<String, Integer> updateCounts() throws SQLException, IOException, ParseException {
        Map<String, StoredConcept> conceptMap = searcherApp.getSessionManager().
                getStoredConceptManager().getConceptMap();
        Map<Integer, Query> queries = new HashMap<>();
        Map<Integer, String> nameIdMap = new HashMap<>();
        int i = 0;
        for (Map.Entry<String, StoredConcept> e : conceptMap.entrySet()) {
            int id = i++;
            nameIdMap.put(id, e.getKey());
            StoredQueryBuilder storedQueryBuilder = new StoredQueryBuilder(id, e.getKey());
            storedQueryBuilder.add(SQField.DEFAULT_FIELD,
                    searcherApp.getStringParameter(DynamicParameters.DEFAULT_CONTENT_FIELD));
            storedQueryBuilder.add(SQField.MAIN_QUERY, e.getValue().getConceptQuery());
            StoredQuery sq = storedQueryBuilder.build();
            try {
                ComplexQuery cq = ComplexQueryBuilder.buildQuery(
                        sq,
                        searcherApp.getParserPlugin(),
                        searcherApp.getSessionManager().getStoredConceptManager(),
                        searcherApp.getGeoConfig(),
                        searcherApp.getRhapsodeCollection().getIgnoredQuery()
                );
                if (cq != null && cq.getRetrievalQuery() != null) {
                    queries.put(id, cq.getRetrievalQuery());
                }
            } catch (ParseException ex) {
                searcherApp.getSessionManager().getStoredQueryManager().updateQueryExceptions(sq, searcherApp);
            }
        }
        HitCounter hc = new HitCounter();
        Map<Integer, Integer> results = hc.count(queries, searcherApp.getRhapsodeCollection().getIndexManager().getSearcher(),
                searcherApp.getCommonSearchConfig().getNumThreadsForConcurrentSearches(), MAX_WAIT_FOR_COUNTS);

        //now map the results to the concept name
        Map<String, Integer> mappedResults = new HashMap<>();
        for (Map.Entry<Integer, Integer> e : results.entrySet()) {
            mappedResults.put(nameIdMap.get(e.getKey()), e.getValue());
        }
        return mappedResults;
    }

    private void deleteConcepts() throws Exception {
        Path trashDir = Paths.get("resources/trash");
        DateFormat f = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
        String dString = f.format(new Date());
        Path outFile = trashDir.resolve("concepts_" + dString + ".xlsx");
        Files.createDirectories(trashDir);
        StoredQueryWriter w = new StoredQueryWriter(searcherApp);
        w.writeStoredConcepts(outFile);
        searcherApp.getSessionManager().getStoredConceptManager().deleteConcepts();
        searcherApp.getSessionManager().getStoredQueryManager().validateQueries(searcherApp);
    }

    private void writeConceptsTable(Map<String, Integer> docCounts, RhapsodeXHTMLHandler xhtml)
            throws SQLException, SAXException {
        Map<String, StoredConcept> concepts = searcherApp.getSessionManager().getStoredConceptManager().getConceptMap();
        if (concepts.size() == 0) {
            xhtml.characters("The are currently no stored concepts.");
            xhtml.br();
            return;
        }
        if (concepts.size() > 0) {
            xhtml.startElement(H.TABLE,
                    H.BORDER, "2");
        }
        xhtml.startElement(H.TR);
        for (SCField field : SCField.values()) {
            if (field.equals(SCField.CONCEPT_EXCEPTION_MSG)) {
                continue;
            }
            xhtml.element(H.TH, field.getXlsxName());
        }
        xhtml.element(H.TH, "Document Counts");
        xhtml.endElement(H.TR);
        for (StoredConcept sc : concepts.values()) {
            xhtml.startElement(H.TR);
            for (SCField field : SCField.values()) {
                if (field.equals(SCField.CONCEPT_EXCEPTION_MSG)) {
                    continue;
                }
                xhtml.startElement(H.TD);
                if (field.equals(SCField.CONCEPT_QUERY) &&
                        !StringUtils.isBlank(sc.getConceptExcMsg())) {
                    xhtml.startElement(H.SPAN, H.CLASS, CSS.ERROR_MSG);
                    xhtml.characters(sc.getConceptQuery());
                    xhtml.endElement(H.SPAN);
                } else {
                    xhtml.characters(sc.getString(field));
                }
                xhtml.endElement(H.TD);
            }
            if (docCounts != null) {
                Integer val = docCounts.get(sc.getConceptName());
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

    private void addConcept(ConceptRequest cr) throws ParseException, SQLException {
        String name = testBlank("name", cr.getConceptFields().get(SCField.NAME));
        name = name.trim();
        name = name.toLowerCase(Locale.ENGLISH);

        testAlphanumeric(name);
        cr.getConceptFields().put(SCField.NAME, name);
        StoredConcept sc = StoredQCUtils.buildStoredConcept(cr.getConceptFields());
        ComplexQueryBuilder.validate(searcherApp.getRhapsodeCollection().getIndexSchema().getDefaultContentField(),
                sc.getConceptQuery(),
                searcherApp.getParserPlugin(),
                searcherApp.getSessionManager().getStoredConceptManager());
        try {
            searcherApp.getSessionManager()
                    .getStoredConceptManager()
                    .addConcept(sc);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains(SQL.ALREADY_EXISTS)) {
                throw new SQLException("A stored query with that name already exists.");
            }
        }
        searcherApp.getSessionManager().getStoredQueryManager().validateQueries(searcherApp);
    }

    public static void writeDefaultConcept(LanguageDirection ltr, RhapsodeXHTMLHandler xhtml) throws SAXException {
        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.ADD_CONCEPT_DIALOGUE,
                H.VALUE, "Add Concept");
        xhtml.endElement(H.INPUT);

        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.DELETE_CONCEPTS,
                H.VALUE, "Delete Concepts");
        xhtml.endElement(H.INPUT);

        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.UPDATE_DOCUMENT_COUNTS,
                H.VALUE, "Update Document Counts");
        xhtml.endElement(H.INPUT);
        ;
    }

    public static void writeAddConceptDialogue(LanguageDirection direction, ConceptRequest cr, String errorMessage,
                                               RhapsodeXHTMLHandler xhtml) throws SAXException {
        AttributesImpl scAttrs = new AttributesImpl();
        scAttrs.addAttribute("", H.TYPE, H.TYPE, "", H.TEXT);
        scAttrs.addAttribute("", H.NAME, H.NAME, "", C.CONCEPT_NAME);
        scAttrs.addAttribute("", H.SIZE, H.SIZE, "", Integer.toString(40));
        if (cr != null && cr.getConceptFields().get(SCField.NAME) != null) {
            scAttrs.addAttribute("", H.VALUE, H.VALUE, "", cr.getConceptFields().get(SCField.NAME));
        }

        if (!StringUtils.isBlank(errorMessage)) {
            xhtml.startElement(H.SPAN, H.CLASS, CSS.ERROR_MSG);
            xhtml.characters(errorMessage);
            xhtml.endElement(H.SPAN);
            xhtml.br();
        }
        xhtml.startElement(H.TABLE, H.BORDER, "2");

        xhtml.startElement(H.TR);
        xhtml.element(H.TD, "Stored Concept Name: ");
        xhtml.startElement(H.TD);
        xhtml.startElement(H.INPUT, scAttrs);
        xhtml.endElement(H.INPUT);
        xhtml.endElement(H.TD);
        xhtml.endElement(H.TR);

        xhtml.startElement(H.TR);
        xhtml.element(H.TD, "Concept Query: ");
        xhtml.startElement(H.TD);
        xhtml.startElement(H.DIV);
        xhtml.startElement(H.TEXT_AREA,
                H.NAME, C.CONCEPT_QUERY,
                H.ROWS, Integer.toString(5),
                H.COLS, Integer.toString(40));
        if (cr != null) {
            xhtml.characters(cr.getConceptFields().get(SCField.CONCEPT_QUERY));
        }
        xhtml.endElement(H.TEXT_AREA);
        xhtml.endElement(H.DIV);
        xhtml.endElement(H.TD);
        xhtml.endElement(H.TR);

        xhtml.startElement(H.TR);
        xhtml.element(H.TD, "Concept Translation: ");
        xhtml.startElement(H.TD);
        xhtml.startElement(H.DIV);
        xhtml.startElement(H.TEXT_AREA,
                H.NAME, C.CONCEPT_TRANSLATION,
                H.ROWS, Integer.toString(5),
                H.COLS, Integer.toString(40));
        if (cr != null) {
            xhtml.characters(cr.getConceptFields().get(SCField.CONCEPT_TRANSLATION));
        }
        xhtml.endElement(H.TEXT_AREA);
        xhtml.endElement(H.DIV);
        xhtml.endElement(H.TD);
        xhtml.endElement(H.TR);


        xhtml.element(H.TD, "NOTES: ");
        xhtml.startElement(H.TD);
        xhtml.startElement(H.DIV);
        xhtml.startElement(H.TEXT_AREA,
                H.NAME, C.CONCEPT_NOTES,
                H.ROWS, Integer.toString(5),
                H.COLS, Integer.toString(40));
        if (cr != null) {
            xhtml.characters(cr.getConceptFields().get(SCField.NOTES));
        }
        xhtml.endElement(H.TEXT_AREA);
        xhtml.endElement(H.DIV);
        xhtml.endElement(H.TD);
        xhtml.endElement(H.TR);
        xhtml.endElement(H.TABLE);

        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.ADD_CONCEPT,
                H.VALUE, "Add",
                "default", "");
        xhtml.endElement(H.INPUT);

        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.CANCEL,
                H.VALUE, "Cancel");
        xhtml.endElement(H.INPUT);
    }


}

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

package org.rhapsode.app.handlers.indexer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.FieldType;
import org.eclipse.jetty.server.Request;
import org.rhapsode.RhapsodeCollection;
import org.rhapsode.app.RhapsodeSearcherApp;
import org.rhapsode.app.contants.C;
import org.rhapsode.app.contants.H;
import org.rhapsode.app.decorators.RhapsodeDecorator;
import org.rhapsode.app.decorators.RhapsodeXHTMLHandler;
import org.rhapsode.app.handlers.search.AbstractSearchHandler;
import org.rhapsode.app.io.AbstractTableReader;
import org.rhapsode.app.io.AbstractWorkbookTableReader;
import org.rhapsode.app.io.CSVTableReader;
import org.rhapsode.app.io.EncodingDelimiterPair;
import org.rhapsode.app.io.HeaderReader;
import org.rhapsode.app.io.TableReaderException;
import org.rhapsode.app.io.XLSBStreamingTableReader;
import org.rhapsode.app.io.XLSTableReader;
import org.rhapsode.app.io.XLSXStreamingTableReader;
import org.rhapsode.app.session.DynamicParameters;
import org.rhapsode.app.tasks.RhapsodeTaskStatus;
import org.rhapsode.app.tasks.TableIndexerTask;
import org.rhapsode.app.tasks.Tasker;
import org.rhapsode.app.utils.DBException;
import org.rhapsode.lucene.schema.FieldDef;
import org.rhapsode.lucene.schema.IdentityFieldMapper;
import org.rhapsode.lucene.schema.IndexSchema;
import org.rhapsode.lucene.schema.NamedAnalyzer;
import org.rhapsode.lucene.search.StoredQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;


public class TableFileHandler extends AbstractSearchHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TableFileHandler.class);

    private final RhapsodeSearcherApp searcherApp;
    private final TableFileRequestBuilder requestBuilder;

    public TableFileHandler(RhapsodeSearcherApp searcherApp) {
        super("CSV/XLS/XLSX");
        this.searcherApp = searcherApp;
        requestBuilder = new TableFileRequestBuilder();
    }

    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest,
                       HttpServletResponse response) throws IOException, ServletException {

        init(request, response);

        RhapsodeXHTMLHandler xhtml = null;
        try {
            xhtml = initResponse(response, null);
        } catch (SAXException e) {
            LOG.error("prob with init", e);
            throw new IOException(e);
        }

        RhapsodeTaskStatus status = searcherApp.getTaskStatus();
        if (status == null || status.getState() == Tasker.STATE.COMPLETED) {
            //all is well
        } else {
            try {
                xhtml.element("p", "There is already one task running in the background");
                xhtml.element("p", "Please wait for it to finish:");
                xhtml.element("p", status.getMessage());
                xhtml.endDocument();
            } catch (SAXException e) {
                e.printStackTrace();
            }
            return;
        }
        String errorMsg = null;
        TableFileRequest tableFileRequest = new TableFileRequest();
        try {
            requestBuilder.extract(searcherApp, httpServletRequest, tableFileRequest);
        } catch (IllegalArgumentException e) {
            errorMsg = e.getMessage();
            tableFileRequest.actionType = TableFileRequest.ActionType.SELECT_ENCODING_DELIMITER;
        }
        try {
            xhtml.startElement(H.TABLE);
            String pathDir =
                    searcherApp.getSessionManager().getDynamicParameterConfig()
                            .getString(DynamicParameters.TFI_TABLE_DIRECTORY);
            if (pathDir != null) {
                xhtml.startElement(H.TR);
                xhtml.startElement(H.TD);
                xhtml.characters("Input File Directory:");
                xhtml.endElement(H.TD);
                xhtml.startElement(H.TD);
                xhtml.characters(pathDir);
                xhtml.endElement(H.TD);
                xhtml.endElement(H.TR);
            }
            if (!StringUtils.isBlank(tableFileRequest.collectionName)) {
                xhtml.startElement(H.TR);
                xhtml.startElement(H.TD);
                xhtml.characters("Collection Name:");
                xhtml.endElement(H.TD);
                xhtml.startElement(H.TD);
                xhtml.characters(tableFileRequest.collectionName);
                xhtml.endElement(H.TD);
                xhtml.endElement(H.TR);
            }
            if (!StringUtils.isBlank(tableFileRequest.inputFileName)) {
                xhtml.startElement(H.TR);
                xhtml.startElement(H.TD);
                xhtml.characters("File:");
                xhtml.endElement(H.TD);
                xhtml.startElement(H.TD);
                xhtml.characters(tableFileRequest.inputFileName);
                xhtml.endElement(H.TD);
                xhtml.endElement(H.TR);
            }
            if (!StringUtils.isBlank(tableFileRequest.worksheetName)) {
                xhtml.startElement(H.TR);
                xhtml.startElement(H.TD);
                xhtml.characters("Worksheet Name:");
                xhtml.endElement(H.TD);
                xhtml.startElement(H.TD);
                xhtml.characters(tableFileRequest.worksheetName);
                xhtml.endElement(H.TD);
                xhtml.endElement(H.TR);
            }
            if (!StringUtils.isBlank(errorMsg)) {
                RhapsodeDecorator.writeWarnMessage(errorMsg, xhtml);
            }
            switch (tableFileRequest.actionType) {
                case SELECT_COLLECTION_NAME:
                    selectCollectionName(tableFileRequest, searcherApp, xhtml);
                    break;
                case SELECT_INPUT_FILE:
                    selectInputFileDialogue(tableFileRequest, searcherApp, xhtml);
                    break;
                case SELECT_ENCODING_DELIMITER:
                    selectEncodingDelimiter(tableFileRequest, searcherApp, xhtml);
                    break;
                case SELECT_WORKSHEET:
                    List<String> sheets = getSheets(tableFileRequest, searcherApp);
                    selectWorksheet(sheets, tableFileRequest, xhtml);
                    break;
                case SELECT_COLUMNS:
                    selectColumns(tableFileRequest, searcherApp, xhtml);
                    break;
                case START_INDEXING:
                    int luceneFields = 0;
                    for (FieldTypePair fieldTypePair : tableFileRequest.getFields().values()) {
                        if (! StringUtils.isBlank(fieldTypePair.luceneFieldName)) {
                            luceneFields++;
                        }
                    }
                    //if no fields have been selected
                    if (luceneFields == 0) {
                        selectColumns(tableFileRequest, searcherApp, xhtml);
                    } else {
                        buildIndex(tableFileRequest, searcherApp, xhtml);
                    }
                    break;
                case LOAD_COLLECTION:
                    loadCollection(tableFileRequest, searcherApp, xhtml);
                    break;
            }


            xhtml.endDocument();
        } catch (SAXException e) {
            LOG.error("sax problem", e);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            xhtml.endElement(H.TABLE);
            RhapsodeDecorator.addFooter(xhtml);
            xhtml.endDocument();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        response.getOutputStream().flush();
    }

    private void selectEncodingDelimiter(TableFileRequest tableFileRequest,
                                         RhapsodeSearcherApp searcherApp,
                                         RhapsodeXHTMLHandler xhtml) throws Exception {
        Path inputDir = validateInputDirectory(searcherApp);
        Path file = inputDir.resolve(tableFileRequest.inputFileName);
        EncodingDelimiterPair encodingDelimiterPair = null;
        try {
            encodingDelimiterPair = CSVTableReader.guessEncodingDelimiter(file);
        } catch (Exception e) {
            LOG.warn("exception trying to guess encoding and delimiter", e);
            return;
        }
        xhtml.startElement(H.FORM, H.METHOD, H.POST);

        RhapsodeDecorator.writeQueryBox("CharacterEncoding",
                C.TABLE_ENCODING, encodingDelimiterPair.getEncoding(),
                1, 20,
                searcherApp.getSessionManager().getDynamicParameterConfig()
                        .getLanguageDirection(DynamicParameters.DEFAULT_LANGUAGE_DIRECTION),
                xhtml);
        xhtml.br();
        StringBuilder sb = new StringBuilder();
        if (encodingDelimiterPair.getDelimiter() != null) {
            if (encodingDelimiterPair.getDelimiter().equals('\t')) {
                sb.append("<tab>");
            } else {
                sb.append(encodingDelimiterPair.getDelimiter());
            }
        }
        RhapsodeDecorator.writeQueryBox("Delimiter",
                C.TABLE_DELIMITER, sb.toString(),
                1, 20,
                searcherApp.getSessionManager().getDynamicParameterConfig()
                        .getLanguageDirection(DynamicParameters.DEFAULT_LANGUAGE_DIRECTION),
                xhtml);

        xhtml.br();
        writeTableHasHeaders(xhtml);

        xhtml.startElement(H.INPUT,
                H.TYPE, H.HIDDEN,
                H.NAME, C.COLLECTION_NAME,
                H.VALUE, tableFileRequest.collectionName);
        xhtml.endElement(H.INPUT);

        xhtml.startElement(H.INPUT,
                H.TYPE, H.HIDDEN,
                H.NAME, C.TABLE_FILE_NAME,
                H.VALUE, tableFileRequest.inputFileName);
        xhtml.endElement(H.INPUT);

        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.SELECT_COLLECTION_NAME,
                H.VALUE, "Start Over",
                "default", "");

        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.SELECT_TABLE_COLUMNS,
                H.VALUE, "Next",
                "default", "");

        xhtml.endElement(H.FORM);

    }

    private String loadCollection(TableFileRequest tableFileRequest, RhapsodeSearcherApp searcherApp, RhapsodeXHTMLHandler xhtml) {
        Path p = Paths.get(searcherApp.getStringParameter(DynamicParameters.COLLECTIONS_ROOT));
        Path collectionPath = p.resolve(tableFileRequest.collectionName);
        try {
            searcherApp.tryToLoadRhapsodeCollection(collectionPath);
            searcherApp.getSessionManager().getCollectionsHistory().addLoaded(p);
            searcherApp.getSessionManager().getStoredQueryManager().validateQueries(searcherApp);
            String defaultContentField = searcherApp.getRhapsodeCollection().getIndexSchema().getDefaultContentField();
            if (!StringUtils.isBlank(defaultContentField)) {
                searcherApp.getSessionManager().getDynamicParameterConfig().update(DynamicParameters.DEFAULT_CONTENT_FIELD, defaultContentField);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "IOException " + e.getMessage();
        }
        return null;
    }

    private void selectWorksheet(List<String> sheets,
                                 TableFileRequest tableFileRequest,
                                 RhapsodeXHTMLHandler xhtml) throws SAXException {
        xhtml.startElement(H.FORM, H.METHOD, H.POST);
        xhtml.startElement(H.TABLE);
        if (sheets.size() > 1) {
            for (String sheet : sheets) {
                xhtml.startElement(H.TR);
                xhtml.element(H.TD, sheet);
                xhtml.startElement(H.TD);
                xhtml.startElement(H.INPUT,
                        H.TYPE, H.RADIO,
                        H.NAME, C.TABLE_WORKSHEET_NAME,
                        H.VALUE, sheet);
                xhtml.endElement(H.TD);

                xhtml.endElement(H.TR);
            }
        } else {
            xhtml.startElement(H.TR);
            xhtml.element(H.TD, "Sheet:");
            xhtml.element(H.TD, sheets.get(0));
            xhtml.endElement(H.TR);
        }

        xhtml.endElement(H.TABLE);

        writeTableHasHeaders(xhtml);

        xhtml.startElement(H.INPUT,
                H.TYPE, H.HIDDEN,
                H.NAME, C.COLLECTION_NAME,
                H.VALUE, tableFileRequest.collectionName);
        xhtml.endElement(H.INPUT);

        xhtml.startElement(H.INPUT,
                H.TYPE, H.HIDDEN,
                H.NAME, C.TABLE_FILE_NAME,
                H.VALUE, tableFileRequest.inputFileName);
        xhtml.endElement(H.INPUT);

        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.SELECT_COLLECTION_NAME,
                H.VALUE, "Start Over",
                "default", "");

        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.SELECT_TABLE_COLUMNS,
                H.VALUE, "Next",
                "default", "");

        xhtml.endElement(H.FORM);
    }

    private void writeTableHasHeaders(RhapsodeXHTMLHandler xhtml) throws SAXException {
        xhtml.br();
        xhtml.startElement(H.TABLE);
        xhtml.startElement(H.TR);
        xhtml.element(H.TD, "Table has headers");
        xhtml.startElement(H.TD);
        xhtml.startElement(H.INPUT,
                H.TYPE, H.RADIO,
                H.NAME, C.TABLE_HAS_HEADERS,
                H.VALUE, "true");
        xhtml.endElement(H.INPUT);
        xhtml.endElement(H.TD);

        xhtml.element(H.TD, "Table does not have headers");
        xhtml.startElement(H.TD);
        xhtml.startElement(H.INPUT,
                H.TYPE, H.RADIO,
                H.NAME, C.TABLE_HAS_HEADERS,
                H.VALUE, "false");
        xhtml.endElement(H.INPUT);
        xhtml.endElement(H.TD);
        xhtml.endElement(H.TR);
        xhtml.endElement(H.TABLE);
    }

    private List<String> getSheets(TableFileRequest tableFileRequest,
                                   RhapsodeSearcherApp searcherApp) throws TableReaderException {
        Path inputDir = validateInputDirectory(searcherApp);
        Path file = inputDir.resolve(tableFileRequest.inputFileName);
        AbstractWorkbookTableReader reader = null;
        List<String> sheets = null;
        try {
            if (file.toString().endsWith(".xlsx") || file.toString().endsWith("xlsm")) {
                reader = new XLSXStreamingTableReader(file, null, null, true);
            } else if (file.toString().endsWith(".xlsb")) {
                reader = new XLSBStreamingTableReader(file, null, null, true);
            } else if (file.toString().endsWith(".xls")) {
                reader = new XLSTableReader(file, null, null, true);
            } else {
                throw new IllegalArgumentException("Not sure how we got here; file name should end in xls, xlsx or xlsm: " + tableFileRequest.inputFileName);
            }
            sheets = reader.getSheets();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    throw new TableReaderException("IOException closing reader", e);
                }
            }
        }
        return sheets;
    }

    private void selectCollectionName(TableFileRequest tableFileRequest,
                                      RhapsodeSearcherApp searcherApp,
                                      RhapsodeXHTMLHandler xhtml) throws SAXException {
        xhtml.startElement(H.FORM, H.METHOD, H.POST);
        xhtml.element(H.P, "Select Collection");
        RhapsodeDecorator.writeQueryBox("Collection Name",
                C.COLLECTION_NAME, "",
                1, 80,
                searcherApp.getSessionManager().getDynamicParameterConfig()
                        .getLanguageDirection(DynamicParameters.DEFAULT_LANGUAGE_DIRECTION),
                xhtml);
        xhtml.br();

        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.SELECT_TABLE_FILE,
                H.VALUE, "Next",
                "default", "");
        xhtml.endElement(H.INPUT);
        xhtml.br();
        xhtml.startElement(H.INPUT,
                H.TYPE, H.BUTTON,
                H.NAME, C.RESTART_TABLE_LOADING_DIALOGUE,
                H.VALUE, "Start Over");
        xhtml.endElement(H.INPUT);


        xhtml.endElement(H.FORM);

    }

    private void buildIndex(TableFileRequest tableFileRequest,
                            RhapsodeSearcherApp searcherApp,
                            RhapsodeXHTMLHandler xhtml) throws Exception {

        Path inputTableFile = validateInputDirectory(searcherApp).resolve(tableFileRequest.inputFileName);
        Path collectionPath = validateCollectionDirectory(searcherApp).resolve(tableFileRequest.collectionName);
        //check p
        Path tmpSchemaPath = buildTempSchema(tableFileRequest);
        RhapsodeCollection rc = RhapsodeCollection.build(Paths.get(""), collectionPath, tmpSchemaPath);
        TableIndexerTask tableIndexerTask = new TableIndexerTask(rc, inputTableFile, tableFileRequest);
        try {
            searcherApp.startTask(tableIndexerTask);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } finally {
            Files.delete(tmpSchemaPath);
        }
        xhtml.element(H.P, "Indexer has been started.");
        xhtml.startElement(H.P);
        xhtml.characters("Please navigate to ");
        xhtml.href("/rhapsode/tasks", "tasks");
        xhtml.characters(" for the current status");
        xhtml.endElement(H.P);
/*        long elapsed = System.currentTimeMillis()-start;

        xhtml.startElement(H.P);
        xhtml.characters("Successfully indexed "+perRowIndexer.getRowsRead() + " rows");
        xhtml.br();
        xhtml.characters("from: ");
        xhtml.br();
        xhtml.characters(inputTableFile.toAbsolutePath().toString());
        xhtml.br();
        xhtml.characters(" for collection:" + tableFileRequest.collectionName);
        xhtml.br();
        xhtml.characters("in "+elapsed+" milliseconds.");
        xhtml.startElement(H.FORM, H.METHOD, H.POST);



            xhtml.startElement(H.INPUT,
                    H.TYPE, H.SUBMIT,
                    H.NAME, C.OPEN_NEW_COLLECTION,
                    H.VALUE, "Load Collection");
        xhtml.endElement(H.INPUT);
        xhtml.startElement(H.INPUT,
                H.TYPE, H.HIDDEN,
                H.NAME, C.COLLECTION_NAME,
                H.VALUE, tableFileRequest.collectionName);
        xhtml.endElement(H.INPUT);

        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.RESTART_TABLE_LOADING_DIALOGUE,
                H.VALUE, "Start Over");
        xhtml.endElement(H.INPUT);
        xhtml.endElement(H.FORM);
        */

    }

    private Path buildTempSchema(TableFileRequest tableFileRequest) throws IOException {
        Path tmp = Files.createTempFile("tmp-table-reader-index-schema", ".json");
        FieldType ft = new FieldType();
        ft.setTokenized(true);
        ft.setStored(true);

        FieldType linkDisplayFieldType = new FieldType();
        linkDisplayFieldType.setTokenized(false);
        linkDisplayFieldType.setStored(true);

        try (InputStream is = Files.newInputStream(Paths.get("resources/config/default_table_index_schema.json"))) {
            IndexSchema indexSchema = IndexSchema.load(is);
            indexSchema.clearUserFields();
            indexSchema.getFieldMapper().clear();

            for (Map.Entry<String, FieldTypePair> e : tableFileRequest.fields.entrySet()) {

                String luceneFieldName = e.getValue().luceneFieldName;
                if (StringUtils.isBlank(luceneFieldName)) {
                    continue;
                }
                FieldDef fieldDef = new FieldDef(luceneFieldName, true, ft);
                fieldDef.setAnalyzers(new NamedAnalyzer("text_general", null), null, null, null);

                indexSchema.addField(luceneFieldName, fieldDef);
                indexSchema.getFieldMapper().add(luceneFieldName, new IdentityFieldMapper(luceneFieldName));
                if (e.getValue().isLinkField) {
                    String linkField = indexSchema.getLinkDisplayField();
                    FieldDef linkFieldDef = new FieldDef(linkField, false, linkDisplayFieldType);
                    indexSchema.addField(linkField, linkFieldDef);
                    indexSchema.getFieldMapper().add(luceneFieldName, new IdentityFieldMapper(linkField));
                }
                if (e.getValue().isDefaultContent) {
                    indexSchema.setDefaultContentField(luceneFieldName);
                }
            }
            try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(tmp, StandardOpenOption.CREATE))) {
                IndexSchema.write(indexSchema, os);
            }
        }
        return tmp;
    }

    private void selectColumns(TableFileRequest tableFileRequest,
                               RhapsodeSearcherApp searcherApp,
                               RhapsodeXHTMLHandler xhtml) throws Exception {
        Path dir = validateInputDirectory(searcherApp);
        String fileName = tableFileRequest.inputFileName;
        List<String> headers;
        HeaderReader headerReader = new HeaderReader(tableFileRequest.getTableHasHeaders());
        String worksheetName = tableFileRequest.worksheetName;
        AbstractTableReader reader = null;
        //open all initially as if they do have headers to capture that first value
        if (fileName.endsWith(".xlsx") || fileName.endsWith(".xlsm")) {
            reader = new XLSXStreamingTableReader(dir.resolve(fileName), worksheetName,
                    headerReader,
                    true);
        } else if (fileName.endsWith(".xlsb")) {
                reader = new XLSBStreamingTableReader(dir.resolve(fileName), worksheetName,
                        headerReader, true);
        } else if (fileName.endsWith(".xls")) {
            reader = new XLSTableReader(dir.resolve(fileName), worksheetName,
                    headerReader, true);
        } else if (fileName.endsWith(".txt") || fileName.endsWith(".csv")) {
            reader = new CSVTableReader(
                    dir.resolve(fileName), headerReader,
                    tableFileRequest.getCSVDelimiterChar(),
                    tableFileRequest.getCSVEncoding(), true);
        } else {
            LOG.error("Table file doesn't end with known suffix: xls, xlsm, xls, txt, csv");
            return;
        }
        reader.parse();
        headers = headerReader.getHeaders();

        Set<String> defaultFields = new HashSet<>();
        for (StoredQuery q : searcherApp.getSessionManager().getStoredQueryManager().getStoredQueryMap().values()) {
            String defaultField = q.getDefaultField();
            if (defaultField != null) {
                defaultFields.add(defaultField);
            }
        }

        if (defaultFields.size() > 0) {
            xhtml.br();
            String thisField = (defaultFields.size() == 1) ? "this field" : "these fields";
            xhtml.element("p", "Your stored queries currently require " + thisField + ":");
            for (String d : defaultFields) {
                xhtml.element("p", "\"" + d + "\"");
            }
        }
        xhtml.element(H.P, "Select Columns");

        xhtml.startElement(H.FORM, H.METHOD, H.POST);
        xhtml.startElement(H.TABLE);
        xhtml.startElement(H.TR);
        xhtml.startElement(H.TD);
        if (tableFileRequest.getTableHasHeaders()) {
            xhtml.characters("Column Name");
        } else {
            xhtml.characters("First Row Values");
        }
        xhtml.endElement(H.TD);
        xhtml.startElement(H.TD);
        xhtml.characters("Lucene Field Name");
        xhtml.endElement(H.TD);
        xhtml.startElement(H.TD);
        xhtml.characters("Is Display Link");
        xhtml.endElement(H.TD);
        xhtml.startElement(H.TD);
        xhtml.characters("Default Content");
        xhtml.endElement(H.TD);
        xhtml.endElement(H.TR);
        int i = 0;
        for (String h : headers) {
            xhtml.startElement(H.TR);
            xhtml.startElement(H.TD);
            xhtml.characters(h);
            xhtml.endElement(H.TD);
            xhtml.startElement(H.TD);
            AttributesImpl attrs = new AttributesImpl();
            attrs.addAttribute("", H.TYPE, H.TYPE, "", H.TEXT);
            String headerKey = tableFileRequest.tableHasHeaders ? TableFileRequest.COL_HEADER_PREFIX+h:
                    TableFileRequest.COL_HEADER_PREFIX+AbstractTableReader.getNonHeaderLabel(i);
            attrs.addAttribute("", H.NAME, H.NAME, "", headerKey);
//TODO:            attrs.addAttribute("", H.DIRECTION, H.DIRECTION, "", direction.name().toLowerCase());
            attrs.addAttribute("", H.SIZE, H.SIZE, "", "40");
            if (tableFileRequest.tableHasHeaders) {
                attrs.addAttribute("", H.VALUE, H.VALUE, "", reduceColHeaders(h));
            } else {
                attrs.addAttribute("", H.VALUE, H.VALUE, "", "");
            }
            xhtml.startElement(H.INPUT, attrs);
            xhtml.endElement(H.INPUT);
            xhtml.endElement(H.TD);

            xhtml.startElement(H.TD);
            xhtml.startElement(H.INPUT,
                    H.TYPE, H.RADIO,
                    H.NAME, C.TABLE_COL_IS_LINK,
                    H.VALUE, h);
            xhtml.endElement(H.TD);

            xhtml.startElement(H.TD);
            xhtml.startElement(H.INPUT,
                    H.TYPE, H.RADIO,
                    H.NAME, C.TABLE_COL_IS_CONTENT,
                    H.VALUE, h);
            xhtml.endElement(H.TD);

            xhtml.endElement(H.TR);
        }
        xhtml.endElement(H.TABLE);
        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.START_INDEXER,
                H.VALUE, "Next",
                "default", "");
        xhtml.endElement(H.INPUT);

        xhtml.startElement(H.INPUT,
                H.TYPE, H.HIDDEN,
                H.NAME, C.TABLE_WORKSHEET_NAME,
                H.VALUE, tableFileRequest.worksheetName);
        xhtml.endElement(H.INPUT);

        xhtml.startElement(H.INPUT,
                H.TYPE, H.HIDDEN,
                H.NAME, C.TABLE_FILE_NAME,
                H.VALUE, tableFileRequest.inputFileName);
        xhtml.endElement(H.INPUT);

        xhtml.startElement(H.INPUT,
                H.TYPE, H.HIDDEN,
                H.NAME, C.COLLECTION_NAME,
                H.VALUE, tableFileRequest.collectionName);
        xhtml.endElement(H.INPUT);
        xhtml.startElement(H.INPUT,
                H.TYPE, H.HIDDEN,
                H.NAME, C.TABLE_HAS_HEADERS,
                H.VALUE, Boolean.toString(tableFileRequest.tableHasHeaders));
        xhtml.endElement(H.INPUT);
        xhtml.br();
        if (tableFileRequest.inputFileName.endsWith(".csv") ||
                tableFileRequest.inputFileName.endsWith(".txt")) {
            xhtml.startElement(H.INPUT,
                    H.TYPE, H.SUBMIT,
                    H.NAME, C.SELECT_ENCODING_DELIMITER,
                    H.VALUE, "Back");
        } else {
            xhtml.startElement(H.INPUT,
                    H.TYPE, H.SUBMIT,
                    H.NAME, C.SELECT_WORKSHEET,
                    H.VALUE, "Back");
        }
        xhtml.endElement(H.INPUT);

        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.RESTART_TABLE_LOADING_DIALOGUE,
                H.VALUE, "Start Over");


        xhtml.endElement(H.FORM);
    }

    private String reduceColHeaders(String h) {
        if (h == null) {
            return "";
        }
        String col = h.trim().replaceAll("(?i)[^a-z0-9]", "_");
        col = col.toLowerCase(Locale.US);
        return col;
    }

    private void selectInputFileDialogue(TableFileRequest tableFileRequest,
                                         RhapsodeSearcherApp searcherApp,
                                         RhapsodeXHTMLHandler xhtml) throws SAXException {
        Path p = validateInputDirectory(searcherApp);
        xhtml.startElement(H.FORM, H.METHOD, H.POST);
        xhtml.element(H.P, "Select File");

        xhtml.startElement(H.TABLE);
        for (File f : p.toFile().listFiles()) {
            xhtml.startElement(H.TR);
            xhtml.startElement(H.TD);
            xhtml.characters(f.getName());
            xhtml.endElement(H.TD);
            xhtml.startElement(H.TD);
            if (f.getName().equals(tableFileRequest.inputFileName)) {
                xhtml.startElement(H.INPUT,
                        H.TYPE, H.RADIO,
                        H.NAME, C.TABLE_FILE_NAME,
                        H.VALUE, f.getName(),
                        H.CHECKED, H.CHECKED);
            } else {
                xhtml.startElement(H.INPUT,
                        H.TYPE, H.RADIO,
                        H.NAME, C.TABLE_FILE_NAME,
                        H.VALUE, f.getName());
            }
            xhtml.endElement(H.TD);
            xhtml.endElement(H.TR);
        }
        xhtml.endElement(H.TABLE);

        xhtml.startElement(H.INPUT,
                H.TYPE, H.HIDDEN,
                H.NAME, C.COLLECTION_NAME,
                H.VALUE, tableFileRequest.collectionName);
        xhtml.endElement(H.INPUT);

        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.SELECT_ENCODING_DELIMITER_WORKSHEET,
                H.VALUE, "Next",
                "default", "default");
        xhtml.br();

        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.RESTART_TABLE_LOADING_DIALOGUE,
                H.VALUE, "Start Over");
        xhtml.endElement(H.INPUT);

        xhtml.endElement(H.INPUT);

        xhtml.endElement(H.FORM);

    }

    private void updateInputDirectoryDialogue(TableFileRequest tableFileRequest,
                                              RhapsodeSearcherApp app,
                                              RhapsodeXHTMLHandler xhtml) throws SAXException {
        Path lastDir = app.getSessionManager().getTableManager().getInputDirectory();
        String lastDirString = "";
        if (lastDir != null) {
            lastDirString = lastDir.toAbsolutePath().toString();
        }
        xhtml.startElement(H.FORM, H.METHOD, H.POST);
        RhapsodeDecorator.writeQueryBox("Input Directory",
                C.TABLE_INPUT_DIRECTORY, lastDirString,
                1, 80, searcherApp.getSessionManager().getDynamicParameterConfig().getLanguageDirection(DynamicParameters.DEFAULT_LANGUAGE_DIRECTION),
                xhtml);
        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.SET_TABLE_INPUT_DIRECTORY,
                H.VALUE, "Set Input Directory",
                "default", "");
        xhtml.endElement(H.FORM);

    }

    private void updateInputDirectory(TableFileRequest tableFileRequest, RhapsodeSearcherApp searcherApp) {
        String pathString = tableFileRequest.inputDirectoryPath;
        if (StringUtils.isEmpty(pathString)) {
            throw new RequestException("Need to specify table root directory");
        }
        Path path = null;
        try {
            path = Paths.get(pathString);
        } catch (InvalidPathException e) {
            throw new RequestException("Must specify a valid path: " + e.getMessage());
        }
        if (!Files.isDirectory(path)) {
            throw new RequestException("I'm sorry, but this path doesn't yet exist:" + path.toAbsolutePath().toString());
        }
        try {
            searcherApp.getSessionManager().getDynamicParameterConfig().update(DynamicParameters.TFI_TABLE_DIRECTORY, pathString);
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    private Path validateInputDirectory(RhapsodeSearcherApp app) {
        String pathString = app.getSessionManager().getDynamicParameterConfig().getString(DynamicParameters.TFI_TABLE_DIRECTORY);
        if (StringUtils.isBlank(pathString)) {
            throw new RequestException("Empty path is stored for input directory");
        }
        Path path = null;
        try {
            path = Paths.get(pathString);
        } catch (InvalidPathException e) {
            throw new RequestException("Must specify a valid path: " + e.getMessage());
        }
        if (!Files.isDirectory(path)) {
            throw new RequestException("I'm sorry, but this path doesn't yet exist:" + path.toAbsolutePath().toString());
        }
        return path;
    }

    private Path validateCollectionDirectory(RhapsodeSearcherApp app) {
        String pathString = app.getSessionManager().getDynamicParameterConfig().getString(DynamicParameters.COLLECTIONS_ROOT);
        if (StringUtils.isBlank(pathString)) {
            throw new RequestException("Empty path is stored for collection directory");
        }
        Path path = null;
        try {
            path = Paths.get(pathString);
        } catch (InvalidPathException e) {
            throw new RequestException("Must specify a valid path for the collection directory: " + e.getMessage());
        }
        return path;
    }
}

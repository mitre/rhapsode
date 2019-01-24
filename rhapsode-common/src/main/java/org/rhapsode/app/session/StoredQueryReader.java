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

package org.rhapsode.app.session;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.rhapsode.app.io.XLSXTableReader;
import org.rhapsode.lucene.queryparsers.ParserPlugin;
import org.rhapsode.lucene.schema.IndexSchema;
import org.rhapsode.lucene.search.ComplexQueryBuilder;
import org.rhapsode.lucene.search.SCField;
import org.rhapsode.lucene.search.SQField;
import org.rhapsode.lucene.search.StoredConcept;
import org.rhapsode.lucene.search.StoredConceptBuilder;
import org.rhapsode.lucene.search.StoredQuery;
import org.rhapsode.lucene.search.StoredQueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;


public class StoredQueryReader {

    private static final Logger LOG = LoggerFactory.getLogger(StoredQueryReader.class);
    private final DBStoredConceptManager storedConceptManager;
    private final DBStoredQueryManager storedQueryManager;
    private final ParserPlugin parserPlugin;
    private final IndexSchema indexSchema;
    private final Connection connection;
    private String storedQueryWarningMsg;
    private String storedQueryErrorMsg;
    private String storedConceptErrorMsg;
    private String storedConceptWarningMsg;

    public StoredQueryReader(DBStoredConceptManager storedConceptManager,
                             DBStoredQueryManager storedQueryManager,
                             ParserPlugin parserPlugin, IndexSchema indexSchema,
                             Connection connection) {
        this.storedConceptManager = storedConceptManager;
        this.storedQueryManager = storedQueryManager;
        this.parserPlugin = parserPlugin;

        this.indexSchema = indexSchema;
        this.connection = connection;
    }


    public void loadBoth(InputStream is) throws Exception {
        XSSFWorkbook wb = XLSXTableReader.loadWorkbook(is);
        //need to load concepts first, because
        //they can be included in stored queries...for validation
        if (wb.getSheetIndex(StoredQueryWriter.CONCEPT_SHEET_NAME) < 0) {
            storedConceptWarningMsg = "Couldn't find sheet named: " +
                    StoredQueryWriter.CONCEPT_SHEET_NAME;
        } else {
            loadConcepts(new XLSXTableReader(wb, StoredQueryWriter.CONCEPT_SHEET_NAME));
        }
        if (wb.getSheetIndex(StoredQueryWriter.SQ_SHEET_NAME) < 0) {
            storedQueryWarningMsg = "Couldn't find sheet named: " +
                    StoredQueryWriter.SQ_SHEET_NAME;
        } else {
            loadStoredQueries(new XLSXTableReader(wb, StoredQueryWriter.SQ_SHEET_NAME));
        }
    }

    private void loadConcepts(XLSXTableReader reader) throws Exception {
        List<String> headers = new ArrayList<>();
        Map<String, String> cols = new HashMap<>();

        String[] data = reader.readNext();
        for (int i = 0; i < data.length; i++) {
            headers.add(data[i]);
        }
        int loaded = 0;
        data = reader.readNext();
        while (data != null) {
            cols.clear();
            for (int i = 0; i < data.length; i++) {
                cols.put(headers.get(i), data[i]);
            }
            loadStoredConcept(cols, loaded);
            loaded++;
            data = reader.readNext();
        }
        connection.commit();
        //after loading them, then validate.
        //can't validate per row because an earlier
        //concept may contain a later concept
        for (Map.Entry<String, StoredConcept> entry : storedConceptManager.getConceptMap().entrySet()) {
            try {
                StoredConcept sc = entry.getValue();
                ComplexQueryBuilder.validate(indexSchema.getDefaultContentField(),
                        sc.getConceptQuery(),
                        parserPlugin, storedConceptManager);
            } catch (ParseException e) {
                String msg = e.getMessage();
                msg = (StringUtils.isBlank(msg)) ? "Parse exception" : msg;
                storedConceptManager.updateQueryExceptionMessage(entry.getKey(), msg);
            }

        }
    }

    private void loadStoredConcept(Map<String, String> cols, int rowNum)
            throws StoredQCException {
        String name = cols.get(SCField.NAME.getXlsxName());
        if (StringUtils.isBlank(name)) {
            throw new StoredQCException("Need to specify a name in the " +
                    SCField.NAME.getXlsxName() + " column on row " + rowNum);
        }
        StoredConceptBuilder scBuilder = new StoredConceptBuilder(name);
        NumberFormat nf = new DecimalFormat("#");
        for (SCField field : SCField.values()) {
            if (field.equals(SCField.NAME))
                continue;

            if (cols.containsKey(field.getXlsxName())) {
                try {
                    String val = cols.get(field.getXlsxName());
                    //numbers are treated as floats in poi/excel
                    if (field.getType() == Types.INTEGER && !StringUtils.isBlank(val)) {
                        try {
                            double dval = Double.parseDouble(val);
                            val = nf.format(dval);
                        } catch (NumberFormatException e) {
                            //if something went wrong, so be it.
                            //there will be an error in scbuilder
                        }
                    }
                    scBuilder.add(field, val);
                } catch (Exception e) {
                    throw new StoredQCException(e.getMessage());
                }
            }
        }

        try {
            storedConceptManager.addConceptNoCommit(scBuilder.build());
        } catch (SQLException e) {
            LOG.error("couldn't add concept", e);
            throw new RuntimeException(e);
        }

    }


    private void loadStoredQueries(XLSXTableReader reader) throws SAXException, IOException, SQLException {

        List<String> headers = new ArrayList<>();
        Map<String, String> cols = new HashMap<>();

        String[] data = reader.readNext();
        for (int i = 0; i < data.length; i++) {
            headers.add(data[i]);
        }
        int loaded = 0;
        data = reader.readNext();
        while (data != null) {
            cols.clear();
            for (int i = 0; i < data.length; i++) {
                cols.put(headers.get(i), data[i]);
            }
            loadStoredQuery(loaded, cols);
            loaded++;
            data = reader.readNext();
        }
        connection.commit();
        storedQueryManager.resetMax();
    }

    private void loadStoredQuery(int rowNum, Map<String, String> cols) throws StoredQCException {
        String name = cols.get(SQField.NAME.getXlsxName());
        if (StringUtils.isBlank(name)) {
            throw new StoredQCException("Need to specify a name in the " +
                    SQField.NAME.getXlsxName() + " column on row " + rowNum);
        }
        StoredQueryBuilder sqBuilder = new StoredQueryBuilder(rowNum, name);
        NumberFormat nf = new DecimalFormat("#");
        for (SQField field : SQField.values()) {
            if (field.equals(SQField.NAME))
                continue;

            if (cols.containsKey(field.getXlsxName())) {
                try {
                    String val = cols.get(field.getXlsxName());
                    //numbers are treated as floats in poi/excel
                    if (field.getType() == Types.INTEGER && !StringUtils.isBlank(val)) {
                        try {
                            double dval = Double.parseDouble(val);
                            val = nf.format(dval);
                        } catch (NumberFormatException e) {
                            //if something went wrong, so be it.
                            //there will be an error in sqbuilder
                        }
                    }
                    sqBuilder.add(field, val);
                } catch (Exception e) {
                    throw new StoredQCException(e.getMessage());
                }
            }
        }

        StoredQuery sq = sqBuilder.build();
        try {
            ComplexQueryBuilder.validateMain(sq, parserPlugin, storedConceptManager);
        } catch (ParseException e) {
            sqBuilder.add(SQField.MAIN_QUERY_EXCEPTION_MSG, e.getMessage());
        }
        try {
            ComplexQueryBuilder.validateFilter(sq, parserPlugin, storedConceptManager);
        } catch (ParseException e) {
            sqBuilder.add(SQField.FILTER_QUERY_EXCEPTION_MSG, e.getMessage());
        }

        try {
            storedQueryManager.addQueryNoCommit(sq);
        } catch (SQLException e) {
            LOG.error("couldn't add query", e);
            throw new RuntimeException(e);
        }

    }
}

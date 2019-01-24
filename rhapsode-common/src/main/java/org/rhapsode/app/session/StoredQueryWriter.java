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
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Types;
import java.util.Map;

import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.rhapsode.app.RhapsodeSearcherApp;
import org.rhapsode.lucene.search.SCField;
import org.rhapsode.lucene.search.SQField;
import org.rhapsode.lucene.search.StoredConcept;
import org.rhapsode.lucene.search.StoredQuery;


public class StoredQueryWriter {

    public static final String CONCEPT_SHEET_NAME = "Stored Concepts";
    public static final String SQ_SHEET_NAME = "Stored Queries";


    private final RhapsodeSearcherApp searcherConfig;

    public StoredQueryWriter(RhapsodeSearcherApp searcherConfig) {
        this.searcherConfig = searcherConfig;

    }

    public static void updateMetadata(XSSFWorkbook wb, String creator, String title) {
        POIXMLProperties.CoreProperties coreProperties = wb.getProperties().getCoreProperties();
        coreProperties.setCreator(creator);
        coreProperties.setTitle(title);

        wb.getProperties().getExtendedProperties().getUnderlyingProperties().setCompany("The MITRE Corporation");
        wb.getProperties().getExtendedProperties().getUnderlyingProperties().setApplication("Rhapsode");

    }

    public void writeStoredConcepts(Path p) throws IOException {
        SXSSFWorkbook wb = new SXSSFWorkbook(new XSSFWorkbook(), 100, true, true);
        wb.setCompressTempFiles(true);

        CreationHelper creationHelper = wb.getCreationHelper();
        DataFormat dataFormat = wb.createDataFormat();
        CellStyle floatStyle = wb.createCellStyle();
        floatStyle.setDataFormat(dataFormat.getFormat("0.000"));
        addConceptSheet(wb);
        try (OutputStream os = Files.newOutputStream(p)) {
            wb.write(os);
        } finally {
            wb.dispose();
            wb.close();
        }
    }

    public void writeStoredQueries(Path p, String creator) throws IOException {
        SXSSFWorkbook wb = new SXSSFWorkbook(new XSSFWorkbook(), 100, true, true);
        updateMetadata(wb.getXSSFWorkbook(), creator, "Stored Queries");
        wb.setCompressTempFiles(true);

        CreationHelper creationHelper = wb.getCreationHelper();
        DataFormat dataFormat = wb.createDataFormat();
        CellStyle floatStyle = wb.createCellStyle();
        floatStyle.setDataFormat(dataFormat.getFormat("0.000"));
        addConceptSheet(wb);
        addStoredQuerySheet(wb);
        try (OutputStream os = Files.newOutputStream(p)) {
            wb.write(os);
        } finally {
            wb.dispose();
            wb.close();
        }
    }

    public void addStoredQuerySheet(SXSSFWorkbook wb) throws IOException {
        Sheet sheet = wb.createSheet(SQ_SHEET_NAME);
        Map<Integer, StoredQuery> storedQueries = searcherConfig.
                getSessionManager().getStoredQueryManager().getStoredQueryMap();
        int rowCount = 0;
        Row row = sheet.createRow(rowCount);
        int colCount = 0;
        for (SQField f : SQField.values()) {
            //don't bother dumping the exception messages
            if (f.equals(SQField.MAIN_QUERY_EXCEPTION_MSG) ||
                    f.equals(SQField.FILTER_QUERY_EXCEPTION_MSG) ||
                    f.equals(SQField.ID)) {
                continue;
            }

            Cell cell = row.createCell(colCount++);
            cell.setCellValue(f.getXlsxName());
        }
        rowCount++;
        for (Map.Entry<Integer, StoredQuery> e : storedQueries.entrySet()) {
            colCount = 0;
            row = sheet.createRow(rowCount++);
            StoredQuery sq = e.getValue();
            for (SQField f : SQField.values()) {
                //don't bother dumping the exception messages
                if (f.equals(SQField.MAIN_QUERY_EXCEPTION_MSG) ||
                        f.equals(SQField.FILTER_QUERY_EXCEPTION_MSG) ||
                        f.equals(SQField.ID)) {
                    continue;
                }

                if (f.getType() == Types.INTEGER) {
                    row.createCell(colCount++).setCellValue(sq.getInteger(f));
                } else {
                    row.createCell(colCount++).setCellValue(sq.getString(f));
                }
            }
        }

    }

    public void addConceptSheet(SXSSFWorkbook wb) throws IOException {
        Sheet sheet = wb.createSheet(CONCEPT_SHEET_NAME);
        Map<String, StoredConcept> storedConcepts = searcherConfig.
                getSessionManager().getStoredConceptManager().getConceptMap();
        int rowCount = 0;
        Row row = sheet.createRow(rowCount);
        int colCount = 0;
        //headers
        for (SCField f : SCField.values()) {
            //don't bother dumping the exception messages
            if (f.equals(SCField.CONCEPT_EXCEPTION_MSG)) {
                continue;
            }
            Cell cell = row.createCell(colCount++);
            cell.setCellValue(f.getXlsxName());
        }
        rowCount++;
        for (Map.Entry<String, StoredConcept> e : storedConcepts.entrySet()) {
            colCount = 0;
            row = sheet.createRow(rowCount++);
            StoredConcept sc = e.getValue();
            for (SCField f : SCField.values()) {
                //don't bother dumping the exception messages
                if (f.equals(SCField.CONCEPT_EXCEPTION_MSG)) {
                    continue;
                }
                row.createCell(colCount++).setCellValue(sc.getString(f));
            }
        }
    }
}

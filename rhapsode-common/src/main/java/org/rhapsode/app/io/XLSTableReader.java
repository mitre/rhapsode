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

package org.rhapsode.app.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XLSTableReader extends AbstractWorkbookTableReader {
    private static final Logger LOG = LoggerFactory.getLogger(XLSTableReader.class);

    final InputStream is;
    final Workbook wb;
    final String sheetName;
    final DataFormatter dataFormatter = new DataFormatter(true);
    final FormulaEvaluator formulaEvaluator;
    Sheet sheet;
    RowReader rowReader;

    public XLSTableReader(Path path, String worksheetName, RowReader rowReader, boolean hasHeaders)
            throws TableReaderException {
        super(hasHeaders);
        try {
            is = Files.newInputStream(path);
            wb = new HSSFWorkbook(is);
        } catch (IOException e) {
            throw new TableReaderException("IOException reading file", e);
        }
        this.rowReader = rowReader;
        this.formulaEvaluator = wb.getCreationHelper().createFormulaEvaluator();
        this.sheetName = worksheetName;
    }

    public void parse() throws TableReaderException {

        setSheet();
        Iterator<Row> rows = sheet.rowIterator();
        List<String> headers = null;
        if (getHasHeaders()) {
            headers = loadHeaders(rows);
        } else {
            Row r = sheet.getRow(sheet.getFirstRowNum());
            headers = new ArrayList<>();
            for (int i = 0; i <= r.getLastCellNum(); i++) {
                headers.add(getNonHeaderLabel(i));
            }
        }

        rowReader.setHeaders(headers);
        Map<String, String> cols = new HashMap<>();
        boolean shouldKeepGoing = false;
        Iterator<Row> rowIterator = sheet.rowIterator();
        int rowsProcessed = 0;
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            if (rowsProcessed++ == 0 && getHasHeaders()) {
                continue;
            }

            cols.clear();
            Iterator<Cell> cellIterator = row.cellIterator();
            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                int col = cell.getColumnIndex();
                if (col < headers.size()) {
                    String val = "";
                    if (cell.getCellType() != CellType.FORMULA) {
                        val = dataFormatter.formatCellValue(cell);
                    } else {
                        val = dataFormatter.formatCellValue(cell, formulaEvaluator);
                    }
                    cols.put(headers.get(col), val);
                } else {
                    LOG.warn("column with index (" + col + ") is not within header range.  I'm skipping it");
                }
            }
            try {
                shouldKeepGoing = rowReader.process(cols);
                if (!shouldKeepGoing) {
                    break;
                }
            } catch (IOException e) {
                throw new TableReaderException("IOException while reading?!", e);
            }

        }

    }

    private List<String> loadHeaders(Iterator<Row> rows) throws TableReaderException {
        List<String> headers = new ArrayList<>();
        if (!rows.hasNext()) {
            throw new TableReaderException("Couldn't find any rows?!");
        }
        Row r = rows.next();
        for (short i = 0; i < r.getLastCellNum(); i++) {
            Cell cell = r.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            String header = null;
            if (cell != null) {
                header = cell.getStringCellValue();
            } else {
                header = getNonHeaderLabel(i);
            }
            headers.add(header);
        }
        Collections.unmodifiableList(headers);
        return headers;
    }

    private void setSheet() throws TableReaderException {

        if (sheetName != null) {
            sheet = wb.getSheet(sheetName);
            if (sheet == null) {
                StringBuilder sb = new StringBuilder();
                boolean appended = false;
                for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                    Sheet sheet = wb.getSheetAt(i);
                    if (sheet != null) {
                        if (appended) {
                            sb.append(", ");
                        }
                        sb.append(wb.getSheetAt(i).getSheetName());
                        appended = true;
                    }
                }
                throw new TableReaderException("I regret, I could not find a sheet with name'" + sheetName + "'.\n" +
                        "I did find: " + sb.toString());
            }

        } else {
            sheet = wb.getSheetAt(0);
        }


    }

    public void close() throws Exception {
        if (is != null) {
            is.close();
        }
    }

    public List<String> getSheets() {
        List<String> workSheets = new ArrayList<>();
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            workSheets.add(wb.getSheetAt(i).getSheetName());
        }
        return workSheets;
    }
}

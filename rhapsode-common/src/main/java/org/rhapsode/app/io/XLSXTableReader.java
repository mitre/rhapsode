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

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class XLSXTableReader implements AutoCloseable {

    InputStream is;
    final XSSFWorkbook wb;
    XSSFSheet sheet;
    Iterator<Row> iterator;

    public static XSSFWorkbook loadWorkbook(InputStream is) throws IOException {
        return new XSSFWorkbook(is);
    }

    public XLSXTableReader(InputStream is) throws IOException {
        this(is, null);
    }

    public XLSXTableReader(InputStream is, String worksheetName) throws IOException {
        this(loadWorkbook(is), worksheetName);
    }

    public XLSXTableReader(XSSFWorkbook wb, String worksheetName) throws IOException {
        this.wb = wb;
        if (worksheetName == null) {
            sheet = wb.getSheetAt(0);
        } else {
            sheet = wb.getSheet(worksheetName);
        }
        if (sheet == null) {
            throw new IOException("Couldn't find sheet: " + worksheetName);
        }
        iterator = sheet.iterator();
    }

    public String[] readNext() throws IOException {
        if (iterator.hasNext()) {
            Row r = iterator.next();
            List<String> cells = new LinkedList<>();
            for (short i = 0; i <= r.getLastCellNum(); i++) {
                Cell c = r.getCell(i, Row.RETURN_BLANK_AS_NULL);
                if (c == null) {
                    cells.add(StringUtils.EMPTY);
                } else {
                    cells.add(getStringVal(c));
                }
            }
            return cells.toArray(new String[cells.size()]);
        }
        return null;
    }

    private String getStringVal(Cell c) {

        switch (c.getCellTypeEnum()) {
            case STRING:
                return c.getStringCellValue();
            case NUMERIC:
                return Double.toString(c.getNumericCellValue());
            case BOOLEAN:
                return Boolean.toString(c.getBooleanCellValue());
            case BLANK:
                return "";
            case ERROR:
                return "ERROR";
            case FORMULA:
                return Double.toString(c.getNumericCellValue());
        }
        return c.toString();
    }

    public void close() throws TableReaderException {
        try {
            wb.close();
        } catch (IOException e) {
            throw new TableReaderException("IOException closing workbook", e);
        }
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                throw new TableReaderException("IOException closing xlsx table", e);
            }
        }
    }
}


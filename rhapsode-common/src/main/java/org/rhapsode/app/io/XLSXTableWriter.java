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
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


public class XLSXTableWriter implements TableWriter {
    Path outFile;
    SXSSFWorkbook wb;
    Sheet sheet;
    int rowNum = 0;

    public XLSXTableWriter(Path outFile, String sheetName) {
        wb = new SXSSFWorkbook(new XSSFWorkbook(), 100, true, true);
        sheet = wb.createSheet(sheetName);
        this.outFile = outFile;
    }

    @Override
    public void writeNext(List<String> row) throws IOException {
        Row xssfRow = sheet.createRow(rowNum++);
        for (int i = 0; i < row.size(); i++) {
            Cell cell = xssfRow.createCell(i);
            cell.setCellValue(row.get(i));
        }
    }

    @Override
    public void close() throws Exception {
        if (wb != null) {
            try (OutputStream os = Files.newOutputStream(outFile)) {
                wb.write(os);
            } finally {
                wb.dispose();
            }
        }
    }
}

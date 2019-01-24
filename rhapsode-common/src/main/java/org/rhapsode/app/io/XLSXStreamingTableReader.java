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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.IOUtils;
import org.apache.tika.parser.ParseContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class XLSXStreamingTableReader extends AbstractWorkbookTableReader {

    private static final Logger LOG = LoggerFactory.getLogger(XLSXStreamingTableReader.class);
    final RowReader rowReader;
    final OPCPackage opcPackage;
    final String worksheetName;
    final XSSFReader xssfReader;
    InputStream sheetInputStream;

    public XLSXStreamingTableReader(Path path, String worksheetName, RowReader rowReader, boolean hasHeaders)
            throws TableReaderException {
        super(hasHeaders);
        try {
            this.opcPackage = OPCPackage.open(path.toFile(), PackageAccess.READ);
            this.xssfReader = new XSSFReader(opcPackage);
        } catch (IOException | OpenXML4JException e) {
            throw new TableReaderException("Bad XML", e);
        }
        this.worksheetName = worksheetName;
        this.rowReader = rowReader;
    }

    public void parse() throws TableReaderException {
        ReadOnlySharedStringsTable sharedStringsTable = null;
        StylesTable stylesTable = null;
        try {
            sharedStringsTable = new ReadOnlySharedStringsTable(opcPackage);
            stylesTable = xssfReader.getStylesTable();
        } catch (IOException | SAXException | OpenXML4JException e) {
            throw new TableReaderException("Bad XML");
        }
        setSheetInputStream(worksheetName, xssfReader);

        try {
            XMLReader reader = new ParseContext().getXMLReader();
            DataFormatter formatter = new DataFormatter();
            ContentHandler handler = new XSSFSheetXMLHandler(
                    stylesTable, null, sharedStringsTable, new RowWrapper(rowReader), formatter, false);
            reader.setContentHandler(handler);
            try (Reader inputStreamReader = new InputStreamReader(sheetInputStream, "UTF-8")) {
                reader.parse(new InputSource(inputStreamReader));
            }
        } catch (PleaseStopReadingException e) {
            //ok, we'll stop reading
        } catch (TikaException | SAXException | IOException e) {
            throw new TableReaderException("Bad XML", e);
        }
    }

    private void setSheetInputStream(String worksheetName, XSSFReader xssfReader)
            throws TableReaderException {
        int index = 0;
        XSSFReader.SheetIterator iter = null;
        try {
            iter = (XSSFReader.SheetIterator) xssfReader.getSheetsData();
        } catch (IOException | InvalidFormatException e) {
            throw new TableReaderException("Bad xml", e);
        }

        while (iter.hasNext()) {
            InputStream stream = iter.next();
            String sheetName = iter.getSheetName();
            if (index == 0 && StringUtils.isBlank(worksheetName)) {
                sheetInputStream = new BufferedInputStream(stream);
                break;
            } else if (worksheetName.equals(sheetName)) {
                sheetInputStream = new BufferedInputStream(stream);
                break;
            }
            index++;
        }

        if (worksheetName == null && index > 1) {
            LOG.warn("No work sheet name and more than one worksheet. " +
                    "I'm defaulting to trying to read the first worksheet");
        }
        if (sheetInputStream == null) {
            List<String> worksheets = getSheets();
            StringBuilder sb = new StringBuilder();
            int i = 0;
            for (String w : worksheets) {
                if (i++ > 0) {
                    sb.append(", ");
                }
                sb.append(w);
            }
            throw new TableReaderException("I'm sorry, but I couldn't find a worksheet named \"" +
                    worksheetName + "\", but I did see: " + sb.toString());
        }
    }

    public void close() throws Exception {
        IOUtils.closeQuietly(sheetInputStream);

        if (opcPackage != null) {
            opcPackage.close();
        }
    }

    public List<String> getSheets() throws TableReaderException {

        XSSFReader.SheetIterator iter = null;
        try {
            iter = (XSSFReader.SheetIterator) xssfReader.getSheetsData();
        } catch (IOException | InvalidFormatException e) {
            throw new TableReaderException("Bad XML", e);
        }

        List<String> worksheets = new ArrayList<>();
        while (iter.hasNext()) {
            iter.next();
            worksheets.add(iter.getSheetName());
        }

        return worksheets;
    }

    private class RowWrapper implements XSSFSheetXMLHandler.SheetContentsHandler {

        final RowReader rowReader;
        final Map<String, String> buffer = new TreeMap<>();
        int rowsProcessed = 0;
        List<String> headers = new ArrayList<>();
        boolean collectHeaders = false;

        public RowWrapper(RowReader reader) {
            this.rowReader = reader;
        }


        @Override
        public void startRow(int i) {
            if (rowsProcessed == 0 && getHasHeaders()) {
                collectHeaders = true;
            }
            //for now we don't care about empty rows
        }

        @Override
        public void endRow(int rowIndex) {
            if (rowsProcessed++ == 0) {
                collectHeaders = false;
                if (getHasHeaders()) {
                    List<String> copy = new ArrayList<>(headers);
                    Collections.unmodifiableList(copy);
                    rowReader.setHeaders(copy);
                    return;
                } else {

                    for (int i = 0; i < buffer.size(); i++) {
                        headers.add(getNonHeaderLabel(i));
                    }
                    rowReader.setHeaders(headers);
                }
            }

            try {
                boolean keepGoing = rowReader.process(buffer);
                if (!keepGoing) {
                    throw new PleaseStopReadingException();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            buffer.clear();
            rowsProcessed++;
        }

        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment xssfComment) {

            CellReference cr = new CellReference(cellReference);
            if (collectHeaders) {
                for (int i = headers.size(); i < cr.getCol(); i++) {
                    headers.add(getNonHeaderLabel(i));
                }
                headers.add(formattedValue);
                return;
            }

            String header = "";
            if (cr.getCol() < headers.size()) {
                header = headers.get(cr.getCol());
                buffer.put(header, formattedValue);
            } else {
                LOG.warn("column with index (" + cr.getCol() + ") is not within header range.  I'm skipping it");
            }
        }

        @Override
        public void headerFooter(String s, boolean b, String s1) {

        }

        public List<String> headers() {
            return headers;
        }
    }

    private class PleaseStopReadingException extends RuntimeException {

    }
}

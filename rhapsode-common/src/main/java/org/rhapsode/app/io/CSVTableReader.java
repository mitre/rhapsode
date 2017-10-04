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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CSVTableReader extends AbstractTableReader {

    private static final Logger LOG = LoggerFactory.getLogger(CSVTableReader.class);


    private static final Charset ENCODING = StandardCharsets.UTF_8;
    private final static char[] TRIAL_DELIMITERS = new char[]{',', '\t', '|'};

    private final static Charset[] TRIAL_ENCODINGS = new Charset[]{
            StandardCharsets.UTF_16LE,
            StandardCharsets.UTF_8
    };

    private static char DEFAULT_DELIMITER = ',';

    Reader reader = null;
    CSVParser parser = null;
    Iterator<CSVRecord> iterator = null;

    public static EncodingDelimiterPair guessEncodingDelimiter(Path p) throws IOException {
        String charsetName = "UTF-8";
        try (BOMInputStream inputStream = new BOMInputStream(Files.newInputStream(p),
                ByteOrderMark.UTF_8, ByteOrderMark.UTF_16LE)) {
            if (inputStream.hasBOM()) {
                charsetName = inputStream.getBOMCharsetName();
            }
        }

        for (char c : TRIAL_DELIMITERS) {
            boolean looksGood = false;
            try {
                looksGood = testFirstX(p, Charset.forName(charsetName), c, 10);
            } catch (Exception e) {

            }
            if (looksGood) {
                return new EncodingDelimiterPair(charsetName, c);
            }
        }
        return new EncodingDelimiterPair(charsetName);


    }

    private static boolean testFirstX(Path p, Charset charset, char c, int x) throws Exception {
        ColCounter colCounter = new ColCounter(x);
        try (CSVTableReader tableReader = new CSVTableReader(p, colCounter, c, charset, false)) {
            tableReader.parse();
        }
        return colCounter.hasSameNumCols() && colCounter.lastNumColumns > 1;
    }

    private final RowReader rowReader;

    public static CSVTableReader build(Path p, RowReader rowReader) throws IOException {
        return new CSVTableReader(p, rowReader);
    }

    public static CSVTableReader build(Path p, RowReader rowReader, char delimiter) throws IOException {
        return new CSVTableReader(p, rowReader, delimiter);
    }

    private CSVTableReader(Path path, RowReader rowReader) throws IOException {
        this(path, rowReader, DEFAULT_DELIMITER);
    }


    private CSVTableReader(Path path, RowReader rowReader, char delimiter) throws IOException {
        this(path, rowReader, delimiter, ENCODING, true);
    }

    public CSVTableReader(Path path, RowReader rowReader, char delimiter, Charset encoding, boolean hasHeaders) throws IOException {
        super(hasHeaders);
        this.rowReader = rowReader;
        ByteOrderMark mark = null;
        if (encoding.equals(StandardCharsets.UTF_8)) {
            mark = ByteOrderMark.UTF_8;
        } else if (encoding.equals(StandardCharsets.UTF_16LE)) {
            mark = ByteOrderMark.UTF_16LE;
        } else if (encoding.equals(StandardCharsets.UTF_16BE)) {
            mark = ByteOrderMark.UTF_16BE;
        }

        if (mark == null) {
            reader = new BufferedReader(
                    new InputStreamReader(
                            new BufferedInputStream(Files.newInputStream(path)), encoding));
        } else {
            reader = new BufferedReader(
                    new InputStreamReader(
                            new BOMInputStream(
                                    new BufferedInputStream(Files.newInputStream(path)), mark), encoding));
        }
        parser = new CSVParser(reader, CSVFormat.EXCEL.withDelimiter(delimiter));
        iterator = parser.iterator();
    }


    private String[] readNext() throws TableReaderException {

        if (iterator.hasNext()) {
            CSVRecord r = iterator.next();
            List<String> cells = new LinkedList<>();
            Iterator<String> cellIt = r.iterator();
            while (cellIt.hasNext()) {
                cells.add(cellIt.next());
            }
            return cells.toArray(new String[cells.size()]);
        }
        return null;
    }

    @Override
    public void close() throws TableReaderException {
        try {
            parser.close();
        } catch (IOException e) {

        }
        try {
            reader.close();
        } catch (IOException e) {
            throw new TableReaderException("IOException closing reader", e);
        }
    }

    @Override
    public void parse() throws TableReaderException {
        String[] row = readNext();
        if (row == null) {
            throw new TableReaderException("CSV file has no rows?!");
        }
        List<String> headers = new ArrayList<>();
        if (getHasHeaders()) {
            for (int i = 0; i < row.length; i++) {
                String h = row[i];
                if (StringUtils.isBlank(h)) {
                    h = getNonHeaderLabel(i);
                }
                headers.add(h);
            }
        } else {
            for (int i = 0; i < row.length; i++) {
                headers.add(getNonHeaderLabel(i));
            }
        }
        headers = Collections.unmodifiableList(headers);
        rowReader.setHeaders(headers);
        Map<String, String> data = new HashMap<>();
        while (row != null) {
            data.clear();
            for (int i = 0; i < Math.min(headers.size(), row.length); i++) {
                data.put(headers.get(i), row[i]);
                if (row.length > headers.size()) {
                    LOG.warn("columns after index (" + headers.size() + ") are not within header range.  I'm skipping them");
                }
            }

            try {
                boolean keepGoing = rowReader.process(data);
                if (!keepGoing) {
                    break;
                }
            } catch (IOException e) {
                throw new TableReaderException("IOException while reading csv?!", e);
            }
            row = readNext();
        }
    }

    private static class ColCounter extends RowReader {
        private final int rowsToCount;

        int lastNumColumns = -1;

        boolean hasSameNumColumns = true;
        int rowsProcessed = 0;

        public ColCounter(int x) {
            super();
            rowsToCount = x;
        }

        @Override
        public boolean process(Map<String, String> data) throws IOException {
            if (lastNumColumns > -1 && data.size() != lastNumColumns) {
                hasSameNumColumns = false;
            }
            lastNumColumns = data.size();
            if (rowsProcessed++ >= rowsToCount) {
                return false;
            }
            return true;
        }

        boolean hasSameNumCols() {
            return hasSameNumColumns;
        }

        int getLastNumCols() {
            return lastNumColumns;
        }
    }
}

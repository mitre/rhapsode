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

import org.apache.commons.lang.StringUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;


public class TableFileRequest {
    public static final String IS_CONTENT_PREFIX = "is_content:";
    public static final String IS_LINK_PREFIX = "is_link:";
    public static final String COL_HEADER_PREFIX = "col_name:";

    public Map<String, FieldTypePair> getFields() {
        return fields;
    }

    public String getTableFile() {
        return inputFileName;
    }

    public String getWorksheetName() {
        return worksheetName;
    }

    public boolean getTableHasHeaders() {
        return tableHasHeaders;
    }

    public void setEncoding(String encoding) {
        if (StringUtils.isBlank(encoding)) {
            this.encoding = StandardCharsets.UTF_8;
            return;
        }
        try {
            this.encoding = Charset.forName(encoding);
        } catch (Exception e) {
            throw new IllegalArgumentException("I'm sorry but I don't recognize this encoding: " + encoding);
        }
    }

    public void setDelimiter(String delimiter) {
        this.delimiterString = delimiter;
        if ("\t".equals(delimiter)) {
            this.delimiterString = "<tab>";
        }
    }

    public char getCSVDelimiterChar() {
        if (delimiterString != null) {
            if (delimiterString.equals(",") || delimiterString.equalsIgnoreCase("comma")) {
                return ',';
            } else if (delimiterString.toLowerCase(Locale.US).contains("tab")
                    || delimiterString.equals("\t") || delimiterString.equals("\\t")) {
                return '\t';
            } else if (delimiterString.equalsIgnoreCase("pipe") || delimiterString.equals("|")) {
                return ',';
            } else if (delimiterString.length() == 1) {
                return delimiterString.charAt(0);
            }
        }
        return ',';
    }

    public Charset getCSVEncoding() {
        return encoding;
    }

    enum ActionType {
        SELECT_COLLECTION_NAME,
        SELECT_INPUT_FILE,
        SELECT_WORKSHEET,
        SELECT_ENCODING_DELIMITER,
        SELECT_COLUMNS,
        START_INDEXING,
        LOAD_COLLECTION;
    }

    ActionType actionType;
    String inputDirectoryPath;
    String inputFileName;
    String collectionName;

    String worksheetName;

    private Charset encoding;
    private String delimiterString;


    Map<String, FieldTypePair> fields;

    boolean tableHasHeaders;


}

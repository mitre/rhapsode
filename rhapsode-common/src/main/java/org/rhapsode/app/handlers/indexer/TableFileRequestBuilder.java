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

import javax.servlet.http.HttpServletRequest;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.rhapsode.app.RhapsodeSearcherApp;
import org.rhapsode.app.contants.C;


public class TableFileRequestBuilder {
    public void extract(RhapsodeSearcherApp searcherApp,
                        HttpServletRequest httpServletRequest, TableFileRequest tableFileRequest) {

        tableFileRequest.inputDirectoryPath = httpServletRequest.getParameter(C.TABLE_INPUT_DIRECTORY);

        Enumeration<String> names = httpServletRequest.getParameterNames();
        while (names.hasMoreElements()) {
            String k = names.nextElement();
            for (String v : httpServletRequest.getParameterValues(k)) {
                //System.out.println("K: " +k + " : " + v);
            }
        }

        tableFileRequest.inputFileName = httpServletRequest.getParameter(C.TABLE_FILE_NAME);
        tableFileRequest.collectionName = httpServletRequest.getParameter(C.COLLECTION_NAME);
        tableFileRequest.setEncoding(httpServletRequest.getParameter(C.TABLE_ENCODING));
        tableFileRequest.setDelimiter(httpServletRequest.getParameter(C.TABLE_DELIMITER));
        tableFileRequest.worksheetName = httpServletRequest.getParameter(C.TABLE_WORKSHEET_NAME);
        String thh = httpServletRequest.getParameter(C.TABLE_HAS_HEADERS);
        if ("false".equals(thh)) {
            tableFileRequest.tableHasHeaders = false;
        } else {
            tableFileRequest.tableHasHeaders = true;
        }

        if (httpServletRequest.getParameter(C.RESTART_TABLE_LOADING_DIALOGUE) != null) {
            tableFileRequest.setEncoding(null);
            tableFileRequest.setDelimiter(null);
            tableFileRequest.worksheetName = null;
            tableFileRequest.collectionName = null;
            tableFileRequest.inputFileName = null;
            tableFileRequest.tableHasHeaders = false;
            tableFileRequest.actionType = TableFileRequest.ActionType.SELECT_COLLECTION_NAME;
        } else if (httpServletRequest.getParameter(C.SELECT_COLLECTION_NAME) != null) {
            tableFileRequest.collectionName = null;
            tableFileRequest.inputFileName = null;
            tableFileRequest.setEncoding(null);
            tableFileRequest.setDelimiter(null);
            tableFileRequest.worksheetName = null;
            tableFileRequest.tableHasHeaders = false;
            tableFileRequest.actionType = TableFileRequest.ActionType.SELECT_COLLECTION_NAME;
        } else if (httpServletRequest.getParameter(C.SELECT_TABLE_FILE) != null) {
            tableFileRequest.setEncoding(null);
            tableFileRequest.setDelimiter(null);
            tableFileRequest.worksheetName = null;
            tableFileRequest.tableHasHeaders = false;
            if (StringUtils.isBlank(tableFileRequest.collectionName)) {
                tableFileRequest.actionType = TableFileRequest.ActionType.SELECT_COLLECTION_NAME;
            } else {
                tableFileRequest.actionType = TableFileRequest.ActionType.SELECT_INPUT_FILE;
            }
        } else if (httpServletRequest.getParameter(C.SELECT_ENCODING_DELIMITER_WORKSHEET) != null) {
            tableFileRequest.setEncoding(null);
            tableFileRequest.setDelimiter(null);
            tableFileRequest.worksheetName = null;

            if (StringUtils.isBlank(tableFileRequest.collectionName)) {
                tableFileRequest.actionType = TableFileRequest.ActionType.SELECT_COLLECTION_NAME;
            } else if (StringUtils.isBlank(tableFileRequest.inputFileName)) {
                tableFileRequest.actionType = TableFileRequest.ActionType.SELECT_INPUT_FILE;
            } else if (tableFileRequest.inputFileName.endsWith(".xlsx") ||
                    tableFileRequest.inputFileName.endsWith("xls") || tableFileRequest.inputFileName.endsWith(".xlsb")) {
                tableFileRequest.actionType = TableFileRequest.ActionType.SELECT_WORKSHEET;
            } else if (tableFileRequest.inputFileName.endsWith(".csv") || tableFileRequest.inputFileName.endsWith(".txt")) {
                tableFileRequest.actionType = TableFileRequest.ActionType.SELECT_ENCODING_DELIMITER;
            }
        } else if (httpServletRequest.getParameter(C.SELECT_TABLE_COLUMNS) != null) {
            if (StringUtils.isBlank(tableFileRequest.collectionName)) {
                tableFileRequest.actionType = TableFileRequest.ActionType.SELECT_COLLECTION_NAME;
            } else if (StringUtils.isBlank(tableFileRequest.inputFileName)) {
                tableFileRequest.actionType = TableFileRequest.ActionType.SELECT_INPUT_FILE;
            } else {
                tableFileRequest.actionType = TableFileRequest.ActionType.SELECT_COLUMNS;
                loadColumns(httpServletRequest, tableFileRequest);
            }
        } else if (httpServletRequest.getParameter(C.START_INDEXER) != null) {
            loadColumns(httpServletRequest, tableFileRequest);
            tableFileRequest.actionType = TableFileRequest.ActionType.START_INDEXING;
        } else if (httpServletRequest.getParameter(C.OPEN_NEW_COLLECTION) != null) {
            tableFileRequest.actionType = TableFileRequest.ActionType.LOAD_COLLECTION;
        } else {
            tableFileRequest.actionType = TableFileRequest.ActionType.SELECT_COLLECTION_NAME;
        }
    }

    private void loadColumns(HttpServletRequest httpServletRequest, TableFileRequest tableFileRequest) {
        Map<String, FieldTypePair> map = new HashMap<>();
        Enumeration<String> keys = httpServletRequest.getParameterNames();
        while (keys.hasMoreElements()) {
            String k = keys.nextElement();
            if (k.startsWith(TableFileRequest.COL_HEADER_PREFIX)) {
                String tableColName = k.substring(TableFileRequest.COL_HEADER_PREFIX.length());
                String luceneColName = httpServletRequest.getParameter(k);
                map.put(tableColName, new FieldTypePair(luceneColName, "string"));
            }
        }
        String contentField = httpServletRequest.getParameter(C.TABLE_COL_IS_CONTENT);
        if (contentField != null) {
            FieldTypePair ft = map.get(contentField);
            if (ft == null) {
                //log something went very wrong
            } else {
                ft.isDefaultContent = true;
            }
        }
        String linkField = httpServletRequest.getParameter(C.TABLE_COL_IS_LINK);
        FieldTypePair ft = map.get(linkField);
        if (ft == null) {
            //log something went very wrong
        } else {
            ft.isLinkField = true;
        }
        tableFileRequest.fields = map;
    }
}

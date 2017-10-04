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

package org.rhapsode.app.handlers.admin;

import org.apache.commons.lang3.StringUtils;
import org.rhapsode.app.contants.C;
import org.rhapsode.app.utils.StoredQCUtils;
import org.rhapsode.lucene.search.SQField;
import org.rhapsode.lucene.search.StoredQuery;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StoredQueryRequest {


    enum ActionType {
        DEFAULT,
        ADD_QUERY,
        ADD_DIALOGUE,
        LOAD_STORED_QUERIES_DIALOGUE,
        LOAD_STORED_QUERIES,
        DELETE_ALL_STORED_QUERIES,
        SELECT_QUERIES_FOR_UPDATE,
        UPDATE_SELECTED_QUERY_DIALOGUE,
        UPDATE_SELECTED_QUERY,
        SAVE_DIALOGUE,
        SAVE_QUERIES,
        UPDATE_DOC_COUNTS,
        DELETE_SELECTED_QUERIES;
    }

    ;

    public static StoredQueryRequest build(HttpServletRequest httpServletRequest) {
        StoredQueryRequest r = new StoredQueryRequest();

        String[] queryIds = httpServletRequest.getParameterValues(C.SELECTED_STORED_QUERY);
        if (queryIds != null) {
            for (String queryId : queryIds) {
                try {
                    r.selectedQueries.add(Integer.parseInt(queryId));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Can't parse " + queryId + " as an integer");
                }
            }
        }

        r.t = ActionType.DEFAULT;
        if (httpServletRequest.getParameter(C.ADD_STORED_QUERY) != null) {
            r.t = ActionType.ADD_QUERY;
        } else if (httpServletRequest.getParameter(C.ADD_STORED_QUERY_DIALOGUE) != null) {
            r.t = ActionType.ADD_DIALOGUE;
        } else if (httpServletRequest.getParameter(C.DELETE_ALL_STORED_QUERIES) != null) {
            r.t = ActionType.DELETE_ALL_STORED_QUERIES;
        } else if (httpServletRequest.getParameter(C.DELETE_SELECTED_QUERIES) != null) {
            r.t = ActionType.DELETE_SELECTED_QUERIES;
        } else if (httpServletRequest.getParameter(C.UPDATE_DOCUMENT_COUNTS) != null) {
            r.t = ActionType.UPDATE_DOC_COUNTS;
        } else if (httpServletRequest.getParameter(C.LOAD_STORED_QUERIES_DIALOGUE) != null) {
            r.t = ActionType.LOAD_STORED_QUERIES_DIALOGUE;
        } else if (httpServletRequest.getParameter(C.LOAD_STORED_QUERIES) != null) {
            r.t = ActionType.LOAD_STORED_QUERIES;
        } else if (httpServletRequest.getParameter(C.SELECT_QUERIES_FOR_UPDATE) != null) {
            r.t = ActionType.SELECT_QUERIES_FOR_UPDATE;
        } else if (httpServletRequest.getParameter(C.UPDATE_SELECTED_QUERY_DIALOGUE) != null) {
            r.t = ActionType.UPDATE_SELECTED_QUERY_DIALOGUE;
        } else if (httpServletRequest.getParameter(C.UPDATE_SELECTED_QUERY) != null) {
            r.t = ActionType.UPDATE_SELECTED_QUERY;
        } else if (httpServletRequest.getParameter(C.SAVE_STORED_QUERIES_DIALOGUE) != null) {
            r.t = ActionType.SAVE_DIALOGUE;
        } else if (httpServletRequest.getParameter(C.SAVE_STORED_QUERIES) != null) {
            r.t = ActionType.SAVE_QUERIES;
        }

        r.query = StoredQCUtils.getQueryFields(httpServletRequest, null);


        String pathToLoad = httpServletRequest.getParameter(C.STORED_QUERIES_PATH);
        if (!StringUtils.isBlank(pathToLoad)) {
            r.fileToLoad = pathToLoad;
        }

        return r;
    }

    static int getMaxResults(String s) {
        if (s == null) {
            return StoredQuery.RETRIEVE_ALL_HITS;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {

        }
        return StoredQuery.RETRIEVE_ALL_HITS;
    }

    private Map<SQField, String> query;
    private ActionType t;
    private String fileToLoad;
    private List<Integer> selectedQueries = new ArrayList<>();

    public ActionType getActionType() {
        return t;
    }

    public Map<SQField, String> getQuery() {
        return query;
    }

    public String getFileToLoad() {
        return fileToLoad;
    }

    public List<Integer> getSelectedQueries() {
        return selectedQueries;
    }

    public void setQuery(Map<SQField, String> query) {
        this.query = query;
    }

}

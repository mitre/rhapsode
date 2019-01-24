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

package org.rhapsode.app.utils;

import javax.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.rhapsode.app.contants.C;
import org.rhapsode.lucene.search.SCField;
import org.rhapsode.lucene.search.SQField;
import org.rhapsode.lucene.search.StoredConcept;
import org.rhapsode.lucene.search.StoredConceptBuilder;
import org.rhapsode.lucene.search.StoredQuery;
import org.rhapsode.lucene.search.StoredQueryBuilder;

//StoredQueryConceptUtils
public class StoredQCUtils {

    public static Map<SQField, String> getQueryFields(HttpServletRequest servletRequest,
                                                      String defaultQueryField) {
        String tmpDefaultQueryField =
                defaultQueryField == null ?
                        servletRequest.getParameter(C.DEFAULT_QUERY_FIELD) :
                        defaultQueryField;

        String idString = servletRequest.getParameter(C.STORED_QUERY_ID);
        int id = StoredQuery.NOT_YET_LOADED;
        if (!StringUtils.isBlank(idString)) {
            try {
                id = Integer.parseInt(idString);
            } catch (NumberFormatException e) {
                //swallow
            }
        }
        String name = servletRequest.getParameter(C.STORED_QUERY_NAME);
        String maxHits = servletRequest.getParameter(C.MAX_SEARCH_RESULTS);
        if (StringUtils.isBlank(maxHits) || maxHits.equals("default")) {
            maxHits = Integer.toString(StoredQuery.RETRIEVE_ALL_HITS);
        }
        String priority = servletRequest.getParameter(C.STORED_QUERY_PRIORITY);
        if (StringUtils.isBlank(priority) || priority.equals("default") ||
                priority.equals("all")) {
            priority = Integer.toString(StoredQuery.DEFAULT_PRIORITY);
        }
        Map<SQField, String> map = new HashMap<>();
        map.put(SQField.ID, Integer.toString(id));
        map.put(SQField.NAME, name);
        map.put(SQField.DEFAULT_FIELD, tmpDefaultQueryField);
        map.put(SQField.MAIN_QUERY, servletRequest.getParameter(C.MAIN_QUERY));
        map.put(SQField.MAIN_QUERY_TRANSLATION, servletRequest.getParameter(C.MAIN_QUERY_TRANSLATION));
        map.put(SQField.FILTER_QUERY, servletRequest.getParameter(C.FILTER_QUERY));
        map.put(SQField.FILTER_QUERY_TRANSLATION, servletRequest.getParameter(C.FILTER_QUERY_TRANSLATION));
        map.put(SQField.GEO_QUERY_STRING, servletRequest.getParameter(C.GEO_QUERY));
        map.put(SQField.GEO_QUERY_RADIUS_STRING, servletRequest.getParameter(C.GEO_RADIUS));
        map.put(SQField.HIGHLIGHTING_STYLE, servletRequest.getParameter(C.HIGHLIGHT_STYLE));
        map.put(SQField.MAX_HITS, maxHits);
        map.put(SQField.PRIORITY, priority);
        map.put(SQField.NOTES, servletRequest.getParameter(C.STORED_QUERY_NOTES));
        return map;
    }

    public static StoredQuery buildStoredQuery(Map<SQField, String> fields) {
        int id = StoredQuery.NOT_YET_LOADED;
        String intVal = fields.get(SQField.ID);
        if (intVal != null) {
            try {
                id = Integer.parseInt(intVal);
            } catch (NumberFormatException e) {

            }
        }
        StoredQueryBuilder storedQueryBuilder = new StoredQueryBuilder(id, fields.get(SQField.NAME));
        for (Map.Entry<SQField, String> e : fields.entrySet()) {
            if (e.getKey().equals(SQField.NAME)) {
                continue;
            }
            storedQueryBuilder.add(e.getKey(), e.getValue());
        }
        return storedQueryBuilder.build();
    }

    public static StoredConcept buildStoredConcept(Map<SCField, String> fields) {
        StoredConceptBuilder storedConceptBuilder = new StoredConceptBuilder(fields.get(SCField.NAME));
        for (Map.Entry<SCField, String> e : fields.entrySet()) {
            if (e.getKey().equals(SCField.NAME)) {
                continue;
            }
            storedConceptBuilder.add(e.getKey(), e.getValue());
        }
        return storedConceptBuilder.build();
    }
}

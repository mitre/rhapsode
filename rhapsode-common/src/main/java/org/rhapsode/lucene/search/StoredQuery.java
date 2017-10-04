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

package org.rhapsode.lucene.search;

import java.lang.reflect.Field;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class StoredQuery {

    public static final int DEFAULT_PRIORITY = -1;
    public static final int RETRIEVE_ALL_HITS = -1;
    public static final int NOT_YET_LOADED = -1;
    public static final String DEFAULT_FIELD = "content";
    public static final String DEFAULT_STYLE = "regular";
    int id = NOT_YET_LOADED;
    final String queryName;
    String defaultField = DEFAULT_FIELD;
    String mainQuery;
    String mainQueryTranslation;
    String filterQuery;
    String filterQueryTranslation;
    String geoQueryString;
    String geoQueryRadiusString;
    int maxHits = RETRIEVE_ALL_HITS;
    int priority = DEFAULT_PRIORITY;
    String highlightingStyle = DEFAULT_STYLE;
    String notes;
    String mainQueryExcMsg;
    String filterQueryExcMsg;

    StoredQuery(String name) {
        this(NOT_YET_LOADED, name);
    }

    StoredQuery(int id, String name) {
        this.id = id;
        this.queryName = name;
    }

    public String getQueryName() {
        return queryName;
    }

    public String getDefaultField() {
        return defaultField;
    }

    public String getMainQueryString() {
        return mainQuery;
    }

    public String getMainQueryTranslation() {
        return mainQueryTranslation;
    }

    public String getFilterQueryString() {
        return filterQuery;
    }

    public String getFilterQueryTranslation() {
        return filterQueryTranslation;
    }

    public String getGeoQueryString() {
        return geoQueryString;
    }

    public String getGeoQueryRadiusString() {
        return geoQueryRadiusString;
    }

    public int getMaxHits() {
        return maxHits;
    }

    public int getPriority() {
        return priority;
    }

    public String getHighlightingStyle() {
        return highlightingStyle;
    }

    public String getNotes() {
        return notes;
    }

    public String getString(SQField sqField) {
        if (sqField.getType() == Types.INTEGER) {
            return Integer.toString(getInteger(sqField));
        }
        try {
            Field field = this.getClass().getDeclaredField(sqField.getObjFieldName());
            return (String) field.get(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public int getInteger(SQField sqField) {
        try {
            Field field = this.getClass().getDeclaredField(sqField.getObjFieldName());
            return field.getInt(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> getDBColNames() {
        List<String> cols = new ArrayList<>();
        for (SQField field : SQField.values()) {
            cols.add(field.getDbName());
        }
        return cols;
    }

    public String getMainQueryExcMsg() {
        return mainQueryExcMsg;
    }

    public String getFilterQueryExcMsg() {
        return filterQueryExcMsg;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "StoredQuery{" +
                "id=" + id +
                ", queryName='" + queryName + '\'' +
                ", defaultField='" + defaultField + '\'' +
                ", mainQuery='" + mainQuery + '\'' +
                ", mainQueryTranslation='" + mainQueryTranslation + '\'' +
                ", filterQuery='" + filterQuery + '\'' +
                ", filterQueryTranslation='" + filterQueryTranslation + '\'' +
                ", geoQueryString='" + geoQueryString + '\'' +
                ", geoQueryRadiusString='" + geoQueryRadiusString + '\'' +
                ", maxHits=" + maxHits +
                ", priority=" + priority +
                ", highlightingStyle='" + highlightingStyle + '\'' +
                ", notes='" + notes + '\'' +
                ", mainQueryExcMsg='" + mainQueryExcMsg + '\'' +
                ", filterQueryExcMsg='" + filterQueryExcMsg + '\'' +
                '}';
    }
}

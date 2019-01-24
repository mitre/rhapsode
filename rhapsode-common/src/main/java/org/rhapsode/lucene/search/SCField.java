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


import java.sql.Types;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

//stored concept field
public enum SCField {


    NAME("CONCEPT_NAME", Types.VARCHAR, "", 100),

    CONCEPT_QUERY(
            "CONCEPT_QUERY",
            Types.VARCHAR,
            "",
            10000
    ),
    CONCEPT_TRANSLATION(
            "CONCEPT_TRANSLATION",
            Types.VARCHAR,
            "",
            10000
    ),
    NOTES(
            "NOTES",
            Types.VARCHAR,
            "",
            30000
    ),
    CONCEPT_EXCEPTION_MSG(
            "CONCEPT_EXC_MSG",
            Types.VARCHAR,
            "",
            300
    );

    private final String dbName;
    private final String xlsxName;
    private final String objFieldName;
    private final int type;
    private final String defaultVal;
    private final int maxLength;

    SCField(String dbName, int type,
            String defaultVal, int maxLength) {
        this.dbName = dbName;
        this.xlsxName = getXlsxName(dbName);
        this.objFieldName = getObjFieldName(dbName);
        this.type = type;
        this.defaultVal = defaultVal;
        this.maxLength = maxLength;

    }

    private String getXlsxName(String dbName) {
        String[] parts = dbName.split("_");
        String[] cased = new String[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            cased[i] = p.substring(0, 1)
                    + p.substring(1, p.length()).toLowerCase(Locale.ENGLISH);
        }
        return StringUtils.join(cased, " ");
    }

    private String getObjFieldName(String dbName) {
        String[] parts = dbName.split("_");
        String[] cased = new String[parts.length];
        cased[0] = parts[0].toLowerCase(Locale.ENGLISH);
        for (int i = 1; i < parts.length; i++) {
            String p = parts[i];
            cased[i] = p.substring(0, 1)
                    + p.substring(1, p.length()).toLowerCase(Locale.ENGLISH);
        }
        return StringUtils.join(cased, "");
    }

    public String getDbName() {
        return dbName;
    }

    public String getXlsxName() {
        return xlsxName;
    }

    public int getType() {
        return type;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public String getObjFieldName() {
        return objFieldName;
    }

    public String getDefaultVal() {
        return defaultVal;
    }
}

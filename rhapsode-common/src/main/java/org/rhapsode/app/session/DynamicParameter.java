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
package org.rhapsode.app.session;

abstract public class DynamicParameter<T> {


    public enum PREFIX {
        COMMON("Common Search"),//common to most search features
        BS("Basic Search"),//basic_search
        CONC("Concordance Search"), //concordance search
        COOCCUR("Co-occurrence Search"),
        REPORT_WRITER("Report Writer"),
        STORED_QUERY("Stored Query Admin"),
        TABLE_FILE_INDEXER("Table File Indexer");

        private final String displayName;

        PREFIX(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    ;

    private final String displayName;
    private final PREFIX prefix;
    private final String name;
    private final T defaultValue;
    private final String fullName;

    public DynamicParameter(String displayName,
                            PREFIX prefix, String name, T defaultValue) {
        this.displayName = displayName;
        this.prefix = prefix;
        this.name = name;
        this.fullName = prefix + ":" + name;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }


    public String getFullName() {
        return fullName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    abstract public String getDefaultValueAsString();

    abstract public T getValueFromString(String s) throws IllegalArgumentException;

    public PREFIX getPrefix() {
        return prefix;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DynamicParameter<?> that = (DynamicParameter<?>) o;

        if (displayName != null ? !displayName.equals(that.displayName) : that.displayName != null) return false;
        if (prefix != that.prefix) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (defaultValue != null ? !defaultValue.equals(that.defaultValue) : that.defaultValue != null) return false;
        return !(fullName != null ? !fullName.equals(that.fullName) : that.fullName != null);

    }

    @Override
    public int hashCode() {
        int result = displayName != null ? displayName.hashCode() : 0;
        result = 31 * result + (prefix != null ? prefix.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (defaultValue != null ? defaultValue.hashCode() : 0);
        result = 31 * result + (fullName != null ? fullName.hashCode() : 0);
        return result;
    }


}

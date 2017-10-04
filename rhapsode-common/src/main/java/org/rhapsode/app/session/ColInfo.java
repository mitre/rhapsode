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


import org.rhapsode.lucene.utils.SqlUtil;

import java.sql.Types;

public class ColInfo {
    private final String name;
    private final int type;
    private final Integer precision;
    private final String constraints;


    public ColInfo(String name, int type) {
        this(name, type, null, null);
    }

    public ColInfo(String name, int type, String constraints) {
        this(name, type, null, constraints);
    }

    public ColInfo(String name, int type, Integer precision) {
        this(name, type, precision, null);
    }


    public ColInfo(String name, int type, Integer precision, String constraints) {
        this.name = name;
        this.type = type;
        this.precision = precision;
        this.constraints = constraints;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    /**
     * @return constraints string or null
     */
    public String getConstraints() {
        return constraints;
    }

    /**
     * Gets the precision.  This can be null!
     *
     * @return precision or null
     */
    public Integer getPrecision() {
        return precision;
    }

    public String getSqlDef() {
        if (type == Types.VARCHAR) {
            return "VARCHAR(" + precision + ")";
        } else if (type == Types.CHAR) {
            return "CHAR(" + precision + ")";
        }
        switch (type) {
            case Types.FLOAT:
                return "FLOAT";
            case Types.DOUBLE:
                return "DOUBLE";
            case Types.BLOB:
                return "BLOB";
            case Types.INTEGER:
                return "INTEGER";
            case Types.BIGINT:
                return "BIGINT";
            case Types.BOOLEAN:
                return "BOOLEAN";
            case Types.DATE:
                return "DATE";
            case Types.TIMESTAMP:
                return "TIMESTAMP";
        }
        throw new UnsupportedOperationException("Don't yet recognize a type for: " + SqlUtil.getTypeName(type));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ColInfo colInfo = (ColInfo) o;

        if (type != colInfo.type) return false;
        if (!name.equals(colInfo.name)) return false;
        if (precision != null ? !precision.equals(colInfo.precision) : colInfo.precision != null) return false;
        return !(constraints != null ? !constraints.equals(colInfo.constraints) : colInfo.constraints != null);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + type;
        result = 31 * result + (precision != null ? precision.hashCode() : 0);
        result = 31 * result + (constraints != null ? constraints.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ColInfo{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", precision=" + precision +
                ", constraints='" + constraints + '\'' +
                '}';
    }
}

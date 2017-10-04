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

import org.apache.lucene.search.BooleanQuery;
import org.rhapsode.util.LanguageDirection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DynamicParameterConfig {

    private static final Logger LOG = LoggerFactory.getLogger(DynamicParameter.class);
    final static String PARAM_TABLE = "params";

    final static ColInfo PARAM_NAME =
            new ColInfo("NAME", Types.VARCHAR, 32, "PRIMARY KEY");

    final static ColInfo PARAM_VALUE =
            new ColInfo("VALUE", Types.VARCHAR, 10000);


    private Connection connection;
    private TableDef paramTable;
    private PreparedStatement preparedLookup;
    private PreparedStatement preparedMerge;
    private PreparedStatement preparedCopy;


    public static DynamicParameterConfig load(Connection connection) throws IOException, SQLException {
        return new DynamicParameterConfig(connection);
    }

    private DynamicParameterConfig(Connection connection) throws SQLException {
        this.connection = connection;
        this.paramTable = new TableDef(PARAM_TABLE, PARAM_NAME, PARAM_VALUE);
//        connection.createStatement().execute("drop table "+PARAM_TABLE);
        this.paramTable.createIfNotExists(connection);
        preparedLookup =
                connection.prepareStatement(
                        "select " + PARAM_VALUE.getName() + " from " + PARAM_TABLE +
                                " where " + PARAM_NAME.getName() + "= ?"
                );
        preparedMerge =
                connection.prepareStatement(
                        "MERGE INTO " + PARAM_TABLE + " KEY(" + PARAM_NAME.getName() + ")" +
                                " VALUES (?,?)"
                );

        preparedCopy =
                connection.prepareStatement(
                        "select " + PARAM_NAME.getName() + ", " +
                                PARAM_VALUE.getName() + " FROM " + PARAM_TABLE
                );

        //init if table is empty
        Map<DynamicParameter, String> m = getParamCopy();
        if (m.size() == 0) {
            for (Map.Entry<String, DynamicParameter> e : DynamicParameters.params.entrySet()) {
                update(e.getValue(), e.getValue().getDefaultValueAsString());
            }
        }
        int maxClauses = getInt(DynamicParameters.MAX_BOOLEAN_CLAUSES);
        try {
            BooleanQuery.setMaxClauseCount(maxClauses);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public Map<DynamicParameter, String> getParamCopy() {
        Map<DynamicParameter, String> ret = new LinkedHashMap<>();
        try {
            ResultSet rs = preparedCopy.executeQuery();
            while (rs.next()) {
                String name = rs.getString(1);
                String val = rs.getString(2);
                DynamicParameter p = DynamicParameters.params.get(name);
                if (p == null) {
                    LOG.warn("Value in db isn't a constant(?):" + name);
                } else {
                    ret.put(p, val);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return ret;
    }


    private void merge(DynamicParameter parameter, String newVal) throws SQLException {
        //assume the type has already been checked for parseabillity
        preparedMerge.clearParameters();
        preparedMerge.setString(1, parameter.getFullName());
        preparedMerge.setString(2, newVal);
        preparedMerge.execute();
    }


    public void update(DynamicParameter p, String val) throws SQLException {
        //check that this value is parseable
        Object v = p.getValueFromString(val);
        merge(p, val);
    }


    /*
        public DisplayFields getDisplayFields(DisplayFieldsDynamicParameter<DisplayFields> parameter) {
            String val = null;
            int i = 0;
            try {
                preparedLookup.clearParameters();
                preparedLookup.setString(1, parameter.getFullName());

                ResultSet rs = preparedLookup.executeQuery();
                while (rs.next()) {
                    val = rs.getString(1);
                    i++;
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            if (i++ > 1) {
                throw new
                        IllegalArgumentException(
                        "Shouldn't have more than one value for "+parameter.getFullName());
            }

            return parameter.getValueFromString(val);
        }
    */
    public boolean getBoolean(BooleanDynamicParameter parameter) {
        String val = null;
        int i = 0;
        try {
            preparedLookup.clearParameters();
            preparedLookup.setString(1, parameter.getFullName());

            ResultSet rs = preparedLookup.executeQuery();
            while (rs.next()) {
                val = rs.getString(1);
                i++;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (i++ > 1) {
            throw new
                    IllegalArgumentException(
                    "Shouldn't have more than one value for " + parameter.getFullName());
        }

        return parameter.getValueFromString(val);
    }


    public int getInt(IntDynamicParameter parameter) {
        String val = null;
        int i = 0;
        try {
            preparedLookup.clearParameters();
            preparedLookup.setString(1, parameter.getFullName());

            ResultSet rs = preparedLookup.executeQuery();
            while (rs.next()) {
                val = rs.getString(1);
                i++;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (i++ > 1) {
            throw new
                    IllegalArgumentException(
                    "Shouldn't have more than one value for " + parameter.getFullName());
        }

        return parameter.getValueFromString(val);
    }

    public LanguageDirection getLanguageDirection(LangDirDynamicParameter parameter) {
        String val = null;
        int i = 0;
        try {
            preparedLookup.clearParameters();
            preparedLookup.setString(1, parameter.getFullName());

            ResultSet rs = preparedLookup.executeQuery();
            while (rs.next()) {
                val = rs.getString(1);
                i++;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (i++ > 1) {
            throw new
                    IllegalArgumentException(
                    "Shouldn't have more than one value for " + parameter.getFullName());
        }

        return parameter.getValueFromString(val);
    }

    public String getString(StringDynamicParameter parameter) {
        int i = 0;
        String val = null;
        try {
            preparedLookup.clearParameters();
            preparedLookup.setString(1, parameter.getFullName());

            ResultSet rs = preparedLookup.executeQuery();
            while (rs.next()) {
                val = rs.getString(1);
                i++;
            }
            rs.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (i++ > 1) {
            throw new
                    IllegalArgumentException(
                    "Shouldn't have more than one value for " + parameter.getFullName());
        }

        return parameter.getValueFromString(val);
    }


    public void commit() throws SQLException {
        connection.commit();
    }

    public List<String> getStringList(StringListDynamicParameter parameter) {
        int i = 0;
        String val = null;
        try {
            preparedLookup.clearParameters();
            preparedLookup.setString(1, parameter.getFullName());

            ResultSet rs = preparedLookup.executeQuery();
            while (rs.next()) {
                val = rs.getString(1);
                i++;
            }
            rs.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (i++ > 1) {
            throw new
                    IllegalArgumentException(
                    "Shouldn't have more than one value for " + parameter.getFullName());
        }

        return parameter.getValueFromString(val);
    }
}

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

import org.apache.lucene.queryparser.classic.ParseException;
import org.rhapsode.app.utils.DBUtils;
import org.rhapsode.lucene.search.SCField;
import org.rhapsode.lucene.search.StoredConcept;
import org.rhapsode.lucene.search.StoredConceptBuilder;
import org.rhapsode.lucene.utils.SqlUtil;
import org.rhapsode.lucene.utils.StoredConceptManager;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DBStoredConceptManager implements StoredConceptManager, Closeable {

    final static String STORED_CONCEPT_TABLE = "stored_concepts";
    //1 name
    //2 concept query
    //3 concept query translation
    //4 notes
    //5 exception message

    final static ColInfo[] COL_INFO_LIST = new ColInfo[SCField.values().length];

    static {
        int i = 0;
        for (SCField field : SCField.values()) {
            if (i == 0) {
                COL_INFO_LIST[i] = build(field, "PRIMARY KEY");
            } else {
                COL_INFO_LIST[i] = build(field);
            }
            i++;
        }
    }

    static ColInfo build(SCField scField) {
        return new ColInfo(
                scField.getDbName(),
                scField.getType(),
                scField.getMaxLength()
        );
    }

    public static ColInfo build(SCField scField, String constraints) {
        return new ColInfo(
                scField.getDbName(),
                scField.getType(),
                scField.getMaxLength(),
                constraints
        );
    }

    private final static int MAX_REWRITES = 20;

    private final Matcher regexRangeMatcher = Pattern.compile("(?:^\\s*\\d+\\s*(,\\s*\\d*)?$)|(?:^\\s*,\\d+$)").matcher("");
    private final Matcher storedMatcher = Pattern.compile("\\{\\s*([^{}]+)\\s*\\}").matcher("");
    //private final Matcher priorityNameMatcher = Pattern.compile("^(?:(\\d+)_)?(.*?)\\.txt$").matcher("");
    //private final Matcher junkCleaner = Pattern.compile("-").matcher("");
    private final Connection connection;
    private final PreparedStatement preparedLookup;
    private final PreparedStatement preparedInsert;
    private final PreparedStatement preparedUpdate;
    private final PreparedStatement preparedCopy;
    private final PreparedStatement preparedUpdateException;
    private final TableDef conceptTable;

    public static DBStoredConceptManager load(Connection connection) throws IOException, SQLException {
        return new DBStoredConceptManager(connection);
    }


    private DBStoredConceptManager(Connection connection) throws SQLException {
        this.connection = connection;
        this.conceptTable = new TableDef(STORED_CONCEPT_TABLE,
                COL_INFO_LIST);
        //connection.createStatement().execute("DROP TABLE IF EXISTS "+ STORED_CONCEPT_TABLE);

        conceptTable.createIfNotExists(connection);
        preparedLookup =
                connection.prepareStatement(
                        "select * from " + STORED_CONCEPT_TABLE + " where " +
                                SCField.NAME.getDbName() + "= ?"
                );
        preparedInsert =
                connection.prepareStatement(
                        "insert into " + STORED_CONCEPT_TABLE + " VALUES (?,?,?,?,?)"
                );
        preparedUpdateException =
                connection.prepareStatement(
                        "update " + STORED_CONCEPT_TABLE + " set " +
                                SCField.CONCEPT_EXCEPTION_MSG.getDbName() + "= ? " +
                                "where " + SCField.NAME.getDbName() + "=?"
                );

        preparedUpdate =
                connection.prepareStatement(
                        "update " + STORED_CONCEPT_TABLE + " set " +
                                SCField.CONCEPT_QUERY.getDbName() + "=?" +
                                " where " + SCField.NAME.getDbName() + "=?"
                );
        preparedCopy =
                connection.prepareStatement(
                        "select " +
                                SCField.NAME.getDbName() + ", " +
                                SCField.CONCEPT_QUERY.getDbName() + ", " +
                                SCField.CONCEPT_TRANSLATION.getDbName() + ", " +
                                SCField.NOTES.getDbName() + ", " +
                                SCField.CONCEPT_EXCEPTION_MSG.getDbName() +
                                " from " + STORED_CONCEPT_TABLE
                );
    }

    public void close() throws IOException {
        Exception throwIO = null;
        for (PreparedStatement p : new PreparedStatement[]{
                preparedCopy,
                preparedInsert,
                preparedLookup,
                preparedUpdate
        }) {
            try {
                p.close();
            } catch (SQLException e) {
                throwIO = e;
            }
        }
        try {
            connection.commit();
        } catch (SQLException e) {
            throwIO = e;
        }
        try {
            connection.close();
        } catch (SQLException e) {
            throwIO = e;
        }
        if (throwIO != null) {
            throw new IOException(throwIO);
        }
    }

    @Override
    public String rewriteQuery(String s) throws ParseException {
        if (s == null) {
            return null;
        }
        int rewrites = 0;
        while (rewrites++ < MAX_REWRITES) {
            storedMatcher.reset(s);
            int last = 0;
            StringBuilder sb = new StringBuilder();
            boolean found = false;
            while (storedMatcher.find()) {
                String cName = storedMatcher.group(1);
                if (regexRangeMatcher.reset(cName).find()) {
                    last = storedMatcher.end();
                    continue;
                }
                String q = null;
                try {
                    q = map(cName);
                    //parse exception is thrown from here if cName can't be found
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                sb.append(s.substring(last, storedMatcher.start()));
                sb.append("(").append(q).append(")");
                found = true;
                last = storedMatcher.end();
            }
            if (found == false) {
                return s;
            }
            sb.append(s.substring(last));
            s = sb.toString();
        }
        return s;
    }

    @Override
    public void addConcept(StoredConcept storedConcept) {
        try {
            addConceptNoCommit(storedConcept);
            connection.commit();
        } catch (SQLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void addConceptNoCommit(StoredConcept storedConcept) throws SQLException {
        int i = 1;
        for (SCField field : SCField.values()) {
            preparedInsert.setString(i++, storedConcept.getString(field));
        }
        preparedInsert.execute();
    }

    public void updateConcept(String concept, String query) throws SQLException {
        preparedUpdate.setString(1, DBUtils.escapeSQL(query));
        preparedUpdate.setString(2, DBUtils.escapeSQL(concept));
        int cnt = preparedUpdate.executeUpdate();
        if (cnt < 1) {
            throw new SQLException("Concept didn't exist for updating:" + concept);
        }
        if (cnt > 1) {
            throw new SQLException("More than one concept matched: " + concept);
        }
        connection.commit();
    }

    @Override
    public Map<String, StoredConcept> getConceptMap() {
        Map<String, StoredConcept> concepts = new LinkedHashMap<>();
        try (ResultSet rs = preparedCopy.executeQuery()) {
            while (rs.next()) {
                String name = rs.getString(1);
                StoredConceptBuilder storedConceptBuilder = new StoredConceptBuilder(name);
                int i = 1;
                for (SCField field : SCField.values()) {
                    if (field.equals(SCField.NAME)) {
                        i++;
                        continue;
                    }

                    switch (field.getType()) {
                        case Types.VARCHAR:
                            storedConceptBuilder.add(field, rs.getString(i));
                            break;
                        default:
                            throw new RuntimeException("Need to support " + SqlUtil.getTypeName(field.getType()));
                    }
                    i++;
                }
                concepts.put(name, storedConceptBuilder.build());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Collections.unmodifiableMap(concepts);
    }

    public void deleteConcepts() {

        String sql = "TRUNCATE TABLE " + STORED_CONCEPT_TABLE;
        try (Statement st = connection.createStatement()) {
            st.execute(sql);
            connection.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String map(String cName) throws SQLException, ParseException {
        preparedLookup.setString(1, cName);
        ResultSet rs = preparedLookup.executeQuery();
        String target = null;
        int i = 0;
        while (rs.next()) {
            target = rs.getString(SCField.CONCEPT_QUERY.getDbName());
            i++;
        }
        if (i > 1) {
            throw new RuntimeException("Shouldn't be more than one result: " + cName);
        }
        if (target != null) {
            return target;
        }
        throw new ParseException("Couldn't find:" + cName + " as a stored concept");
    }

    //TODO just for testing ... clean up
    Connection getConnection() {
        return connection;
    }

    public void updateQueryExceptionMessage(String name, String msg) throws SQLException {
        preparedUpdateException.clearParameters();
        if (msg == null) {
            preparedUpdateException.setNull(1, Types.VARCHAR);
        } else {
            preparedUpdateException.setString(1, msg);
        }
        preparedUpdateException.setString(2, name);
        preparedUpdateException.execute();
    }
}

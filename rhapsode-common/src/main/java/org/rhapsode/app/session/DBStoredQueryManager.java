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

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.classic.ParseException;
import org.rhapsode.app.config.RhapsodeSearcherApp;
import org.rhapsode.app.utils.DBUtils;
import org.rhapsode.lucene.search.ComplexQueryBuilder;
import org.rhapsode.lucene.search.SQField;
import org.rhapsode.lucene.search.StoredQuery;
import org.rhapsode.lucene.search.StoredQueryBuilder;
import org.rhapsode.lucene.utils.SqlUtil;

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
import java.util.concurrent.atomic.AtomicInteger;

public class DBStoredQueryManager implements Closeable {

    final static String STORED_QUERY_TABLE = "stored_queries";

    //1 name
    //2 main
    //3 main translation
    //4 filter
    //5 filter translation
    //6 max hits
    //7 default field
    //8 highlighting style
    //9 highlighting priority
    //10 notes

    final static ColInfo[] COL_INFO_LIST = new ColInfo[SQField.values().length];

    static {
        int i = 0;
        for (SQField field : SQField.values()) {
            if (i == 0) {
                COL_INFO_LIST[i] = build(field, "PRIMARY KEY");
            } else {
                COL_INFO_LIST[i] = build(field);
            }
            i++;
        }
    }


    private final Connection connection;
    private final PreparedStatement preparedLookup;
    private final PreparedStatement preparedInsert;
    private final PreparedStatement preparedUpdate;
    private final PreparedStatement preparedCopy;
    private final PreparedStatement prepareMainQueryExceptionMsg;
    private final PreparedStatement prepareFilterQueryExceptionMsg;
    private final PreparedStatement prepareDeleteSelectedQuery;

    private final TableDef storedQueryTable;

    private final static AtomicInteger NUM_RECORDS = new AtomicInteger(-1);

    public static DBStoredQueryManager load(Connection connection) throws IOException, SQLException {
        return new DBStoredQueryManager(connection);
    }

    static ColInfo build(SQField sqField) {
        return new ColInfo(
                sqField.getDbName(),
                sqField.getType(),
                sqField.getMaxLength()
        );
    }

    public static ColInfo build(SQField sqField, String constraints) {
        return new ColInfo(
                sqField.getDbName(),
                sqField.getType(),
                sqField.getMaxLength(),
                constraints
        );
    }


    private DBStoredQueryManager(Connection connection) throws SQLException {
        this.connection = connection;
        this.storedQueryTable = new TableDef(STORED_QUERY_TABLE, COL_INFO_LIST);

        //connection.createStatement().execute("DROP TABLE IF EXISTS "+STORED_QUERY_TABLE);
        storedQueryTable.createIfNotExists(connection);
        prepareMainQueryExceptionMsg =
                connection.prepareStatement(
                        "update " + STORED_QUERY_TABLE + " set " +
                                SQField.MAIN_QUERY_EXCEPTION_MSG.getDbName() + " = ? " +
                                "where " + SQField.ID.getDbName() + "=?");

        prepareFilterQueryExceptionMsg =
                connection.prepareStatement(
                        "update " + STORED_QUERY_TABLE + " set " +
                                SQField.FILTER_QUERY_EXCEPTION_MSG.getDbName() + " = ? " +
                                "where " + SQField.ID.getDbName() + "=?");

        preparedLookup =
                connection.prepareStatement(
                        "select " +
                                StringUtils.join(
                                        StoredQuery.getDBColNames(), ", ") +
                                " from " + STORED_QUERY_TABLE +
                                " where " + SQField.ID.getDbName() + "= ?"
                );
        preparedInsert =
                connection.prepareStatement(
                        "insert into " + STORED_QUERY_TABLE + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
                );

        preparedCopy =
                connection.prepareStatement(
                        "select " +
                                StringUtils.join(
                                        StoredQuery.getDBColNames(), ", ") +
                                " from " + STORED_QUERY_TABLE +
                                " order by " + SQField.ID.getDbName()
                );
        prepareDeleteSelectedQuery =
                connection.prepareStatement(
                        "delete from " + STORED_QUERY_TABLE +
                                " where " + SQField.ID.getDbName() + "= ?"
                );


        StringBuilder sb = new StringBuilder();
        sb.append("update ").append(STORED_QUERY_TABLE).append(" ");
        sb.append("set ");
        int i = 0;
        for (SQField sqField : SQField.values()) {

            if (!sqField.equals(SQField.NAME)) {
                if (i++ > 0) {
                    sb.append(", ");
                }
                sb.append(sqField.getDbName()).append("=?");
            }
        }
        sb.append(" where ").append(SQField.ID.getDbName()).append("=?");
        preparedUpdate =
                connection.prepareStatement(sb.toString());

        resetMax();
    }

    public synchronized void resetMax() throws SQLException {
        NUM_RECORDS.set(-1);
        String sql = "select max(" + SQField.ID.getDbName() + ") from " + STORED_QUERY_TABLE;
        try (Statement st = connection.createStatement()) {
            try (ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    NUM_RECORDS.set(rs.getInt(1));
                }
            }
        }
    }

    public void close() throws IOException {
        Exception throwIO = null;
        for (PreparedStatement p : new PreparedStatement[]{
                preparedCopy,
                preparedInsert,
                preparedLookup,
                // preparedUpdate
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

    public void addQuery(StoredQuery q) throws SQLException {
        addQueryNoCommit(q);
        connection.commit();
        resetMax();
    }

    public void addQueryNoCommit(StoredQuery q) throws SQLException {
        updatePreparedInsert(q, preparedInsert);
        preparedInsert.execute();
    }

   /* public void updateQuery(String name, StoredQuery q) throws SQLException {
//TODO: fix this        updatePreparedInsert(name, q, preparedUpdate);
        int cnt = preparedUpdate.executeUpdate();
        if (cnt < 1) {
            throw new SQLException("Stored query didn't exist for updating:" + name);
        }
        if (cnt > 1) {
            throw new SQLException("More than one query matched: " + name);
        }
        connection.commit();
    }*/

    private void updatePreparedInsert(StoredQuery q,
                                      PreparedStatement ps) throws SQLException {
        int i = 1;
        ps.clearParameters();
        if (q.getId() == StoredQuery.NOT_YET_LOADED) {
            q.setId(NUM_RECORDS.incrementAndGet());
        }
        for (SQField sqField : SQField.values()) {
            switch (sqField.getType()) {
                case Types.VARCHAR:
                    String v = q.getString(sqField);
                    if (StringUtils.isBlank(v)) {
                        ps.setNull(i, Types.VARCHAR);
                    } else {
                        ps.setString(i, v);
                    }
                    break;
                case Types.INTEGER:
                    Integer intv = q.getInteger(sqField);
                    if (intv == null) {
                        ps.setNull(i, Types.INTEGER);
                    } else {
                        ps.setInt(i, intv);
                    }
                    break;
                default:
                    throw new RuntimeException("Can't yet handle: " + SqlUtil.getTypeName(sqField.getType()));
            }
            i++;
        }

    }

    public int update(StoredQuery q) throws SQLException {

        int i = 1;
        preparedUpdate.clearParameters();
        for (SQField sqField : SQField.values()) {
            if (sqField.equals(SQField.NAME)) {
                continue;
            }
            switch (sqField.getType()) {
                case Types.VARCHAR:
                    String v = q.getString(sqField);
                    if (StringUtils.isBlank(v)) {
                        preparedUpdate.setNull(i, Types.VARCHAR);
                    } else {
                        preparedUpdate.setString(i, v);
                    }
                    break;
                case Types.INTEGER:
                    Integer intv = q.getInteger(sqField);
                    if (intv == null) {
                        preparedUpdate.setNull(i, Types.INTEGER);
                    } else {
                        preparedUpdate.setInt(i, intv);
                    }
                    break;
                default:
                    throw new RuntimeException("Can't yet handle: " + SqlUtil.getTypeName(sqField.getType()));
            }
            i++;
        }
        preparedUpdate.setInt(i, q.getId());
        return preparedUpdate.executeUpdate();
    }

    public void deleteAllQueries() throws SQLException {
        String sql = "TRUNCATE TABLE " + STORED_QUERY_TABLE;
        try (Statement st = connection.createStatement()) {
            st.execute(sql);
            connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
        resetMax();
    }

    //TODO just for testing ... clean up
    Connection getConnection() {
        return connection;
    }

    public Map<Integer, StoredQuery> getStoredQueryMap() {
        Map<Integer, StoredQuery> queries = new LinkedHashMap<>();
        try (ResultSet rs = preparedCopy.executeQuery()) {
            while (rs.next()) {
                Integer id = rs.getInt(1);
                String name = DBUtils.unescapeSQL(rs.getString(2));
                StoredQueryBuilder storedQueryBuilder = new StoredQueryBuilder(id, name);
                int i = 1;
                for (SQField field : SQField.values()) {
                    if (field.equals(SQField.NAME) || field.equals(SQField.ID)) {
                        i++;
                        continue;
                    }

                    switch (field.getType()) {
                        case Types.VARCHAR:
                            storedQueryBuilder.add(field, rs.getString(i));
                            break;
                        case Types.INTEGER:
                            int val = rs.getInt(i);
                            storedQueryBuilder.add(field, Integer.toString(val));
                            break;
                        default:
                            throw new RuntimeException("Need to support " + SqlUtil.getTypeName(field.getType()));
                    }
                    i++;
                }
                queries.put(id, storedQueryBuilder.build());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Collections.unmodifiableMap(queries);
    }

    //will return null if sqid doesn't exist!!!
    public StoredQuery getStoredQuery(int id) throws IOException {
        StoredQuery sq = null;
        try {
            preparedLookup.clearParameters();
            preparedLookup.setInt(1, id);
            ResultSet rs = preparedLookup.executeQuery();
            int cnt = 0;
            while (rs.next()) {
                String sqName = rs.getString(2);
                StoredQueryBuilder storedQueryBuilder = new StoredQueryBuilder(id, sqName);
                int i = 1;
                for (SQField field : SQField.values()) {
                    if (field.equals(SQField.NAME) || field.equals(SQField.ID)) {
                        i++;
                        continue;
                    }
                    switch (field.getType()) {
                        case Types.VARCHAR:
                            storedQueryBuilder.add(field, rs.getString(i));
                            break;
                        case Types.INTEGER:
                            int val = rs.getInt(i);
                            storedQueryBuilder.add(field, Integer.toString(val));
                            break;
                        default:
                            throw new RuntimeException("Need to support " + SqlUtil.getTypeName(field.getType()));
                    }
                    i++;
                }

                sq = storedQueryBuilder.build();
                cnt++;
            }
            rs.close();
            if (cnt > 1) {
                throw new IllegalArgumentException("Shouldn't have more than one match for: " + id);
            }
            return sq;
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    public void updateMainQueryExceptionMessage(Integer id, String message) {
        try {
            prepareMainQueryExceptionMsg.clearParameters();
            if (message == null) {
                prepareMainQueryExceptionMsg.setNull(1, Types.VARCHAR);
            } else {
                prepareMainQueryExceptionMsg.setString(1, message);
            }
            prepareMainQueryExceptionMsg.setInt(2, id);
            prepareMainQueryExceptionMsg.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateFilterQueryExceptionMessage(Integer id, String message) {
        try {
            prepareFilterQueryExceptionMsg.clearParameters();
            if (message == null) {
                prepareFilterQueryExceptionMsg.setNull(1, Types.VARCHAR);
            } else {
                prepareFilterQueryExceptionMsg.setString(1, message);
            }
            prepareFilterQueryExceptionMsg.setInt(2, id);
            prepareFilterQueryExceptionMsg.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int deleteQuery(Integer queryId) throws SQLException {
        prepareDeleteSelectedQuery.clearParameters();
        prepareDeleteSelectedQuery.setInt(1, queryId);
        int modified = prepareDeleteSelectedQuery.executeUpdate();
        resetMax();
        return modified;
    }

    public void updateQueryExceptions(StoredQuery sq, RhapsodeSearcherApp searcherApp) {
        try {
            ComplexQueryBuilder.validateMain(sq,
                    searcherApp.getParserPlugin(), searcherApp.getSessionManager().getStoredConceptManager());
        } catch (ParseException e) {
            updateMainQueryExceptionMessage(sq.getId(), e.getMessage());
        }
        try {
            ComplexQueryBuilder.validateFilter(sq,
                    searcherApp.getParserPlugin(), searcherApp.getSessionManager().getStoredConceptManager());
        } catch (ParseException e) {
            updateFilterQueryExceptionMessage(sq.getId(), e.getMessage());
        }
    }

    public void validateQueries(RhapsodeSearcherApp searcherApp) throws SQLException {
        Map<Integer, StoredQuery> map = getStoredQueryMap();
        for (Map.Entry<Integer, StoredQuery> e : map.entrySet()) {
            StoredQuery sq = e.getValue();
            try {
                ComplexQueryBuilder.validateMain(sq,
                        searcherApp.getParserPlugin(), searcherApp.getSessionManager().getStoredConceptManager());
                prepareMainQueryExceptionMsg.clearParameters();
                prepareMainQueryExceptionMsg.setNull(1, Types.VARCHAR);
                prepareMainQueryExceptionMsg.setInt(2, sq.getId());
                prepareMainQueryExceptionMsg.execute();
            } catch (ParseException ex) {
                updateMainQueryExceptionMessage(sq.getId(), ex.getMessage());
            }
            try {
                ComplexQueryBuilder.validateFilter(sq,
                        searcherApp.getParserPlugin(), searcherApp.getSessionManager().getStoredConceptManager());
                prepareFilterQueryExceptionMsg.clearParameters();
                prepareFilterQueryExceptionMsg.setNull(1, Types.VARCHAR);
                prepareFilterQueryExceptionMsg.setInt(2, sq.getId());
                prepareFilterQueryExceptionMsg.execute();
            } catch (ParseException ex) {
                updateFilterQueryExceptionMessage(sq.getId(), ex.getMessage());
            }
        }
    }
}

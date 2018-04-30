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

import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class CollectionsHistory {

    final static String COLLECTIONS_HISTORY_TABLE = "collections_history";

    final static ColInfo PATH =
            new ColInfo("PATH", Types.VARCHAR, 1024, "PRIMARY KEY");

    final static ColInfo LAST_LOADED =
            new ColInfo("LAST_LOADED", Types.TIMESTAMP);

    private Connection connection;
    private TableDef table;

    PreparedStatement deleteRow;
    PreparedStatement winnowOnTimeStamp;
    PreparedStatement insert;
    PreparedStatement selectStar;

    public static CollectionsHistory load(Connection connection) throws SQLException {
        return new CollectionsHistory(connection);
    }

    private CollectionsHistory(Connection connection) throws SQLException {
        this.connection = connection;
        this.table = new TableDef(COLLECTIONS_HISTORY_TABLE, PATH, LAST_LOADED);
        this.table.createIfNotExists(connection);

        deleteRow = connection.prepareStatement("DELETE FROM " + COLLECTIONS_HISTORY_TABLE +
                " WHERE " + PATH.getName() + " = ?");
        insert = connection.prepareStatement("INSERT INTO " + COLLECTIONS_HISTORY_TABLE +
                " VALUES (?, ?)");

        selectStar = connection.prepareStatement("SELECT " + PATH.getName() + ", " + LAST_LOADED.getName() +
                " FROM " + COLLECTIONS_HISTORY_TABLE + " ORDER BY " + LAST_LOADED.getName() + " DESC");

        winnowOnTimeStamp = connection.prepareStatement("DELETE FROM " + COLLECTIONS_HISTORY_TABLE +
                " WHERE " + LAST_LOADED.getName() + " < ?");
    }

    public void addLoaded(Path path) throws SQLException {
        //delete it if it already exists;
        deleteRow.setString(1, path.toAbsolutePath().toString());
        deleteRow.execute();

        insert.setString(1, path.toAbsolutePath().toString());
        insert.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
        insert.execute();
        connection.commit();
        //now delete any that are older than 10
        String sql = "SELECT " + LAST_LOADED.getName() + " FROM " + COLLECTIONS_HISTORY_TABLE +
                " ORDER BY " + LAST_LOADED.getName() + " DESC LIMIT 11";
        Date last10 = null;
        try (Statement st = connection.createStatement()) {
            ResultSet rs = st.executeQuery(sql);
            int i = 0;
            while (rs.next()) {
                if (i == 9) {
                    last10 = rs.getTimestamp(1);
                    break;
                }
                i++;
            }
            rs.close();
        }
        if (last10 != null) {
            winnowOnTimeStamp.setTimestamp(1, new Timestamp(last10.getTime()));
            winnowOnTimeStamp.execute();
        }
        connection.commit();
    }

    public List<Pair<Path, Date>> getPaths() throws SQLException {
        ResultSet rs = selectStar.executeQuery();
        List<Pair<Path, Date>> results = new LinkedList<>();
        while (rs.next()) {
            results.add(Pair.of(
                    Paths.get(rs.getString(1)),
                    rs.getTimestamp(2)));
        }
        return results;
    }

}

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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;


public class InputTableManager {

    final static ColInfo[] COL_INFO_LIST = new ColInfo[2];
    private final static String KEY = "k";
    private final static String VALUE = "v";

    static {
        COL_INFO_LIST[0] = new ColInfo(KEY, Types.VARCHAR, 256, "PRIMARY KEY");
        COL_INFO_LIST[1] = new ColInfo(VALUE, Types.VARCHAR, 10000);
    }


    public static InputTableManager load(Connection connection) throws IOException, SQLException {
        return new InputTableManager(connection);
    }

    private final String TABLE_NAME = "input_table";
    private final String INPUT_DIR_KEY = "input_dir";
    private final Connection connection;
    private final TableDef inputTableTable;

    private final PreparedStatement getInputDirectory;
    private final PreparedStatement setInputDirectory;

    private InputTableManager(Connection connection) throws SQLException {
        this.connection = connection;
        this.inputTableTable = new TableDef(TABLE_NAME, COL_INFO_LIST);
        connection.createStatement().execute("DROP TABLE IF EXISTS " + TABLE_NAME);

        inputTableTable.createIfNotExists(connection);

        getInputDirectory = connection.prepareStatement("select " + VALUE + " from " + TABLE_NAME + " where " + KEY + "='" + INPUT_DIR_KEY + "'");
        setInputDirectory = connection.prepareStatement("update " + TABLE_NAME + " set " + VALUE + "= ? where " + KEY + "='" + INPUT_DIR_KEY + "'");
    }

    public Path getInputDirectory() {
        try (ResultSet rs = getInputDirectory.executeQuery()) {
            while (rs.next()) {
                return Paths.get(rs.getString(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setInputDirectory(Path p) throws SQLException {
        setInputDirectory.clearParameters();
        setInputDirectory.setString(1, p.toAbsolutePath().toString());
        setInputDirectory.setString(1, INPUT_DIR_KEY);
        setInputDirectory.executeUpdate();
    }
}

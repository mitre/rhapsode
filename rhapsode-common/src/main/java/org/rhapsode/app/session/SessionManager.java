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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.google.gson.JsonObject;

public class SessionManager {
    final static String CONNECTION_STRING_KEY = "connection_string";
    final Connection connection;
    CollectionsHistory collectionsHistory;
    DBStoredConceptManager storedConceptManager;
    InputTableManager inputTableManager;

    private DBStoredQueryManager storedQueryManager;
    private DynamicParameterConfig dynamicParameterConfig;

    private SessionManager(Connection connection) {
        this.connection = connection;
    }

    public static SessionManager load(JsonObject session_manager) throws SQLException, IOException {
        String connectionString = session_manager.get(CONNECTION_STRING_KEY).getAsString().toString();
        Connection connection = DriverManager.getConnection(connectionString);
        SessionManager sm = new SessionManager(connection);
        sm.collectionsHistory = CollectionsHistory.load(connection);
        sm.storedConceptManager = DBStoredConceptManager.load(connection);
        sm.storedQueryManager = DBStoredQueryManager.load(connection);
        sm.dynamicParameterConfig = DynamicParameterConfig.load(connection);
        sm.inputTableManager = InputTableManager.load(connection);
        return sm;
    }

    public DBStoredConceptManager getStoredConceptManager() {
        return storedConceptManager;
    }

    public CollectionsHistory getCollectionsHistory() {
        return collectionsHistory;
    }

    public DBStoredQueryManager getStoredQueryManager() {
        return storedQueryManager;
    }

    public DynamicParameterConfig getDynamicParameterConfig() {
        return dynamicParameterConfig;
    }

    public Connection getConnection() {
        return connection;
    }

    public InputTableManager getTableManager() {
        return inputTableManager;
    }
}

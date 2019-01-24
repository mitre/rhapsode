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

package org.rhapsode.indexer;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.rhapsode.RhapsodeCollection;

public class RhapsodeIndexerConfig {
    RhapsodeCollection rc;
    IndexerSettings indexerSettings;
    private int numDocsBetweenCommits;

    public static RhapsodeIndexerConfig load(RhapsodeCollection rc, Path searchConfigFile) throws IOException {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeHierarchyAdapter(RhapsodeIndexerConfig.class, new RhapsodeIndexerConfigDeserializer());
        Gson gson = builder.create();
        RhapsodeIndexerConfig config = null;
        try (Reader r = Files.newBufferedReader(searchConfigFile, StandardCharsets.UTF_8)) {
            config = gson.fromJson(r, RhapsodeIndexerConfig.class);
        }
        config.rc = rc;
        return config;
    }

    public RhapsodeCollection getRhapsodeCollection() {
        return rc;
    }

    public int getNumDocsBetweenCommits() {
        return numDocsBetweenCommits;
    }

    public void setNumDocsBetweenCommits(int numDocsBetweenCommits) {
        this.numDocsBetweenCommits = numDocsBetweenCommits;
    }
}

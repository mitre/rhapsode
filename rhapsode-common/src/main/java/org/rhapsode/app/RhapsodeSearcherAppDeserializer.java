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
package org.rhapsode.app;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.rhapsode.app.session.SessionManager;
import org.rhapsode.lucene.search.CommonSearchConfig;
import org.rhapsode.lucene.search.basic.BasicSearchConfig;
import org.rhapsode.lucene.search.concordance.ConcordanceSearchConfig;
import org.rhapsode.lucene.search.cooccur.CooccurConfig;

public class RhapsodeSearcherAppDeserializer
        implements JsonDeserializer<RhapsodeSearcherApp> {

    @Override
    public RhapsodeSearcherApp deserialize(JsonElement jsonElement, Type type,
                                           JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        RhapsodeSearcherApp config = new RhapsodeSearcherApp();
        if (!jsonElement.isJsonObject()) {
            throw new IllegalArgumentException("RhapsodeIndexConfig's main element must be a map");
        }
        JsonObject root = (JsonObject) jsonElement;
        config.commonSearchConfig = CommonSearchConfig.build(root.get("common_search"));
        config.basicSearchConfig = BasicSearchConfig.build(root.get("basic_search"));
        config.concordanceSearchConfig = ConcordanceSearchConfig.Builder.build(root.get("concordance_search"));
        config.cooccurConfig = CooccurConfig.Builder.build(root.get("cooccur_search"));
        //TODO: parameterize this
        config.queryParserType = RhapsodeSearcherApp.ParserType.SPAN_QUERY;

        //needs to happen before building linkwriter
        try {
            config.sessionManager = SessionManager.load(root.getAsJsonObject("session_manager"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return config;

    }

}

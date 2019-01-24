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

package org.rhapsode.lucene.search;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.rhapsode.lucene.utils.JsonUtil;


public class CommonSearchConfig {

    private int numThreadsForConcurrentSearches;
    //maps queryName of style to literal css
    private Map<String, String> highlightingStyles = new LinkedHashMap<>();


    public static CommonSearchConfig build(JsonElement el) {
        CommonSearchConfig config = new CommonSearchConfig();
        config.numThreadsForConcurrentSearches = JsonUtil.getInt(el, "numThreadsForConcurrentSearches");

        JsonObject styles = ((JsonObject) el).getAsJsonObject("styles");
        for (Map.Entry<String, JsonElement> e : styles.entrySet()) {
            String k = e.getKey();
            String v = e.getValue().getAsString();
            if (!StringUtils.isBlank(k) && !StringUtils.isBlank(v)) {
                config.highlightingStyles.put(k, v);
            }
        }
        config.highlightingStyles = Collections.unmodifiableMap(config.highlightingStyles);
        return config;
    }

    public int getNumThreadsForConcurrentSearches() {
        return numThreadsForConcurrentSearches;
    }

    public Map<String, String> getHighlightingStyles() {
        return highlightingStyles;
    }
}

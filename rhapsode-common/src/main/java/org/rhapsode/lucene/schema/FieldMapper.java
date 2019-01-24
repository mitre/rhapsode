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
package org.rhapsode.lucene.schema;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class FieldMapper {

    public static final String NAME = "field_mapper";
    static String IGNORE_CASE_KEY = "ignore_case";


    Map<String, List<IndivFieldMapper>> mappers = new HashMap<>();
    private boolean ignoreCase = true;

    public static FieldMapper load(JsonElement el) {
        if (el == null) {
            throw new IllegalArgumentException(NAME + " must not be empty");
        }
        JsonObject root = el.getAsJsonObject();

        if (root.size() == 0) {
            return new FieldMapper();
        }
        //build ignore case element
        JsonElement ignoreCaseElement = root.get(IGNORE_CASE_KEY);
        if (ignoreCaseElement == null || !ignoreCaseElement.isJsonPrimitive()) {
            throw new IllegalArgumentException(
                    "ignore case element in field mapper must not be null and must be a primitive: "
                            + ((ignoreCaseElement == null) ? "" : ignoreCaseElement.toString()));
        }
        String ignoreCaseString = ((JsonPrimitive) ignoreCaseElement).getAsString().toLowerCase();
        FieldMapper mapper = new FieldMapper();
        if ("true".equals(ignoreCaseString)) {
            mapper.setIgnoreCase(true);
        } else if ("false".equals(ignoreCaseString)) {
            mapper.setIgnoreCase(false);
        } else {
            throw new IllegalArgumentException(IGNORE_CASE_KEY + " must have a value of \"true\" or \"false\"");
        }


        JsonArray mappings = root.getAsJsonArray("mappings");
        for (JsonElement mappingElement : mappings) {
            JsonObject mappingObj = mappingElement.getAsJsonObject();
            if (mappingObj.has("f")) {
                String from = mappingObj.getAsJsonPrimitive("f").getAsString();
                IndivFieldMapper indivFieldMapper = buildMapper(mappingObj);
                mapper.add(from, indivFieldMapper);
            } else {
                throw new IllegalArgumentException("mapping must have an 'f' (from) field");
            }
        }
        return mapper;
    }

    private static IndivFieldMapper buildMapper(JsonObject mappingObj) {
        List<IndivFieldMapper> tmp = new LinkedList<>();
        String to = mappingObj.getAsJsonPrimitive("t").getAsString();
        JsonObject mapper = mappingObj.getAsJsonObject("capture");
        if (mapper != null) {
            String pattern = mapper.getAsJsonPrimitive("find").getAsString();
            String replace = mapper.getAsJsonPrimitive("replace").getAsString();
            String failPolicyString = mapper.getAsJsonPrimitive("fail_policy").getAsString().toLowerCase(Locale.ENGLISH);

            CaptureFieldMapper.FAIL_POLICY fp = null;
            if (failPolicyString == null) {
                //can this even happen?
                fp = CaptureFieldMapper.FAIL_POLICY.SKIP_FIELD;
            } else if (failPolicyString.equals("skip")) {
                fp = CaptureFieldMapper.FAIL_POLICY.SKIP_FIELD;
            } else if (failPolicyString.equals("store_as_is")) {
                fp = CaptureFieldMapper.FAIL_POLICY.STORE_AS_IS;
            } else if (failPolicyString.equals("exception")) {
                fp = CaptureFieldMapper.FAIL_POLICY.EXCEPTION;
            }
            tmp.add(new CaptureFieldMapper(to, pattern, replace, fp));
        }
        mapper = mappingObj.getAsJsonObject(IndexSchemaSerializer.LANG_ID);
        if (mapper != null) {
            //todo specify model directory
            int maxChars = mapper.getAsJsonPrimitive(IndexSchemaSerializer.MAX_CHARS).getAsInt();
            int minChars = mapper.getAsJsonPrimitive(IndexSchemaSerializer.MIN_CHARS).getAsInt();
            boolean langOnly = mapper.getAsJsonPrimitive(IndexSchemaSerializer.LANG_ONLY).getAsBoolean();
            tmp.add(new LangIdMapper(to, minChars, maxChars, langOnly));
        }
        mapper = mappingObj.getAsJsonObject(IndexSchemaSerializer.LANG_IDS);
        if (mapper != null) {
            //todo specify model directory
            int maxChars = mapper.getAsJsonPrimitive(IndexSchemaSerializer.MAX_CHARS).getAsInt();
            int minChars = mapper.getAsJsonPrimitive(IndexSchemaSerializer.MIN_CHARS).getAsInt();
            boolean langOnly = mapper.getAsJsonPrimitive(IndexSchemaSerializer.LANG_ONLY).getAsBoolean();
            double minConfidence = mapper.getAsJsonPrimitive(IndexSchemaSerializer.MIN_CONFIDENCE).getAsDouble();
            tmp.add(new LangIdsMapper(to, minChars, maxChars, langOnly, minConfidence));
        }


        if (tmp.size() == 0) {
            return new IdentityFieldMapper(to);
        } else if (tmp.size() == 1) {
            return tmp.get(0);
        } else {
            return new ChainedFieldMapper(to, tmp);
        }
    }


    public void add(String k, IndivFieldMapper m) {
        List<IndivFieldMapper> ms = mappers.get(k);
        if (ms == null) {
            ms = new LinkedList<>();
        }
        ms.add(m);
        mappers.put(k, ms);
    }

    public List<IndivFieldMapper> get(String field) {
        return mappers.get(field);
    }

    public Set<String> getTikaFields() {
        return mappers.keySet();
    }

    public boolean getIgnoreCase() {
        return ignoreCase;
    }

    public void setIgnoreCase(boolean v) {
        this.ignoreCase = v;
    }

    public void clear() {
        mappers.clear();
    }
}

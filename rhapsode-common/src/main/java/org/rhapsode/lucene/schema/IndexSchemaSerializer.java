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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.document.FieldType;
import org.rhapsode.lucene.analysis.MyTokenizerChain;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class IndexSchemaSerializer implements JsonSerializer<IndexSchema> {

    static final String FIELD_MAPPER = "field_mapper";
    static final String FIELDS = "fields";
    static final String ANALYZERS = "analyzers";

    static final String MIN_CHARS = "min_chars";
    static final String MAX_CHARS = "max_chars";
    static final String LANG_ONLY = "lang_only";
    static final String LANG_ID = "lang_id";
    static final String LANG_IDS = "lang_ids";
    public static final String MIN_CONFIDENCE = "min_confidence";

    static String SYSTEM_FIELDS = "system_fields";
    static String MULTIVALUED = "multivalued";
    static String TEXT = "text";
    static String STRING = "string";
    static String FIELD_TYPE = "type";
    static String TOKEN_FILTERS = "tokenfilters";
    static String CHAR_FILTERS = "charfilters";
    static String TOKENIZER = "tokenizer";
    static String FACTORY = "factory";
    static String PARAMS = "params";

    static String IGNORE_CASE = "ignore_case";
    static String MAPPINGS = "mappings";
    static String FROM_FIELD = "f";
    static String TO_FIELD = "t";
    static String CAPTURE = "capture";
    static String FIND = "find";
    static String REPLACE = "replace";
    static String FAIL_POLICY = "fail_policy";

    static String INDEX_ANALYZER = "index_analyzer";
    static String QUERY_ANALYZER = "query_analyzer";
    static String MT_QUERY_ANALYZER = "mt_query_analyzer";
    static String OFFSET_ANALYZER = "offset_analyzer";


    @Override
    public JsonElement serialize(IndexSchema indexSchema, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject root = new JsonObject();
        root.add(SYSTEM_FIELDS, serializeSystemFields(indexSchema));
        root.add(FIELD_MAPPER, serializeFieldMapper(indexSchema));
        root.add(FIELDS, serializeFields(indexSchema));
        root.add(ANALYZERS, serializeAnalyzers(indexSchema));
        return root;
    }


    private JsonElement serializeFields(IndexSchema indexSchema) {
        JsonObject jsonFields = new JsonObject();
        for (Map.Entry<String, FieldDef> e : indexSchema.fields.entrySet()) {
            jsonFields.add(e.getKey(), fromFieldDef(e.getValue()));
        }
        return jsonFields;
    }

    private JsonElement fromFieldDef(FieldDef fieldDef) {
        JsonObject jsonFieldDef = new JsonObject();
        jsonFieldDef.add(MULTIVALUED, new JsonPrimitive(fieldDef.allowMulti));
        FieldType ft = fieldDef.fieldType;
        //TODO: need to add other data types
        if (ft.tokenized()) {
            jsonFieldDef.add(FIELD_TYPE, new JsonPrimitive(TEXT));
        } else {
            jsonFieldDef.add(FIELD_TYPE, new JsonPrimitive(STRING));
        }

        if (fieldDef.getIndexAnalyzerName() != null) {
            jsonFieldDef.add(INDEX_ANALYZER, new JsonPrimitive(fieldDef.getIndexAnalyzerName()));
        }
        if (fieldDef.getQueryAnalyzerName() != null) {
            jsonFieldDef.add(QUERY_ANALYZER, new JsonPrimitive(fieldDef.getQueryAnalyzerName()));
        }
        if (fieldDef.getMtQueryAnalyzerName() != null) {
            jsonFieldDef.add(MT_QUERY_ANALYZER, new JsonPrimitive(fieldDef.getMtQueryAnalyzerName()));
        }
        if (fieldDef.getOffsetAnalyzerName() != null) {
            jsonFieldDef.add(OFFSET_ANALYZER, new JsonPrimitive(fieldDef.getOffsetAnalyzerName()));
        }
        return jsonFieldDef;
    }

    private JsonElement serializeFieldMapper(IndexSchema indexSchema) {
        JsonObject jsonFieldMapper = new JsonObject();
        jsonFieldMapper.add(IGNORE_CASE, new JsonPrimitive(indexSchema.fieldMapper.getIgnoreCase()));
        JsonArray jsonMappings = new JsonArray();

        for (Map.Entry<String, List<IndivFieldMapper>> e : indexSchema.fieldMapper.mappers.entrySet()) {
            String fromField = e.getKey();
            for (IndivFieldMapper indivFieldMapper : e.getValue()) {
                jsonMappings.add(serializeIndivFieldMapper(fromField, indivFieldMapper));
            }
        }
        jsonFieldMapper.add(MAPPINGS, jsonMappings);
        return jsonFieldMapper;
    }

    private JsonElement serializeIndivFieldMapper(String fromField, IndivFieldMapper indivFieldMapper) {
        JsonObject jsonIndivFieldMapper = new JsonObject();
        jsonIndivFieldMapper.add(FROM_FIELD, new JsonPrimitive(fromField));
        jsonIndivFieldMapper.add(TO_FIELD, new JsonPrimitive(indivFieldMapper.getToField()));
        if (indivFieldMapper instanceof CaptureFieldMapper) {
            CaptureFieldMapper cfm = (CaptureFieldMapper) indivFieldMapper;
            jsonIndivFieldMapper.add(TO_FIELD, new JsonPrimitive(cfm.getToField()));
            JsonObject jsonCFM = new JsonObject();
            jsonCFM.add(FIND, new JsonPrimitive(cfm.getCaptureString()));
            jsonCFM.add(REPLACE, new JsonPrimitive(cfm.getReplace()));
            jsonCFM.add(FAIL_POLICY, new JsonPrimitive(cfm.getFailPolicy().toString().toLowerCase(Locale.ENGLISH)));

            jsonIndivFieldMapper.add(CAPTURE, jsonCFM);
        } else if (indivFieldMapper instanceof LangIdMapper) {
            LangIdMapper lid = (LangIdMapper) indivFieldMapper;
            JsonObject jsonLid = new JsonObject();
            jsonLid.add(MIN_CHARS, new JsonPrimitive(lid.minChars));
            jsonLid.add(MAX_CHARS, new JsonPrimitive(lid.maxChars));
            jsonLid.add(LANG_ONLY, new JsonPrimitive(lid.langOnly));
            jsonIndivFieldMapper.add(LANG_ID, jsonLid);
        } else if (indivFieldMapper instanceof LangIdsMapper) {
            LangIdsMapper lid = (LangIdsMapper) indivFieldMapper;
            JsonObject jsonLid = new JsonObject();
            jsonLid.add(MIN_CHARS, new JsonPrimitive(lid.minChars));
            jsonLid.add(MAX_CHARS, new JsonPrimitive(lid.maxChars));
            jsonLid.add(LANG_ONLY, new JsonPrimitive(lid.langOnly));
            jsonLid.add(MIN_CONFIDENCE, new JsonPrimitive(lid.minConfidence));
            jsonIndivFieldMapper.add(LANG_ID, jsonLid);
        }
        return jsonIndivFieldMapper;
    }

    private JsonElement serializeSystemFields(IndexSchema indexSchema) {
        JsonObject jsonSystemFields = new JsonObject();
        for (Map.Entry<String, String> e : indexSchema.systemFields.entrySet()) {
            jsonSystemFields.add(e.getKey(), new JsonPrimitive(e.getValue()));
        }
        return jsonSystemFields;
    }

    private JsonElement serializeAnalyzers(IndexSchema indexSchema) {
        JsonObject analyzers = new JsonObject();
        for (Map.Entry<String, Analyzer> e : indexSchema.analyzers.entrySet()) {
            analyzers.add(e.getKey(), serializeAnalyzer(e.getKey(), e.getValue()));
        }
        return analyzers;
    }

    private JsonElement serializeAnalyzer(String name, Analyzer analyzer) {
        if (!(analyzer instanceof MyTokenizerChain)) {
            throw new IllegalArgumentException("analyzer (" + name + ") must be a MyTokenizerChain!\n" +
                    "However, I see that it is: " + ((analyzer == null) ? "null" : analyzer.getClass()));
        }
        JsonObject jsonAnalyzer = new JsonObject();
        MyTokenizerChain tc = (MyTokenizerChain) analyzer;
        JsonArray charFilters = new JsonArray();
        for (CharFilterFactory cff : tc.getCharFilterFactories()) {

        }
        if (charFilters.size() > 0) {
            jsonAnalyzer.add(CHAR_FILTERS, charFilters);
        }

        JsonObject jsonTokenizer = new JsonObject();
        jsonTokenizer.add(FACTORY, new JsonPrimitive(tc.getTokenizerFactory().getClass().getName()));
        JsonObject tokenizerParams = new JsonObject();
        for (Map.Entry<String, String> e : tc.getTokenizerFactory().getOriginalArgs().entrySet()) {
            tokenizerParams.add(e.getKey(), new JsonPrimitive(e.getValue()));
        }
        jsonTokenizer.add(PARAMS, tokenizerParams);
        jsonAnalyzer.add(TOKENIZER, jsonTokenizer);

        JsonArray jsonFilters = new JsonArray();
        for (TokenFilterFactory tff : tc.getTokenFilterFactories()) {
            JsonObject jsonTFF = new JsonObject();
            jsonTFF.add(FACTORY, new JsonPrimitive(tff.getClass().getName()));
            JsonObject tffParams = new JsonObject();
            for (Map.Entry<String, String> e : tc.getTokenizerFactory().getOriginalArgs().entrySet()) {
                tffParams.add(e.getKey(), new JsonPrimitive(e.getValue()));
            }
            jsonTFF.add(PARAMS, tffParams);
            jsonFilters.add(jsonTFF);
        }

        if (jsonFilters.size() > 0) {
            jsonAnalyzer.add(TOKEN_FILTERS, jsonFilters);
        }
        return jsonAnalyzer;
    }

}

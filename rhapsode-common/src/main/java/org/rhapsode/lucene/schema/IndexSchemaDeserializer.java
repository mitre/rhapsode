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

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;

class IndexSchemaDeserializer implements JsonDeserializer<org.rhapsode.lucene.schema.IndexSchema> {


    @Override
    public IndexSchema deserialize(JsonElement element, Type type,
                                   JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        final JsonObject root = element.getAsJsonObject();
        org.rhapsode.lucene.schema.IndexSchema indexSchema = new org.rhapsode.lucene.schema.IndexSchema();
        addSystemFields(indexSchema, root.getAsJsonObject("system_fields"));
        try {
            addAnalyzers(indexSchema, root.getAsJsonObject("analyzers"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            addFields(indexSchema, root.getAsJsonObject("fields"));
        } catch (IOException e) {
            throw new JsonParseException("IOException loading analyzer", e);
        }
        indexSchema.fieldMapper = FieldMapper.load(root.getAsJsonObject(FieldMapper.NAME));

        JsonElement iof = root.get("indexer_overwrite_field");
        if (iof != null) {

            indexSchema.indexerOverwriteField = iof.getAsString();
        }
        testMissingField(indexSchema);
        testOverwriteField(indexSchema);
        return indexSchema;
    }

    private void testOverwriteField(IndexSchema indexSchema) {
        String indexerOverwriteField = indexSchema.getIndexerOverwriteField();
        if (indexerOverwriteField == null) {
            return;
        }
        if (!indexSchema.getDefinedFields().contains(indexerOverwriteField)) {
            throw new IllegalArgumentException(
                    "Must define a field for the indexer_overwrite_field: " + indexerOverwriteField);
        }
    }


    private void addSystemFields(IndexSchema indexSchema, JsonObject system_fields) {
        if (system_fields == null) {
            throw new IllegalArgumentException("Must specify \"system_fields\" element in index schema");
        }
        for (Map.Entry<String, JsonElement> e : system_fields.entrySet()) {
            String k = e.getKey();
            if (!e.getValue().isJsonPrimitive()) {
                throw new IllegalArgumentException("value for system field " +
                        k + " must be a json primitive");
            }
            indexSchema.systemFields.put(k, e.getValue().getAsString());
        }
        if (!indexSchema.systemFields.containsKey(IndexSchema.UNIQUE_FILE_KEY_FIELD_KEY)) {
            throw new IllegalArgumentException("Must specify a " +
                    IndexSchema.UNIQUE_FILE_KEY_FIELD_KEY + " within the \"system_fields\"" +
                    " element of the index schema");
        }
        if (!indexSchema.systemFields.containsKey(IndexSchema.UNIQUE_DOC_KEY_FIELD_KEY)) {
            throw new IllegalArgumentException("Must specify a " +
                    IndexSchema.UNIQUE_DOC_KEY_FIELD_KEY + " within the \"system_fields\"" +
                    " element of the index schema");
        }
        if (!indexSchema.systemFields.containsKey(IndexSchema.DEFAULT_CONTENT_FIELD_KEY)) {
            throw new IllegalArgumentException("Must specify a " +
                    IndexSchema.DEFAULT_CONTENT_FIELD_KEY + " within the \"system_fields\"" +
                    " element of the index schema");
        }

        if (!indexSchema.systemFields.containsKey(IndexSchema.ATTACHMENT_OFFSET_KEY)) {
            throw new IllegalArgumentException("Must specify a " +
                    IndexSchema.ATTACHMENT_OFFSET_KEY + " within the \"system_fields\"" +
                    " element of the index schema");
        }
        if (!indexSchema.systemFields.containsKey(IndexSchema.ATTACHMENT_INDEX_SORT_FIELD_KEY)) {
            throw new IllegalArgumentException("Must specify a " +
                    IndexSchema.ATTACHMENT_INDEX_SORT_FIELD_KEY + " within the \"system_fields\"" +
                    " element of the index schema");
        }

    }

    private void addAnalyzers(org.rhapsode.lucene.schema.IndexSchema indexSchema, JsonObject analyzers) throws IOException {
        for (Map.Entry<String, JsonElement> e : analyzers.entrySet()) {
            String analyzerName = e.getKey();

            if (e.getValue() == null || !e.getValue().isJsonObject()) {
                throw new IllegalArgumentException("Must have map of keys values after analyzer name");
            }
            Analyzer analyzer = AnalyzerDeserializer.buildAnalyzer(analyzerName, e.getValue());
            indexSchema.addAnalyzer(analyzerName, analyzer);
        }

    }


    private void addFields(IndexSchema indexSchema, JsonObject fields) throws IOException {
        for (Map.Entry<String, JsonElement> e : fields.entrySet()) {
            String fieldName = e.getKey();

            if (e.getValue() == null || !e.getValue().isJsonObject()) {
                throw new IllegalArgumentException("Must have map of keys values after field name");
            }
            FieldType type = buildFieldType((JsonObject) e.getValue());
            boolean allowMulti = figureAllowMulti((JsonObject) e.getValue());
            FieldDef fieldDef = new FieldDef(fieldName, allowMulti, type);
            addAnalyzersToField(fieldDef, ((JsonObject) e.getValue()), indexSchema);
            indexSchema.addField(fieldName, fieldDef);
        }
    }

    private void addAnalyzersToField(FieldDef fieldDef, JsonObject jsonObject,
                                     IndexSchema schema) throws IOException {
        NamedAnalyzer indexAnalyzer = getAnalyzer(fieldDef, jsonObject, IndexSchemaSerializer.INDEX_ANALYZER, false, schema);
        NamedAnalyzer queryAnalyzer = getAnalyzer(fieldDef, jsonObject, IndexSchemaSerializer.QUERY_ANALYZER, true, schema);
        NamedAnalyzer mtQueryAnalyzer = getAnalyzer(fieldDef, jsonObject, IndexSchemaSerializer.MT_QUERY_ANALYZER, true, schema);
        NamedAnalyzer offsetAnalyzer = getAnalyzer(fieldDef, jsonObject, IndexSchemaSerializer.OFFSET_ANALYZER, true, schema);

        if (!fieldDef.fieldType.tokenized() && (
                indexAnalyzer != null ||
                        queryAnalyzer != null ||
                        mtQueryAnalyzer != null ||
                        offsetAnalyzer != null
        )) {
            throw new IllegalArgumentException("Shouldn't specify an analyzer for a field " +
                    "that isn't tokenized: " + fieldDef.fieldName);
        }

        if (fieldDef.fieldType.tokenized() && indexAnalyzer == null) {
            throw new IllegalArgumentException("Must specify at least an " +
                    IndexSchemaSerializer.INDEX_ANALYZER +
                    " for this tokenized field:" + fieldDef.fieldName);
        }
        fieldDef.setAnalyzers(indexAnalyzer, queryAnalyzer, mtQueryAnalyzer, offsetAnalyzer);
    }

    private NamedAnalyzer getAnalyzer(FieldDef fieldDef, JsonObject jsonObject, String whichAnalyzer,
                                      boolean allowNull, IndexSchema schema) {
        JsonPrimitive el = jsonObject.getAsJsonPrimitive(whichAnalyzer);
        if (el == null) {
            if (!fieldDef.fieldType.tokenized() || allowNull) {
                return null;
            } else {
                throw new IllegalArgumentException(whichAnalyzer + " cannot be null for field: " +
                        fieldDef.fieldName);
            }
        }
        String analyzerName = el.getAsString();
        Analyzer analyzer = schema.getAnalyzerByName(analyzerName);
        if (analyzer == null) {
            throw new IllegalArgumentException("Must define analyzer named \"" + analyzerName + "\" " +
                    "for field: " + fieldDef.fieldName);
        }
        return new NamedAnalyzer(analyzerName, analyzer);
    }

    private boolean figureAllowMulti(JsonObject value) {
        JsonElement el = value.getAsJsonPrimitive(IndexSchemaSerializer.MULTIVALUED);
        if (el == null) {
            return true;
        }
        String mString = el.getAsString();
        if (StringUtils.isEmpty(mString)) {
            return true;
        } else if ("true".equals(mString.toLowerCase(Locale.ENGLISH))) {
            return true;
        } else if ("false".equals(mString.toLowerCase(Locale.ENGLISH))) {
            return false;
        } else {
            throw new IllegalArgumentException(IndexSchemaSerializer.MULTIVALUED +
                    " must have value of \"true\" or \"false\"");
        }
    }

    private FieldType buildFieldType(JsonObject value) {
        JsonElement el = value.getAsJsonPrimitive("type");
        if (el == null) {
            throw new IllegalArgumentException("Must specify field \"type\"");
        }
        FieldType type = new FieldType();
        String typeString = el.getAsString();
        if (typeString.equals(IndexSchemaSerializer.TEXT)) {
            type.setTokenized(true);
            //TODO: make this configurable..do we need this for keyword tokenizer?
            type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
        } else if (typeString.equals(IndexSchemaSerializer.STRING)) {
            type.setTokenized(false);
            type.setIndexOptions(IndexOptions.NONE);
        } else {
            throw new IllegalArgumentException("Can only support \"text\" or \"string\" field types so far");
        }

        //TODO: make these configurable
        type.setStored(true);

        return type;
    }

    private void testMissingField(IndexSchema indexSchema) {
        FieldMapper m = indexSchema.getFieldMapper();
        for (String from : m.getTikaFields()) {
            for (IndivFieldMapper f : m.get(from)) {
                String luceneField = f.getToField();
                if (!indexSchema.getDefinedFields().contains(luceneField)) {
                    throw new IllegalArgumentException("Field mapper's 'to' field (" +
                            luceneField + ") must have a field definition");
                }
            }
        }
    }

}

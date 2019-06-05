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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexSchema {

    private static final Logger LOG = LoggerFactory.getLogger(IndexSchema.class);

    static String DEFAULT_CONTENT_FIELD_KEY = "default_content_field";
    static String UNIQUE_FILE_KEY_FIELD_KEY = "unique_file_key_field";//this has the key for the original input doc
    //indexer copies this from the parent document
    static String UNIQUE_DOC_KEY_FIELD_KEY = "unique_doc_key_field";//this is generated at index time by the Rhapsode
    // Indexer and isunique per lucene document
    static String LINK_DISPLAY_FIELD_KEY = "display_name_field";

    static String REL_PATH_KEY = "rel_path_field";
    static String CONTAINS_DIGEST_KEY = "contains_digest_field";
    static String ATTACHMENT_OFFSET_KEY = "attachment_offset_field";
    static String ATTACHMENT_INDEX_SORT_FIELD_KEY = "attachment_offset_sort_field";
    static String EMBEDDED_PATH_FIELD_KEY = "embedded_path_field";


    Map<String, FieldDef> fields = new HashMap<>();
    Map<String, Analyzer> analyzers = new HashMap<>();
    Map<String, String> systemFields = new HashMap<>();

    FieldMapper fieldMapper = new FieldMapper();
    String indexerOverwriteField = null;//should move this into indexer config!

    public static IndexSchema load(InputStream is) throws IOException {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeHierarchyAdapter(IndexSchema.class, new IndexSchemaDeserializer());
        Gson gson = builder.create();
        return gson.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), IndexSchema.class);
    }

    public static IndexSchema load(Path p) throws IOException {
        LOG.debug("about to load this schema file: " + p.toAbsolutePath());
        try (InputStream is = new BufferedInputStream(Files.newInputStream(p))) {
            return load(is);
        }
    }

    public static void write(IndexSchema indexSchema, OutputStream os) throws IOException {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeHierarchyAdapter(IndexSchema.class, new IndexSchemaSerializer());
        Gson gson = builder.create();
        Writer writer = new OutputStreamWriter(os, StandardCharsets.UTF_8);
        gson.toJson(indexSchema, writer);
        writer.flush();
    }

    public Analyzer getIndexAnalyzer() {
        Map<String, Analyzer> map = new HashMap<>();
        for (Map.Entry<String, FieldDef> e : fields.entrySet()) {
            String fieldName = e.getKey();
            if (e.getValue().fieldType.tokenized()) {
                map.put(fieldName, e.getValue().getIndexAnalyzer());
            }
        }
        return new PerFieldAnalyzerWrapper(null, map);
    }

    public void addField(String fieldName, org.rhapsode.lucene.schema.FieldDef fieldDef) {
        fields.put(fieldName, fieldDef);
    }

    protected void addAnalyzer(String analyzerName, Analyzer analyzer) {
        analyzers.put(analyzerName, analyzer);
    }

    public FieldDef getFieldDef(String fieldName) {
        return fields.get(fieldName);
    }

    public Set<String> getDefinedFields() {
        return fields.keySet();
    }

    protected Analyzer getAnalyzerByName(String analyzerName) {
        return analyzers.get(analyzerName);
    }

    public Analyzer getOffsetAnalyzer() {
        Map<String, Analyzer> map = new HashMap<>();
        for (Map.Entry<String, FieldDef> e : fields.entrySet()) {
            String fieldName = e.getKey();
            if (e.getValue().fieldType.tokenized()) {
                map.put(fieldName, e.getValue().getOffsetAnalyzer());
            }
        }
        return new PerFieldAnalyzerWrapper(null, map);
    }

    public Analyzer getOffsetAnalyzer(String field) {
        return fields.get(field).getOffsetAnalyzer();
    }

    public Analyzer getQueryAnalyzer() {
        Map<String, Analyzer> map = new HashMap<>();
        for (Map.Entry<String, FieldDef> e : fields.entrySet()) {
            String fieldName = e.getKey();
            if (e.getValue().fieldType.tokenized()) {
                map.put(fieldName, e.getValue().getQueryAnalyzer());
            }
        }
        return new PerFieldAnalyzerWrapper(null, map);
    }

    /**
     * The unique id for each Lucene document.
     * If a complex document has many many attachments,
     * this will be unique for each attachment.
     * <p>
     * This is system generated.
     *
     * @return the unique id for each indexed document
     */
    public String getUniqueDocField() {
        return systemFields.get(UNIQUE_DOC_KEY_FIELD_KEY);
    }

    /**
     * This is the unique key for an input file.
     * If a complex document has attachments, the main document
     * and its attachments will all share this key.
     *
     * @return
     */
    public String getUniqueFileField() {
        return systemFields.get(UNIQUE_FILE_KEY_FIELD_KEY);
    }

    /**
     * @return field that contains the relative path of the input document
     * without the added .json/.txt or anything else.
     */
    public String getRelPathField() {
        return systemFields.get(REL_PATH_KEY);
    }

    /**
     * field that is used to record the child md5s in the parent document
     *
     * @return
     */
    public String getContainsDigestField() {
        return systemFields.get(CONTAINS_DIGEST_KEY);
    }

    /**
     * If nothing is specified, use this field as the default "content" field
     *
     * @return
     */
    public String getDefaultContentField() {
        return systemFields.get(DEFAULT_CONTENT_FIELD_KEY);
    }

    public void setDefaultContentField(String defaultContentField) {
        systemFields.put(DEFAULT_CONTENT_FIELD_KEY, defaultContentField);
    }

    public String getLinkDisplayField() {
        return systemFields.get(LINK_DISPLAY_FIELD_KEY);
    }

    public void setLinkDisplayField(String displayField) {
        systemFields.put(LINK_DISPLAY_FIELD_KEY, displayField);
    }

    public FieldMapper getFieldMapper() {
        return fieldMapper;
    }

    public String getAttachmentIndexField() {
        return systemFields.get(ATTACHMENT_OFFSET_KEY);
    }

    public String getIndexerOverwriteField() {
        return indexerOverwriteField;
    }

    public String getEmbeddedPathField() {
        return systemFields.get(EMBEDDED_PATH_FIELD_KEY);
    }

    public String getAttachmentSortField() {
        return systemFields.get(ATTACHMENT_INDEX_SORT_FIELD_KEY);
    }

    public void clearUserFields() {
        Set<String> toRemove = new HashSet<>();
        for (String k : fields.keySet()) {
            if (!k.startsWith("_")) {
                toRemove.add(k);
            }
        }
        for (String r : toRemove) {
            fields.remove(r);
        }
    }
}

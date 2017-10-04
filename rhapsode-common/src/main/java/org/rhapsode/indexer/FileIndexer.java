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

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.tika.batch.fs.FSProperties;
import org.apache.tika.metadata.Metadata;
import org.rhapsode.lucene.schema.FieldDef;
import org.rhapsode.lucene.schema.FieldMapper;
import org.rhapsode.lucene.schema.IndexSchema;
import org.rhapsode.lucene.schema.IndivFieldMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class FileIndexer {
    private static final Logger LOG = LoggerFactory.getLogger(FileIndexer.class);
    private static final String TIKA_MD5_KEY = "X-TIKA:digest:MD5";
    private final Object lock = new Object();
    private boolean committing = false;
    final RhapsodeIndexerConfig config;
    final IndexWriter writer;
    final IndexSchema indexSchema;

    public FileIndexer(RhapsodeIndexerConfig config, IndexWriter writer) {
        this.config = config;
        this.indexSchema = config.getRhapsodeCollection().getIndexSchema();
        this.writer = writer;
    }

    public void writeDocument(List<Metadata> metadataList) throws IOException {
        if (metadataList == null || metadataList.size() == 0) {
            return;

        }
        Metadata parent = metadataList.get(0);
        //insert attachment index fields and update childmd5s in parent
        for (int i = 1; i < metadataList.size(); i++) {
            Metadata child = metadataList.get(i);
            child.set(config.getRhapsodeCollection().getIndexSchema().getAttachmentIndexField(), Integer.toString(i));
            String childMD5 = child.get(TIKA_MD5_KEY);
            if (childMD5 != null) {
                parent.add(indexSchema.getContainsDigestField(), childMD5);
            }
        }

        String parentPath = parent.get(FSProperties.FS_REL_PATH);
        List<Document> documents = new ArrayList<>();
        String uniqFileValue = UUID.randomUUID().toString();//this is the unique key for the initial input document
        Document parentDocument = buildDocument(parent, uniqFileValue, uniqFileValue);
        if (parentDocument != null) {
            parentDocument.add(new SortedNumericDocValuesField(indexSchema.getAttachmentSortField(), 0));
            documents.add(parentDocument);
        }
        String indexerOverwriteValue = null;
        String indexerOverwriteField = indexSchema.getIndexerOverwriteField();
        if (indexerOverwriteField != null) {
            indexerOverwriteValue = parentDocument.get(indexerOverwriteField);
        }

        for (int i = 1; i < metadataList.size(); i++) {
            Metadata child = metadataList.get(i);
            child.set(indexSchema.getAttachmentIndexField(), Integer.toString(i));

            //add the parent path into the child's
            child.set(FSProperties.FS_REL_PATH, parentPath);
            Document d = buildDocument(metadataList.get(i), uniqFileValue);
            //need to overwrite child document's overwrite field.
            //the value has already been mapped in the parent's
            if (indexerOverwriteField != null) {
                d.removeField(indexerOverwriteField);
                d.add(new TextField(indexerOverwriteField, indexerOverwriteValue, Field.Store.YES));
            }
            d.add(new SortedNumericDocValuesField(indexSchema.getAttachmentSortField(), i));

            if (d != null) {
                documents.add(d);
            }
        }
        writer.addDocuments(documents);
    }

    private Document buildDocument(Metadata m, String uniqFileId) throws IOException {
        return buildDocument(m, uniqFileId, UUID.randomUUID().toString());
    }

    private Document buildDocument(Metadata m, String uniqFileId,
                                   String uniqLuceneDocId) throws IOException {
        Map<String, String> lcKeyMap = new HashMap<>();
        FieldMapper fieldMapper = config.getRhapsodeCollection().getIndexSchema().getFieldMapper();
        if (fieldMapper.getIgnoreCase()) {
            for (String n : m.names()) {
                String lc = n.toLowerCase(Locale.ENGLISH);
                if (fieldMapper.getTikaFields().contains(lc)) {
                    lcKeyMap.put(n, lc);
                }
            }
        } else {
            for (String n : m.names()) {
                if (fieldMapper.getTikaFields().contains(n)) {
                    lcKeyMap.put(n, n);
                } else {
                }
            }
        }
        Document document = new Document();

        for (String n : fieldMapper.getTikaFields()) {
            String[] tikaValues = m.getValues(lcKeyMap.get(n));
            for (IndivFieldMapper indivFieldMapper : fieldMapper.get(n)) {
                FieldDef fieldDef = getDef(indexSchema, indivFieldMapper.getToField());
                //write the primary fields to the document
                fieldDef.addFields(indivFieldMapper.map(tikaValues), document);
            }
        }
        FieldDef def = getDef(indexSchema, indexSchema.getUniqueDocField());
        def.addFields(new String[]{
                uniqLuceneDocId
        }, document);


        def = getDef(indexSchema, indexSchema.getUniqueFileField());
        def.addFields(new String[]{
                uniqFileId
        }, document);
        return document;
    }

    private FieldDef getDef(IndexSchema indexSchema, String targetField) {
        FieldDef def = indexSchema.getFieldDef(targetField);
        if (def == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Couldn't find \"")
                    .append(targetField)
                    .append("\" specified in the index_schema.");
            sb.append("\n");
            sb.append("I am aware of these:\n");
            for (String s : indexSchema.getDefinedFields()) {
                sb.append(s).append("\n");
            }
            throw new IllegalArgumentException(sb.toString());
        }
        return def;
    }

}

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

package org.rhapsode.lucene.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldDocs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocRetriever {

    private static final Logger LOG = LoggerFactory.getLogger(DocRetriever.class);

    private static int MAX_EMBEDDED_DOCS = 10000;

    public static List<Document> getAllDocsFromAnyDocId(int id, String fileIdField,
                                                        Sort sort, Set<String> fieldsToRetrieve, IndexSearcher searcher) throws IOException {
        Set<String> initialFieldsToRetrieve = new HashSet<>();
        initialFieldsToRetrieve.add(fileIdField);
        Document doc = searcher.doc(id, initialFieldsToRetrieve);
        String fileId = doc.get(fileIdField);
        return getAllDocsByFileId(fileIdField, fileId, sort, fieldsToRetrieve, searcher);
    }

    public static List<Document> getAllDocsFromAnyDocId(String docId, String docIdField, String fileIdField,
                                                        Sort sort,
                                                        Set<String> fieldsToRetrieve, IndexSearcher searcher) throws IOException {
        Query q = new TermQuery(new Term(docIdField, docId));
        TopDocs topDocs = searcher.search(q, 2);
        ScoreDoc[] sd = topDocs.scoreDocs;
        if (sd == null) {
            throw new IllegalArgumentException("Couldn't find doc with docId=" + docId + " in this field:" + docIdField);
        }
        if (sd.length == 0) {
            throw new IllegalArgumentException("Couldn't find doc with docId=" + docId + " in this field:" + docIdField);
        }
        if (sd.length == 2) {
            throw new IllegalArgumentException("Found more than one document with docId=" + docId + " in this field:" + docIdField);
        }
        Set<String> initialFieldsToRetrieve = new HashSet<>();
        initialFieldsToRetrieve.add(fileIdField);
        Document doc = searcher.doc(sd[0].doc, initialFieldsToRetrieve);
        String fileId = doc.get(fileIdField);
        return getAllDocsByFileId(fileIdField, fileId, sort, fieldsToRetrieve, searcher);
    }


    public static List<Document> getAllDocsByFileId(String fileIdField,
                                                    String fileId,
                                                    Sort sort, Set<String> fieldsToRetrieve,
                                                    IndexSearcher searcher) throws IOException {
        if (fileIdField == null) {
            throw new IllegalArgumentException("fileIdField must not be null");
        }
        if (fileId == null) {
            throw new IllegalArgumentException("fileId must not be null for: " + fileIdField + " file id field");
        }


        Query q = new TermQuery(new Term(fileIdField, fileId));
        ScoreDoc[] scoreDocs = null;
        if (sort == null) {
            TopDocs topDocs = searcher.search(q, MAX_EMBEDDED_DOCS);
            scoreDocs = topDocs.scoreDocs;
        } else {
            TopFieldDocs topFieldDocs = searcher.search(q, MAX_EMBEDDED_DOCS, sort, false, false);
            scoreDocs = topFieldDocs.scoreDocs;
        }

        if (scoreDocs == null) {
            LOG.info("NO RESULTS for " + fileId + " in " + fileIdField);
            return new ArrayList<>();
        }

        List<Document> ret = new ArrayList<>();
        for (ScoreDoc sd : scoreDocs) {
            Document d = null;
            if (fieldsToRetrieve == null) {
                d = searcher.doc(sd.doc);
            } else {
                d = searcher.doc(sd.doc, fieldsToRetrieve);
            }
            ret.add(d);
        }
        return ret;
    }
}

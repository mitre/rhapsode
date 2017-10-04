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

import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


public class SearcherUtils {

    public static Document getUniqueDocument(IndexSearcher searcher, int docId, String keyField,
                                             String docKey, Set<String> fieldsToRetrieve) throws IOException {
        //try docId first
        Set<String> tmp = new HashSet<>();
        tmp.addAll(fieldsToRetrieve);
        tmp.add(keyField);
        if (docId > -1) {
            Document d = searcher.doc(docId, fieldsToRetrieve);
            if (d != null) {
                return d;
            }
        }

        Query q = new TermQuery(new Term(keyField, docKey));
        TopDocs td = searcher.search(q, 2);
        ScoreDoc[] scoreDocs = td.scoreDocs;
        if (scoreDocs.length == 0) {
            throw new IllegalArgumentException("Couldn't find document with key: " + docKey +
                    " in field: " + keyField);
        } else if (scoreDocs.length > 1) {
            throw new IllegalArgumentException("Found more than one document with this key: " + docKey);
        }
        return searcher.doc(scoreDocs[0].doc, tmp);
    }


    public static Pair<DocLuceneIdPair, DocLuceneIdPair> getBeforeAndAfter(IndexSearcher searcher, Query retrievalQuery,
                                                                           int rank, int docId,
                                                                           String keyField, String docKey, Set<String> fieldsToRetrieve)
            throws IOException {
        TopDocs topDocs = searcher.search(retrievalQuery, rank + 1);
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        if (scoreDocs.length == 0) {
            return null;
        }
        if (scoreDocs.length > rank - 1 && scoreDocs[rank - 1].doc == docId) {
            return getPair(searcher, scoreDocs, rank, fieldsToRetrieve);
        }

        topDocs = searcher.search(retrievalQuery, rank + 1000);
        scoreDocs = topDocs.scoreDocs;
        int pre = rank;
        int post = rank + 1;
        Set<String> keyFields = new HashSet<>();
        keyFields.add(keyField);
        int actualRank = -1;
        while (pre >= 0 || post < scoreDocs.length) {
            if (post > -1 && post < scoreDocs.length) {
                Document d = searcher.doc(scoreDocs[post].doc, keyFields);
                if (d.get(keyField) != null && d.get(keyField).equals(docKey)) {
                    actualRank = post + 1;
                    break;
                }
            }
            if (pre > -1 && pre < scoreDocs.length) {
                Document d = searcher.doc(scoreDocs[pre].doc, keyFields);
                if (d.get(keyField) != null && d.get(keyField).equals(docKey)) {
                    actualRank = pre + 1;
                    break;
                }
            }
            post++;
            pre--;
        }
        if (actualRank > -1) {
            return getPair(searcher, scoreDocs, actualRank, fieldsToRetrieve);
        }
        return null;
    }

    private static Pair<DocLuceneIdPair, DocLuceneIdPair> getPair(IndexSearcher searcher, ScoreDoc[] scoreDocs,
                                                                  int rank, Set<String> fieldsToRetrieve) throws IOException {
        int rankPre = rank - 2;
        Document preDocument = null;
        int docIdPre = -1;
        if (rankPre >= 0) {
            preDocument = searcher.doc(scoreDocs[rankPre].doc);
            docIdPre = scoreDocs[rankPre].doc;
        }
        int rankPost = rank;
        Document postDocument = null;
        int docIdPost = -1;
        if (rank < scoreDocs.length) {
            postDocument = searcher.doc(scoreDocs[rankPost].doc);
            docIdPost = scoreDocs[rankPost].doc;
        }
        return Pair.of(new DocLuceneIdPair(preDocument, docIdPre), new DocLuceneIdPair(postDocument, docIdPost));
    }
}

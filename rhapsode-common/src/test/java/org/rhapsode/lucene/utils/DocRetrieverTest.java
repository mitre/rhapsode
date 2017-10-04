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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.rhapsode.util.PathUtils;

public class DocRetrieverTest {

    static final String DOC_ID_FIELD = "_did";
    static final String FILE_ID_FIELD = "_fid";
    static final String EMBEDDED_ID_FIELD = "_eid";
    static final String SORT_ORDER_FIELD = "_so";
    static final String CONTENT_FIELD = "c";
    static Path luceneDirectoryPath = null;
    static Directory luceneDirectory = null;

    static String TARG_FILE_ID;
    static Set<String> TARG_DOC_IDS = new HashSet<>();

    static IndexSearcher searcher;
    @BeforeClass
    public static void init() throws IOException {
        Analyzer standardAnalyzer = new StandardAnalyzer();
        Analyzer keywordAnalyzer = new KeywordAnalyzer();
        Map<String, Analyzer> analyzers = new HashMap<>();
        analyzers.put(CONTENT_FIELD, standardAnalyzer);
        Analyzer analyzer = new PerFieldAnalyzerWrapper(keywordAnalyzer, analyzers);
        luceneDirectoryPath = Files.createTempDirectory("doc-retriever-test");
        luceneDirectory = FSDirectory.open(luceneDirectoryPath);
        IndexWriterConfig c = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(luceneDirectory, c);

        TARG_FILE_ID = UUID.randomUUID().toString();
        //write ascending to 10 parent (0) and child documents
        for (int i = 0 ; i < 10; i++) {
            Document d = new Document();
            d.add(new TextField(FILE_ID_FIELD, TARG_FILE_ID, Field.Store.YES));
            String docId = UUID.randomUUID().toString();
            TARG_DOC_IDS.add(docId);
            d.add(new TextField(DOC_ID_FIELD, docId, Field.Store.YES));
            d.add(new TextField(EMBEDDED_ID_FIELD, Integer.toString(i), Field.Store.YES));
            d.add(new SortedNumericDocValuesField(SORT_ORDER_FIELD, i));
            writer.addDocument(d);
        }
        //write descending from 20 to 10 more child documents
        for (int i = 20 ; i >= 10; i--) {
            Document d = new Document();
            d.add(new TextField(FILE_ID_FIELD, TARG_FILE_ID, Field.Store.YES));
            String docId = UUID.randomUUID().toString();
            TARG_DOC_IDS.add(docId);
            d.add(new TextField(DOC_ID_FIELD, docId, Field.Store.YES));
            d.add(new TextField(EMBEDDED_ID_FIELD, Integer.toString(i), Field.Store.YES));
            d.add(new SortedNumericDocValuesField(SORT_ORDER_FIELD, i));
            d.add(new TextField(CONTENT_FIELD, "the quick brown fox", Field.Store.YES));
            writer.addDocument(d);
        }
        //now write some other docs
        for (int i = 0; i < 30; i++) {
            Document d = new Document();
            d.add(new TextField(FILE_ID_FIELD, UUID.randomUUID().toString(), Field.Store.YES));
            d.add(new TextField(DOC_ID_FIELD, UUID.randomUUID().toString(), Field.Store.YES));
            d.add(new TextField(EMBEDDED_ID_FIELD, Integer.toString(0), Field.Store.YES));
            d.add(new SortedNumericDocValuesField(SORT_ORDER_FIELD, 0));
            if (i < 10) {
                d.add(new TextField(CONTENT_FIELD, "the quick brown fox", Field.Store.YES));
            }
            writer.addDocument(d);

        }
        writer.commit();
        writer.close();
        luceneDirectory.close();
        luceneDirectory = FSDirectory.open(luceneDirectoryPath);
        searcher = new IndexSearcher(DirectoryReader.open(luceneDirectory));
    }

    @AfterClass
    public static void cleanUp() throws IOException {
        searcher.getIndexReader().close();
        luceneDirectory.close();
        PathUtils.deleteDirectory(luceneDirectoryPath);
    }

    @Test
    public void testBasic() throws Exception {
        SortField sortField = new SortedNumericSortField(SORT_ORDER_FIELD, SortField.Type.INT);
        sortField.setMissingValue(0);
        Sort sort = new Sort(sortField);

        Set<String> fieldsToRetrieve = null;

        //by doc id...this assumes default insertion order
        List<Document> docs = DocRetriever.getAllDocsFromAnyDocId(4,
                FILE_ID_FIELD, sort, fieldsToRetrieve, searcher);
        checkResults(docs);

        docs = DocRetriever.getAllDocsByFileId(FILE_ID_FIELD, TARG_FILE_ID, sort, fieldsToRetrieve, searcher);
        checkResults(docs);

        for (String doc_id : TARG_DOC_IDS) {
            docs = DocRetriever.getAllDocsFromAnyDocId(doc_id, DOC_ID_FIELD, FILE_ID_FIELD, sort, fieldsToRetrieve, searcher);
            checkResults(docs);
        }

    }

    @Test
    @Ignore
    public void oneOffTestBooleanQuery() throws Exception {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new TermQuery(new Term(CONTENT_FIELD, "fox")), BooleanClause.Occur.SHOULD);

        BooleanQuery.Builder sub = new BooleanQuery.Builder();

        for (String id : TARG_DOC_IDS) {
            sub.add(new TermQuery(new Term(DOC_ID_FIELD, id)), BooleanClause.Occur.SHOULD);
        }
        builder.add(sub.build(), BooleanClause.Occur.MUST_NOT);

        Query q = builder.build();
        TotalHitCountCollector collector = new TotalHitCountCollector();
        searcher.search(q, collector);
        System.out.println(collector.getTotalHits());
    }

    private void checkResults(List<Document> docs) {
        if (docs == null) {
            assertTrue("docs cannot be null", false);
        }
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < docs.size(); i++) {
            String attachment_id = docs.get(i).get(EMBEDDED_ID_FIELD);
            assertEquals("equals", Integer.toString(i), attachment_id);
            String doc_id = docs.get(i).get(DOC_ID_FIELD);
            seen.add(doc_id);
        }
        assertEquals("must be same size", seen.size(), TARG_DOC_IDS.size());
        for (String targ : TARG_DOC_IDS) {
            if (! seen.contains(targ)) {
                assertTrue("failed to find: "+targ, false);
            }

        }
    }
}

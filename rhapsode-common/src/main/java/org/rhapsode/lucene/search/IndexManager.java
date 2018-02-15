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

import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.rhapsode.RhapsodeCollection;
import org.rhapsode.app.handlers.search.BasicSearchHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tallison.util.SlowCompositeReaderWrapper;

import java.io.IOException;
import java.nio.file.Path;


public class IndexManager {
    private static final Logger LOG = LoggerFactory.getLogger(IndexManager.class);

    //    IndexSearcher searcher = null;
    SearcherManager searcherManager = null;

    public static void load(RhapsodeCollection collection) throws IOException {
        IndexManager indexManager = collection.getIndexManager();
        if (indexManager == null) {
            indexManager = new IndexManager();
        }

        try {
            indexManager.loadNewIndex(collection.getIndexSchemaPath(), collection.getLuceneIndexPath());
        } catch (IndexNotFoundException e) {
            LOG.warn("haven't found an index yet");
            //nope, no index yet
        }
        collection.setIndexManager(indexManager);
    }

    private IndexManager() {
    }

    public void close() throws IOException {
        searcherManager.close();
    }

    public IndexSearcher getSearcher() throws IOException {
        return searcherManager.acquire();
    }

    public void maybeRefresh() throws IOException {
        searcherManager.maybeRefresh();
    }

    public void release(IndexSearcher searcher) throws IOException {
        searcherManager.release(searcher);
    }

    public void loadNewIndex(Path indexSchemaPath, Path luceneIndexPath) throws IOException {
        if (searcherManager != null) {
            searcherManager.close();
        }
        LOG.debug("luceneIndexPath: " + luceneIndexPath + " : " + luceneIndexPath.toAbsolutePath());
        Directory luceneDirectory = FSDirectory.open(luceneIndexPath);
        SearcherManager searcherManager = new SearcherManager(luceneDirectory, new SearcherFactory());
        this.searcherManager = searcherManager;
    }

    public void deleteDuplicates(RhapsodeCollection collection, String df) throws IOException {
        //TODO -- pick up here
        IndexWriterConfig config = new IndexWriterConfig(collection.getIndexSchema().getIndexAnalyzer());
        Directory luceneDirectory = FSDirectory.open(collection.getLuceneIndexPath());
        IndexWriter writer = new IndexWriter(luceneDirectory, config);
        IndexSearcher searcher = getSearcher();
        IndexReader reader = searcher.getIndexReader();
        LeafReader wrapper = SlowCompositeReaderWrapper.wrap(reader);
        Terms terms = wrapper.terms(df);
        TermsEnum te = terms.iterator();
        BytesRef t = te.next();
        while (t != null) {
            Term term = new Term(df, t);
            if (wrapper.docFreq(term) > 1) {

                PostingsEnum postings = wrapper.postings(term);
                postings.nextPosition();
                postings.docID();
            }
            t = te.next();
        }

        writer.forceMergeDeletes();

    }

    public void merge(RhapsodeCollection collection, int segs) throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(collection.getIndexSchema().getIndexAnalyzer());
        Directory luceneDirectory = FSDirectory.open(collection.getLuceneIndexPath());
        IndexWriter writer = new IndexWriter(luceneDirectory, config);
        LOG.info("about to merge");
        writer.forceMerge(segs);
        LOG.info("about to force merge deletes");
        writer.forceMergeDeletes();
        LOG.info("about to remove unused files");
        writer.deleteUnusedFiles();
        LOG.info("about to commit");
        writer.commit();
        LOG.info("about to close");
        writer.close();

    }
}

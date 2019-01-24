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

package org.rhapsode.indexer.builders;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.IndexWriter;
import org.apache.tika.batch.ConsumersManager;
import org.apache.tika.batch.FileResourceConsumer;
import org.rhapsode.RhapsodeCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexerConsumersManager extends ConsumersManager {

    private static final Logger LOG = LoggerFactory.getLogger(IndexerConsumersManager.class);


    final IndexWriter indexWriter;
    final RhapsodeCollection rhapsodeCollection;

    public IndexerConsumersManager(RhapsodeCollection collection,
                                   List<FileResourceConsumer> consumers, IndexWriter indexWriter) {
        super(consumers);
        this.indexWriter = indexWriter;
        this.rhapsodeCollection = collection;
    }

    @Override
    public void init() {
        //noop
    }

    @Override
    public void shutdown() {
        LOG.info("ConsumersManager about to shut down.");
        try {
            LOG.info("ConsumersManager about to delete unused files");
            indexWriter.deleteUnusedFiles();
            LOG.info("ConsumersManager about to commit");
            indexWriter.commit();
        } catch (IOException e) {
            LOG.warn("io exception closing", e);
        }
        try {
            LOG.info("ConsumersManager about to close");
            indexWriter.close();
        } catch (IOException e) {
            LOG.warn("io exception closing indexwriter", e);
        }
        LOG.info("ConsumersManager has shutdown.");
    }


}

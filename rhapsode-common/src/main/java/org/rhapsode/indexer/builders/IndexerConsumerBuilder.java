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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.tika.batch.ConsumersManager;
import org.apache.tika.batch.FileResource;
import org.apache.tika.batch.FileResourceConsumer;
import org.apache.tika.batch.builders.AbstractConsumersBuilder;
import org.apache.tika.batch.builders.BatchProcessBuilder;
import org.apache.tika.util.PropsUtil;
import org.apache.tika.util.XMLDOMUtil;
import org.rhapsode.RhapsodeCollection;
import org.rhapsode.app.handlers.search.BasicSearchHandler;
import org.rhapsode.indexer.IndexerConsumer;
import org.rhapsode.indexer.RhapsodeIndexerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

public class IndexerConsumerBuilder extends AbstractConsumersBuilder {

    public final static String RHAPSODE_INDEXER_CONFIG_PATH = "indexerConfigPath";
    public final static String RHAPSODE_COLLECTION_PATH_KEY = "collectionPath";
    private static final Logger LOG = LoggerFactory.getLogger(BasicSearchHandler.class);

    @Override
    public ConsumersManager build(Node node, Map<String, String> runtimeAttributes,
                                  ArrayBlockingQueue<FileResource> queue) {

        RhapsodeIndexerConfig rhapsodeIndexerConfig = null;
        RhapsodeCollection rhapsodeCollection;
        IndexWriter indexWriter = null;
        Map<String, String> localAttrs = XMLDOMUtil.mapifyAttrs(node, runtimeAttributes);

        try {
            rhapsodeCollection = getRhapsodeCollection(node, runtimeAttributes);
            rhapsodeIndexerConfig = getIndexerConfig(rhapsodeCollection, node, runtimeAttributes);

            indexWriter = buildIndexWriter(localAttrs, rhapsodeCollection);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //how long to let the consumersManager run on init() and shutdown()
        Long consumersManagerMaxMillis = null;
        String consumersManagerMaxMillisString = runtimeAttributes.get("consumersManagerMaxMillis");
        if (consumersManagerMaxMillisString != null) {
            consumersManagerMaxMillis = PropsUtil.getLong(consumersManagerMaxMillisString, null);
        } else {
            Node consumersManagerMaxMillisNode = node.getAttributes().getNamedItem("consumersManagerMaxMillis");
            if (consumersManagerMaxMillis == null && consumersManagerMaxMillisNode != null) {
                consumersManagerMaxMillis = PropsUtil.getLong(consumersManagerMaxMillisNode.getNodeValue(),
                        null);
            }
        }

        List<FileResourceConsumer> consumers = new LinkedList<>();
        int numConsumers = BatchProcessBuilder.getNumConsumers(runtimeAttributes);

        for (int i = 0; i < numConsumers; i++) {
            FileResourceConsumer c = new IndexerConsumer(queue, rhapsodeIndexerConfig, indexWriter);
            consumers.add(c);
        }
        ConsumersManager manager = new IndexerConsumersManager(rhapsodeCollection, consumers, indexWriter);
        if (consumersManagerMaxMillis != null) {
            manager.setConsumersManagerMaxMillis(consumersManagerMaxMillis);
        }
        return manager;
    }

    private RhapsodeCollection getRhapsodeCollection(Node node,
                                                     Map<String, String> runtimeAttributes) throws IOException {
        Map<String, String> attrs = XMLDOMUtil.mapifyAttrs(node, runtimeAttributes);
        for (String k : attrs.keySet()) {
            LOG.info("getting collection: " + k + " : " + attrs.get(k));
        }
        return RhapsodeCollection.loadExisting(Paths.get(attrs.get(RHAPSODE_COLLECTION_PATH_KEY)));
    }

    private RhapsodeIndexerConfig getIndexerConfig(RhapsodeCollection rc, Node node, Map<String, String> runtimeAttributes) throws IOException {
        Map<String, String> attrs = XMLDOMUtil.mapifyAttrs(node, runtimeAttributes);
        Path path = Paths.get(attrs.get(RHAPSODE_INDEXER_CONFIG_PATH));
        return RhapsodeIndexerConfig.load(rc, path);
    }

    private IndexWriter buildIndexWriter(Map<String, String> attrs, RhapsodeCollection rc) throws IOException {
        IndexWriterConfig iwConfig = new IndexWriterConfig(rc.getIndexSchema().getIndexAnalyzer());
        LOG.info("building indexwriter with codec: " + iwConfig.getCodec());
        //TODO: update iwConfig based on other info in RhapsodeIndexerConfig
        iwConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        if (attrs.containsKey("ramBufferSizeMB")) {
            int sz = Integer.parseInt(attrs.get("ramBufferSizeMB"));
            LOG.debug("setting rambuffer:" + sz);
            iwConfig.setRAMBufferSizeMB(sz);
        }
        if (attrs.containsKey("ramPerThreadMB")) {
            int sz = Integer.parseInt(attrs.get("ramPerThreadMB"));
            LOG.debug("setting ramPerThread:" + sz);
            iwConfig.setRAMPerThreadHardLimitMB(sz);
        }
        if (attrs.containsKey("maxBufferedDocs")) {
            int max = Integer.parseInt(attrs.get("maxBufferedDocs"));
            LOG.debug("setting maxBufferedDocs: " + max);
            iwConfig.setMaxBufferedDocs(max);
        }
        Directory directory = FSDirectory.open(rc.getLuceneIndexPath());
        IndexWriter indexWriter = new IndexWriter(directory, iwConfig);

        return indexWriter;
    }
}

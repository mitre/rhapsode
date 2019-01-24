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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.lucene.index.IndexWriter;
import org.apache.tika.batch.FileResource;
import org.apache.tika.batch.FileResourceConsumer;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.serialization.JsonMetadataList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexerConsumer extends FileResourceConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(IndexerConsumer.class);
    private final FileIndexer fileIndexer;


    public IndexerConsumer(ArrayBlockingQueue<FileResource> fileQueue,
                           RhapsodeIndexerConfig config, IndexWriter writer) {
        super(fileQueue);
        fileIndexer = new FileIndexer(config, writer);
    }

    @Override
    public boolean processFileResource(FileResource fileResource) {
        LOG.debug("Indexing: " + fileResource.getResourceId());
        List<Metadata> metadataList = null;
        try (Reader r =
                     new BufferedReader(
                             new InputStreamReader(
                                     fileResource.openInputStream(), StandardCharsets.UTF_8))) {
            metadataList = JsonMetadataList.fromJson(r);

        } catch (IOException | TikaException e) {
            LOG.warn("Error reading: " + fileResource.getResourceId(), e);
            incrementHandledExceptions();
            return false;
        }

        if (metadataList == null) {
            return false;
        }

        try {
            fileIndexer.writeDocument(metadataList);
        } catch (Exception e) {
            LOG.warn(getXMLifiedLogMsg("Exception writing to index",
                    fileResource.getResourceId(), e));
            incrementHandledExceptions();
            return false;
        }
        return true;
    }
}

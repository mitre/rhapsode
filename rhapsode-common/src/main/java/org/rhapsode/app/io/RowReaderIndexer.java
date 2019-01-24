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

package org.rhapsode.app.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.tika.metadata.Metadata;
import org.rhapsode.app.handlers.indexer.FieldTypePair;
import org.rhapsode.indexer.FileIndexer;

public class RowReaderIndexer extends RowReader {
    private final Map<String, FieldTypePair> colMappings;
    private final FileIndexer fileIndexer;
    private final List<Metadata> metadataList = new ArrayList<>();
    private int rowsRead = 0;
    private int missingId = 0;

    public RowReaderIndexer(Map<String, FieldTypePair> colMappings, FileIndexer fileIndexer) {
        this.colMappings = colMappings;
        this.fileIndexer = fileIndexer;
    }

    @Override
    public boolean process(Map<String, String> data) throws IOException {
        Metadata m = new Metadata();
        for (Map.Entry<String, FieldTypePair> e : colMappings.entrySet()) {
            String v = data.get(e.getKey());
            if (e.getValue().isLinkField() && StringUtils.isBlank(v)) {
                v = "MISSING_ID_" + missingId++;
            }
            m.set(e.getValue().getLuceneFieldName(), v);
        }
        metadataList.add(m);
        try {
            fileIndexer.writeDocument(metadataList);
        } finally {
            metadataList.clear();
        }
        rowsRead++;
        return true;
    }

    public int getRowsRead() {
        return rowsRead;
    }

}

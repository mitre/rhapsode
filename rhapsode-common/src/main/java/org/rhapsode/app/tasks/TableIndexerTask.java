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

package org.rhapsode.app.tasks;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.rhapsode.RhapsodeCollection;
import org.rhapsode.app.handlers.indexer.TableFileRequest;
import org.rhapsode.app.io.AbstractTableReader;
import org.rhapsode.app.io.CSVTableReader;
import org.rhapsode.app.io.RowReaderIndexer;
import org.rhapsode.app.io.XLSTableReader;
import org.rhapsode.app.io.XLSXStreamingTableReader;
import org.rhapsode.indexer.FileIndexer;
import org.rhapsode.indexer.RhapsodeIndexerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

public class TableIndexerTask extends RhapsodeTask {

    private static final Logger LOG = LoggerFactory.getLogger(TableIndexerTask.class);


    private final RhapsodeCollection rc;
    private final Path inputTableFile;
    private final TableFileRequest tableFileRequest;
    private Date started;
    private final Directory luceneIndexDirectory;
    private final IndexWriter indexWriter;
    private final RowReaderIndexer perRowIndexer;
    private final String worksheetName;

    RhapsodeTaskStatus finishedStatus = null;

    public TableIndexerTask(RhapsodeCollection rc, Path inputTableFile, TableFileRequest tableFileRequest)
            throws IOException {
        super("Table Indexer");
        this.rc = rc;
        this.inputTableFile = inputTableFile;
        this.tableFileRequest = tableFileRequest;
        RhapsodeIndexerConfig rhapsodeIndexerConfig = RhapsodeIndexerConfig.load(rc, Paths.get("resources/config/indexer_config.json"));
        IndexWriterConfig iwConfig = new IndexWriterConfig(rc.getIndexSchema().getIndexAnalyzer());
        luceneIndexDirectory = FSDirectory.open(rc.getLuceneIndexPath());

        indexWriter = new IndexWriter(luceneIndexDirectory, iwConfig);

        worksheetName = tableFileRequest.getWorksheetName();
        FileIndexer fileIndexer = new FileIndexer(rhapsodeIndexerConfig, indexWriter);

        perRowIndexer = new RowReaderIndexer(tableFileRequest.getFields(),
                fileIndexer);
    }

    @Override
    public RhapsodeTaskStatus getIntermediateResult() {
        if (finishedStatus != null) {
            return finishedStatus;
        }
        long elapsed = (new Date().getTime() - started.getTime());

        return new RhapsodeTaskStatus(
                Tasker.STATE.PROCESSING,
                Tasker.REASON_FOR_COMPLETION.NA,
                new Date(),
                "Indexed " + perRowIndexer.getRowsRead() + " rows so far " +
                        "in " + elapsed + " milliseconds from file \"" + inputTableFile.getFileName().toString() +
                        "\" into collection \"" +
                        rc.getCollectionPath().getFileName().toString() + "\"");

    }

    @Override
    public RhapsodeTaskStatus call() throws Exception {
        started = new Date();
        AbstractTableReader reader = null;
        try {
            if (inputTableFile.toString().endsWith(".xlsx")) {
                reader =
                        new XLSXStreamingTableReader(inputTableFile, worksheetName,
                                perRowIndexer, tableFileRequest.getTableHasHeaders());
            } else if (inputTableFile.toString().endsWith(".xls")) {
                reader = new XLSTableReader(inputTableFile, worksheetName, perRowIndexer,
                        tableFileRequest.getTableHasHeaders());
            } else if (inputTableFile.toString().endsWith(".txt") || inputTableFile.toString().endsWith(".csv")) {
                reader = new CSVTableReader(inputTableFile, perRowIndexer, tableFileRequest.getCSVDelimiterChar(),
                        tableFileRequest.getCSVEncoding(), tableFileRequest.getTableHasHeaders());
            } else {
                throw new RuntimeException("I'm sorry, table files must end in .xlsx, .xlsm, .xls, .txt or .csv");
            }
            reader.parse();
            indexWriter.flush();
            indexWriter.close();
            luceneIndexDirectory.close();
            long elapsed = new Date().getTime() - started.getTime();
            finishedStatus = new RhapsodeTaskStatus(Tasker.STATE.COMPLETED,
                    Tasker.REASON_FOR_COMPLETION.SUCCESS,
                    new Date(),
                    "Indexed " + perRowIndexer.getRowsRead() + " rows " +
                            "in " + elapsed + " milliseconds from file \"" + inputTableFile.getFileName().toString() +
                            "\" into collection \"" +
                            rc.getCollectionPath().getFileName().toString() + "\"");

        } catch (Throwable t) {
            LOG.warn("serious problem indexing table", t);
            finishedStatus = new RhapsodeTaskStatus(Tasker.STATE.COMPLETED,
                    Tasker.REASON_FOR_COMPLETION.EXCEPTION,
                    new Date(),
                    t.getMessage());

        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return finishedStatus;
    }
}

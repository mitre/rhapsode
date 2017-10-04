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

package org.rhapsode.app.tagger;


import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.rhapsode.app.HitCounter;
import org.rhapsode.app.config.RhapsodeSearcherApp;
import org.rhapsode.lucene.search.MaxResultsQuery;
import org.rhapsode.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Tagger {

    private static final Logger LOG = LoggerFactory.getLogger(Tagger.class);

    static final String QUERY_NAME_TABLE = "QNAMES";
    static final String SCORES_TABLE = "SCORES";
    static final String SCORE_SORT_TABLE = "OVERALL_DOC_SCORES";
    static final String NAME_PATH_TABLE = "NAME_PATH";//display name and path


    static final String TOTAL_SCORE_COL = "SUMMED_SCORE";
    static final String Q_NAME_COL = "NAME";
    static final String FILE_ID_COL = "F_ID";//file id
    //    static final String DOC_ID_COL = "D_ID";//doc id
    static final String QUERY_ID_COL = "Q_ID";
    static final String SCORES_COL = "SCORES";
    static final String DISPLAY_NAME_COL = "DISPLAY";
    static final String ORIG_REL_PATH = "REL_PATH";

    private static final int POISON_KEY = -1;

    private static final int NUM_THREADS = 10;
    private static final int HARD_LIMIT_MAX_RESULTS = 100000;

    private static final int FILE_ID_MAX_LEN = 512;
    private static final int DISPLAY_NAME_MAX_LEN = 512;
    private static final int ORIG_REL_PATH_MAX_LEN = 2048;

    private Object lock = new Object();

    boolean alreadyRun = false;
    final TaggerRequest request;
    final RhapsodeSearcherApp searcherApp;
    Map<Integer, Integer> maxHits;

    private String statusMessage = "";

    public Tagger(TaggerRequest request, RhapsodeSearcherApp searcherApp) {
        this.request = request;
        this.searcherApp = searcherApp;
    }

    public void execute() throws Exception {
        if (alreadyRun) {
            throw new IllegalArgumentException("Can't run a given tagger more than once!");
        }
        alreadyRun = true;
        calcMaxHits();
        LOG.debug("max hits: " + maxHits);
        Path tmpDir = Files.createTempDirectory("tagger-tmp");
        Path dbFile = tmpDir.resolve("taggerdb");
        try (Connection conn = openConnection(dbFile)) {
            long start = new Date().getTime();
            try {
                createTables(conn);
                updateStatusMessage("created tables");
                fillNamesTable(conn);
                updateStatusMessage("filled names table");
                int numThreads = Math.min(request.queries.size(), NUM_THREADS);
                fillWithSearchResults(maxHits, numThreads, conn);
                updateStatusMessage("filled search results");

                calculateTotalScores(conn);

/*                DBUtils.debugDumpTable(conn, QUERY_NAME_TABLE);
                DBUtils.debugDumpTable(conn, NAME_PATH_TABLE);
                DBUtils.debugDumpTable(conn, SCORES_TABLE);
                DBUtils.debugDumpTable(conn, SCORE_SORT_TABLE);*/
                updateStatusMessage("Calculated scores in " + (new Date().getTime() - start) + "(ms)");
                LinkTaggerWriter writer = new LinkTaggerWriter(request, searcherApp);
                writer.write(conn);
                updateStatusMessage("finished writing table");
            } catch (Exception e) {
                LOG.warn("problem loading tables", e);
                throw (e);
            }
        } finally {
            PathUtils.deleteDirectory(tmpDir);
        }

    }

    private void updateStatusMessage(String s) {
        synchronized (lock) {
            statusMessage = s;
        }
    }

    private void calcMaxHits() throws IOException {
        HitCounter c = new HitCounter();
        Map<Integer, Query> retrievalQueries = new HashMap<>();
        for (Map.Entry<Integer, MaxResultsQuery> e : request.queries.entrySet()) {
            retrievalQueries.put(e.getKey(), e.getValue().getQuery());
        }
        //parameterize number of threads and max time!!!
        maxHits = c.count(retrievalQueries, request.searcher, 10, 5000);
    }

    private Connection openConnection(Path dbFile) throws SQLException {
        Connection connection = DriverManager.getConnection(
                "jdbc:h2:file:" + dbFile.toAbsolutePath().toString());
        return connection;
    }

    private void createTables(Connection conn) throws SQLException {
        //TODO: figure out how to add unique constraint on q name

        //"QUERY_NAME_ID,QUERY_NAME"
        String sql = "CREATE TABLE " + QUERY_NAME_TABLE + " (" + QUERY_ID_COL + " INTEGER, " +
                "" + Q_NAME_COL + " VARCHAR(512))";
        Statement st = conn.createStatement();
        st.execute(sql);

        //FILE_ID,NAME_ID,SCORE
        sql = "CREATE TABLE " + SCORES_TABLE + " (" + FILE_ID_COL + " CHAR(36), " + QUERY_ID_COL + " INTEGER, " +
                SCORES_COL + " FLOAT)";

        st.execute(sql);
        //FILE_ID, TOTAL_SCORE
        sql = "CREATE TABLE " + SCORE_SORT_TABLE + " (" + FILE_ID_COL + " CHAR(36) PRIMARY KEY, " + TOTAL_SCORE_COL + " FLOAT)";
        st.execute(sql);

        sql = "CREATE TABLE " + NAME_PATH_TABLE + " (" +
                FILE_ID_COL + " VARCHAR(" + FILE_ID_MAX_LEN + ") PRIMARY KEY, " +
                DISPLAY_NAME_COL + " VARCHAR(" + DISPLAY_NAME_MAX_LEN + ")," +
                ORIG_REL_PATH + " VARCHAR(" + ORIG_REL_PATH_MAX_LEN + "))";
        st.execute(sql);
        //this actually takes more time!!!
        //sql = "CREATE INDEX idx on "+SCORES_TABLE +"("+FILE_ID_COL+")";
        //st.execute(sql);
    }

    private void fillNamesTable(Connection conn) throws SQLException {
        PreparedStatement insertStatement = conn.prepareStatement("INSERT INTO " + QUERY_NAME_TABLE + "(" + QUERY_ID_COL +
                "," + Q_NAME_COL + ") values (?,?)");

        for (Map.Entry<Integer, MaxResultsQuery> e : request.queries.entrySet()) {
            insertStatement.setInt(1, e.getKey());
            insertStatement.setString(2, e.getValue().getName());
            insertStatement.execute();
        }
        conn.commit();
    }

    private void calculateTotalScores(Connection conn) throws SQLException {
//        String sql = "INSERT INTO "+SCORE_SORT_TABLE + " ("+FILE_ID_COL+", "+TOTAL_SCORE_COL+") "+
//                "SELECT "+FILE_ID_COL+", SUM("+SCORES_COL+") from "+SCORES_TABLE + " group by "+FILE_ID_COL;
        String sql = "INSERT INTO " + SCORE_SORT_TABLE + " (" + FILE_ID_COL + ", " + TOTAL_SCORE_COL + ") " +
                "SELECT " + FILE_ID_COL + ", SUM(" + SCORES_COL + ") from " + SCORES_TABLE + " group by " + FILE_ID_COL;

        Statement st = conn.createStatement();
        st.execute(sql);
        conn.commit();
    }


    private void fillWithSearchResults(Map<Integer, Integer> maxHits, int numThreads, Connection conn) throws Exception {
        ArrayBlockingQueue<Pair<Integer, MaxResultsQuery>> q = new ArrayBlockingQueue<>(request.queries.size() + numThreads);
        for (Map.Entry<Integer, MaxResultsQuery> e : request.queries.entrySet()) {
            q.add(Pair.of(e.getKey(), e.getValue()));
        }
        for (int i = 0; i < numThreads; i++) {
            q.add(Pair.of(POISON_KEY, null));
        }

        ExecutorService es = Executors.newFixedThreadPool(numThreads);
        ExecutorCompletionService<Integer> ex =
                new ExecutorCompletionService<>(es);
        for (int i = 0; i < numThreads; i++) {
            ex.submit(new TagSearcherThread(q, request.searcher, request, conn));
        }
        int finished = 0;
        while (finished < numThreads) {
            try {
                Future<Integer> future = ex.poll(1, TimeUnit.SECONDS);

                if (future != null) {
                    future.get();
                    finished++;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                es.shutdown();
                es.shutdownNow();
                e.printStackTrace();
                throw e;
            } catch (Throwable t) {
                t.printStackTrace();
                throw t;
            }
        }
        es.shutdown();
        es.shutdownNow();
        try {
            es.awaitTermination(1L, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        LOG.debug("finished filling search results: " + es.isShutdown() + " : " + es.isTerminated());
        conn.commit();
    }

    public String getStatusMessage() {
        String ret;
        synchronized (lock) {
            ret = statusMessage;
        }
        return ret;
    }


    class TagSearcherThread implements Callable<Integer> {
        final ArrayBlockingQueue<Pair<Integer, MaxResultsQuery>> q;
        final IndexSearcher indexSearcher;
        final Connection conn;
        final TaggerRequest tr;

        TagSearcherThread(ArrayBlockingQueue<Pair<Integer, MaxResultsQuery>> q, IndexSearcher searcher,
                          TaggerRequest tr, Connection conn) {
            this.q = q;
            this.indexSearcher = searcher;
            this.conn = conn;
            this.tr = tr;

        }

        @Override
        public Integer call() throws Exception {
            try (PreparedStatement insertStatement =
                         conn.prepareStatement("INSERT INTO " + SCORES_TABLE + " VALUES (?,?,?)")) {
                try (PreparedStatement mergeDisplayRelPath =
                             conn.prepareStatement(
                                     "MERGE INTO " + NAME_PATH_TABLE + " VALUES (?,?,?)")) {
                    return _call(insertStatement, mergeDisplayRelPath);
                }

            } catch (SQLException e) {
                LOG.warn("prob w sql inserts", e);
                throw e;
            }
        }

        private Integer _call(PreparedStatement insertStatement,
                              PreparedStatement mergeDisplayRelPath) throws Exception {
            final String fileIdField = searcherApp.getRhapsodeCollection().getIndexSchema().getUniqueFileField();
            final String displayField = searcherApp.getRhapsodeCollection().getIndexSchema().getLinkDisplayField();
            final String relPathField = searcherApp.getRhapsodeCollection().getIndexSchema().getRelPathField();
            final Set<String> fields = new HashSet<>();
            fields.add(fileIdField);
            fields.add(displayField);
            fields.add(relPathField);
            while (true) {
                Pair<Integer, MaxResultsQuery> p = q.poll(1, TimeUnit.SECONDS);
                if (p == null) {
                    //TODO: check for stopped, interrupted thread....think about stopping
                    continue;
                }
                if (p.getKey().equals(POISON_KEY)) {
                    break;
                }
                Integer qId = p.getLeft();
                MaxResultsQuery mxrq = p.getValue();
                Query retrievalQuery = mxrq.getQuery();
                int maxResults = mxrq.getMaxResults();
                if (maxResults < 0) {
                    maxResults = HARD_LIMIT_MAX_RESULTS;
                }
                int priority = mxrq.getPriority();
                LOG.debug("ABOUT TO SEARCH: maxresults(" + maxResults + ") priority(" + priority + ")");
                float inversePriority = (priority == 0) ? 1.0f : (float) 1 / (float) priority;
                TopDocs topDocs = request.searcher.search(retrievalQuery, maxResults);
                ScoreDoc[] scoreDocs = topDocs.scoreDocs;
                LOG.debug("hits: " + scoreDocs.length);
                int r = 0;
                Set<String> seen = new HashSet<>();//
                for (ScoreDoc sd : scoreDocs) {
                    Document d = indexSearcher.doc(sd.doc, fields);
                    String fileId = d.get(fileIdField);
                    String relPath = d.get(relPathField);
                    String displayName = d.get(displayField);
                    relPath = (relPath == null) ? "rel" : relPath;
                    displayName = (displayName == null) ? "display" : displayName;
                    if (relPath.length() > ORIG_REL_PATH_MAX_LEN) {
                        LOG.warn("relative path length is greater than " +
                                ORIG_REL_PATH_MAX_LEN +
                                ". The path will be truncated, and the link might not work:\n" + relPath + "");
                        relPath = relPath.substring(0, ORIG_REL_PATH_MAX_LEN);
                    }
                    if (fileId.length() > FILE_ID_MAX_LEN) {
                        LOG.warn("file id length is greater than " +
                                FILE_ID_MAX_LEN +
                                ":\n" + relPath + "");
                        fileId = fileId.substring(0, FILE_ID_MAX_LEN);
                    }
                    if (displayName.length() > DISPLAY_NAME_MAX_LEN) {
                        LOG.warn("display name length is greater than " +
                                DISPLAY_NAME_MAX_LEN +
                                ":\n" + relPath + "");
                        displayName = displayName.substring(0, DISPLAY_NAME_MAX_LEN);
                    }
                    try {
                        mergeDisplayRelPath.clearParameters();
                        mergeDisplayRelPath.setString(1, fileId);
                        mergeDisplayRelPath.setString(2, displayName);
                        mergeDisplayRelPath.setString(3, relPath);
                        mergeDisplayRelPath.execute();
                    } catch (Exception e) {
                        LOG.warn("problem merging", e);
                        throw e;
                    }
                    r++;
                    if (seen.contains(fileId)) { //take the best score for each file
                        continue;
                    }
                    seen.add(fileId);
                    try {
                        insertStatement.clearParameters();
                        insertStatement.setString(1, fileId);
                        insertStatement.setInt(2, qId);
                        float weight = -1.0f;
                        switch (request.normType) {
                            case ONE:
                                weight = 1.0f;
                                break;
                            case INVERSE_RANK:
                                weight = 1.0f / (float) r;
                                break;
                            case WEIGHTED_INVERSE_RANK:
                                weight = (float) 1 / (float) Math.sqrt(r);
                                break;
                            case INVERSE_PRIORITY:
                                weight = inversePriority;
                        }
                        weight = (weight < 0.001) ? 0.001f : weight;
                        insertStatement.setFloat(3, weight);
                        insertStatement.execute();
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw e;
                    }
                }
            }
            return 1;
        }
    }


}

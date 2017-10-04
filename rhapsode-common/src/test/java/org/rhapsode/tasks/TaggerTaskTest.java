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
package org.rhapsode.tasks;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.WildcardQuery;
import org.rhapsode.app.config.RhapsodeSearcherApp;
import org.rhapsode.app.handlers.admin.ReportRequest;
import org.rhapsode.app.tagger.TaggerRequest;
import org.rhapsode.app.tasks.TaggerTask;
import org.rhapsode.app.tasks.Tasker;
import org.rhapsode.lucene.search.MaxResultsQuery;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class TaggerTaskTest {


    //TODO: convert to actual unit tests!!!
    public static void main(String[] args) throws Exception {
        Path collection = Paths.get("collections/test2");
        RhapsodeSearcherApp searcherApp = RhapsodeSearcherApp.load(Paths.get("resources/config/search_config.json"));
        searcherApp.tryToLoadRhapsodeCollection(collection);

        Map<Integer, MaxResultsQuery> queries = new HashMap<>();
        queries.put(0, new MaxResultsQuery("fox1", new WildcardQuery(new Term("content", "f*")), -1, -1));
        queries.put(1, new MaxResultsQuery("d_words", new WildcardQuery(new Term("content", "d*")), -1, -1));
        IndexSearcher searcher =  searcherApp.getRhapsodeCollection().getIndexManager().getSearcher();
        Path outputPath = Paths.get("C:/data/tmp/");
        TaggerRequest request = new TaggerRequest(queries, searcher, "", outputPath, "test_out",
                ReportRequest.REPORT_TYPE.NO_LINKS, 100, TaggerRequest.NORM_TYPE.ONE);
        TaggerTask tagger = new TaggerTask(request, searcherApp);
        Tasker tasker = new Tasker(tagger, 3000);
        tasker.start();
        while (true) {
            Thread.sleep(500);
            if (tasker.getState().equals(Tasker.STATE.COMPLETED)) {
                break;
            }
        }
    }
}

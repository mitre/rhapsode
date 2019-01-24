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

import java.nio.file.Path;
import java.util.Map;

import org.apache.lucene.search.IndexSearcher;
import org.rhapsode.app.handlers.admin.ReportRequest;
import org.rhapsode.lucene.search.MaxResultsQuery;

public class TaggerRequest {


    final Map<Integer, MaxResultsQuery> queries;
    final IndexSearcher searcher;
    final Path reportFile;
    final Path staticDir;
    final String fileNameField;
    final NORM_TYPE normType;
    final int topNCombinedResults;
    final ReportRequest.REPORT_TYPE reportType;
    public TaggerRequest(Map<Integer, MaxResultsQuery> queries, IndexSearcher searcher,
                         String fileNameField, Path reportsDir, String reportName,
                         ReportRequest.REPORT_TYPE reportType,
                         int topNCombinedResults, NORM_TYPE normType) {
        this.queries = queries;
        this.searcher = searcher;
        this.fileNameField = fileNameField;
        this.reportFile = reportsDir.resolve(reportName + ".xlsx");
        this.staticDir = reportsDir.resolve(reportName + "_docs");
        this.reportType = reportType;
        this.topNCombinedResults = topNCombinedResults;
        this.normType = normType;
    }

    public String getDocIdFieldName() {
        //TODO: parameterize this!!!!
        return "_fid";
    }

    public String getRelPathFieldName() {
        //TODO: parameterize this!!!!
        return "rel_path";
    }

    public enum NORM_TYPE {
        ONE("One"),
        INVERSE_PRIORITY("Inverse Priority"),
        INVERSE_RANK("Inverse Rank"),
        WEIGHTED_INVERSE_RANK("Weighted Relevance");

        private final String displayString;

        NORM_TYPE(String s) {
            displayString = s;
        }

        public String getDisplayString() {
            return displayString;
        }
    }

}

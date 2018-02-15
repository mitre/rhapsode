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
package org.rhapsode.lucene.search.basic;

import java.util.List;

public class BasicSearchResults {
    private long totalHits = -1;
    private int start = -1;
    private int end = -1;
    List<BasicSearchResult> results = null;
    private boolean hasResults = false;
    private long totalDocs;

    public BasicSearchResults() {
        hasResults = false;
    }

    public BasicSearchResults(List<BasicSearchResult> results, int start, int end, long totalHits, long totalDocs) {
        this.results = results;
        this.start = start;
        this.end = end;
        this.totalHits = totalHits;
        hasResults = true;
        this.totalDocs = totalDocs;
    }

    public List<BasicSearchResult> getResults() {
        return results;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public long getTotalHits() {
        return totalHits;
    }

    public boolean hasResults() {
        return hasResults;
    }

    public long getTotalDocs() {
        return totalDocs;
    }
}

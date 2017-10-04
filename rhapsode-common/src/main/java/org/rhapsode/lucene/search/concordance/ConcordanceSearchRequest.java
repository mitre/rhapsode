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
package org.rhapsode.lucene.search.concordance;

import org.rhapsode.lucene.search.BaseSearchRequest;
import org.tallison.lucene.search.concordance.classic.ConcordanceSortOrder;

import java.util.Map;

public class ConcordanceSearchRequest extends BaseSearchRequest {
    final ConcordanceSearchConfig config;

    private boolean ignoreDuplicateWindows = false;
    private int maxStoredWindows = 1000;
    private int tokensBefore = 10;
    private int tokensAfter = 10;
    private ConcordanceSortOrder sortOrder = ConcordanceSortOrder.PRE;
    private Class clusteringAlgo;
    private Map<String, Object> clusteringAttrs;

    public ConcordanceSearchRequest(ConcordanceSearchConfig concordanceSearchConfig) {
        this.config = concordanceSearchConfig;
    }

    public boolean getIgnoreDuplicateWindows() {
        return ignoreDuplicateWindows;
    }

    public int getMaxStoredWindows() {
        return maxStoredWindows;
    }

    public int getTokensBefore() {
        return tokensBefore;
    }

    public int getTokensAfter() {
        return tokensAfter;
    }

    public ConcordanceSortOrder getSortOrder() {
        return sortOrder;
    }

    public void setTokensBefore(int tokensBefore) {
        this.tokensBefore = tokensBefore;
    }

    public void setTokensAfter(int tokensAfter) {
        this.tokensAfter = tokensAfter;
    }

    public void setMaxStoredWindows(int maxStoredWindows) {
        this.maxStoredWindows = maxStoredWindows;
    }

    public void setIgnoreDuplicateWindows(boolean ignoreDuplicateWindows) {
        this.ignoreDuplicateWindows = ignoreDuplicateWindows;
    }

    public void setSortOrder(ConcordanceSortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setClusteringAlgo(Class clusteringAlgo) {
        this.clusteringAlgo = clusteringAlgo;
    }

    public void setClusteringAttrs(Map<String, Object> clusteringAttrs) {
        this.clusteringAttrs = clusteringAttrs;
    }

    public Class getClusteringAlgo() {
        return clusteringAlgo;
    }

    public Map<String, Object> getClusteringAttrs() {
        return clusteringAttrs;
    }
}

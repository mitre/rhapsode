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

package org.rhapsode.app.handlers.search;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.classic.ParseException;
import org.carrot2.clustering.kmeans.BisectingKMeansClusteringAlgorithm;
import org.carrot2.clustering.lingo.LingoClusteringAlgorithm;
import org.carrot2.clustering.stc.STCClusteringAlgorithm;
import org.rhapsode.app.config.RhapsodeSearcherApp;
import org.rhapsode.app.contants.C;
import org.rhapsode.app.session.DynamicParameters;
import org.rhapsode.lucene.search.concordance.ConcordanceSearchRequest;
import org.tallison.lucene.search.concordance.classic.ConcordanceSortOrder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConcordanceSearchRequestBuilder extends ConcSearchRequestBuilderBase {

    public void extract(RhapsodeSearcherApp searcherApp, HttpServletRequest servletRequest,
                        final ConcordanceSearchRequest request) throws ParseException, IOException {
        //order matters.  be careful!
        super.extractConcBase(searcherApp, servletRequest, request);
        //TODO: use dynamic parameter searcherApp to pull default sort order
        //DynamicParameterConfig params = searcherApp.getSessionManager().getDynamicParameterConfig();

        String sortOrderString = servletRequest.getParameter(C.CONC_SORT_ORDER_SELECTOR);
        ConcordanceSortOrder sortOrder = ConcordanceSortOrder.PRE;
        if (sortOrderString == null || sortOrderString.equals(C.CONC_SORT_ORDER_PRE)) {
            //do nothing
        } else if (sortOrderString.equals(C.CONC_SORT_ORDER_POST)) {
            sortOrder = ConcordanceSortOrder.POST;
        } else if (sortOrderString.equals(C.CONC_SORT_ORDER_TARGET_PRE)) {
            sortOrder = ConcordanceSortOrder.TARGET_PRE;
        } else if (sortOrderString.equals(C.CONC_SORT_ORDER_POST)) {
            sortOrder = ConcordanceSortOrder.TARGET_POST;
        } else if (sortOrderString.equals(C.CONC_SORT_ORDER_DOC)) {
            sortOrder = ConcordanceSortOrder.DOC;
        }
        request.setSortOrder(sortOrder);
        Set<String> fields = new HashSet<>();
        fields.add(searcherApp.getRhapsodeCollection().getIndexSchema().getUniqueDocField());
        fields.add(searcherApp.getRhapsodeCollection().getIndexSchema().getAttachmentIndexField());
        fields.add(request.getContentField());
        fields.add(searcherApp.getRhapsodeCollection().getIndexSchema().getLinkDisplayField());
        fields.addAll(searcherApp.getSessionManager().getDynamicParameterConfig().getStringList(DynamicParameters.ROW_VIEWER_DISPLAY_FIELDS));


        request.setFields(fields);
        configureClustering(servletRequest, request);
        super.parse(searcherApp, request);
    }

    private void configureClustering(HttpServletRequest servletRequest, ConcordanceSearchRequest request) {
        String clusteringAlgo = servletRequest.getParameter(C.CLUSTER_ALGO_SELECTOR);
        if (StringUtils.isBlank(clusteringAlgo)) {
            return;
        }
        if (C.KMEANS.equals(clusteringAlgo)) {
            request.setClusteringAlgo(BisectingKMeansClusteringAlgorithm.class);
        } else if (C.LINGO.equals(clusteringAlgo)) {
            request.setClusteringAlgo(LingoClusteringAlgorithm.class);
        } else if (C.STC.equals(clusteringAlgo)) {
            request.setClusteringAlgo(STCClusteringAlgorithm.class);
        } else {
            //TODO: log
            return;
        }
        Map<String, Object> map = new HashMap<>();
        String numClustersString = servletRequest.getParameter(C.CLUSTERING_NUM_CLUSTERS);
        if (!StringUtils.isBlank(numClustersString)) {
            try {
                Integer numClusters = Integer.parseInt(numClustersString);
                map.put(ConcordanceClusterer.CLUSTER_COUNT, numClusters);
            } catch (NumberFormatException e) {
                //
            }
        }
        request.setClusteringAttrs(map);
    }
}

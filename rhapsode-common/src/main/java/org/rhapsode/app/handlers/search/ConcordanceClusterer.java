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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.carrot2.core.Cluster;
import org.carrot2.core.Controller;
import org.carrot2.core.ControllerFactory;
import org.carrot2.core.Document;
import org.carrot2.core.ProcessingResult;
import org.tallison.lucene.search.concordance.classic.ConcordanceWindow;

class ConcordanceClusterer {

    public static final String DOCUMENTS = "documents";
    public static final String CLUSTER_COUNT = "BisectingKMeansClusteringAlgorithm.clusterCount";
    public static final String MAX_ITERATIONS = "BisectingKMeansClusteringAlgorithm.maxIterations";


    public static List<Cluster> cluster(Class<? extends org.carrot2.core.ProcessingComponentBase> clustererClass,
                                        Map<String, Object> attrs, List<ConcordanceWindow> windows) {
        List<Document> carrotDocs = new ArrayList<>();
        int i = 0;
        for (ConcordanceWindow window : windows) {

            Document carrotDoc = new Document(window.getTarget(), //title
                    window.getPre() + " " + window.getPost(),//summary
                    "",//content url
                    null, //language code
                    Integer.toString(i++));//id
            carrotDocs.add(carrotDoc);
        }
        final Controller controller = ControllerFactory.createSimple();

        Map<String, Object> localAttrs = new HashMap<>(attrs);
        localAttrs.put(DOCUMENTS, carrotDocs);

        ProcessingResult clusters = controller.process(localAttrs,
                clustererClass);
        return clusters.getClusters();
    }
}

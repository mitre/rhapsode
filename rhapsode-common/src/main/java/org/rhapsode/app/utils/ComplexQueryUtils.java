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

package org.rhapsode.app.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.queryparser.classic.ParseException;
import org.rhapsode.app.RhapsodeSearcherApp;
import org.rhapsode.lucene.search.ComplexQuery;
import org.rhapsode.lucene.search.ComplexQueryBuilder;
import org.rhapsode.lucene.search.StoredQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComplexQueryUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ComplexQueryUtils.class);

    public static Map<Integer, ComplexQuery> parseAllStoredQueries(String commaSeparatedStoredQueryNames,
                                                                   RhapsodeSearcherApp searcherConfig) throws IOException {
        String[] ids = commaSeparatedStoredQueryNames.split(",");
        List<Integer> nameList = new ArrayList<>();
        for (int i = 0; i < ids.length; i++) {
            nameList.add(Integer.parseInt(ids[i]));
        }
        return parseAllStoredQueries(nameList, searcherConfig);
    }

    public static Map<Integer, ComplexQuery> parseAllStoredQueries(List<Integer> storedQueryIds,
                                                                   RhapsodeSearcherApp searcherApp) throws IOException {
        Map<Integer, StoredQuery> sqs = searcherApp.getSessionManager().getStoredQueryManager().getStoredQueryMap();
        Map<Integer, ComplexQuery> ret = new HashMap<>();
        for (Integer id : storedQueryIds) {
            StoredQuery sq = sqs.get(id);
            if (sq == null) {
                LOG.warn("Couldn't find " + id + " as a stored query while trying to highlightSingleFieldValue output");
                continue;
            }

            ComplexQuery cq = null;
            try {
                cq = ComplexQueryBuilder.buildQuery(
                        sq,
                        searcherApp.getParserPlugin(),
                        searcherApp.getSessionManager().getStoredConceptManager(),
                        searcherApp.getGeoConfig(),
                        searcherApp.getRhapsodeCollection().getIgnoredQuery());

                ret.put(id, cq);
            } catch (ParseException e) {
                //silently swallow
                e.printStackTrace();
            }
        }
        return ret;
    }

}

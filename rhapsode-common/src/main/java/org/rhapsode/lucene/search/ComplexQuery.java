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
package org.rhapsode.lucene.search;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.locationtech.spatial4j.shape.Shape;
import org.tallison.lucene.search.spans.SimpleSpanQueryConverter;


//QUERIES HAVE NOT BEEN REWRITTEN!!!
//MUST REWRITE QUERIES!!!
public class ComplexQuery {


    final StoredQuery storedQuery;

    final Query highlightingQuery;
    final Query retrievalQuery;
    final BooleanClause geoClause;
    final Shape queryShape;
    final Map<String, HighlightingQuery> highlightingQueries = new HashMap<>();


    public ComplexQuery(StoredQuery cqs,
                        Query retrievalQuery,
                        Query highlightingQuery,
                        BooleanClause geoClause,
                        Shape queryShape) {
        this.storedQuery = cqs;
        this.retrievalQuery = retrievalQuery;
        this.highlightingQuery = highlightingQuery;
        this.geoClause = geoClause;
        this.queryShape = queryShape;
    }

    public Query getRetrievalQuery() {
        return retrievalQuery;
    }

    public BooleanClause getGeoClause() {
        return geoClause;
    }

    public Shape getQueryShape() {
        return queryShape;
    }

    public boolean isEmpty() {
        if (retrievalQuery != null || geoClause != null) {
            return false;
        }
        return true;
    }

    public StoredQuery getStoredQuery() {
        return storedQuery;
    }

    public Query getHighlightingQuery() {
        return highlightingQuery;
    }

    /**
     * @param field
     * @param defaultPriority (actual value to set default priority to), based on other highlighting queries!!!
     * @return
     * @throws IOException
     */
    public HighlightingQuery getHighlightingQuery(String field, int defaultPriority) throws IOException {
        if (highlightingQueries.containsKey(field)) {
            return highlightingQueries.get(field);
        }
        SimpleSpanQueryConverter c = new SimpleSpanQueryConverter();
        SpanQuery sq = null;
        if (highlightingQuery == null) {
            sq = new SpanOrQuery();//null/empty query
        } else {
            sq = c.convert(field, highlightingQuery);
        }
        int priority = (storedQuery.getPriority() == StoredQuery.DEFAULT_PRIORITY) ? defaultPriority : storedQuery.getPriority();
        HighlightingQuery hq = new HighlightingQuery(sq, priority, storedQuery.getHighlightingStyle());
        highlightingQueries.put(field, hq);
        return hq;
    }

    @Override
    public String toString() {
        return "ComplexQuery{" +
                "storedQuery=" + storedQuery +
                ", highlightingQuery=" + highlightingQuery +
                ", retrievalQuery=" + retrievalQuery +
                ", geoClause=" + geoClause +
                ", queryShape=" + queryShape +
                ", highlightingQueries=" + highlightingQueries +
                '}';
    }
}

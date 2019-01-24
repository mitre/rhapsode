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

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.locationtech.spatial4j.shape.Shape;
import org.rhapsode.geo.GeoConfig;
import org.rhapsode.geo.GeoQueryBuilder;
import org.rhapsode.lucene.queryparsers.ParserPlugin;
import org.rhapsode.lucene.utils.StoredConceptManager;


public class ComplexQueryBuilder {

    public static void validate(String field, String queryString,
                                ParserPlugin parser, StoredConceptManager scm) throws ParseException {
        if (StringUtils.isBlank(queryString)) {
            return;
        }
        String rewritten = scm.rewriteQuery(queryString);
        buildQuery(parser, field, rewritten);

    }

    public static void validateFilter(StoredQuery query, ParserPlugin parser, StoredConceptManager scm)
            throws ParseException {
        try {
            validate(query.getDefaultField(), query.getFilterQueryString(),
                    parser, scm);
        } catch (ParseException e) {
            throw new ParseException("In filter query: " + e.getMessage());
        }
    }

    public static void validateMain(StoredQuery query, ParserPlugin parser, StoredConceptManager scm)
            throws ParseException {
        try {
            validate(query.getDefaultField(), query.getMainQueryString(),
                    parser, scm);
        } catch (ParseException e) {
            throw new ParseException("In main query: " + e.getMessage());
        }
    }

    public static ComplexQuery buildQuery(StoredQuery storedQuery, ParserPlugin parser,
                                          StoredConceptManager scm,
                                          GeoConfig geoConfig, Query ignoreQuery)
            throws ParseException {

        String rewrittenMainQuery = scm.rewriteQuery(storedQuery.getMainQueryString());
        String rewrittenFilterQuery = scm.rewriteQuery(storedQuery.getFilterQueryString());

        Query highlightingQuery = buildQuery(parser, storedQuery.getDefaultField(),
                rewrittenMainQuery);

        BooleanClause filterClause = buildFilter(parser, storedQuery.getDefaultField(),
                rewrittenFilterQuery);

        BooleanClause geoClause = null;
        Shape queryShape = null;
        GeoQueryBuilder builder = null;
        if (!StringUtils.isBlank(storedQuery.getGeoQueryString())) {
            builder = new GeoQueryBuilder(geoConfig);

            geoClause = builder.buildGeoClause(geoConfig.GEOHASHES_FIELD_NAME,
                    storedQuery.getGeoQueryString(),
                    storedQuery.getGeoQueryRadiusString());
            queryShape = builder.buildQueryShape(storedQuery.getGeoQueryString(),
                    storedQuery.getGeoQueryRadiusString());
        }
        Query retrievalQuery = combineQueries(highlightingQuery, geoClause, filterClause,
                ignoreQuery);

        //if there is no highlighting query, colorize the filterquery
        if (highlightingQuery == null && filterClause != null &&
                filterClause.getOccur() != Occur.MUST_NOT) {
            highlightingQuery = filterClause.getQuery();
        }

        return new ComplexQuery(storedQuery,
                retrievalQuery,
                highlightingQuery,
                geoClause,
                queryShape);

    }


    static BooleanClause buildFilter(ParserPlugin parser, String fieldName, String queryString)
            throws ParseException {
        if (StringUtils.isBlank(queryString)) {
            return null;
        }

        BooleanClause.Occur occur = BooleanClause.Occur.SHOULD;
        if (queryString.startsWith("AND ")) {
            queryString = queryString.substring(4);
            occur = BooleanClause.Occur.MUST;
        } else if (queryString.startsWith("NOT ")) {
            queryString = queryString.substring(4);
            occur = BooleanClause.Occur.MUST_NOT;
        }

        Query q = buildQuery(parser, fieldName, queryString);
        BooleanClause clause = new BooleanClause(q, occur);
        return clause;
    }

    static Query buildQuery(ParserPlugin parser, String defaultField, String queryString) throws ParseException {

        if (StringUtils.isEmpty(queryString)) {
            return null;
        }
        return parser.parse(defaultField, queryString);
    }

    static Query combineQueries(Query mainQuery, BooleanClause geoFilter,
                                BooleanClause simpleFilter,
                                Query ignoreQuery) throws ParseException {
        List<BooleanClause> clauses = new LinkedList<>();
        if (geoFilter != null) {
            clauses.add(geoFilter);
        }
        if (simpleFilter != null) {
            if (simpleFilter.getOccur() == Occur.SHOULD) {
                simpleFilter = new BooleanClause(simpleFilter.getQuery(), Occur.FILTER);
            }
            clauses.add(simpleFilter);
        }

        boolean allShoulds = true;
        for (BooleanClause c : clauses) {
            if (c.getOccur() != BooleanClause.Occur.SHOULD) {
                allShoulds = false;
            }
        }
        //can't remember why we need this...do we?
        if (mainQuery != null) {
            if (allShoulds) {
                clauses.add(new BooleanClause(mainQuery, BooleanClause.Occur.SHOULD));
            } else {
                clauses.add(new BooleanClause(mainQuery, BooleanClause.Occur.MUST));
            }
        }
        if (clauses.size() == 0) {
            return null;
        }
        if (ignoreQuery != null) {
            clauses.add(new BooleanClause(ignoreQuery, Occur.MUST_NOT));
        }
        boolean allNots = true;
        for (BooleanClause c : clauses) {
            if (c.getOccur() != Occur.MUST_NOT) {
                allNots = false;
                break;
            }
        }

        if (allNots) {
            clauses.add(new BooleanClause(new MatchAllDocsQuery(), Occur.SHOULD));
        }
        BooleanQuery.Builder b = new BooleanQuery.Builder();
        for (BooleanClause c : clauses) {
            b.add(c);
        }
        return b.build();
    }
      /*
        if (geoFilter == null && simpleFilter == null){
			//1
			return null;
		} else if (mainQuery == null && geoFilter != null && simpleFilter == null){
			//2
			return geoFilter;
		} else if (mainQuery == null  && geoFilter == null && simpleFilter != null){
			//3
			throw new ParseException("Sorry, but I can't figure out what you're searching for...filter only query?!");
		} else if (mainQuery != null && geoFilter != null && simpleFilter == null){
			if (config.isAndGeo() == true){
				//4
				return geoFilter;
			} else {
				//5
				return null;
			}
		} else if (mainQuery != null && geoFilter == null && simpleFilter != null){
			//6
			return simpleFilter;
		} else if (mainQuery == null && geoFilter != null && simpleFilter != null){
			//7
			BooleanFilter b = new BooleanFilter();
			b.add(geoFilter, Occur.MUST);
			b.add(simpleFilter, Occur.MUST);
			return b;
		} else if (mainQuery != null && geoFilter != null && simpleFilter != null){
			if(config.isAndGeo() == true && config.isAndFilterQuery() == true){
				//8
				BooleanFilter b = new BooleanFilter();
				b.add(geoFilter, Occur.MUST);
				b.add(simpleFilter, Occur.MUST);
				return b;
			} else if(config.isAndGeo() == true && config.isAndFilterQuery() != true){
				//9
				BooleanFilter b = new BooleanFilter();
				b.add(geoFilter, Occur.SHOULD);
				b.add(simpleFilter, Occur.SHOULD);
				return b;
			} else if(config.isAndGeo() == false && config.isAndFilterQuery() == true){
				//10 is this right?!
				return simpleFilter;
			} else if (config.isAndGeo() == false && config.isAndFilterQuery() == false){
				return null;
			}


		}
		return null;*/


}

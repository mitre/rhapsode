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

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.Query;
import org.rhapsode.app.RhapsodeSearcherApp;
import org.rhapsode.app.contants.C;
import org.rhapsode.app.contants.H;
import org.rhapsode.app.session.DynamicParameters;
import org.rhapsode.app.utils.StoredQCUtils;
import org.rhapsode.lucene.search.BaseSearchRequest;
import org.rhapsode.lucene.search.ComplexQuery;
import org.rhapsode.lucene.search.ComplexQueryBuilder;
import org.rhapsode.lucene.search.SQField;
import org.rhapsode.lucene.search.StoredQuery;
import org.rhapsode.lucene.search.StoredQueryBuilder;
import org.rhapsode.util.LanguageDirection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


public class BaseRequestBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(BaseRequestBuilder.class);

    public void extractBase(RhapsodeSearcherApp searcherApp, HttpServletRequest servletRequest,
                            BaseSearchRequest request) {
        LanguageDirection defaultLanguageDirection =
                searcherApp.getSessionManager().getDynamicParameterConfig().getLanguageDirection(DynamicParameters.DEFAULT_LANGUAGE_DIRECTION);

        setLanguageDirection(servletRequest, request, defaultLanguageDirection);
        String defaultQueryField = searcherApp.getSessionManager()
                .getDynamicParameterConfig()
                .getString(DynamicParameters.DEFAULT_CONTENT_FIELD);
        String defaultQueryFieldString = servletRequest.getParameter(C.DEFAULT_QUERY_FIELD);
        defaultQueryField = (defaultQueryFieldString == null) ? defaultQueryField : defaultQueryFieldString;

        String sqIdString = servletRequest.getParameter(C.STORED_QUERY_ID);
        StoredQuery storedQuery = null;
        //if no sq name is specified
        //buildStoredQuery one with a name=null
        if (sqIdString == null) {
            //TODO: we used to grab the style from the first
            //Set<String> styles = searcherApp.getCommonSearchConfig().getHighlightingStyles().keySet();
            //Iterator<String> it = styles.iterator();String firstStyle = it.next();
            //TODO: need to add exception handling here!!!
            storedQuery = StoredQCUtils.buildStoredQuery(
                    StoredQCUtils.getQueryFields(servletRequest, defaultQueryField));
        } else {
            storedQuery = loadStoredQuery(sqIdString, searcherApp);

        }

        if (servletRequest.getParameter(C.USE_IGNORE_QUERY) != null) {
            request.setUseIgnoreQuery(true);
            storedQuery = getDefaultEmptyCQCs(searcherApp.getRhapsodeCollection().getIndexSchema().getDefaultContentField());
        }
        if (servletRequest.getParameter(C.USE_FAVORITES_QUERY) != null) {
            request.setUseFavoritesQuery(true);
            storedQuery = getDefaultEmptyCQCs(searcherApp.getRhapsodeCollection().getIndexSchema().getDefaultContentField());
        }
        request.setStoredQuery(storedQuery);

        Set<String> selected = new HashSet<>();

        if (servletRequest.getParameter(C.SEARCH) != null) {
            request.setActionType(BaseSearchRequest.ActionType.SEARCH);
        } else if (servletRequest.getParameter(C.SELECT_ALL) != null) {
            request.setActionType(BaseSearchRequest.ActionType.SELECT_ALL);
        } else if (servletRequest.getParameter(C.ADD_SELECTED_TO_FAVORITES) != null) {
            request.setActionType(BaseSearchRequest.ActionType.ADD_SELECTED_TO_FAVORITES);
        } else if (servletRequest.getParameter(C.ADD_SELECTED_TO_IGNORE) != null) {
            request.setActionType(BaseSearchRequest.ActionType.ADD_SELECTED_TO_IGNORE);
        } else if (servletRequest.getParameter(C.NEXT) != null) {
            request.setActionType(BaseSearchRequest.ActionType.NEXT);
        } else if (servletRequest.getParameter(C.PREVIOUS) != null) {
            request.setActionType(BaseSearchRequest.ActionType.PREVIOUS);
        } else if (servletRequest.getParameter(C.DESELECT_ALL) != null) {
            request.setActionType((BaseSearchRequest.ActionType.DESELECT_ALL));
        } else {
            //default
            request.setActionType(BaseSearchRequest.ActionType.SEARCH);
        }

        String[] selectedArr = servletRequest.getParameterValues(C.SELECTED_DOC_IDS);
        if (selectedArr != null &&
                request.getActionType() != BaseSearchRequest.ActionType.DESELECT_ALL) {
            for (String docId : servletRequest.getParameterValues(C.SELECTED_DOC_IDS)) {
                selected.add(docId);
            }
        }
        request.setSelectedDocIds(selected);
    }

    private StoredQuery getDefaultEmptyCQCs(String defaultContentField) {
        return new StoredQueryBuilder(-1, null)
                .add(SQField.DEFAULT_FIELD, defaultContentField)
                .build();
    }

    public void parse(RhapsodeSearcherApp searcherApp, BaseSearchRequest request)
            throws IOException, ParseException {

        if (request.getUseIgnoreQuery() || request.getUseFavoritesQuery()) {
            setIgnoreOrFavorites(searcherApp, request);
            return;
        }

        LOG.trace("stored query: " + request.getStoredQuery());
        ComplexQuery cq = ComplexQueryBuilder.buildQuery(request.getStoredQuery(),
                searcherApp.getParserPlugin(),
                searcherApp.getSessionManager().getStoredConceptManager(),
                searcherApp.getGeoConfig(),
                searcherApp.getRhapsodeCollection().getIgnoredQuery());
        request.setComplexQuery(cq);

    }

    public void parse(RhapsodeSearcherApp searcherApp, BaseSearchRequest request, MultiTermQuery.RewriteMethod rewriteMethod)
            throws IOException, ParseException {

        if (request.getUseIgnoreQuery() || request.getUseFavoritesQuery()) {
            setIgnoreOrFavorites(searcherApp, request);
            return;
        }


        ComplexQuery cq = ComplexQueryBuilder.buildQuery(request.getStoredQuery(),
                searcherApp.getParserPlugin(rewriteMethod),
                searcherApp.getSessionManager().getStoredConceptManager(),
                searcherApp.getGeoConfig(),
                searcherApp.getRhapsodeCollection().getIgnoredQuery());
        request.setComplexQuery(cq);

    }

    private void setIgnoreOrFavorites(RhapsodeSearcherApp searcherApp, BaseSearchRequest request) throws IOException {
        Query retrievalQuery = null;
        if (request.getUseIgnoreQuery()) {
            retrievalQuery = searcherApp.getRhapsodeCollection().getIgnoredQuery();
        } else if (request.getUseFavoritesQuery()) {
            retrievalQuery = searcherApp.getRhapsodeCollection().getFavoritesQuery();
        }

        ComplexQuery cq = new ComplexQuery(request.getStoredQuery(), retrievalQuery, null, null, null);
        request.setComplexQuery(cq);
    }

    private void setLanguageDirection(HttpServletRequest servletRequest, BaseSearchRequest request,
                                      LanguageDirection defaultLanguageDirection) {
        request.setLanguageDirection(getLanguageDirection(servletRequest, defaultLanguageDirection));
    }

    private LanguageDirection getLanguageDirection(HttpServletRequest servletRequest,
                                                   LanguageDirection defaultLanguageDirection) {
        String ldir = servletRequest.getParameter(C.LANG_DIR);
        if (ldir != null) {
            if (ldir.equals(H.LTR)) {
                return LanguageDirection.LTR;
            } else if (ldir.equals(H.RTL)) {
                return LanguageDirection.RTL;
            }
        }
        return defaultLanguageDirection;
    }
/*
    private ComplexQueryComponents getQueryFields(StoredQuery sq, String defaultField) {
        if (sq == null) {
            return new ComplexQueryComponents("","","","", "", -1, "");
        }
        String storedDefaultField = sq.getDefaultField();
        if (storedDefaultField == null) {
            storedDefaultField = defaultField;
        }
        return new ComplexQueryComponents(storedDefaultField,
                sq.getMainQueryString(), sq.getFilterQueryString(), null, null,
                sq.getPriority(), sq.getHighlightingStyle());

    }
    */
    //TODO, we used to grab the styles from the common search config
    //we aren't doing that any more...try to re-inject
    /*
            Set<String> styles = searcherApp.getCommonSearchConfig().getHighlightingStyles().keySet();
        Iterator<String> it = styles.iterator();
        String firstStyle = it.next();

     */

    //returns null if not found
    private StoredQuery loadStoredQuery(String idString, RhapsodeSearcherApp config) {
        int id = Integer.parseInt(idString);
        try {
            StoredQuery sq = config.getSessionManager().getStoredQueryManager().getStoredQuery(id);
            if (sq == null) {
                LOG.debug("couldn't find stored query: " + id);
                return null;//new ComplexQueryStrings("","","","","");
            }
            return sq;
/*            //TODO: geo not yet supported
            String storedDefaultField = sq.getDefaultField();
            if (storedDefaultField == null) {
                storedDefaultField = defaultField;
            }
            return new ComplexQueryStrings(storedDefaultField, sq.getMainQuery(), sq.getFilterQuery(), null, null);*/
        } catch (IOException e) {
            LOG.warn("couldn't load stored query with id: " + id, e);
        }
        return null;
    }
}

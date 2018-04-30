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

package org.rhapsode.app.handlers.admin;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.Query;
import org.eclipse.jetty.server.Request;
import org.rhapsode.app.RhapsodeSearcherApp;
import org.rhapsode.app.contants.C;
import org.rhapsode.app.contants.H;
import org.rhapsode.app.contants.Internal;
import org.rhapsode.app.decorators.RhapsodeDecorator;
import org.rhapsode.app.decorators.RhapsodeXHTMLHandler;
import org.rhapsode.app.handlers.BasicSearchUtil;
import org.rhapsode.app.session.DynamicParameters;
import org.rhapsode.lucene.search.BaseSearchRequest;
import org.rhapsode.lucene.search.ComplexQuery;
import org.rhapsode.lucene.search.SQField;
import org.rhapsode.lucene.search.StoredQuery;
import org.rhapsode.lucene.search.StoredQueryBuilder;
import org.rhapsode.lucene.search.basic.BasicSearchConfig;
import org.rhapsode.lucene.search.basic.BasicSearchRequest;
import org.rhapsode.lucene.search.basic.BasicSearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


public class SelectedDocumentHandler extends AdminHandler {

    private final RhapsodeSearcherApp searcherApp;
    private static final Logger LOG = LoggerFactory.getLogger(SelectedDocumentHandler.class);
    private final BasicSearchConfig bsc;

    public SelectedDocumentHandler(RhapsodeSearcherApp searcherApp) {
        super("Selected Documents");
        this.searcherApp = searcherApp;
        bsc = searcherApp.getBasicSearchConfig().deepCopy();


    }

    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest,
                       HttpServletResponse response) throws IOException, ServletException {

        RhapsodeXHTMLHandler xhtml = null;

        SelectedDocumentRequest sr = SelectedDocumentRequest.build(searcherApp, httpServletRequest);
        String errorMessage = null;
        try {
            xhtml = initResponse(response, null);
            if (!searcherApp.hasCollection()) {
                try {
                    RhapsodeDecorator.writeNoCollection(xhtml);
                    response.getOutputStream().flush();
                } catch (SAXException e) {
                    LOG.error("problem writing no collection", e);
                }
                return;
            }
            xhtml.startElement(H.FORM, H.METHOD, H.POST);

            writeButtons(sr, xhtml);
            switch (sr.getActionType()) {
                case VIEW_FAVORITES:
                    showFavorites(sr, xhtml);
                    break;
                case VIEW_IGNORED:
                    showIgnored(sr, xhtml);
                    break;
                case SELECT_ALL_IGNORED:
                    showIgnored(sr, xhtml);
                    break;
                case CLEAR_SELECTED_IGNORED:
                    clearIgnored(sr);
                    showIgnored(sr, xhtml);
                    break;
                case SELECT_ALL_FAVORITES:
                    showFavorites(sr, xhtml);
                    break;
                case CLEAR_SELECTED_FAVORITES:
                    clearFavorites(sr);
                    showFavorites(sr, xhtml);
                    break;
                default:
                    showFavorites(sr, xhtml);
            }
            if (!StringUtils.isBlank(errorMessage)) {
                xhtml.br();
                RhapsodeDecorator.writeErrorMessage(errorMessage, xhtml);
                xhtml.br();
            }

            xhtml.endElement(H.FORM);
            RhapsodeDecorator.addFooter(xhtml);
            xhtml.endDocument();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearFavorites(SelectedDocumentRequest sr) {
        searcherApp.getRhapsodeCollection().removeFavorites(sr.getDocIds());
    }

    private void clearIgnored(SelectedDocumentRequest sr) {
        searcherApp.getRhapsodeCollection().removeIgnoreds(sr.getDocIds());
    }

    private void writeButtons(SelectedDocumentRequest sr, RhapsodeXHTMLHandler xhtml) throws SAXException {
        xhtml.br();

        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.SELECTED_VIEW_FAVORITES,
                H.VALUE, "View Favorite Documents",
                "default", "default");
        xhtml.endElement(H.INPUT);
        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.SELECTED_VIEW_IGNORED,
                H.VALUE, "View Ignored Documents",
                "default", "default");
        xhtml.endElement(H.INPUT);

        boolean showFavorites = true;
        switch (sr.getActionType()) {
            case VIEW_IGNORED:
                showFavorites = false;
                break;
            case SELECT_ALL_IGNORED:
                showFavorites = false;
                break;
            case CLEAR_SELECTED_IGNORED:
                showFavorites = false;
                break;
        }


        if (showFavorites == true && searcherApp.getRhapsodeCollection().getFavoritesSize() > 0
                && searcherApp.getSessionManager().getDynamicParameterConfig().getBoolean(DynamicParameters.SHOW_SELECTED)) {
            xhtml.startElement(H.INPUT,
                    H.TYPE, H.SUBMIT,
                    H.NAME, C.SELECT_ALL_FAVORITES,
                    H.VALUE, "Select All");
            xhtml.endElement(H.INPUT);
            xhtml.startElement(H.INPUT,
                    H.TYPE, H.SUBMIT,
                    H.NAME, C.CLEAR_SELECTED_FAVORITES,
                    H.VALUE, "Remove Selected");
            xhtml.endElement(H.INPUT);
        } else if (showFavorites == false && searcherApp.getRhapsodeCollection().getIgnoredSize() > 0 &&
                searcherApp.getSessionManager().getDynamicParameterConfig().getBoolean(DynamicParameters.SHOW_SELECTED)) {
            xhtml.startElement(H.INPUT,
                    H.TYPE, H.SUBMIT,
                    H.NAME, C.SELECT_ALL_IGNORED,
                    H.VALUE, "Select All");
            xhtml.endElement(H.INPUT);
            xhtml.startElement(H.INPUT,
                    H.TYPE, H.SUBMIT,
                    H.NAME, C.CLEAR_SELECTED_IGNORED,
                    H.VALUE, "Remove Selected");
            xhtml.endElement(H.INPUT);
        }

        xhtml.br();

    }


    //REFACTOR THIS MESS!!!
    private void showFavorites(SelectedDocumentRequest sr, RhapsodeXHTMLHandler xhtml) throws Exception {
        int sz = searcherApp.getRhapsodeCollection().getFavoritesSize();
        if (sz <= 0) {
            xhtml.characters("There are currently no 'favorite' documents.");
            return;
        }
        if (sz == 1) {
            xhtml.characters("There is currently one 'favorite' document.");
        } else {
            xhtml.characters("There are currently " + searcherApp.getRhapsodeCollection().getFavoritesSize() + " 'favorite' documents.");
        }

        Query q = searcherApp.getRhapsodeCollection().getFavoritesQuery();

        BasicSearchRequest r = new BasicSearchRequest(searcherApp.getBasicSearchConfig());
        if (sr.getActionType() == SelectedDocumentRequest.ACTION_TYPE.SELECT_ALL_FAVORITES) {
            r.setActionType(BaseSearchRequest.ActionType.SELECT_ALL);
        } else {
            r.setActionType(BaseSearchRequest.ActionType.SEARCH);
        }
        r.setStartAndEndResult(0, searcherApp.getRhapsodeCollection().getFavoritesSize());
        r.setResultsPerPage(searcherApp.getRhapsodeCollection().getFavoritesSize());

        StoredQuery storedQuery = new StoredQueryBuilder(-1, Internal.FAVORITES_QUERY_NAME).
                add(SQField.DEFAULT_FIELD,
                        searcherApp.getRhapsodeCollection().getIndexSchema().getDefaultContentField()).
                build();
        ComplexQuery cq = new ComplexQuery(storedQuery, q, null, null, null);
        r.setComplexQuery(cq);
        r.setStoredQuery(storedQuery);
        r.setLanguageDirection(searcherApp.getSessionManager().getDynamicParameterConfig().getLanguageDirection(DynamicParameters.DEFAULT_LANGUAGE_DIRECTION));
        //fornow
        r.setSelectedDocIds(new HashSet<>());
        r.setUseFavoritesQuery(true);

        //TODO: figure out how not to duplicate this!
        Set<String> fields = new HashSet<>();
        fields.add(searcherApp.getRhapsodeCollection().getIndexSchema().getUniqueDocField());
        fields.add(searcherApp.getRhapsodeCollection().getIndexSchema().getAttachmentIndexField());
        fields.add(searcherApp.getRhapsodeCollection().getIndexSchema().getDefaultContentField());
        fields.add(searcherApp.getRhapsodeCollection().getIndexSchema().getLinkDisplayField());

        //TODO: add in fields to display
        r.setFields(fields);

        BasicSearchResults results = BasicSearchUtil.executeSearch(searcherApp, r);
        BasicSearchUtil.writeBasicResults(searcherApp, r, results, xhtml);
    }

    private void showIgnored(SelectedDocumentRequest sr, RhapsodeXHTMLHandler xhtml) throws Exception {

        int sz = searcherApp.getRhapsodeCollection().getIgnoredSize();
        if (sz <= 0) {
            xhtml.characters("There are currently no 'ignored' documents.");
            return;
        }
        if (sz == 1) {
            xhtml.characters("There is currently one 'ignored' document.");
        } else {
            xhtml.characters("There are currently " + searcherApp.getRhapsodeCollection().getIgnoredSize() + " 'ignored' documents.");
        }

        Query q = searcherApp.getRhapsodeCollection().getIgnoredQuery();

        BasicSearchRequest r = new BasicSearchRequest(searcherApp.getBasicSearchConfig());
        if (sr.getActionType() == SelectedDocumentRequest.ACTION_TYPE.SELECT_ALL_IGNORED) {
            r.setActionType(BaseSearchRequest.ActionType.SELECT_ALL);
        } else {
            r.setActionType(BaseSearchRequest.ActionType.SEARCH);
        }
        r.setStartAndEndResult(0, searcherApp.getRhapsodeCollection().getIgnoredSize());
        r.setResultsPerPage(searcherApp.getRhapsodeCollection().getIgnoredSize());

        StoredQuery storedQuery = new StoredQueryBuilder(-1, Internal.IGNORED_QUERY_NAME)
                .add(SQField.DEFAULT_FIELD,
                        searcherApp.getRhapsodeCollection().getIndexSchema().getDefaultContentField())
                .build();
        ComplexQuery cq = new ComplexQuery(storedQuery, q, null, null, null);
        r.setComplexQuery(cq);
        r.setStoredQuery(storedQuery);
        r.setLanguageDirection(searcherApp.getSessionManager().getDynamicParameterConfig().getLanguageDirection(DynamicParameters.DEFAULT_LANGUAGE_DIRECTION));
        //fornow
        r.setSelectedDocIds(new HashSet<>());
        r.setUseIgnoreQuery(true);

        //TODO: figure out how not to duplicate this!
        Set<String> fields = new HashSet<>();
        fields.add(searcherApp.getRhapsodeCollection().getIndexSchema().getUniqueDocField());
        fields.add(searcherApp.getRhapsodeCollection().getIndexSchema().getAttachmentIndexField());
        fields.add(searcherApp.getRhapsodeCollection().getIndexSchema().getDefaultContentField());
        fields.add(searcherApp.getRhapsodeCollection().getIndexSchema().getLinkDisplayField());

        //TODO: add in fields to display
        r.setFields(fields);

        BasicSearchResults results = BasicSearchUtil.executeSearch(searcherApp, r);
        BasicSearchUtil.writeBasicResults(searcherApp, r, results, xhtml);

    }
}

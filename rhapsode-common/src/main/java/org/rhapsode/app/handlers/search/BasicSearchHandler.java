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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Locale;

import org.apache.lucene.queryparser.classic.ParseException;
import org.eclipse.jetty.server.Request;
import org.rhapsode.app.RhapsodeSearcherApp;
import org.rhapsode.app.contants.C;
import org.rhapsode.app.contants.H;
import org.rhapsode.app.decorators.RhapsodeDecorator;
import org.rhapsode.app.decorators.RhapsodeXHTMLHandler;
import org.rhapsode.app.handlers.BasicSearchUtil;
import org.rhapsode.app.session.DynamicParameters;
import org.rhapsode.lucene.search.BaseSearchRequest;
import org.rhapsode.lucene.search.basic.BasicSearchRequest;
import org.rhapsode.lucene.search.basic.BasicSearchResults;
import org.rhapsode.util.UserLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;


public class BasicSearchHandler extends AbstractSearchHandler {

    private static final Logger LOG = LoggerFactory.getLogger(BasicSearchHandler.class);
    private static final String TOOL_NAME = "Basic Search";

    private final RhapsodeSearcherApp searcherApp;
    private final BasicSearchRequestBuilder requestBuilder;

    public BasicSearchHandler(RhapsodeSearcherApp searcherApp) {
        super(TOOL_NAME);
        this.searcherApp = searcherApp;
        requestBuilder = new BasicSearchRequestBuilder();
    }

    private static void writeNextPrev(BasicSearchRequest basicSearchRequest,
                                      long totalHits, RhapsodeXHTMLHandler xhtml) throws SAXException {
        if (totalHits < 2) {
            return;
        }
        xhtml.startElement(H.TABLE);
        xhtml.startElement(H.TR);
        //previous
        xhtml.startElement(H.TD, H.ALIGN, "left");
        if (basicSearchRequest.getLastStart() > 0) {
            xhtml.startElement(H.INPUT,
                    H.TYPE, H.SUBMIT,
                    H.VALUE, "Previous",
                    H.NAME, C.PREVIOUS);
            xhtml.endElement(H.INPUT);
        }
        xhtml.endElement(H.TD);

        //next
        xhtml.startElement(H.TD, H.ALIGN, "right");
        if (basicSearchRequest.getLastEnd() < totalHits - 1) {
            xhtml.startElement(H.INPUT,
                    H.TYPE, H.SUBMIT,
                    H.VALUE, "Next",
                    H.NAME, C.NEXT);
            xhtml.endElement(H.INPUT);
        }
        xhtml.endElement(H.TD);

        xhtml.endElement(H.TR);
        xhtml.endElement(H.TABLE);
    }

    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest,
                       HttpServletResponse response) throws IOException, ServletException {

        init(request, response);
        RhapsodeXHTMLHandler xhtml = null;
        try {
            xhtml = initResponse(response, null);
        } catch (SAXException e) {
            LOG.error("problem init", e);
            throw new IOException(e);
        }

        if (!searcherApp.hasCollection()) {
            try {
                RhapsodeDecorator.writeNoCollection(xhtml);
                response.getOutputStream().flush();
            } catch (SAXException e) {
                LOG.error("problem no collection", e);
            }
            return;
        }

        String errorMsg = null;
        BasicSearchRequest basicSearchRequest = null;
        BasicSearchResults results = null;
        try {
            basicSearchRequest = new BasicSearchRequest(searcherApp.getBasicSearchConfig());
            requestBuilder.extract(searcherApp, httpServletRequest, basicSearchRequest);
        } catch (ParseException e) {
            errorMsg = "Parse Exception: " + e.getMessage();
            UserLogger.logException(TOOL_NAME, errorMsg, httpServletRequest);
            e.printStackTrace();
        } catch (NullPointerException e) {
            errorMsg = "Parse Exception: didn't recognize field";
            UserLogger.logException(TOOL_NAME, errorMsg, httpServletRequest);
            e.printStackTrace();
        }
        if (errorMsg == null) {
            try {
                long startTime = System.currentTimeMillis();
                results = BasicSearchUtil.executeSearch(searcherApp, basicSearchRequest);
                UserLogger.log(TOOL_NAME, basicSearchRequest.getComplexQuery(), results.getTotalHits(), (System.currentTimeMillis() - startTime));
            } catch (Exception e) {
                e.printStackTrace();
                errorMsg = e.getMessage();
                LOG.error("prob w search", e);
            } finally {
                //release collection
            }
        }

        try {
            writeFormAndResults(basicSearchRequest, results, errorMsg, xhtml);
        } catch (Exception e) {
            LOG.error("prob writing form", e);
        }
        try {
            RhapsodeDecorator.addFooter(xhtml);
            xhtml.endDocument();
        } catch (SAXException e) {
            LOG.error("prob at footer", e);
        }

        response.getOutputStream().flush();
    }

    private void writeFormAndResults(BasicSearchRequest basicSearchRequest,
                                     BasicSearchResults results, String errorMessage,
                                     RhapsodeXHTMLHandler xhtml) throws IOException {
        try {
            xhtml.startElement(H.FORM,
                    H.ACTION, "/rhapsode/basic/",//clear query from url
                    H.METHOD, H.POST);

            addQueryWindow(searcherApp, basicSearchRequest, xhtml);
            xhtml.br();
            addResultsPerPage(basicSearchRequest, xhtml);
            xhtml.br();
            RhapsodeDecorator.writeLanguageDirection(searcherApp.getSessionManager()
                            .getDynamicParameterConfig()
                            .getBoolean(
                                    DynamicParameters.SHOW_LANGUAGE_DIRECTION),
                    basicSearchRequest.getLanguageDirection(), xhtml);
            xhtml.br();
            addNumResults(basicSearchRequest.hasQuery(), results,
                    searcherApp.getRhapsodeCollection().getIgnoredSize(), xhtml);
            xhtml.br();
            xhtml.br();
            addHiddenInputAndButtons(basicSearchRequest, results, xhtml);

            if (errorMessage == null && basicSearchRequest.hasQuery() &&
                    results.getResults().size() > 0) {
                writeNextPrev(basicSearchRequest, results.getTotalHits(), xhtml);
                BasicSearchUtil.writeBasicResults(searcherApp, basicSearchRequest, results, xhtml);
                writeNextPrev(basicSearchRequest, results.getTotalHits(), xhtml);
            } else if (errorMessage != null) {
                RhapsodeDecorator.writeErrorMessage(errorMessage, xhtml);
            }
            xhtml.endElement(H.FORM);
        } catch (SAXException e) {
            e.printStackTrace();
            throw new IOException(e);
        }
    }

    private void addResultsPerPage(BasicSearchRequest basicSearchRequest,
                                   RhapsodeXHTMLHandler xhtml) throws SAXException {
        xhtml.characters("Results per page: ");
        xhtml.startElement(H.INPUT,
                H.TYPE, H.TEXT,
                H.NAME, C.RESULTS_PER_PAGE,
                H.VALUE, Integer.toString(basicSearchRequest.getResultsPerPage()),
                H.SIZE, "2");
        xhtml.endElement(H.INPUT);
    }

    private void addNumResults(boolean hasQuery, BasicSearchResults results,
                               int numIgnoredDocs,
                               RhapsodeXHTMLHandler xhtml) throws SAXException {
        if (!hasQuery) {
            return;
        }
        if (results == null) {
            return;
        }
        DecimalFormat formatter = new DecimalFormat("###,###,###,###,###");

        if (results.getTotalHits() == 1) {
            xhtml.characters("There was one result within " +
                    formatter.format(results.getTotalDocs()) + " documents.");

        } else if (results.getTotalHits() == 0 || results.getTotalHits() > 1) {
            xhtml.characters("There were " + formatter.format(results.getTotalHits()) + " results within " +
                    formatter.format(results.getTotalDocs()) + " documents.");
        }
        if (numIgnoredDocs > 0) {
            xhtml.br();
            if (numIgnoredDocs == 1) {
                xhtml.characters("There is currently one ignored document.");
            } else {
                xhtml.characters("There are currently " + numIgnoredDocs + " ignored documents.");
            }
        }
        xhtml.br();

    }

    private void addHiddenInputAndButtons(BasicSearchRequest basicSearchRequest, BasicSearchResults results,
                                          RhapsodeXHTMLHandler xhtml) throws SAXException {
        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.SEARCH,
                H.VALUE, "Search",
                "default", "");
        xhtml.endElement(H.INPUT);

        if (results != null && results.getResults() != null && results.getResults().size() > 0 &&
                searcherApp.getSessionManager().getDynamicParameterConfig().getBoolean(DynamicParameters.SHOW_SELECTED)) {
            xhtml.br();
            if (basicSearchRequest.getActionType().equals(BaseSearchRequest.ActionType.SELECT_ALL)) {
                xhtml.startElement(H.INPUT,
                        H.TYPE, H.SUBMIT,
                        H.NAME, C.DESELECT_ALL,
                        H.VALUE, "Deselect All");
                xhtml.endElement(H.INPUT);
            } else {
                xhtml.startElement(H.INPUT,
                        H.TYPE, H.SUBMIT,
                        H.NAME, C.SELECT_ALL,
                        H.VALUE, "Select All");
                xhtml.endElement(H.INPUT);
            }

            xhtml.startElement(H.INPUT,
                    H.TYPE, H.SUBMIT,
                    H.NAME, C.ADD_SELECTED_TO_FAVORITES,
                    H.VALUE, "Add Selected to Favorite List");
            xhtml.endElement(H.INPUT);
            xhtml.startElement(H.INPUT,
                    H.TYPE, H.SUBMIT,
                    H.NAME, C.ADD_SELECTED_TO_IGNORE,
                    H.VALUE, "Add Selected to Ignored List");
            xhtml.endElement(H.INPUT);

        }

        //last start
        xhtml.startElement(H.INPUT,
                H.TYPE, H.HIDDEN,
                H.NAME, C.LAST_START,
                H.VALUE, Integer.toString(basicSearchRequest.getLastStart()));
        xhtml.endElement(H.INPUT);

        //last end
        xhtml.startElement(H.INPUT,
                H.TYPE, H.HIDDEN,
                H.NAME, C.LAST_END,
                H.VALUE, Integer.toString(basicSearchRequest.getLastEnd()));
        xhtml.endElement(H.INPUT);

        //paging direction
        xhtml.startElement(H.INPUT,
                H.TYPE, H.HIDDEN,
                H.NAME, C.PAGING_DIRECTION,
                H.VALUE, basicSearchRequest.getPagingDirection().toString().toLowerCase(Locale.ENGLISH));
        xhtml.endElement(H.INPUT);
    }

    @Override
    int getMainQueryBoxWidth(RhapsodeSearcherApp searcherApp) {
        return searcherApp.getSessionManager().getDynamicParameterConfig().getInt(DynamicParameters.BS_MAIN_QUERY_WIDTH);

    }

    @Override
    int getMainQueryBoxHeight(RhapsodeSearcherApp searcherApp) {
        return searcherApp.getSessionManager().getDynamicParameterConfig().getInt(DynamicParameters.BS_MAIN_QUERY_HEIGHT);

    }

    @Override
    int getFilterQueryBoxWidth(RhapsodeSearcherApp searcherApp) {
        return searcherApp.getSessionManager().getDynamicParameterConfig().getInt(DynamicParameters.BS_FILTER_QUERY_WIDTH);
    }

    @Override
    int getFilterQueryBoxHeight(RhapsodeSearcherApp searcherApp) {
        return searcherApp.getSessionManager().getDynamicParameterConfig().getInt(DynamicParameters.BS_FILTER_QUERY_HEIGHT);
    }


}

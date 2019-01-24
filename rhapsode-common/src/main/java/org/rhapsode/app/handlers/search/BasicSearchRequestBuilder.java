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

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.queryparser.classic.ParseException;
import org.rhapsode.app.RhapsodeSearcherApp;
import org.rhapsode.app.contants.C;
import org.rhapsode.app.session.DynamicParameters;
import org.rhapsode.lucene.search.BaseSearchRequest;
import org.rhapsode.lucene.search.basic.BasicSearchRequest;
import org.rhapsode.lucene.search.basic.PagingDirection;
import org.rhapsode.util.ParamUtil;

public class BasicSearchRequestBuilder extends BaseRequestBuilder {

    public void extract(RhapsodeSearcherApp searcherApp, HttpServletRequest servletRequest,
                        final BasicSearchRequest request) throws ParseException, IOException {
        super.extractBase(searcherApp, servletRequest, request);
        //order matters.  be careful!
        request.setPagingDirection(getPagingDirection(request.getActionType(), servletRequest));
        request.setResultsPerPage(
                ParamUtil.getInt(servletRequest.getParameter(
                        C.RESULTS_PER_PAGE),
                        searcherApp.getSessionManager().getDynamicParameterConfig().getInt(
                                DynamicParameters.BS_RESULTS_PER_PAGE
                        ),
                        0, Integer.MAX_VALUE));
        setStartAndEndResult(servletRequest, request);
        Set<String> fields = new HashSet<>();
        fields.add(searcherApp.getRhapsodeCollection().getIndexSchema().getUniqueDocField());
        fields.add(searcherApp.getRhapsodeCollection().getIndexSchema().getAttachmentIndexField());
        fields.add(request.getContentField());
        fields.add(searcherApp.getRhapsodeCollection().getIndexSchema().getLinkDisplayField());
        fields.addAll(searcherApp.getSessionManager().getDynamicParameterConfig().getStringList(DynamicParameters.ROW_VIEWER_DISPLAY_FIELDS));

        //TODO: add in fields to display
        request.setFields(fields);

        //now actually add these to the searcherApp so that they take effect before parse!!!
        if (request.getActionType().equals(BaseSearchRequest.ActionType.ADD_SELECTED_TO_FAVORITES)) {
            searcherApp.getRhapsodeCollection().addFavorites(request.getSelectedDocIds());
        } else if (request.getActionType().equals(BaseSearchRequest.ActionType.ADD_SELECTED_TO_IGNORE)) {
            searcherApp.getRhapsodeCollection().addIgnoreds(request.getSelectedDocIds());
        }
        super.parse(searcherApp, request);
    }


    private PagingDirection getPagingDirection(BaseSearchRequest.ActionType actionType, HttpServletRequest servletRequest) {

        if (actionType.equals(BaseSearchRequest.ActionType.SEARCH)) {
            return PagingDirection.NEXT;
        } else if (actionType.equals(BaseSearchRequest.ActionType.NEXT)) {
            return PagingDirection.NEXT;
        } else if (actionType.equals(BaseSearchRequest.ActionType.PREVIOUS)) {
            return PagingDirection.PREVIOUS;
        }
        return PagingDirection.SAME;
        /*
            if (servletRequest.getParameter(C.PREVIOUS) != null &&
                servletRequest.getParameter(C.PREVIOUS).equalsIgnoreCase("previous")){
            return PagingDirection.PREVIOUS;
        } else if (servletRequest.getParameter(C.EXPORT) != null){
            return PagingDirection.SAME;
        } else if (servletRequest.getParameter(C.SELECT_ALL) != null
                && servletRequest.getParameter(C.SELECT_ALL).equalsIgnoreCase(C.SELECT_ALL)){
            return PagingDirection.SAME;
        } else if (servletRequest.getParameter(C.NEXT) != null &&
                servletRequest.getParameter(C.NEXT).equalsIgnoreCase("next")) {
            return PagingDirection.NEXT;
        }
        return PagingDirection.NEXT;*/
    }

    private void setStartAndEndResult(HttpServletRequest servletRequest, BasicSearchRequest request) {
        int lastStart = -1;
        int lastEnd = -1;
        if (servletRequest.getParameter(C.SEARCH) == null &&
                servletRequest.getParameter(C.STORED_QUERY_ID) == null) {
            lastStart = ParamUtil.getInt(servletRequest.getParameter(C.LAST_START), -1);
            lastEnd = ParamUtil.getInt(servletRequest.getParameter(C.LAST_END), 0);
        }
        int start = -1;
        int end = -1;
        if (request.getPagingDirection() == PagingDirection.NEXT) {
            start = lastEnd + 1;
        } else if (request.getPagingDirection() == PagingDirection.PREVIOUS) {
            start = lastStart - request.getResultsPerPage();
        } else if (request.getPagingDirection() == PagingDirection.SAME) {
            start = lastStart;
        }
        start = (start > -1) ? start : 0;
        //TODO: need to adjust start and end at search time!
        // start = (start > scoreDocs.length) ? 0 : start;

        end = start + request.getResultsPerPage() - 1;
        request.setStartAndEndResult(start, end);
    }
}

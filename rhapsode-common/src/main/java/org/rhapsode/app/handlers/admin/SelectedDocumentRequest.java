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

import org.rhapsode.app.RhapsodeSearcherApp;
import org.rhapsode.app.contants.C;

import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.Set;

public class SelectedDocumentRequest {


    public enum ACTION_TYPE {
        VIEW_FAVORITES,
        CLEAR_SELECTED_FAVORITES,
        SELECT_ALL_FAVORITES,
        VIEW_IGNORED,
        CLEAR_SELECTED_IGNORED,
        SELECT_ALL_IGNORED,
        //write favorites
        //write ignored
    }


    public static SelectedDocumentRequest build(RhapsodeSearcherApp searcherApp, HttpServletRequest
            httpServletRequest) {
        SelectedDocumentRequest sdr = new SelectedDocumentRequest();
        if (httpServletRequest.getParameter(C.SELECTED_VIEW_FAVORITES) != null) {
            sdr.actionType = ACTION_TYPE.VIEW_FAVORITES;
        } else if (httpServletRequest.getParameter(C.SELECTED_VIEW_IGNORED) != null) {
            sdr.actionType = ACTION_TYPE.VIEW_IGNORED;
        } else if (httpServletRequest.getParameter(C.SELECT_ALL_IGNORED) != null) {
            sdr.actionType = ACTION_TYPE.SELECT_ALL_IGNORED;
        } else if (httpServletRequest.getParameter(C.SELECT_ALL_FAVORITES) != null) {
            sdr.actionType = ACTION_TYPE.SELECT_ALL_FAVORITES;
        } else if (httpServletRequest.getParameter(C.CLEAR_SELECTED_IGNORED) != null) {
            sdr.actionType = ACTION_TYPE.CLEAR_SELECTED_IGNORED;
        } else if (httpServletRequest.getParameter(C.CLEAR_SELECTED_FAVORITES) != null) {
            sdr.actionType = ACTION_TYPE.CLEAR_SELECTED_FAVORITES;
        } else {
            //default
            sdr.actionType = ACTION_TYPE.VIEW_FAVORITES;
        }

        String[] docIds = httpServletRequest.getParameterValues(C.SELECTED_DOC_IDS);
        Set<String> idSet = new HashSet<>();
        if (docIds != null) {
            for (String id : docIds) {
                idSet.add(id);
            }
        }
        sdr.docIds = idSet;

        return sdr;
    }

    ACTION_TYPE actionType;

    Set<String> docIds;

    public ACTION_TYPE getActionType() {
        return actionType;
    }

    public Set<String> getDocIds() {
        return docIds;
    }

}

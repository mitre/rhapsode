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

import javax.servlet.http.HttpServletRequest;

import java.util.LinkedHashSet;
import java.util.Set;

import org.rhapsode.app.contants.C;
import org.rhapsode.app.tagger.TaggerRequest;
import org.rhapsode.lucene.search.StoredQuery;
import org.rhapsode.util.ParamUtil;

public class ReportRequest {


    private String reportName;
    private int absMaxHitsPerQuery = StoredQuery.RETRIEVE_ALL_HITS;
    private int topNCombinedReportResults = -1;
    private Set<Integer> storedQueryIds = new LinkedHashSet<>();
    private TaggerRequest.NORM_TYPE normType;
    private REPORT_TYPE reportType;
    private ACTION_TYPE actionType;
    private boolean includeFavorites;

    public static ReportRequest build(HttpServletRequest request) {
        ReportRequest r = new ReportRequest();
        r.reportName = request.getParameter(C.REPORT_NAME);
        r.absMaxHitsPerQuery = ParamUtil.getInt(
                request.getParameter(C.ABS_MAX_REPORT_RESULTS),
                -1,
                -1, Integer.MAX_VALUE);
        r.topNCombinedReportResults = ParamUtil.getInt(
                request.getParameter(C.TOP_N_COMBINED_REPORT_RESULTS),
                -1,
                -1, Integer.MAX_VALUE
        );

        String[] storedQueryIds = request.getParameterValues(C.QUERIES_FOR_REPORT);
        if (storedQueryIds != null) {
            for (String idString : storedQueryIds) {
                r.storedQueryIds.add(Integer.parseInt(idString));
            }
        }

        String normType = request.getParameter(C.REPORT_NORM_TYPE);

        if (normType == null) {
            r.normType = TaggerRequest.NORM_TYPE.ONE;
        } else {
            r.normType = TaggerRequest.NORM_TYPE.valueOf(normType);
        }

        if (request.getParameter(C.WRITE_REPORT) != null) {
            r.actionType = ACTION_TYPE.WRITE_REPORT;
        } else if (request.getParameter(C.SELECT_ALL) != null) {
            r.actionType = ACTION_TYPE.SELECT_ALL;
        } else if (request.getParameter(C.DESELECT_ALL) != null) {
            r.actionType = ACTION_TYPE.DESELECT_ALL;
        } else {
            r.actionType = ACTION_TYPE.REPORT_DIALOGUE;
        }

        String reportTypeString = request.getParameter(C.REPORT_TYPE);
        if (reportTypeString == null) {
            r.reportType = REPORT_TYPE.NO_LINKS;
        } else if (reportTypeString.equals(C.REPORT_TYPE_LIVE)) {
            r.reportType = REPORT_TYPE.LIVE_LINKS;
        } else if (reportTypeString.equals(C.REPORT_TYPE_STATIC)) {
            r.reportType = REPORT_TYPE.STATIC_LINKS;
        } else {
            r.reportType = REPORT_TYPE.NO_LINKS;
        }

        if (ParamUtil.getBooleanChecked(request.getParameter(C.REPORT_INCLUDE_FAVORITES), true)) {
            r.includeFavorites = true;
        }
        return r;
    }

    public ACTION_TYPE getActionType() {
        return actionType;
    }

    public String getReportName() {
        return reportName;
    }

    public int getAbsMaxHitsPerQuery() {
        return absMaxHitsPerQuery;
    }

    public int getTopNCombinedReportResults() {
        return topNCombinedReportResults;
    }

    public Set<Integer> getStoredQueryIds() {
        return storedQueryIds;
    }

    public TaggerRequest.NORM_TYPE getNormType() {
        return normType;
    }

    public void selectQuery(Integer id) {
        storedQueryIds.add(id);
    }

    public void deselectAll() {
        storedQueryIds.clear();
    }

    public REPORT_TYPE getReportType() {
        return reportType;
    }

    public boolean getIncludeFavorites() {
        return includeFavorites;
    }

    enum ACTION_TYPE {
        SELECT_ALL,
        DESELECT_ALL,
        WRITE_REPORT,
        REPORT_DIALOGUE
    }

    public enum REPORT_TYPE {
        STATIC_LINKS,
        LIVE_LINKS,
        NO_LINKS
    }

}

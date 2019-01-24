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
import org.rhapsode.app.session.DynamicParameterConfig;
import org.rhapsode.app.session.DynamicParameters;
import org.rhapsode.lucene.search.cooccur.CooccurRequest;
import org.rhapsode.util.ParamUtil;

public class CooccurRequestBuilder extends ConcSearchRequestBuilderBase {

    public void extract(RhapsodeSearcherApp searcherApp, HttpServletRequest servletRequest,
                        final CooccurRequest request) throws ParseException, IOException {
        super.extractConcBase(searcherApp, servletRequest, request);
        DynamicParameterConfig params = searcherApp.getSessionManager().getDynamicParameterConfig();

        request.setMinXGram(
                ParamUtil.getInt(servletRequest.getParameter(
                        C.MIN_NGRAM),
                        params.getInt(DynamicParameters.COOCCUR_MIN_NGRAM),
                        0,
                        Integer.MAX_VALUE));
        request.setMaxXGram(
                ParamUtil.getInt(servletRequest.getParameter(
                        C.MAX_NGRAM),
                        params.getInt(DynamicParameters.COOCCUR_MAX_NGRAM),
                        request.getMinXGram(),
                        Integer.MAX_VALUE));
        request.setMinTermFreq(
                ParamUtil.getInt(servletRequest.getParameter(
                        C.MIN_TERM_FREQ),
                        params.getInt(DynamicParameters.COOCCUR_MIN_TERM_FREQ),
                        0,
                        Integer.MAX_VALUE));
        request.setMaxNumResults(
                ParamUtil.getInt(servletRequest.getParameter(
                        C.NUM_RESULTS),
                        params.getInt(DynamicParameters.COOCCUR_MAX_RESULTS),
                        0,
                        Integer.MAX_VALUE));
        Set<String> fields = new HashSet<>();
        fields.add(request.getContentField());

        request.setFields(fields);
        super.parse(searcherApp, request);

    }

}

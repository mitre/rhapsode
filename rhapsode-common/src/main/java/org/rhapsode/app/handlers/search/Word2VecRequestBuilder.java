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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.queryparser.classic.ParseException;
import org.rhapsode.app.RhapsodeSearcherApp;
import org.rhapsode.app.contants.C;
import org.rhapsode.util.ParamUtil;

public class Word2VecRequestBuilder extends BaseRequestBuilder {


    public static Word2VecRequest build(RhapsodeSearcherApp app, HttpServletRequest servletRequest) throws IOException, ParseException {
        if (servletRequest.getParameter(C.SEARCH) != null) {
            //request.setActionType(BaseSearchRequest.ActionType.SEARCH);
        }
        String w2vPositive = servletRequest.getParameter(C.W2V_POSITIVE);
        String w2vNegative = servletRequest.getParameter(C.W2V_NEGATIVE);

        List<String> positives = getTerms(w2vPositive, app);
        List<String> negatives = getTerms(w2vNegative, app);
        Word2VecRequest request = new Word2VecRequest(positives, negatives);

        request.setNumResults(ParamUtil.getInt(servletRequest.getParameter(C.NUM_RESULTS), 10));
        return request;
    }

    private static List<String> getTerms(String s, RhapsodeSearcherApp app) throws IOException, ParseException {
        if (s == null || s.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> ret = new ArrayList<>();

        for (String raw : s.split("[, ]+")) {
            String normalized = getTerm(raw, app);
            if (normalized.length() > 0) {
                ret.add(normalized);
            }
        }
        return ret;
    }

    private static String getTerm(String s, RhapsodeSearcherApp app) throws IOException, ParseException {
        Analyzer analyzer = app.getRhapsodeCollection().getIndexSchema().getIndexAnalyzer();
        TokenStream ts = analyzer.tokenStream(app.getRhapsodeCollection().getIndexSchema().getDefaultContentField(), s);
        CharTermAttribute charTermAttribute = ts.getAttribute(CharTermAttribute.class);
        ts.reset();
        int i = 0;
        String t = "";
        while (ts.incrementToken()) {
            t = charTermAttribute.toString();
            i++;
        }
        ts.close();
        ts.end();
        if (i == 0) {
            throw new ParseException("Couldn't get a term in: " + s);
        } else if (i > 1) {
            throw new ParseException("Single term analyzed into several: " + s);
        }
        return t;
    }
}

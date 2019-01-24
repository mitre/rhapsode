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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.rhapsode.app.RhapsodeSearcherApp;
import org.rhapsode.app.contants.C;
import org.rhapsode.app.session.BooleanDynamicParameter;
import org.rhapsode.app.session.DynamicParameter;
import org.rhapsode.app.session.LangDirDynamicParameter;
import org.rhapsode.app.session.StringListDynamicParameter;
import org.rhapsode.util.LanguageDirection;
import org.rhapsode.util.MapUtil;
import org.rhapsode.util.ParamUtil;

public class SettingsRequest {


    static final String SETTINGS_REQUEST_NS = "sr:";
    static final String LIST_ITEM = SETTINGS_REQUEST_NS + "li:";
    ACTION_TYPE actionType;
    private Map<DynamicParameter, String> settings;

    public static SettingsRequest build(RhapsodeSearcherApp searcherApp, HttpServletRequest
            httpServletRequest) {
        SettingsRequest sr = new SettingsRequest();
        if (httpServletRequest.getParameter(C.SETTINGS_REQUEST_UPDATE) != null) {
            sr.actionType = ACTION_TYPE.UPDATE;
        } else {
            sr.actionType = ACTION_TYPE.VIEW;
        }

        sr.settings = new HashMap<>();
        for (DynamicParameter dp : searcherApp.getSessionManager().getDynamicParameterConfig().getParamCopy().keySet()) {
            String requestValString = httpServletRequest.getParameter(toHTMLTag(dp.getFullName()));
            if (dp instanceof BooleanDynamicParameter) {
                if (requestValString == null) {
                    continue;
                }
                Boolean val = ParamUtil.getBooleanChecked(requestValString,
                        ((BooleanDynamicParameter) dp).getDefaultValue());
                sr.settings.put(dp, val.toString());
            } else if (dp instanceof LangDirDynamicParameter) {
                if (requestValString == null) {
                    continue;
                }
                Boolean val = ParamUtil.getBooleanChecked(requestValString, true);
                if (val) {
                    sr.settings.put(dp, LanguageDirection.LTR.name().toLowerCase(Locale.ENGLISH));
                } else {
                    sr.settings.put(dp, LanguageDirection.RTL.name().toLowerCase(Locale.ENGLISH));
                }
            } else {
                if (requestValString == null) {
                    requestValString = "";
                }
                sr.settings.put(dp, requestValString);
            }
        }

        Enumeration<String> pEnum = httpServletRequest.getParameterNames();
        Map<StringListDynamicParameter, Map<String, Integer>> params = new HashMap<>();

        while (pEnum.hasMoreElements()) {
            String k = pEnum.nextElement();
            if (k.startsWith(LIST_ITEM)) {
                StringListDynamicParameter dp = getStringListItemHTMLTag(k,
                        searcherApp.getSessionManager().getDynamicParameterConfig().getParamCopy().keySet());
                String fieldName = getFieldNameFromListItemHTMLTag(k);
                int priority = -1;
                String val = httpServletRequest.getParameter(k);
                if (!StringUtils.isBlank(val)) {
                    try {
                        priority = Integer.parseInt(val);
                    } catch (NumberFormatException e) {
                        //
                    }
                }
                if (priority > -1) {
                    Map<String, Integer> specificDPParams = params.get(dp);
                    if (specificDPParams == null) {
                        specificDPParams = new HashMap<>();
                    }
                    specificDPParams.put(fieldName, priority);
                    params.put(dp, specificDPParams);
                }
            }
        }

        for (Map.Entry<StringListDynamicParameter, Map<String, Integer>> e : params.entrySet()) {
            DynamicParameter dp = e.getKey();
            String jsonList = jsonify(e.getValue());
            sr.settings.put(dp, jsonList);
        }
        return sr;
    }

    private static String jsonify(Map<String, Integer> map) {
        Map<String, Integer> sorted = MapUtil.sortByValue(map);
        List<String> strings = new LinkedList<>();
        for (String fieldName : sorted.keySet()) {
            strings.add(fieldName);
        }
        return StringListDynamicParameter.valueToString(strings);
    }

    static String toHTMLTag(String fullName) {
        return SETTINGS_REQUEST_NS + fullName;
    }

    public static String toListItemHTMLTag(String fullName, String fieldName) {
        return LIST_ITEM + fullName + ":" + fieldName;
    }

    public static StringListDynamicParameter getStringListItemHTMLTag(String s,
                                                                      Set<DynamicParameter> dynamicParameters) {
        if (s == null) {
            return null;
        }
        String ret = s.substring(LIST_ITEM.length());
        int i = ret.lastIndexOf(":");
        if (i > -1) {
            String dpName = ret.substring(0, i);
            for (DynamicParameter dp : dynamicParameters) {
                if (dp instanceof StringListDynamicParameter && dp.getFullName().equals(dpName)) {
                    return (StringListDynamicParameter) dp;
                }
            }
        }
        return null;
    }

    public static String getFieldNameFromListItemHTMLTag(String s) {
        if (s == null) {
            return "";
        }
        int i = s.lastIndexOf(":");
        if (i > -1 && i + 1 < s.length()) {
            return s.substring(i + 1);
        }
        return "";
    }

    static String fromHTMLTag(String tag) {
        return tag.substring(SETTINGS_REQUEST_NS.length());
    }

    public ACTION_TYPE getActionType() {
        return actionType;
    }

    public Map<DynamicParameter, String> getSettings() {
        return settings;
    }

    public enum ACTION_TYPE {
        UPDATE,
        VIEW
    }

}

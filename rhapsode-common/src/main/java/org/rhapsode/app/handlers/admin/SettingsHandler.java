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
import org.apache.lucene.search.BooleanQuery;
import org.eclipse.jetty.server.Request;
import org.rhapsode.app.RhapsodeSearcherApp;
import org.rhapsode.app.contants.C;
import org.rhapsode.app.contants.H;
import org.rhapsode.app.decorators.RhapsodeDecorator;
import org.rhapsode.app.decorators.RhapsodeXHTMLHandler;
import org.rhapsode.app.session.BooleanDynamicParameter;
import org.rhapsode.app.session.DynamicParameter;
import org.rhapsode.app.session.DynamicParameterConfig;
import org.rhapsode.app.session.DynamicParameters;
import org.rhapsode.app.session.IntDynamicParameter;
import org.rhapsode.app.session.LangDirDynamicParameter;
import org.rhapsode.app.session.StringDynamicParameter;
import org.rhapsode.app.session.StringListDynamicParameter;
import org.rhapsode.util.LanguageDirection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SettingsHandler extends AdminHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SettingsHandler.class);

    private final RhapsodeSearcherApp searcherApp;

    public SettingsHandler(RhapsodeSearcherApp searcherApp) {
        super("Interface Settings");
        this.searcherApp = searcherApp;
    }

    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest,
                       HttpServletResponse response) throws IOException, ServletException {

        RhapsodeXHTMLHandler xhtml = null;

        SettingsRequest sr = SettingsRequest.build(searcherApp, httpServletRequest);
        String errorMessage = null;
        try {
            xhtml = initResponse(response, null);
            if (!searcherApp.hasCollection()) {
                try {
                    RhapsodeDecorator.writeNoCollection(xhtml);
                    response.getOutputStream().flush();
                } catch (SAXException e) {
                    LOG.error("problem no collection", e);
                }
                return;
            }

            xhtml.element(H.P, "Make sure to click the Update button after making changes!");
            xhtml.startElement(H.FORM, H.METHOD, H.POST);
            switch (sr.getActionType()) {
                case UPDATE:
                    errorMessage = update(sr);
                default:
                    writeSettingsForm(xhtml);

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

    private void writeSettingsForm(RhapsodeXHTMLHandler xhtml) throws SAXException {
        DynamicParameter.PREFIX prevNS = null;
        DynamicParameterConfig dpc = searcherApp.getSessionManager().getDynamicParameterConfig();
        for (Map.Entry<String, DynamicParameter> e : DynamicParameters.getParams().entrySet()) {
            DynamicParameter dp = e.getValue();

            if (prevNS == null || !prevNS.equals(dp.getPrefix())) {
                if (prevNS != null) {
                    xhtml.endElement(H.TABLE);
                }
                xhtml.element(H.H2, dp.getPrefix().getDisplayName());
                xhtml.startElement(H.TABLE);
            }
            xhtml.startElement(H.TR);
            if (dp instanceof IntDynamicParameter) {
                int val = dpc.getInt((IntDynamicParameter) dp);
                writeInt((IntDynamicParameter) dp, val, xhtml);
            } else if (dp instanceof BooleanDynamicParameter) {
                boolean val = dpc.getBoolean((BooleanDynamicParameter) dp);
                writeBoolean((BooleanDynamicParameter) dp, val, xhtml);
            } else if (dp instanceof LangDirDynamicParameter) {
                LanguageDirection val = dpc.getLanguageDirection((LangDirDynamicParameter) dp);
                writeLangDir((LangDirDynamicParameter) dp, val, xhtml);
            } else if (dp instanceof StringDynamicParameter) {
                String val = dpc.getString((StringDynamicParameter) dp);
                writeString((StringDynamicParameter) dp, val, xhtml);
            } else if (dp instanceof StringListDynamicParameter) {
                List<String> fields = dpc.getStringList((StringListDynamicParameter) dp);
                writeFieldPriority((StringListDynamicParameter) dp, fields, xhtml);
            } else {
                throw new IllegalArgumentException("Can't recognize " + dp.getClass());
            }
            xhtml.endElement(H.TR);
            prevNS = dp.getPrefix();
        }
        xhtml.endElement(H.TABLE);

        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.SETTINGS_REQUEST_UPDATE,
                H.VALUE, "Update",
                "default", "default");
        xhtml.endElement(H.INPUT);
    }

    private void writeFieldPriority(StringListDynamicParameter dp,
                                    List<String> fields,
                                    RhapsodeXHTMLHandler xhtml) throws SAXException {
        Set<String> selected = new HashSet<>();
        selected.addAll(fields);
        Set<String> definedFields = searcherApp.getRhapsodeCollection().getIndexSchema().getDefinedFields();
        int i = 1;
        xhtml.element(H.TD, dp.getDisplayName() + ":");
        for (String fieldName : fields) {
            xhtml.startElement(H.TR);
            xhtml.element(H.TD, " ");
            AttributesImpl attrs = new AttributesImpl();
            attrs.addAttribute("", H.TYPE, H.TYPE, "", H.TEXT);
            attrs.addAttribute("", H.NAME, H.NAME, "",
                    SettingsRequest.toListItemHTMLTag(dp.getFullName(), fieldName));
            attrs.addAttribute("", H.VALUE, H.VALUE, "", Integer.toString(i++));
            attrs.addAttribute("", H.SIZE, H.SIZE, "", Integer.toString(2));
            xhtml.element(H.TD, fieldName);
            xhtml.startElement(H.TD);
            xhtml.startElement(H.INPUT, attrs);

            xhtml.endElement(H.INPUT);
            xhtml.endElement(H.TD);
            xhtml.endElement(H.TR);
        }
        List<String> sorted = new ArrayList<>();
        sorted.addAll(definedFields);
        Collections.sort(sorted);
        for (String fieldName : sorted) {
            if (selected.contains(fieldName)) {
                continue;
            } else if (fieldName.startsWith("_")) {
                continue;
            }
            xhtml.startElement(H.TR);
            xhtml.element(H.TD, " ");

            AttributesImpl attrs = new AttributesImpl();
            attrs.addAttribute("", H.TYPE, H.TYPE, "", H.TEXT);
            attrs.addAttribute("", H.NAME, H.NAME, "",
                    SettingsRequest.toListItemHTMLTag(dp.getFullName(), fieldName));
            attrs.addAttribute("", H.SIZE, H.SIZE, "", Integer.toString(2));

            xhtml.element(H.TD, fieldName);
            xhtml.startElement(H.TD);
            xhtml.startElement(H.INPUT, attrs);

            xhtml.endElement(H.INPUT);
            xhtml.endElement(H.TD);
            xhtml.endElement(H.TR);
        }
    }

    private void writeLangDir(LangDirDynamicParameter dp, LanguageDirection value,
                              RhapsodeXHTMLHandler xhtml) throws SAXException {
        xhtml.element(H.TD, dp.getDisplayName() + ": ");
        xhtml.startElement(H.TD);
        if (value.equals(LanguageDirection.LTR)) {
            xhtml.characters("Left to Right: ");
            xhtml.startElement(H.INPUT,
                    H.TYPE, H.RADIO,
                    H.NAME, SettingsRequest.toHTMLTag(dp.getFullName()),
                    H.VALUE, C.TRUE,
                    H.CHECKED, H.CHECKED);
            xhtml.endElement(H.INPUT);
            xhtml.characters("Right to Left: ");
            xhtml.startElement(H.INPUT,
                    H.TYPE, H.RADIO,
                    H.NAME, SettingsRequest.toHTMLTag(dp.getFullName()),
                    H.VALUE, C.FALSE);
            xhtml.endElement(H.INPUT);

        } else {
            xhtml.characters("Left to Right: ");
            xhtml.startElement(H.INPUT,
                    H.TYPE, H.RADIO,
                    H.NAME, SettingsRequest.toHTMLTag(dp.getFullName()),
                    H.VALUE, C.TRUE);
            xhtml.endElement(H.INPUT);
            xhtml.characters("Right to Left: ");
            xhtml.startElement(H.INPUT,
                    H.TYPE, H.RADIO,
                    H.NAME, SettingsRequest.toHTMLTag(dp.getFullName()),
                    H.VALUE, C.FALSE,
                    H.CHECKED, H.CHECKED);
            xhtml.endElement(H.INPUT);
        }

        xhtml.endElement(H.TD);

    }

    private void writeString(DynamicParameter dp, String value, RhapsodeXHTMLHandler xhtml) throws SAXException {
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", H.TYPE, H.TYPE, "", H.TEXT);
        attrs.addAttribute("", H.NAME, H.NAME, "", SettingsRequest.toHTMLTag(dp.getFullName()));
        if (!StringUtils.isBlank(value)) {
            attrs.addAttribute("", H.VALUE, H.VALUE, "", value);
        }
        attrs.addAttribute("", H.SIZE, H.SIZE, "", Integer.toString(30));
        xhtml.element(H.TD, dp.getDisplayName());
        xhtml.startElement(H.TD);
        xhtml.startElement(H.INPUT, attrs);

        xhtml.endElement(H.INPUT);
        xhtml.endElement(H.TD);

    }

    private void writeBoolean(BooleanDynamicParameter dp, boolean value, RhapsodeXHTMLHandler xhtml) throws SAXException {
        xhtml.element(H.TD, dp.getDisplayName() + ": ");
        xhtml.startElement(H.TD);
        if (value == true) {
            xhtml.characters("true: ");
            xhtml.startElement(H.INPUT,
                    H.TYPE, H.RADIO,
                    H.NAME, SettingsRequest.toHTMLTag(dp.getFullName()),
                    H.VALUE, C.TRUE,
                    H.CHECKED, H.CHECKED);
            xhtml.endElement(H.INPUT);
            xhtml.characters("false: ");
            xhtml.startElement(H.INPUT,
                    H.TYPE, H.RADIO,
                    H.NAME, SettingsRequest.toHTMLTag(dp.getFullName()),
                    H.VALUE, C.FALSE);
            xhtml.endElement(H.INPUT);

        } else {
            xhtml.characters("true: ");
            xhtml.startElement(H.INPUT,
                    H.TYPE, H.RADIO,
                    H.NAME, SettingsRequest.toHTMLTag(dp.getFullName()),
                    H.VALUE, C.TRUE);
            xhtml.endElement(H.INPUT);
            xhtml.characters("false: ");
            xhtml.startElement(H.INPUT,
                    H.TYPE, H.RADIO,
                    H.NAME, SettingsRequest.toHTMLTag(dp.getFullName()),
                    H.VALUE, C.FALSE,
                    H.CHECKED, H.CHECKED);
            xhtml.endElement(H.INPUT);
        }

        xhtml.endElement(H.TD);

    }

    private void writeInt(IntDynamicParameter dp, int value, RhapsodeXHTMLHandler xhtml) throws SAXException {

        xhtml.element(H.TD, dp.getDisplayName() + ": ");
        xhtml.startElement(H.TD);
        xhtml.startElement(H.INPUT,
                H.TYPE, H.TEXT,
                H.NAME, SettingsRequest.toHTMLTag(dp.getFullName()),
                H.SIZE, "2",
                H.VALUE, Integer.toString(value));
        xhtml.endElement(H.INPUT);
        xhtml.endElement(H.TD);
    }

    private String update(SettingsRequest sr) {
        DynamicParameterConfig config = searcherApp.getSessionManager().getDynamicParameterConfig();
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<DynamicParameter, String> entry : sr.getSettings().entrySet()) {
            try {
                if (entry.getKey().equals(DynamicParameters.MAX_BOOLEAN_CLAUSES)) {
                    tryToUpdateMaxBooleanClauses(entry.getValue(), config.getInt(DynamicParameters.MAX_BOOLEAN_CLAUSES));
                }
                config.update(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                LOG.warn("problem updating", e);
                sb.append("Couldn't update: ").append(entry.getKey().getDisplayName()).append("\n\n");
            }
        }

        try {
            config.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return sb.toString();
    }

    private void tryToUpdateMaxBooleanClauses(String value, int currInt) {
        if (value == null) {
            return;
        }
        try {
            int newVal = Integer.parseInt(value);
            if (newVal != currInt) {
                BooleanQuery.setMaxClauseCount(newVal);
            }
        } catch (NumberFormatException e) {
            //swallow
            LOG.warn("max boolean clauses", e);
        }
    }


}

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

package org.rhapsode.app.decorators;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.tika.sax.XHTMLContentHandler;
import org.rhapsode.app.contants.C;
import org.rhapsode.app.contants.CSS;
import org.rhapsode.app.contants.H;
import org.rhapsode.app.contants.Version;
import org.rhapsode.lucene.schema.FieldDef;
import org.rhapsode.lucene.schema.IndexSchema;
import org.rhapsode.lucene.search.BaseSearchRequest;
import org.rhapsode.lucene.search.basic.BasicSearchRequest;
import org.rhapsode.util.LanguageDirection;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class RhapsodeDecorator {
    public static final String[] RTL_ATTRS = new String[]{
            "dir",
            "RTL"
    };


    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    public static String decorateParseException(ParseException e) {
        return "Parse exception. I'm sorry, but I couldn't parse your query.<br/>" + e.getMessage() + "<br/>";
    }

    /*
        public static String decorateDate(RhapsodeImmutableConfig searcherApp, String yyyyMMDD){
            if (yyyyMMDD == null || ! searcherApp.isShowDate() || searcherApp.getMissingDateValue().equals(yyyyMMDD)){
                return S.EMPTY_STRING;
            } else if (yyyyMMDD.length() < 6){
                return S.EMPTY_STRING;
            } else if (yyyyMMDD.length() == 6){
                return yyyyMMDD.substring(4,6) + "/" + yyyyMMDD.substring(0,4);
            } else if (yyyyMMDD.length() >= 8){
                return yyyyMMDD.substring(4,6) + "/" +
                        yyyyMMDD.substring(6,8)+"/"+yyyyMMDD.substring(0,4);
            }
            return S.EMPTY_STRING;
        }
    */
    public static void writeLanguageDirectionString(BasicSearchRequest r, XHTMLContentHandler xhtml) throws SAXException {

/*        if (! searcherApp.isShowLanguageDirection()){
            return "";
        }*/
        Map<String, String> m = new LinkedHashMap<String, String>();
        m.put(H.LTR, "Left to Right");
        m.put(H.RTL, "Right to Left");
        xhtml.characters("Language Direction, ");
        for (Map.Entry<String, String> entry : m.entrySet()) {
            xhtml.characters(entry.getValue() + ": ");
            AttributesImpl attrs = new AttributesImpl();
            attrs.addAttribute("", H.TYPE, H.TYPE, "", "radio");
            attrs.addAttribute("", H.NAME, H.NAME, "", C.LANG_DIR);
            attrs.addAttribute("", H.VALUE, H.VALUE, "", entry.getKey());
            if (r.getLanguageDirection() == LanguageDirection.LTR && entry.getKey().equals("ltr")) {
                attrs.addAttribute("", H.CHECKED, H.CHECKED, "", H.CHECKED);
            } else if (r.getLanguageDirection() == LanguageDirection.RTL && entry.getKey().equals("rtl")) {
                attrs.addAttribute("", H.CHECKED, H.CHECKED, "", H.CHECKED);
            }
            xhtml.startElement(H.INPUT, attrs);
            xhtml.endElement(H.INPUT);

        }
        xhtml.startElement(H.BR);
        xhtml.endElement(H.BR);
    }

    public static void writeQueryBox(String label, String name, String defaultString, int rows,
                                     int cols, LanguageDirection direction, RhapsodeXHTMLHandler xhtml) throws SAXException {

        xhtml.characters(label + ": ");
        if (rows == 1) {
            AttributesImpl attrs = new AttributesImpl();
            attrs.addAttribute("", H.TYPE, H.TYPE, "", H.TEXT);
            attrs.addAttribute("", H.NAME, H.NAME, "", name);
            attrs.addAttribute("", H.DIRECTION, H.DIRECTION, "", direction.name().toLowerCase());
            if (!StringUtils.isBlank(defaultString)) {
                attrs.addAttribute("", H.VALUE, H.VALUE, "", defaultString);
            }
            attrs.addAttribute("", H.SIZE, H.SIZE, "", Integer.toString(cols));
            xhtml.startElement(H.INPUT, attrs);
            xhtml.endElement(H.INPUT);
        } else {
            xhtml.startElement(H.TEXT_AREA,
                    H.NAME, name,
                    H.DIRECTION, direction.name().toLowerCase(),
                    H.ROWS, Integer.toString(rows),
                    H.COLS, Integer.toString(cols));
            xhtml.characters(defaultString);
            xhtml.endElement(H.TEXT_AREA);
        }
    }

    public static void writeFieldSelector(IndexSchema schema, String currDefaultField,
                                          RhapsodeXHTMLHandler xhtml) throws SAXException {
        xhtml.startElement(H.SELECT,
                H.NAME, C.DEFAULT_QUERY_FIELD);

        List<String> fields = new ArrayList<>();
        fields.add(currDefaultField);
        List<String> rest = new ArrayList<>();
        for (String other : schema.getDefinedFields()) {
            FieldDef def = schema.getFieldDef(other);
            if (def == null) {
                //warn?
                continue;
            }
            FieldType t = def.getFieldType();
            if (t.tokenized() && !other.equals(currDefaultField)) {
                rest.add(other);
            }
        }
        Collections.sort(rest);
        fields.addAll(rest);

        for (String field : fields) {
            xhtml.startElement(H.OPTION,
                    H.VALUE, field);
            xhtml.characters(field);
            xhtml.endElement(H.OPTION);
        }
        xhtml.endElement(H.SELECT);
    }

    public static void writeErrorMessage(String errorMessage, RhapsodeXHTMLHandler xhtml) throws SAXException {
        if (errorMessage == null) {
            errorMessage = "Null error message ?!";
        }

        String[] lines = errorMessage.split("[\\r\\n]+");
        xhtml.br();
        for (String line : lines) {
            xhtml.startElement(H.SPAN, H.CLASS, CSS.ERROR_MSG);
            xhtml.characters(line);
            xhtml.endElement(H.SPAN);
            xhtml.br();
        }
    }

    public static void writeWarnMessage(String warnMessage, RhapsodeXHTMLHandler xhtml) throws SAXException {
        xhtml.br();
        xhtml.startElement(H.SPAN,
                H.CLASS, CSS.WARN_MSG);
        xhtml.characters(warnMessage);
        xhtml.endElement(H.SPAN);
        xhtml.br();

    }




    public static String[] getLangDirAttrs(BaseSearchRequest request) {
        return getLangDirAttrs(request.getLanguageDirection());
    }

    public static String[] getLangDirAttrs(LanguageDirection languageDirection) {
        if (languageDirection == LanguageDirection.RTL) {
            return RTL_ATTRS;
        }
        return EMPTY_STRING_ARRAY;
    }
    public static void writeNoCollection(RhapsodeXHTMLHandler xhtml)
            throws SAXException {
        xhtml.characters("No collection is loaded.");
        xhtml.characters("Please load a collection ");
        xhtml.startElement(H.A,
                H.HREF, "/rhapsode/admin/collection/");
        xhtml.characters("here");
        xhtml.endElement(H.A);
        addFooter(xhtml);
        xhtml.endDocument();

    }

    public static void addFooter(RhapsodeXHTMLHandler xhtml) throws SAXException {
        xhtml.startElement(H.P,
                H.CLASS, CSS.VERSION);
        xhtml.characters(Version.VERSION);
        xhtml.endElement(H.P);

    }

    public static void writeLanguageDirection(boolean showLanguageDirection,
                                              LanguageDirection languageDirection,
                                              RhapsodeXHTMLHandler xhtml) throws SAXException {
        if (!showLanguageDirection) {
            return;
        }
        Map<String, String> m = new LinkedHashMap<>();
        m.put(H.LTR, "Left to Right");
        m.put(H.RTL, "Right to Left");
        xhtml.characters("Language Direction, ");
        for (Map.Entry<String, String> entry : m.entrySet()) {
            xhtml.characters(entry.getValue() + ": ");
            if (languageDirection.equals(LanguageDirection.LTR)) {
                if (entry.getKey().equals(H.LTR)) {
                    xhtml.startElement(H.INPUT,
                            H.TYPE, H.RADIO,
                            H.NAME, C.LANG_DIR,
                            H.VALUE, entry.getKey(),
                            H.CHECKED, H.CHECKED);
                } else if (entry.getKey().equals(H.RTL)) {
                    xhtml.startElement(H.INPUT,
                            H.TYPE, H.RADIO,
                            H.NAME, C.LANG_DIR,
                            H.VALUE, entry.getKey());
                }
            } else if (languageDirection.equals(LanguageDirection.RTL)) {
                if (entry.getKey().equals(H.RTL)) {
                    xhtml.startElement(H.INPUT,
                            H.TYPE, H.RADIO,
                            H.NAME, C.LANG_DIR,
                            H.VALUE, entry.getKey(),
                            H.CHECKED, H.CHECKED);
                } else if (entry.getKey().equals(H.LTR)) {
                    xhtml.startElement(H.INPUT,
                            H.TYPE, H.RADIO,
                            H.NAME, C.LANG_DIR,
                            H.VALUE, entry.getKey());
                }
            }
            xhtml.endElement(H.INPUT);
        }
    }

    public static void writeElapsed(long elapsed, RhapsodeXHTMLHandler xhtml) throws SAXException {
        if (elapsed < 0) {
            return;
        }
        DecimalFormat commaFormatter = new DecimalFormat("###,###,###,###,###");
        xhtml.br();
        xhtml.characters("That search took " + commaFormatter.format(elapsed) + " milliseconds.");
        xhtml.br();


    }

    public static String generateStyleString(Map<String, String> styles) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : styles.entrySet()) {
            sb.append("span.").append(e.getKey()).append("{");
            sb.append(e.getValue()).append("}").append("\n");
        }
        sb.append("table.h, td.h \n").append("{border:1px solid black;}\n");
        return sb.toString();
    }

    public static void writeSelectedDocIdTD(boolean showSelected,
                                            BaseSearchRequest request, String docKey, RhapsodeXHTMLHandler xhtml) throws SAXException {

        if (showSelected) {
            xhtml.startElement(H.TD);
            if (request.isSelectAll() || request.getSelectedDocIds().contains(docKey)) {
                xhtml.startElement(H.INPUT,
                        H.TYPE, H.CHECKBOX,
                        H.NAME, C.SELECTED_DOC_IDS,
                        H.VALUE, docKey,
                        H.CHECKED, H.CHECKED);
            } else {
                xhtml.startElement(H.INPUT,
                        H.TYPE, H.CHECKBOX,
                        H.NAME, C.SELECTED_DOC_IDS,
                        H.VALUE, docKey);
            }
            xhtml.endElement(H.INPUT);
            xhtml.endElement(H.TD);
        }
    }
}


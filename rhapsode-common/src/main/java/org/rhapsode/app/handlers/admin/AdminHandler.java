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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.classic.ParseException;
import org.rhapsode.app.contants.CSS;
import org.rhapsode.app.contants.H;
import org.rhapsode.app.decorators.RhapsodeXHTMLHandler;
import org.rhapsode.app.handlers.AbstractRhapsodeHandler;
import org.xml.sax.SAXException;

public abstract class AdminHandler extends AbstractRhapsodeHandler {
    private static final Pattern ALPHA_NUMERIC_PATTERN = Pattern.compile("\\A[a-zA-Z0-9_]+\\Z");

    public AdminHandler(String toolName) {
        super(toolName);
    }

    protected static String testBlank(String name, String value) throws ParseException {
        if (StringUtils.isBlank(value)) {
            throw new ParseException("Must specify a " + name);
        }
        return value;
    }

    protected static void testAlphanumeric(String name) throws ParseException {
        Matcher matcher = ALPHA_NUMERIC_PATTERN.matcher(name);
        if (!matcher.matches()) {
            throw new ParseException("Names may contain only ASCII alpha numerics and the '_' character");
        }
    }

    @Override
    public void addHeader(RhapsodeXHTMLHandler xhtml, String optionalStyleString) throws SAXException {
        xhtml.startElement(xhtml.XHTML, H.HTML, H.HTML, xhtml.EMPTY_ATTRIBUTES);
        xhtml.newline();
        xhtml.startElement(xhtml.XHTML, H.HEAD, H.HEAD, xhtml.EMPTY_ATTRIBUTES);
        xhtml.newline();
        //css link
        xhtml.startElement(H.LINK,
                H.REL, "stylesheet",
                H.TYPE, "text/css",
                H.HREF, "/rhapsode/css/rhapsode_admin.css");
        xhtml.endElement(H.LINK);
        //content...necessary?
        xhtml.startElement(H.META,
                H.HTTP_EQUIV, "Content-Type",
                H.CONTENT, "text/html; charset=UTF-8");
        xhtml.endElement(H.META);

        xhtml.startElement(H.META,
                H.HTTP_EQUIV, "X-UA-Compatible",
                H.CONTENT, "IE=Edge");
        xhtml.endElement(H.META);

        if (optionalStyleString != null) {
            xhtml.startElement(H.STYLE);
            xhtml.characters(optionalStyleString);
            xhtml.endElement(H.STYLE);
        }

        xhtml.endElement(xhtml.XHTML, H.HEAD, H.HEAD);
        xhtml.newline();
        //start body
        xhtml.startElement(xhtml.XHTML, H.BODY, H.BODY, xhtml.EMPTY_ATTRIBUTES);

        //header table
        xhtml.startElement(H.TABLE, H.CLASS, CSS.TITLE);
        xhtml.startElement(H.TR, H.CLASS, CSS.TITLE_ROW);
        //first cell
       /* xhtml.startElement(H.TD, H.ALIGN, CSS.LEFT);
        xhtml.startElement(H.H1);
        xhtml.characters("Rhapsode Administrative Tools");
        xhtml.endElement(H.H1);
        xhtml.endElement(H.TD);
        //second cell
        xhtml.startElement(H.TD, H.ALIGN, CSS.RIGHT);
        xhtml.startElement(H.IMG,
                H.SRC, "/rhapsode/icon.gif",
                CSS.WIDTH, "142",
                CSS.HEIGHT, "66");
        xhtml.endElement(H.TD);*/
        xhtml.startElement(H.TD, H.ALIGN, CSS.LEFT);
        xhtml.startElement(H.IMG,
                H.SRC, "/rhapsode/icon.gif",
                CSS.WIDTH, "71",
                CSS.HEIGHT, "33");
        xhtml.endElement(H.TD);
        xhtml.endElement(H.TR);
        xhtml.startElement(H.TR);
        xhtml.startElement(H.TD, H.ALIGN, CSS.LEFT);
        xhtml.startElement(H.H1);
        xhtml.characters("Rhapsode Administrative Tools");
        xhtml.endElement(H.H1);
        xhtml.endElement(H.TD);
        xhtml.endElement(H.TR);
        xhtml.endElement(H.TABLE);
        xhtml.startElement(H.P);
        xhtml.endElement(H.P);

        //header bar list
        xhtml.startElement(H.UL,
                H.CLASS, CSS.HEADER_BAR);

        xhtml.startElement(H.LI, H.CLASS, CSS.HEADER_BAR);
        xhtml.startElement(H.A,
                H.CLASS, CSS.HEADER_BAR,
                H.HREF, hrefRoot + "/admin/concepts");
        xhtml.characters("Stored");
        xhtml.br();
        xhtml.characters("Concepts");
        xhtml.endElement(H.A);
        xhtml.endElement(H.LI);

        xhtml.startElement(H.LI, H.CLASS, CSS.HEADER_BAR);
        xhtml.startElement(H.A,
                H.CLASS, CSS.HEADER_BAR,
                H.HREF, hrefRoot + "/admin/queries");
        xhtml.characters("Stored");
        xhtml.br();
        xhtml.characters("Queries");
        xhtml.endElement(H.A);
        xhtml.endElement(H.LI);

        xhtml.startElement(H.LI, H.CLASS, CSS.HEADER_BAR);
        xhtml.startElement(H.A,
                H.CLASS, CSS.HEADER_BAR,
                H.HREF, hrefRoot + "/admin/collection");
        xhtml.characters("Collection");
        xhtml.br();
        xhtml.characters("Manager");

        xhtml.endElement(H.A);
        xhtml.endElement(H.LI);

        xhtml.startElement(H.LI, H.CLASS, CSS.HEADER_BAR);
        xhtml.startElement(H.A,
                H.CLASS, CSS.HEADER_BAR,
                H.HREF, hrefRoot + "/admin/reports");
        xhtml.characters("Report");
        xhtml.br();
        xhtml.characters("Writer");
        xhtml.endElement(H.A);
        xhtml.endElement(H.LI);

        xhtml.startElement(H.LI, H.CLASS, CSS.HEADER_BAR);
        xhtml.startElement(H.A,
                H.CLASS, CSS.HEADER_BAR,
                H.HREF, hrefRoot + "/admin/settings");
        xhtml.characters("Interface");
        xhtml.br();
        xhtml.characters("Settings");
        xhtml.endElement(H.A);
        xhtml.endElement(H.LI);

        xhtml.startElement(H.LI, H.CLASS, CSS.HEADER_BAR);
        xhtml.startElement(H.A,
                H.CLASS, CSS.HEADER_BAR,
                H.HREF, hrefRoot + "/admin/selecteds");
        xhtml.characters("Selected");
        xhtml.br();
        xhtml.characters("Documents");
        xhtml.endElement(H.A);
        xhtml.endElement(H.LI);


        xhtml.startElement(H.LI, H.CLASS, CSS.HEADER_BAR);
        xhtml.startElement(H.A,
                H.CLASS, CSS.SEARCH_HEADER_BAR,
                H.HREF, hrefRoot + "/basic");
        xhtml.characters("Search");
        xhtml.br();
        xhtml.characters("Tools");
        xhtml.endElement(H.A);
        xhtml.endElement(H.LI);

        xhtml.endElement(H.UL);
        xhtml.startElement(H.P);
        xhtml.endElement(H.P);
        xhtml.element(H.H2, getToolName());
        xhtml.startElement(H.P);
        xhtml.endElement(H.P);
    }

}

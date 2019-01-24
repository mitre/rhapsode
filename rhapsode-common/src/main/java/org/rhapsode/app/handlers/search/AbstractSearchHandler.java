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


import java.util.Map;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.sax.XHTMLContentHandler;
import org.rhapsode.app.RhapsodeSearcherApp;
import org.rhapsode.app.contants.C;
import org.rhapsode.app.contants.CSS;
import org.rhapsode.app.contants.H;
import org.rhapsode.app.decorators.RhapsodeDecorator;
import org.rhapsode.app.decorators.RhapsodeXHTMLHandler;
import org.rhapsode.app.handlers.AbstractRhapsodeHandler;
import org.rhapsode.lucene.search.BaseSearchRequest;
import org.rhapsode.lucene.search.StoredQuery;
import org.xml.sax.SAXException;

public abstract class AbstractSearchHandler extends AbstractRhapsodeHandler {

    private static final Metadata EMPTY_METADATA = new Metadata();
    public static boolean INCLUDE_ADMIN_LINK = true;

    public AbstractSearchHandler(String toolName) {
        super(toolName);
    }

    static void addHiddenInputAndButtons(RhapsodeXHTMLHandler xhtml) throws SAXException {
        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.SEARCH,
                H.VALUE, "Search",
                "default", "");
        xhtml.endElement(H.INPUT);
    }

    @Override
    protected void addHeader(RhapsodeXHTMLHandler xhtml, String style) throws SAXException {
        //xhtml.initSearchHeader("/rhapsode", getHandlerName());
        xhtml.startElement(xhtml.XHTML, H.HTML, H.HTML, xhtml.EMPTY_ATTRIBUTES);
        xhtml.newline();
        xhtml.startElement(xhtml.XHTML, H.HEAD, H.HEAD, xhtml.EMPTY_ATTRIBUTES);
        xhtml.newline();
        //css link
        xhtml.startElement(H.LINK,
                H.REL, "stylesheet",
                H.TYPE, "text/css",
                H.HREF, "/rhapsode/css/rhapsode.css");
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

        if (style != null) {
            xhtml.startElement(H.STYLE);
            xhtml.characters(style);
            xhtml.endElement(H.STYLE);
        }
        xhtml.endElement(xhtml.XHTML, H.HEAD, H.HEAD);
        xhtml.newline();
        //start body
        xhtml.startElement(xhtml.XHTML, H.BODY, H.BODY, xhtml.EMPTY_ATTRIBUTES);
        //header table
        xhtml.startElement(H.TABLE, H.CLASS, CSS.TITLE);
        xhtml.startElement(H.TR, H.CLASS, CSS.TITLE_ROW);
        //second cell
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
        xhtml.characters("Rhapsode Search Tools");
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
                H.HREF, hrefRoot + "/basic");
        xhtml.characters("Basic");
        xhtml.br();
        xhtml.characters("Search");
        xhtml.endElement(H.A);
        xhtml.endElement(H.LI);

        //Concordance
        xhtml.startElement(H.LI, H.CLASS, CSS.HEADER_BAR);
        xhtml.startElement(H.A,
                H.CLASS, CSS.HEADER_BAR,
                H.HREF, hrefRoot + "/concordance");
        xhtml.characters("Concordance");
        xhtml.br();
        xhtml.characters("Search");
        xhtml.endElement(H.A);
        xhtml.endElement(H.LI);

        //co-occur
        xhtml.startElement(H.LI, H.CLASS, CSS.HEADER_BAR);
        xhtml.startElement(H.A,
                H.CLASS, CSS.HEADER_BAR,
                H.HREF, hrefRoot + "/cooccur");
        xhtml.characters("Co-");
        xhtml.br();
        xhtml.characters("Occurrences");
        xhtml.endElement(H.A);
        xhtml.endElement(H.LI);


        xhtml.startElement(H.LI, H.CLASS, CSS.HEADER_BAR);
        xhtml.startElement(H.A,
                H.CLASS, CSS.HEADER_BAR,
                H.HREF, hrefRoot + "/target_counter");
        xhtml.characters("Target");
        xhtml.br();
        xhtml.characters("Counter");
        xhtml.endElement(H.A);
        xhtml.endElement(H.LI);

        //variant_term_counter
        xhtml.startElement(H.LI, H.CLASS, CSS.HEADER_BAR);
        xhtml.startElement(H.A,
                H.CLASS, CSS.HEADER_BAR,
                H.HREF, hrefRoot + "/variant_term_counter");
        xhtml.characters("Variant");
        xhtml.br();
        xhtml.characters("Counter");
        xhtml.endElement(H.A);
        xhtml.endElement(H.LI);


        if (INCLUDE_ADMIN_LINK) {
            // if (false) {
            xhtml.startElement(H.LI, H.CLASS, CSS.HEADER_BAR);
            xhtml.startElement(H.A,
                    H.CLASS, CSS.ADMIN_HEADER_BAR,
                    H.HREF, hrefRoot + "/admin/index.html");
            xhtml.characters("Admin");
            xhtml.br();
            xhtml.characters("Tools");
            xhtml.endElement(H.A);
            xhtml.endElement(H.LI);
        }

        //end header bar list
        xhtml.endElement(H.UL);
        xhtml.startElement(H.P);
        xhtml.characters(" ");
        xhtml.endElement(H.P);
        xhtml.element(H.H2, getToolName());
        xhtml.startElement(H.P);
        xhtml.endElement(H.P);

    }

    /**
     * This should be the query, geo query, filter query, etc.
     *
     * @param xhtml
     */
    protected void addQueryWindow(RhapsodeSearcherApp searcherApp,
                                  BaseSearchRequest r, RhapsodeXHTMLHandler xhtml) throws SAXException {
        addMainQueryWindow(searcherApp, r, xhtml);
        String filterQueryString = "";
        StoredQuery cqc = null;
        if (r.getComplexQuery() != null) {
            cqc = r.getComplexQuery().getStoredQuery();
            if (cqc != null && cqc.getFilterQueryString() != null) {
                filterQueryString = cqc.getFilterQueryString();
            }
        }

        RhapsodeDecorator.writeQueryBox("Filter Query", C.FILTER_QUERY,
                filterQueryString,
                getFilterQueryBoxHeight(searcherApp), getFilterQueryBoxWidth(searcherApp), r.getLanguageDirection(), xhtml);
    }

    /**
     * This should be the query, geo query, filter query, etc.
     *
     * @param xhtml
     */
    protected void addMainQueryWindow(RhapsodeSearcherApp searcherApp,
                                      BaseSearchRequest r, RhapsodeXHTMLHandler xhtml) throws SAXException {

        String mainQueryString = "";
        StoredQuery cqc = null;
        if (r.getComplexQuery() != null) {
            cqc = r.getComplexQuery().getStoredQuery();
            if (cqc != null && cqc.getMainQueryString() != null) {
                mainQueryString = cqc.getMainQueryString();
            }
        }
        RhapsodeDecorator.writeQueryBox("Query", C.MAIN_QUERY, mainQueryString,
                getMainQueryBoxHeight(searcherApp), getMainQueryBoxWidth(searcherApp), r.getLanguageDirection(), xhtml);

        if (searcherApp.getRhapsodeCollection() != null) {
            xhtml.characters("   Field: ");
            String defaultQueryField = searcherApp.getRhapsodeCollection().getIndexSchema().getDefaultContentField();
            if (cqc != null) {
                defaultQueryField = r.getContentField();
            }
            RhapsodeDecorator.writeFieldSelector(searcherApp.getRhapsodeCollection().getIndexSchema(),
                    defaultQueryField, xhtml);
        }
        xhtml.br();
        xhtml.br();
    }

    int getMainQueryBoxWidth(RhapsodeSearcherApp searcherApp) {
        return 80;
    }

    int getMainQueryBoxHeight(RhapsodeSearcherApp searcherApp) {
        return 1;
    }

    int getFilterQueryBoxWidth(RhapsodeSearcherApp searcherApp) {
        return 80;
    }

    int getFilterQueryBoxHeight(RhapsodeSearcherApp searcherApp) {
        return 1;
    }

    void addDate(Map<String, String> metadata, XHTMLContentHandler xhtml) {

    }

    void addDocLink(Map<String, String> metadata, XHTMLContentHandler xhtml) {

    }


}

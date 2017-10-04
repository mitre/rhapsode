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
import org.apache.tika.sax.SafeContentHandler;
import org.rhapsode.app.contants.H;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Horrific copy paste of Tika's XHTMLContentHandler.
 */
public class RhapsodeXHTMLHandler extends SafeContentHandler {
    private final static String A = "a";
    private final static String HREF = "href";

    /**
     * The XHTML namespace URI
     */
    public static final String XHTML = "http://www.w3.org/1999/xhtml";

    /**
     * The newline character that gets inserted after block elements.
     */
    private static final char[] NL = new char[]{'\n'};

    /**
     * The tab character gets inserted before table cells and list items.
     */
    private static final char[] TAB = new char[]{'\t'};

    /**
     * The elements that are in the <head> section.
     */
    private static final Set<String> HEAD =
            unmodifiableSet("title", "link", "base", "meta");

    /**
     * The elements that are automatically emitted by lazyStartHead, so
     * skip them if they get sent to startElement/endElement by mistake.
     */
    private static final Set<String> AUTO =
            unmodifiableSet("html", "frameset");

    /**
     * The elements that get prepended with the {@link #TAB} character.
     */
    private static final Set<String> INDENT =
            unmodifiableSet("li", "dd", "dt", "td", "th", "frame");

    /**
     * The elements that get appended with the {@link #NL} character.
     */
    public static final Set<String> ENDLINE = unmodifiableSet(
            "p", "h1", "h2", "h3", "h4", "h5", "h6", "div", "ul", "ol", "dl",
            "pre", "hr", "blockquote", "address", "fieldset", "table", "form",
            "noscript", "li", "dt", "dd", "noframes", "br", "tr", "select", "option");

    public static final Attributes EMPTY_ATTRIBUTES = new AttributesImpl();

    private static Set<String> unmodifiableSet(String... elements) {
        return Collections.unmodifiableSet(
                new HashSet<String>(Arrays.asList(elements)));
    }


    /**
     * Flag to indicate whether the document has been started.
     */
    private boolean documentStarted = false;

    /**
     * Flags to indicate whether the document head element has been started/ended.
     */
    private boolean headStarted = false;
    private boolean headEnded = false;

    public RhapsodeXHTMLHandler(ContentHandler handler) {
        super(handler);
    }

    /**
     * Starts an XHTML document by setting up the namespace mappings
     * when called for the first time.
     * The standard XHTML prefix is generated lazily when the first
     * element is started.
     */
    @Override
    public void startDocument() throws SAXException {
        if (!documentStarted) {
            documentStarted = true;
            super.startDocument();
            startPrefixMapping("", XHTML);
        }
    }

    public void initAdminHeader(String hrefRoot, String toolName) throws SAXException {
        hrefRoot = (hrefRoot == null) ? StringUtils.EMPTY : hrefRoot;

    }


    /**
     * Ends the XHTML document by writing the following footer and
     * clearing the namespace mappings:
     * <pre>
     *   &lt;/body&gt;
     * &lt;/html&gt;
     * </pre>
     */
    @Override
    public void endDocument() throws SAXException {
        super.endElement(XHTML, H.BODY, H.BODY);
        super.endElement(XHTML, H.HTML, H.HTML);
        endPrefixMapping("");
        super.endDocument();
    }

    /**
     * Starts the given element. Table cells and list items are automatically
     * indented by emitting a tab character as ignorable whitespace.
     */
    @Override
    public void startElement(
            String uri, String local, String name, Attributes attributes)
            throws SAXException {


        if (XHTML.equals(uri) && INDENT.contains(name)) {
            ignorableWhitespace(TAB, 0, TAB.length);
        }

        super.startElement(uri, local, name, attributes);

    }

    /**
     * Ends the given element. Block elements are automatically followed
     * by a newline character.
     */
    @Override
    public void endElement(String uri, String local, String name) throws SAXException {
        if (!AUTO.contains(name)) {
            super.endElement(uri, local, name);
            if (XHTML.equals(uri) && ENDLINE.contains(name)) {
                newline();
            }
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        super.characters(ch, start, length);
    }

    //------------------------------------------< public convenience methods >

    public void startElement(String name) throws SAXException {
        startElement(XHTML, name, name, EMPTY_ATTRIBUTES);
    }

    public void startElement(String name, String... attrs)
            throws SAXException {

        if (attrs == null) {
            throw new SAXException("Attrs must not be null");
        }
        AttributesImpl attributes = new AttributesImpl();
        if (attrs.length != 0 && attrs.length % 2 != 0) {
            throw new SAXException("Must have even number of attribute value pairs");
        }

        for (int i = 0; i < attrs.length; i += 2) {
            if (attrs[i] != null && attrs[i + 1] != null) {
                attributes.addAttribute("", attrs[i], attrs[i], "CDATA", attrs[i + 1]);
            }
        }
        startElement(XHTML, name, name, attributes);
    }

    public void startElement(String name, Attributes attributes)
            throws SAXException {
        startElement(XHTML, name, name, attributes);
    }

    public void endElement(String name) throws SAXException {
        endElement(XHTML, name, name);
    }

    public void characters(String characters) throws SAXException {
        if (characters != null && characters.length() > 0) {
            characters(characters.toCharArray(), 0, characters.length());
        }
    }

    public void newline() throws SAXException {
        ignorableWhitespace(NL, 0, NL.length);
    }

    public void br() throws SAXException {
        startElement(H.BR);
        endElement(H.BR);
    }

    public void td(String contents) throws SAXException {
        if (StringUtils.isBlank(contents)) {
            startElement(H.TD);
            characters(" ");
            endElement(H.TD);
        } else {
            element(H.TD, contents);
        }
    }

    /**
     * Emits an XHTML element with the given text content. If the given
     * text value is null or empty, then the element is not written.
     *
     * @param name  XHTML element name
     * @param value element value, possibly <code>null</code>
     * @throws SAXException if the content element could not be written
     */
    public void element(String name, String value) throws SAXException {
        if (value != null && value.length() > 0) {
            startElement(name);
            characters(value);
            endElement(name);
        }
    }

    public static void simpleInit(RhapsodeXHTMLHandler xhtml) throws SAXException {
        xhtml.startDocument();
        xhtml.startElement(xhtml.XHTML, H.HTML, H.HTML, xhtml.EMPTY_ATTRIBUTES);
        xhtml.newline();
        xhtml.startElement(xhtml.XHTML, H.HEAD, H.HEAD, xhtml.EMPTY_ATTRIBUTES);
        xhtml.newline();
        xhtml.startElement(H.META,
                H.HTTP_EQUIV, "Content-Type",
                H.CONTENT, "text/html; charset=UTF-8");
        xhtml.endElement(H.META);
        xhtml.endElement(xhtml.XHTML, H.HEAD, H.HEAD);
        xhtml.newline();
        //start body
        xhtml.startElement(xhtml.XHTML, H.BODY, H.BODY, xhtml.EMPTY_ATTRIBUTES);
    }

    public void href(String url, String label) throws SAXException {
        startElement(A,
                HREF, url);
        characters(label);
        endElement(A);
    }
}

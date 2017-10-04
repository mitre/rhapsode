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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SnippetWriter {
    static Pattern TAG_PATTERN = Pattern.compile("<([a-zA-Z]+)([^>]*)>(.*?)<\\/\\1>");
    static Pattern ATTR_PATTERN = Pattern.compile("([a-zA-Z0-9]+)\\s*=\\s*\"([^\"]+)\"");
    static Pattern LT = Pattern.compile("&lt;");
    static Pattern GT = Pattern.compile("&gt;");
    static Pattern AMP = Pattern.compile("&amp;");

    public static void write(String snippet,
                             RhapsodeXHTMLHandler xhtml) throws SAXException {
        write(snippet, null, null, xhtml);
    }

    /**
     * Takes a snippet from a highlighter, encodes and decodes the non-markup
     * tags and writes to the contenthandler.
     * <p>
     * If tagName is null, then it uses the same tagname that it got from the highlighter.
     * <p>
     * If the attributes object is null, it tries to parse and add the attributes
     * from the highlighter
     *
     * @param snippet
     * @param tagName
     * @param attributes
     * @param xhtml
     * @throws SAXException
     */
    public static void write(String snippet, String tagName, Attributes attributes,
                             RhapsodeXHTMLHandler xhtml) throws SAXException {
        Matcher m = TAG_PATTERN.matcher(snippet);
        int last = 0;
        while (m.find()) {
            String pre = decode(snippet.substring(last, m.start()));
            if (pre.length() > 0) {
                xhtml.characters(pre);
            }
            String tagNameAsIs = m.group(1);
            String attrsString = m.group(2);
            if (tagName == null) {
                tagName = tagNameAsIs;
            }
            if (attributes == null) {
                attributes = parseAttributes(attrsString);
            }

            xhtml.startElement(tagName, attributes);
            String target = m.group(3);
            xhtml.characters(decode(target));
            xhtml.endElement(tagName);
            last = m.end();
        }
        String rest = snippet.substring(last);
        if (rest.length() > 0) {
            xhtml.characters(decode(rest));
        }
    }

    private static Attributes parseAttributes(String attrsString) {
        AttributesImpl attrs = new AttributesImpl();
        Matcher am = ATTR_PATTERN.matcher(attrsString);
        while (am.find()) {
            attrs.addAttribute("", am.group(1), am.group(1), "", am.group(2));
        }
        return attrs;
    }

    private static String decode(String substring) {
        String ret = LT.matcher(substring).replaceAll("<");
        ret = GT.matcher(ret).replaceAll(">");
        ret = AMP.matcher(ret).replaceAll("&");
        return ret;
    }
}

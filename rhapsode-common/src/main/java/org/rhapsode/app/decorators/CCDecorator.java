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

import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import org.rhapsode.app.contants.C;
import org.rhapsode.app.contants.H;
import org.rhapsode.lucene.search.concordance.ConcordanceSearchRequest;
import org.tallison.lucene.search.concordance.classic.AbstractConcordanceWindowCollector;
import org.tallison.lucene.search.concordance.classic.ConcordanceWindow;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Decorator for joint Concordance/Cooccurrence items
 */
public class CCDecorator {

    public static void writeHitMax(boolean hitMax, long windows,
                                   RhapsodeXHTMLHandler xhtml) throws SAXException {
        if (hitMax) {
            xhtml.br();
            xhtml.startElement(H.FONT, H.COLOR, H.RED);
            xhtml.startElement(H.BOLD);
            xhtml.characters("The search hit the maximum number of results. " +
                    "The results only include the first " + windows + " windows.");
            xhtml.endElement(H.BOLD);
            xhtml.endElement(H.FONT);
            xhtml.br();
        }
    }

    public static void includeDuplicateWindows(ConcordanceSearchRequest searchRequest,
                                               RhapsodeXHTMLHandler xhtml) throws SAXException {

        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(C.FALSE, "Include Duplicate Windows: ");
        labels.put(C.TRUE, "Ignore Duplicate Windows: ");
        for (Map.Entry<String, String> entry : labels.entrySet()) {
            xhtml.characters(entry.getValue());
            AttributesImpl attrs = new AttributesImpl();
            attrs.addAttribute("", H.TYPE, H.TYPE, "", H.RADIO);
            attrs.addAttribute("", H.NAME, H.NAME, "", C.IGNORE_DUPLICATE_WINDOWS);
            attrs.addAttribute("", H.VALUE, H.VALUE, "", entry.getKey());

            if (searchRequest.getIgnoreDuplicateWindows() && entry.getKey().equals(C.TRUE)) {
                attrs.addAttribute("", H.CHECKED, H.CHECKED, "", H.CHECKED);
            } else if (!searchRequest.getIgnoreDuplicateWindows() && entry.getKey().equals(C.FALSE)) {
                attrs.addAttribute("", H.CHECKED, H.CHECKED, "", H.CHECKED);
            }
            xhtml.startElement(H.INPUT, attrs);
            xhtml.endElement(H.INPUT);
        }
        xhtml.br();

    }

    public static void addMaxWindows(int maxWindows,
                                     RhapsodeXHTMLHandler xhtml) throws SAXException {
        xhtml.characters("Maximum Windows ");
        xhtml.startElement(H.INPUT,
                H.TYPE, H.TEXT,
                H.NAME, C.MAX_STORED_WINDOWS,
                H.VALUE, Integer.toString(maxWindows),
                H.SIZE, "2");
        xhtml.endElement(H.INPUT);
    }

    public static void addWordsBeforeAfter(ConcordanceSearchRequest searchRequest,
                                           RhapsodeXHTMLHandler xhtml) throws SAXException {
        xhtml.characters("Number of Words Before: ");
        xhtml.startElement(H.INPUT,
                H.TYPE, H.TEXT,
                H.NAME, C.WORDS_BEFORE,
                H.SIZE, "2",
                H.VALUE, Integer.toString(searchRequest.getTokensBefore()));
        xhtml.endElement(H.INPUT);
        xhtml.characters(" ");
        xhtml.characters("Number of Words After: ");
        xhtml.startElement(H.INPUT,
                H.TYPE, H.TEXT,
                H.NAME, C.WORDS_AFTER,
                H.SIZE, "2",
                H.VALUE, Integer.toString(searchRequest.getTokensAfter()));
        xhtml.endElement(H.INPUT);
    }

    public static void writeResultCounts(ConcordanceSearchRequest request,
                                         AbstractConcordanceWindowCollector collector,
                                         int numIgnoredDocs,
                                         RhapsodeXHTMLHandler xhtml) throws SAXException {

        int windowsVisited = -1;
        if (request.getIgnoreDuplicateWindows()) {
            windowsVisited = 0;
            for (ConcordanceWindow w : collector.getWindows()) {
                windowsVisited += w.getCount();
            }
        } else {
            windowsVisited = collector.size();
        }
        DecimalFormat formatter = new DecimalFormat("###,###,###,###,###");
        String totalDocString = formatter.format(collector.getTotalDocs()) + " documents.";
        if (collector.getTotalDocs() == 1) {
            totalDocString = "one document.";
        }
        String hitDocString = formatter.format(collector.getNumDocs()) + " documents out of a total of " + totalDocString;
        if (collector.getNumDocs() == 1) {
            hitDocString = "one document.";
        }

        String uniqueWindowString = "There were " + formatter.format(collector.getWindows().size()) + " unique windows";
        if (collector.getWindows().size() == 1) {
            uniqueWindowString = "There was one unique window";
        }

        String totalWindowString = formatter.format(windowsVisited) + " total windows";
        if (windowsVisited == 1) {
            totalWindowString = " one window";
        }
        if (request.getIgnoreDuplicateWindows() == true) {
            xhtml.characters(uniqueWindowString + " out of " + totalWindowString + " in " + hitDocString);
        } else {
            if (windowsVisited == 1) {
                xhtml.characters("There was one window in " + hitDocString);

            } else {
                xhtml.characters("There were " + formatter.format(collector.getWindows().size()) + " windows in " + hitDocString);
            }
        }

        xhtml.br();
        if (numIgnoredDocs > 0) {
            xhtml.br();
            if (numIgnoredDocs == 1) {
                xhtml.characters("There is currently one ignored document.");
            } else {
                xhtml.characters("There are currently " + numIgnoredDocs + " ignored documents.");
            }
        }
    }

}

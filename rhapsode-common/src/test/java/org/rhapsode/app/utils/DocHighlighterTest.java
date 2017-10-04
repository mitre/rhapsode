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

package org.rhapsode.app.utils;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.tika.sax.ToHTMLContentHandler;
import org.junit.Test;
import org.rhapsode.app.contants.H;
import org.rhapsode.app.decorators.RhapsodeXHTMLHandler;
import org.rhapsode.app.utils.DocHighlighter;
import org.rhapsode.lucene.search.HighlightingQuery;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertTrue;


public class DocHighlighterTest {
    String field = "content";
    SpanQuery foxQuick = new SpanNearQuery(
            new SpanQuery[] {
                    new SpanTermQuery(new Term(field, "fox")),
                    new SpanTermQuery(new Term(field, "quick"))
            },
            3,
            false
    );
    SpanQuery foxAs = new SpanNearQuery(
            new SpanQuery[] {
                    new SpanTermQuery(new Term(field, "fox")),
                    new SpanTermQuery(new Term(field, "as"))
            },
            2,
            true
    );

    @Test
    public void testBasic() throws IOException, SAXException {
        String s = "the quick brown fox as well as some other fox and quick";

        SpanQuery spanQuery = new SpanOrQuery( foxQuick, foxAs, new SpanTermQuery(new Term(field, "fox")));
        Analyzer analyzer = new WhitespaceAnalyzer();
        ContentHandler handler = new ToHTMLContentHandler();
        RhapsodeXHTMLHandler xhtml = new RhapsodeXHTMLHandler(handler);
        RhapsodeXHTMLHandler.simpleInit(xhtml);

        Set<HighlightingQuery> highlightingQueries = new HashSet<>();
        highlightingQueries.add(new HighlightingQuery(spanQuery, 1, "this-the-one"));
        DocHighlighter h = new DocHighlighter();
        h.highlightSingleFieldValue(field, s, highlightingQueries, analyzer, new AtomicBoolean(false), xhtml);
        xhtml.endElement(H.BODY);
        xhtml.endDocument();
        System.out.println(handler.toString());
        assertTrue(handler.toString().contains("<span id=\"first\" class=\"this-the-one\">quick brown fox</span>"));
        assertTrue(handler.toString().contains("<span class=\"this-the-one\">fox and quick</span>"));

        //TODO: for now, the longer and earlier of a matching span is used.
        //We'd probably want to have a "joint" span of "quick brown fox as"
    }

    @Test
    public void testPriority() throws IOException, SAXException {
        String s = "the quick brown fox as well as some other fox and quick";

        SpanQuery spanQuery = new SpanOrQuery( foxQuick, foxAs);
        Analyzer analyzer = new WhitespaceAnalyzer();
        ContentHandler handler = new ToHTMLContentHandler();
        RhapsodeXHTMLHandler xhtml = new RhapsodeXHTMLHandler(handler);
        RhapsodeXHTMLHandler.simpleInit(xhtml);

        Set<HighlightingQuery> highlightingQueries = new HashSet<>();
        highlightingQueries.add(new HighlightingQuery(spanQuery, 2, "this-the-one"));
        highlightingQueries.add(new HighlightingQuery(new SpanTermQuery(new Term(field, "fox")), 1, "this-the-two"));
        DocHighlighter h = new DocHighlighter();
        h.highlightSingleFieldValue(field, s, highlightingQueries, analyzer, new AtomicBoolean(false), xhtml);
        xhtml.endElement(H.BODY);
        xhtml.endDocument();
        System.out.println(handler.toString());
        assertTrue(handler.toString().contains("brown <span id=\"first\" class=\"this-the-two\">fox</span>"));
        assertTrue(handler.toString().contains(" other <span class=\"this-the-two\">fox</span>"));

        //TODO: for now, the longer and earlier of a matching span is used.
        //We'd probably want to have a "joint" span of "quick brown fox as"
    }

}


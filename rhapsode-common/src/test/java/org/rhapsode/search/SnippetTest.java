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

package org.rhapsode.search;

import static org.junit.Assert.assertTrue;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SpanGradientFormatter;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.sax.ToHTMLContentHandler;
import org.junit.Before;
import org.junit.Test;
import org.rhapsode.app.decorators.RhapsodeXHTMLHandler;
import org.rhapsode.app.decorators.SnippetWriter;
import org.rhapsode.lucene.search.basic.LTGTEncoder;

public class SnippetTest {

    private final static String FIELD = "f";
    private final static int FRAGMENT_SIZE = 100;

    Query q = new WildcardQuery(new Term(FIELD, "f*"));
    QueryScorer scorer = new QueryScorer(q, FIELD);

    Fragmenter fragmenter = new SimpleFragmenter(FRAGMENT_SIZE);

    Highlighter highlighter = new Highlighter(
            new SpanGradientFormatter(scorer.getMaxTermWeight(), null, null, "#FFFFFF", "#F2FA06"),
            new LTGTEncoder(),
            scorer);

    @Before
    public void setUp() {
        scorer.setExpandMultiTermQuery(true);
        highlighter.setTextFragmenter(fragmenter);
    }

    @Test
    public void testBasic() throws Exception {
        Analyzer analyzer = new WhitespaceAnalyzer();
        String text = "&the; &amp; quick &lt; fo<&x &gt; &amp; & as well > as <";
        String[] frags = highlighter.getBestFragments(analyzer, FIELD, text, 5);
        StringBuilder sb = new StringBuilder();

        for (String frag : frags) {
            sb.append(frag).append("...");
        }
        Metadata m = new Metadata();
        RhapsodeXHTMLHandler xhtml = new RhapsodeXHTMLHandler(new ToHTMLContentHandler());
        xhtml.startDocument();
        xhtml.startElement("html");
        xhtml.startElement("body");

        SnippetWriter.write(sb.toString(), xhtml);
        xhtml.endDocument();

        assertTrue(xhtml.toString().contains(
                "&amp;the; &amp;amp; quick &amp;lt; <span style=\"background: #F2FA06; \">fo&lt;&amp;x</span> &amp;gt;")
        );
    }

}


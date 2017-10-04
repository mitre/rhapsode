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

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttributeImpl;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.memory.MemoryIndex;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.spans.SpanCollector;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanWeight;
import org.apache.lucene.search.spans.Spans;
import org.apache.tika.io.IOUtils;
import org.apache.tika.sax.ToXMLContentHandler;
import org.rhapsode.app.contants.H;
import org.rhapsode.app.decorators.RhapsodeXHTMLHandler;
import org.rhapsode.lucene.search.ComplexQuery;
import org.rhapsode.lucene.search.HighlightingQuery;
import org.rhapsode.text.Offset;
import org.rhapsode.text.PriorityOffset;
import org.rhapsode.text.PriorityOffsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DocHighlighter {

    private static final Logger LOG = LoggerFactory.getLogger(DocHighlighter.class);

    private static final Pattern NEWLINE_PATTERN = Pattern.compile("(?:\r\n|[\n\r])");
    private String tableClass = null;
    private String thClass = null;
    private String tdClass = null;

    /**
     * @param p
     * @param defaultContentField
     * @param embeddedPathField
     * @param fields
     * @param docs                must be sorted already for presentation order! This assumes first is parent!
     * @param docs                must be sorted already for presentation order! This assumes first is parent!
     * @param queries
     * @param analyzer
     * @param optionalStyleString
     * @throws IOException
     */
    public void highlightDocsToFile(Path p, String defaultContentField,
                                    String embeddedPathField,
                                    List<String> fields, List<Document> docs,
                                    Collection<ComplexQuery> queries,
                                    Analyzer analyzer, String optionalStyleString, int targetAttachmentOffset) throws IOException {
        if (p == null || p.getParent() == null) {
            throw new IOException("path " + p + " must not be null and its parent must not be null");
        }
        LOG.trace("WRITING FILE " + p.toAbsolutePath());
        Files.createDirectories(p.getParent());
        AtomicBoolean anythingHighlighted = new AtomicBoolean(false);
        try (OutputStream os = Files.newOutputStream(p)) {
            RhapsodeXHTMLHandler xhtml = new RhapsodeXHTMLHandler(new ToXMLContentHandler(os,
                    IOUtils.UTF_8.name()));
            initHandler(xhtml, optionalStyleString);
            highlightDocs(defaultContentField, embeddedPathField, fields, docs, queries,
                    analyzer, anythingHighlighted, targetAttachmentOffset, xhtml);
            xhtml.endElement(H.BODY);
            xhtml.endDocument();
        } catch (SAXException e) {
            throw new IOException();
        }
    }

    public void highlightDocToFile(Path p,
                                   String defaultContentField,
                                   List<String> fields, Document doc,
                                   Collection<ComplexQuery> queries,
                                   Analyzer analyzer, String optionalStyleString) throws IOException {
        if (p == null || p.getParent() == null) {
            throw new IOException("path " + p + " must not be null and its parent must not be null");
        }
        LOG.debug("NOW WRITING FILE " + p.toAbsolutePath());

        Files.createDirectories(p.getParent());
        AtomicBoolean anythingHighlighted = new AtomicBoolean(false);
        try (OutputStream os = Files.newOutputStream(p)) {
            RhapsodeXHTMLHandler xhtml = new RhapsodeXHTMLHandler(new ToXMLContentHandler(os,
                    IOUtils.UTF_8.name()));
            initHandler(xhtml, optionalStyleString);
            highlightDoc(defaultContentField, fields, doc, queries, analyzer, anythingHighlighted, xhtml);
            xhtml.endElement(H.BODY);
            xhtml.endDocument();
        } catch (SAXException e) {
            throw new IOException(e);
        }
    }

    public void highlightDocs(String defaultContentField, String embeddedPathField, List<String> fields,
                              List<Document> docs, Collection<ComplexQuery> queries,
                              Analyzer analyzer, AtomicBoolean anythingHighlighted, int targetOffset,
                              RhapsodeXHTMLHandler xhtml) throws IOException, SAXException {
        int i = 0;
        targetOffset = (targetOffset < 0) ? targetOffset = 0 : targetOffset;
        for (Document doc : docs) {
            if (i == 0) {
                if (docs.size() > 0) {
                    xhtml.element(H.H2, "Parent Document");
                }
            } else {
                xhtml.br();
                xhtml.startElement(H.H2);
                xhtml.characters("Embedded Document " + i);
                String embeddedDocPath = doc.get(embeddedPathField);
                if (!StringUtils.isBlank(embeddedDocPath)) {
                    xhtml.characters(": " + embeddedDocPath);
                }
                xhtml.endElement(H.H2);
            }
            if (i == targetOffset) {
                xhtml.startElement(H.SPAN, H.ID, H.JUMP_TO_TARGET);
                xhtml.endElement(H.SPAN);
            }

            highlightDoc(defaultContentField, fields, doc, queries, analyzer,
                    anythingHighlighted, xhtml);
            i++;
        }

    }

    public void highlightDoc(String contentField,
                             List<String> fields, Document doc,
                             Collection<ComplexQuery> queries,
                             Analyzer analyzer, AtomicBoolean anythingHighlighted,
                             RhapsodeXHTMLHandler xhtml) throws IOException, SAXException {

        List<String> fieldsToHighlight = new ArrayList<>(fields);
        if (hasContent(contentField, doc) && ! fields.contains(contentField)) {
            fieldsToHighlight.add(contentField);
        }
        Map<String, Set<HighlightingQuery>> highlightingQueries = getPerFieldHighlightingQueries(fieldsToHighlight, queries);

        xhtml.startElement(H.TABLE,
                H.CLASS, tableClass);

        for (String f : fields) {
            if (f.equals(contentField)) { //skip content field until end
                continue;
            }
            if (!hasContent(f, doc)) {
                continue;
            }
            xhtml.startElement(H.TR);
            xhtml.startElement(H.TD, H.CLASS, tdClass);
            xhtml.characters(f);
            xhtml.endElement(H.TD);
            xhtml.startElement(H.TD, H.CLASS, tdClass);
            highlightFieldValues(f, highlightingQueries.get(f), doc, analyzer,
                    anythingHighlighted, xhtml);
            xhtml.endElement(H.TD);
            xhtml.endElement(H.TR);
        }
        if (hasContent(contentField, doc)) {
            xhtml.startElement(H.TR);
            xhtml.startElement(H.TD, H.CLASS, tdClass);
            xhtml.characters("CONTENT");
            xhtml.endElement(H.TD);
            xhtml.startElement(H.TD, H.CLASS, tdClass);
            highlightFieldValues(contentField, highlightingQueries.get(contentField), doc, analyzer,
                    anythingHighlighted, xhtml);
            xhtml.endElement(H.TD);
            xhtml.endElement(H.TR);
        }
        xhtml.endElement(H.TABLE);
    }

    private boolean hasContent(String f, Document doc) {
        String[] values = doc.getValues(f);
        if (values == null) {
            return false;
        }
        for (String v : values) {
            if (!StringUtils.isBlank(v)) {
                return true;
            }
        }
        return false;
    }

    private void highlightFieldValues(String field, Set<HighlightingQuery> highlightingQueries,
                                      Document doc, Analyzer analyzer,
                                      AtomicBoolean anythingHighlighted,
                                      RhapsodeXHTMLHandler xhtml) throws SAXException, IOException {
        for (String s : doc.getValues(field)) {
            xhtml.startElement(H.DIV);
            highlightSingleFieldValue(field, s, highlightingQueries, analyzer,
                    anythingHighlighted, xhtml);
            xhtml.endElement(H.DIV);
        }
    }

    protected void highlightSingleFieldValue(String field, String s,
                                             Set<HighlightingQuery> highlightingQueries,
                                             Analyzer analyzer,
                                             AtomicBoolean anythingHighlighted,
                                             RhapsodeXHTMLHandler xhtml) throws IOException, SAXException {
        if (highlightingQueries == null || highlightingQueries.size() == 0) {
            xhtml.characters(s);
            return;
        }
        List<PriorityOffset> tokenOffsets = new ArrayList<>();//token offsets
        Map<Integer, Offset> charOffsets = new HashMap<>();//char offsets, indexed by token position
        MemoryIndex index = new MemoryIndex(true);
        index.addField(field, s, analyzer);
        index.freeze();

        IndexSearcher searcher = index.createSearcher();
        for (HighlightingQuery hq : highlightingQueries) {
            addPriorityOffsets(searcher, hq, tokenOffsets, charOffsets);
        }
        List<PriorityOffset> winnowed = PriorityOffsetUtil.removeOverlapsAndSort(tokenOffsets);
        highlight(winnowed, charOffsets, s, anythingHighlighted, xhtml);
    }

    private void addPriorityOffsets(IndexSearcher indexSearcher, HighlightingQuery highlightingQuery,
                                    List<PriorityOffset> tokenOffsets, Map<Integer, Offset> charOffsets) throws IOException {
        IndexReader reader = indexSearcher.getIndexReader();
        SpanQuery sq = highlightingQuery.getSpanQuery();

        sq = (SpanQuery) sq.rewrite(reader);
        SpanWeight weight = sq.createWeight(indexSearcher, false);
        Spans spans = weight.getSpans(reader.leaves().get(0),
                SpanWeight.Postings.OFFSETS);

        if (spans == null) {
            return;
        }

        OffsetSpanCollector offsetSpanCollector = new OffsetSpanCollector();
        while (spans.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
            while (spans.nextStartPosition() != Spans.NO_MORE_POSITIONS) {
                OffsetAttributeImpl offsetAttribute = new OffsetAttributeImpl();
                offsetAttribute.setOffset(spans.startPosition(), spans.endPosition());
                tokenOffsets.add(new PriorityOffset(spans.startPosition(), spans.endPosition(),
                        highlightingQuery.getPriority(), highlightingQuery.getSpanClass()));
                spans.collect(offsetSpanCollector);
            }
        }
        charOffsets.putAll(offsetSpanCollector.getOffsets());
    }

    /**
     * @param winnowed    list of non-overlapping and sorted offsets
     * @param charOffsets
     * @param s
     * @param xhtml
     * @throws SAXException
     */
    private void highlight(List<PriorityOffset> winnowed, Map<Integer, Offset> charOffsets, String s,
                           AtomicBoolean anythingHighlighted, RhapsodeXHTMLHandler xhtml) throws SAXException {
        int last = 0;
        for (PriorityOffset offset : winnowed) {
            writeCharactersWithNewLine(s.substring(last, charOffsets.get(offset.startOffset()).startOffset()), xhtml);
            if (anythingHighlighted.get() == false) {
                xhtml.startElement(H.SPAN,
                        H.ID, H.JUMP_TO_FIRST,
                        H.CLASS, offset.getLabel()
                );
                anythingHighlighted.set(true);
            } else {
                xhtml.startElement(H.SPAN,
                        H.CLASS, offset.getLabel()
                );
            }
            writeCharactersWithNewLine(s.substring(charOffsets.get(offset.startOffset()).startOffset(),
                    charOffsets.get(offset.endOffset() - 1).endOffset()), xhtml);
            xhtml.endElement(H.SPAN);
            last = charOffsets.get(offset.endOffset() - 1).endOffset();
        }
        writeCharactersWithNewLine(s.substring(last), xhtml);
    }

    private void writeCharactersWithNewLine(String s, RhapsodeXHTMLHandler xhtml) throws SAXException {
        Matcher m = NEWLINE_PATTERN.matcher(s);
        int last = 0;
        while (m.find()) {
            xhtml.characters(s.substring(last, m.start()));
            xhtml.br();
            last = m.end();
        }
        xhtml.characters(s.substring(last));
    }

    public void setTableClass(String tableClass) {
        this.tableClass = tableClass;
    }

    public void setTDClass(String tdClass) {
        this.tdClass = tdClass;
    }


    private class OffsetSpanCollector implements SpanCollector {
        Map<Integer, Offset> charOffsets = new HashMap<>();

        @Override
        public void collectLeaf(PostingsEnum postingsEnum, int i, Term term) throws IOException {
            charOffsets.put(i, new Offset(postingsEnum.startOffset(), postingsEnum.endOffset()));
        }

        @Override
        public void reset() {
        }

        public Map<Integer, Offset> getOffsets() {
            return charOffsets;
        }
    }

    private static void initHandler(RhapsodeXHTMLHandler xhtml, String optionalStyleString) throws SAXException {
        xhtml.startDocument();
        xhtml.startElement(xhtml.XHTML, H.HTML, H.HTML, xhtml.EMPTY_ATTRIBUTES);
        xhtml.newline();
        xhtml.startElement(xhtml.XHTML, H.HEAD, H.HEAD, xhtml.EMPTY_ATTRIBUTES);
        xhtml.newline();
        xhtml.startElement(H.META,
                H.HTTP_EQUIV, "Content-Type",
                H.CONTENT, "text/html; charset=UTF-8");
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
    }


    private static Map<String, Set<HighlightingQuery>> getPerFieldHighlightingQueries(List<String> fields, Collection<ComplexQuery> queries) throws IOException {

        Map<String, Set<HighlightingQuery>> tmp = new HashMap<>();
        int maxPriority = -1;
        for (ComplexQuery cq : queries) {
            int p = cq.getStoredQuery().getPriority();
            if (p > maxPriority) {
                maxPriority = p;
            }
        }

        for (ComplexQuery cq : queries) {
            for (String f : fields) {
                HighlightingQuery q = cq.getHighlightingQuery(f, maxPriority + 1);

                Set<HighlightingQuery> qs = tmp.get(f);
                if (qs == null) {
                    qs = new HashSet<>();
                }
                qs.add(q);
                tmp.put(f, qs);
            }
        }
        return tmp;
    }
}

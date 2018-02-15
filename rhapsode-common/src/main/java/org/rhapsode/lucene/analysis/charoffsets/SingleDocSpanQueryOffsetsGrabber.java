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

package org.rhapsode.lucene.analysis.charoffsets;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttributeImpl;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.memory.MemoryIndex;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanWeight;
import org.apache.lucene.search.spans.Spans;
import org.rhapsode.lucene.search.spans.PriorityQuery;
import org.rhapsode.lucene.search.spans.SimplePriorityQuery;
import org.rhapsode.text.PriorityOffset;
import org.tallison.lucene.search.concordance.charoffsets.RandomAccessCharOffsetContainer;
import org.tallison.lucene.search.concordance.charoffsets.ReanalyzingTokenCharOffsetsReader;
import org.tallison.lucene.search.concordance.charoffsets.TokenCharOffsetRequests;
import org.tallison.lucene.search.spans.SimpleSpanQueryConverter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class SingleDocSpanQueryOffsetsGrabber {

    private static IndexSearcher buildMemoryIndex(Document document, String fieldName, Analyzer analyzer)
            throws IOException {
        MemoryIndex index = new MemoryIndex(true);
        String[] content = document.getValues(fieldName);
        for (String c : content) {
            index.addField(fieldName, c, analyzer);
        }
        return index.createSearcher();
    }

    /**
     * @param document
     * @param fieldName
     * @param analyzer
     * @param q
     * @return all character offsets including overlapping!  Make sure to remove overlapping!
     * @throws IOException
     */
    public static List<PriorityOffset> getCharOffsets(Document document, String fieldName,
                                                      Analyzer analyzer,
                                                      Query q) throws IOException {
        return getCharOffsets(document, fieldName, analyzer, new SimplePriorityQuery(q,
                StringUtils.EMPTY, 0));
    }

    public static List<PriorityOffset> getCharOffsets(Document document, String fieldName, Analyzer analyzer,
                                                      List<PriorityQuery> pQueries) throws IOException {

        List<PriorityOffset> tokenOffsets = getTokenOffsets(document, fieldName, analyzer, pQueries);
        ReanalyzingTokenCharOffsetsReader reader = new ReanalyzingTokenCharOffsetsReader(analyzer);
        TokenCharOffsetRequests requests = new TokenCharOffsetRequests();
        for (PriorityOffset offset : tokenOffsets) {
            for (int i = offset.startOffset(); i < offset.endOffset(); i++) {
                requests.add(i);
            }
        }
        RandomAccessCharOffsetContainer results = new RandomAccessCharOffsetContainer();
        reader.getTokenCharOffsetResults(document, fieldName, requests, results);
        List<PriorityOffset> charOffsets = new ArrayList<PriorityOffset>();

        for (PriorityOffset tokenOffset : tokenOffsets) {
            int start = results.getCharacterOffsetStart(tokenOffset.startOffset());
            int end = results.getCharacterOffsetEnd(tokenOffset.endOffset() - 1);
            OffsetAttribute charOffset = new OffsetAttributeImpl();

            charOffset.setOffset(start, end);
            charOffsets.add(new PriorityOffset(charOffset.startOffset(), charOffset.endOffset(),
                    tokenOffset.getPriority(), tokenOffset.getLabel()));
        }

        return charOffsets;
    }

    public static List<PriorityOffset> getCharOffsets(Document document, String fieldName,
                                                      Analyzer analyzer, PriorityQuery pQuery
    ) throws IOException {
        List<PriorityQuery> list = new ArrayList<PriorityQuery>(1);
        list.add(pQuery);
        return getCharOffsets(document, fieldName, analyzer, list);
    }


    private static List<PriorityOffset> getTokenOffsets(Document document, String fieldName,
                                                        Analyzer analyzer, List<PriorityQuery> pQueries) throws IOException {
        IndexSearcher searcher = buildMemoryIndex(document, fieldName, analyzer);
        List<PriorityOffset> results = new ArrayList<>();

        for (PriorityQuery pQuery : pQueries) {
            results.addAll(getTokenOffsets(fieldName, pQuery, searcher));
        }
        return results;
    }


    private static List<PriorityOffset> getTokenOffsets(String fieldName,
                                                        PriorityQuery pQuery, IndexSearcher searcher) throws IOException {

        SimpleSpanQueryConverter converter = new SimpleSpanQueryConverter();
        List<PriorityOffset> offsets = new ArrayList<>();
        List<LeafReaderContext> ctxs = searcher.getIndexReader().leaves();
        assert (ctxs.size() == 1);
        LeafReaderContext ctx = ctxs.get(0);
        if (ctx == null) {
            throw new IOException("Null memory index.  Something awful happened. Sorry.");
        }

        SpanQuery query = converter.convert(fieldName, pQuery.getQuery());
        query = (SpanQuery) query.rewrite(searcher.getIndexReader());
        SpanWeight sw = query.createWeight(searcher, false, 1.0f);
        Spans spans = sw.getSpans(ctx, SpanWeight.Postings.OFFSETS);
        int docs = 0;
        while (spans.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
            while (spans.nextStartPosition() != Spans.NO_MORE_POSITIONS) {
                OffsetAttribute offset = new OffsetAttributeImpl();

                offsets.add(new PriorityOffset(spans.startPosition(), spans.endPosition(),
                        pQuery.getPriority(), pQuery.getLabel()));

            }
            if (++docs > 1) {
                throw new IOException("I was expecting only a single document!!!");
            }
        }
        return offsets;
    }

}

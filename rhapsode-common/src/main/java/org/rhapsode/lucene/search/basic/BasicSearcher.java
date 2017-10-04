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
package org.rhapsode.lucene.search.basic;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.highlight.Encoder;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SpanGradientFormatter;
import org.rhapsode.lucene.schema.IndexSchema;
import org.rhapsode.lucene.search.IndexManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BasicSearcher {
    protected final static String ELLIPSE = "...";
    private final IndexManager indexManager;
    private final IndexSchema indexSchema;

    public BasicSearcher(IndexManager indexManager, IndexSchema indexSchema) {
        this.indexManager = indexManager;
        this.indexSchema = indexSchema;
    }

    public BasicSearchResults search(BasicSearchRequest searchRequest) throws ParseException, IOException {
        //System.out.println("Q: " + config.getContentField() + " : " + q.toString() + " : ");
        int start = searchRequest.getLastStart();
        int end = searchRequest.getLastEnd();
        int howMany = calcHowMany(searchRequest);
        Sort sort = searchRequest.getSort();
        IndexSearcher searcher = indexManager.getSearcher();

        ScoreDoc[] sds = null;
        int totalHits = -1;
        try {
            //according to the javadocs (v4.5.1, including Sort() slightly increases overhead)
            if (sort == null) {
                TopScoreDocCollector collector = TopScoreDocCollector.create(howMany);
                searcher.search(searchRequest.getComplexQuery().getRetrievalQuery(), collector);
                TopDocs topDocs = collector.topDocs();
                sds = topDocs.scoreDocs;
                totalHits = topDocs.totalHits;
            } else {
                TopDocs topDocs = searcher.search(searchRequest.getComplexQuery().getRetrievalQuery(), howMany, sort);
                sds = topDocs.scoreDocs;
                totalHits = topDocs.totalHits;
            }
            return fillResults(searchRequest, sds, totalHits, searcher, start, end);
        } catch (BooleanQuery.TooManyClauses e) {
            throw new ParseException("Too many clauses in boolean query");
        } finally {
            indexManager.release(searcher);
        }
    }

    private BasicSearchResults fillResults(BasicSearchRequest searchRequest,
                                           ScoreDoc[] scoreDocs, int totalHits,
                                           IndexSearcher searcher,
                                           int start, int end) throws IOException {

        List<BasicSearchResult> results = new ArrayList<>();

        Set<String> selected = searchRequest.getFields();
        IndexReader reader = searcher.getIndexReader();
        int totalDocs = reader.numDocs();

        for (int i = start; i <= end && i < scoreDocs.length; i++) {
            String snippetString = StringUtils.EMPTY;
            Document d = reader.document(scoreDocs[i].doc, selected);
            if (searchRequest.shouldGetSnippets()) {
                Encoder encoder = new LTGTEncoder();

                String[] txt = d.getValues(searchRequest.getContentField());

                StringBuilder sb = tryToGetSnippetFromContent(searchRequest, encoder, txt);
                snippetString = sb.toString();

                //last ditch, just take first x characters
                if (StringUtils.isEmpty(snippetString)) {
                    if (txt.length > 0 && txt[0] != null) {
                        int tmpEnd = Math.min(searchRequest.getMaxSnippetLengthChars(), txt[0].length());
                        snippetString = encoder.encodeText(txt[0].substring(0, tmpEnd));
                    }
                }
            }
            BasicSearchResult result = new BasicSearchResult();
            result.setLuceneDocId(scoreDocs[i].doc);
            result.setSnippet(snippetString);

            result.setMetadata(searchRequest.extractMetadata(d));

            result.setN(i + 1);
            results.add(result);
        }
        return new BasicSearchResults(results, start, end, totalHits, totalDocs);
    }

    protected StringBuilder tryToGetSnippetFromContent(BasicSearchRequest request, Encoder encoder,
                                                       String[] txt) throws IOException {
        StringBuilder sb = new StringBuilder();
        if (request.getComplexQuery().getHighlightingQuery() == null)
            return sb;

        QueryScorer scorer = new QueryScorer(request.getComplexQuery().getHighlightingQuery(), request.getContentField());
        scorer.setExpandMultiTermQuery(true);

        Fragmenter fragmenter = new SimpleFragmenter(request.getFragmentSize());

        Highlighter highlighter = new Highlighter(
                new SpanGradientFormatter(scorer.getMaxTermWeight(), null, null, "#FFFFFF", "#F2FA06"),
                encoder,
                scorer);
        highlighter.setTextFragmenter(fragmenter);


        try {
            List<String> bits = tryToGetBits(request, Highlighter.DEFAULT_MAX_CHARS_TO_ANALYZE,
                    highlighter, txt);
            //if nothing so far and configured maxchars is >, then try again
            if (bits.size() == 0 &&
                    request.getMaxCharsToReadForSnippets() > Highlighter.DEFAULT_MAX_CHARS_TO_ANALYZE) {
                bits = tryToGetBits(request, request.getMaxCharsToReadForSnippets(),
                        highlighter, txt);
            }
            for (int j = 0; j < bits.size() - 1; j++) {
                sb.append(bits.get(j));
                sb.append(ELLIPSE);
            }
            if (bits.size() > 0)
                sb.append(bits.get(bits.size() - 1));

        } catch (InvalidTokenOffsetsException e) {
            sb.append("INVALID TOKEN OFFSET EXCEPTION");
        }
        return sb;

    }

    private List<String> tryToGetBits(BasicSearchRequest request, int maxCharsToReadForSnippets, Highlighter highlighter,
                                      String[] text) throws IOException, InvalidTokenOffsetsException {

        highlighter.setMaxDocCharsToAnalyze(maxCharsToReadForSnippets);
        List<String> bits = new ArrayList<>();
        for (int i = 0; i < text.length; i++) {
            String[] tmpBits = highlighter.getBestFragments(indexSchema.getOffsetAnalyzer(),
                    request.getContentField(), text[i],
                    request.getSnippetsPerResult());
            for (String s : tmpBits) {
                bits.add(s);
            }
            if (bits.size() >= request.getSnippetsPerResult()) {
                break;
            }
        }
        return bits;
    }

    protected static int calcHowMany(BasicSearchRequest request) {
        if (request.getPagingDirection() == PagingDirection.NEXT) {
            return request.getResultsPerPage() + request.getLastEnd() + 1;
        } else {
            return request.getResultsPerPage() + request.getLastStart() + 1;
        }
    }

}
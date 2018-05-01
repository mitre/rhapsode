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
package org.rhapsode.indexer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.util.Bits;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.LineSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentencePreProcessor;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class W2VModelBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(W2VModelBuilder.class);
    private static final int MAX_TOKENS_PER_SENT = 50;

    public void build(IndexReader reader, String field, Analyzer analyzer, Path modelFile) {
        SentenceIterator iter = new IndexReaderIterator(reader, field, analyzer);

        TokenizerFactory t = new DefaultTokenizerFactory();
        t.setTokenPreProcessor(new CommonPreprocessor());
        Word2Vec vec = new Word2Vec.Builder()
                .minWordFrequency(5)
                .iterations(2)
                .layerSize(200)
                .seed(42)
                .windowSize(5)
                .iterate(iter)
                .tokenizerFactory(t)
                .build();
        LOG.info("Fitting Word2Vec model....");
        vec.fit();
        LOG.info("Finished fitting Word2Vec model.");
        WordVectorSerializer.writeWord2VecModel(vec, modelFile.toFile());
        LOG.info("Finished writing Word2Vec model.");


    }

    private class IndexReaderIterator implements SentenceIterator {
        Matcher sentMatcher = Pattern.compile("(?i)([a-z]{2,})([?!;\\.])").matcher("");

        List<String> sents = new LinkedList<>();
        int docId = 0;
        final int maxDoc;
        final IndexReader reader;
        final Set<String> fieldsToLoad;
        final String field;
        final Bits liveDocs;
        private Set<String> commonAbbrevs = new HashSet<>();
        private final Analyzer analyzer;

        public IndexReaderIterator(IndexReader reader, String field, Analyzer analyzer) {

            maxDoc = reader.maxDoc();
            this.reader = reader;
            this.fieldsToLoad = new HashSet<>();
            fieldsToLoad.add(field);
            this.field = field;
            liveDocs = MultiFields.getLiveDocs(reader);
            commonAbbrevs.addAll(
                    Arrays.asList(
                            "mr mrs ms dr ft ccp spp qa est am pm seq th bb et iii ii op no nr".split("\\s+"))
            );
            this.analyzer = analyzer;
        }

        @Override
        public String nextSentence() {

            String sent = null;
            if (sents.size() > 0) {
                sent = sents.remove(0);
            }
            while (sents.size() == 0 && docId > -1) {
                try {
                    nextDoc();
                } catch (IOException e) {
                    LOG.warn("ioexception on next doc", e);
                    throw new RuntimeException(e);
                }
            }
            if (sents.size() == 0) {
                sent = "";
            } else {
                sent = sents.remove(0);
            }
            return sent;
        }

        private void nextDoc() throws IOException {
            if (liveDocs != null) {
                while (!liveDocs.get(docId++)) {
                }
            } else {
                docId++;
            }
            if (docId >= maxDoc) {
                docId = -1;
                return;
            }
            LOG.info("working on "+docId);
            Document d = reader.document(docId, fieldsToLoad);
            String content = d.get(field);
            if (content == null) {
                return;
            }
            fillSents(content);
        }

        private void fillSents(String content) throws IOException {
            String[] paragraphs = content.split("[\r\n]+");
            for (String paragraph : paragraphs) {
                sentMatcher.reset(paragraph);
                addParagraph(paragraph);
            }
        }

        private void addParagraph(String paragraph) throws IOException {
            sentMatcher.reset(paragraph);
            int last = 0;
            while (sentMatcher.find()) {
                String w = sentMatcher.group(1);
                w = w.toLowerCase(Locale.US);
                if (commonAbbrevs.contains(w)) {
                    continue;
                } else {
                    String sent = paragraph.substring(last, sentMatcher.end(1));
                    sents.addAll(cleanSent(sent));
                    last = sentMatcher.end(2);
                }
            }
            if (last < paragraph.length()) {
                String lastSent = paragraph.substring(last);
                if (!lastSent.trim().isEmpty()) {
                    sents.addAll(cleanSent(lastSent));
                }
            }
        }

        private List<String> cleanSent(String sent) throws IOException {

            TokenStream ts = analyzer.tokenStream(field, sent);
            StringBuilder sb = new StringBuilder();
            List<String> ret = new ArrayList<>();
            try {
                ts.reset();
            CharTermAttribute charTermAttribute = ts.getAttribute(CharTermAttribute.class);
            int tokens = 0;
            while (ts.incrementToken()) {
                if (tokens > MAX_TOKENS_PER_SENT) {
                    ret.add(sb.toString().trim());
                    sb.setLength(0);
                    tokens = 0;
                }
                sb.append(charTermAttribute.toString());
                sb.append(" ");
                tokens++;
            }
            } finally {
                ts.close();
                ts.end();
            }
            String last = sb.toString().trim();
            if (! last.isEmpty()) {
                ret.add(last);
            }
            return ret;
        }

        @Override
        public boolean hasNext() {
            return docId > -1 || sents.size() > 0;
        }

        @Override
        public void reset() {
            docId = 0;
        }

        @Override
        public void finish() {

        }

        @Override
        public SentencePreProcessor getPreProcessor() {
            return null;
        }

        @Override
        public void setPreProcessor(SentencePreProcessor sentencePreProcessor) {

        }
    }
}

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
package org.rhapsode.lucene.schema;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.KeywordTokenizerFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.util.AbstractAnalysisFactory;
import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.MultiTermAwareComponent;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.analysis.util.TokenizerFactory;

/**
 * Important components stolen directly from Solr
 */
abstract class AnalyzingFieldDefBase {

    final static String INDEX_ANALYZER = "index_analyzer";
    final static String QUERY_ANALYZER = "query_analyzer";
    final static String MTQUERY_ANALYZER = "mt_query_analyzer";
    final static String OFFSET_ANALYZER = "offset_analyzer";

    private Analyzer indexAnalyzer;
    private Analyzer queryAnalyzer;
    private Analyzer mtQueryAnalyzer;
    private Analyzer offsetAnalyzer;

    private String indexAnalyzerName;
    private String queryAnalyzerName;
    private String mtQueryAnalyzerName;
    private String offsetAnalyzerName;

    public AnalyzingFieldDefBase() {
    }

    public void setAnalyzers(NamedAnalyzer namedIndexAnalyzer,
                             NamedAnalyzer namedQueryAnalyzer,
                             NamedAnalyzer namedMTQueryAnalyzer,
                             NamedAnalyzer namedOffsetAnalyzer) throws IOException {
        this.indexAnalyzer = (namedIndexAnalyzer != null) ? namedIndexAnalyzer.analyzer : null;
        this.queryAnalyzer = (namedQueryAnalyzer != null) ? namedQueryAnalyzer.analyzer : this.indexAnalyzer;
        this.mtQueryAnalyzer = (namedMTQueryAnalyzer != null) ? namedMTQueryAnalyzer.analyzer : constructMultiTermAnalyzer(this.queryAnalyzer);
        this.offsetAnalyzer = (namedOffsetAnalyzer != null) ? namedOffsetAnalyzer.analyzer : this.indexAnalyzer;
        indexAnalyzerName = (namedIndexAnalyzer != null) ? namedIndexAnalyzer.name : null;
        queryAnalyzerName = (namedQueryAnalyzer != null) ? namedQueryAnalyzer.name : null;
        mtQueryAnalyzerName = (namedMTQueryAnalyzer != null) ? namedMTQueryAnalyzer.name : null;
        offsetAnalyzerName = (namedOffsetAnalyzer != null) ? namedOffsetAnalyzer.name : null;
    }

    public Analyzer getIndexAnalyzer() {
        return indexAnalyzer;
    }

    public Analyzer getQueryAnalyzer() {
        return queryAnalyzer;
    }

    public Analyzer getMultitermQueryAnalyzer() {
        return mtQueryAnalyzer;
    }

    public Analyzer getOffsetAnalyzer() {
        return offsetAnalyzer;
    }

    public String getIndexAnalyzerName() {
        return indexAnalyzerName;
    }

    public String getQueryAnalyzerName() {
        return queryAnalyzerName;
    }

    public String getMtQueryAnalyzerName() {
        return mtQueryAnalyzerName;
    }

    public String getOffsetAnalyzerName() {
        return offsetAnalyzerName;
    }

    private Analyzer constructMultiTermAnalyzer(Analyzer queryAnalyzer) throws IOException {
        if (queryAnalyzer == null) return null;

        if (!(queryAnalyzer instanceof CustomAnalyzer)) {
            return new KeywordAnalyzer();
        }

        CustomAnalyzer tc = (CustomAnalyzer) queryAnalyzer;
        CustomAnalyzer.Builder mtAwareBuilder = CustomAnalyzer.builder();

        for (CharFilterFactory factory : tc.getCharFilterFactories()) {
            mtAwareBuilder.addCharFilter(factory.getClass(), copyArgs(factory));
        }

        if (tc.getTokenizerFactory() instanceof MultiTermAwareComponent) {
            mtAwareBuilder.withTokenizer(tc.getTokenizerFactory().getClass(), copyArgs(tc.getTokenizerFactory()));
        } else {
            mtAwareBuilder.withTokenizer(KeywordTokenizerFactory.class, copyArgs(null));
        }

        for (TokenFilterFactory fact : tc.getTokenFilterFactories()) {
            mtAwareBuilder.addTokenFilter(fact.getClass(), copyArgs(fact));
        }

        return mtAwareBuilder.build();
    }

    private Map<String, String> copyArgs(AbstractAnalysisFactory factory) {
        if (factory == null) {
            return new HashMap<>();
        }
        return new HashMap<>(factory.getOriginalArgs());
    }

    private static class MultiTermChainBuilder {
        static final KeywordTokenizerFactory keyFactory = new KeywordTokenizerFactory(new HashMap<String, String>());

        ArrayList<CharFilterFactory> charFilters = null;
        ArrayList<TokenFilterFactory> filters = new ArrayList<>(2);
        TokenizerFactory tokenizer = keyFactory;

        public void add(Object current) {
            if (!(current instanceof MultiTermAwareComponent)) return;
            AbstractAnalysisFactory newComponent = ((MultiTermAwareComponent) current).getMultiTermComponent();
            if (newComponent instanceof TokenFilterFactory) {
                if (filters == null) {
                    filters = new ArrayList<>(2);
                }
                filters.add((TokenFilterFactory) newComponent);
            } else if (newComponent instanceof TokenizerFactory) {
                tokenizer = (TokenizerFactory) newComponent;
            } else if (newComponent instanceof CharFilterFactory) {
                if (charFilters == null) {
                    charFilters = new ArrayList<>(1);
                }
                charFilters.add((CharFilterFactory) newComponent);

            } else {
                throw new IllegalArgumentException("Unknown analysis component from MultiTermAwareComponent: " + newComponent);
            }
        }

        public CustomAnalyzer build() throws IOException {
            CustomAnalyzer.Builder builder = CustomAnalyzer.builder();
            for (CharFilterFactory charFilterFactory : charFilters) {
                builder.addCharFilter(charFilterFactory.getClass(), charFilterFactory.getOriginalArgs());
            }
            builder.withTokenizer(tokenizer.getClass(), tokenizer.getOriginalArgs());
            for (TokenFilterFactory tokenFilterFactory : filters) {
                builder.addTokenFilter(tokenFilterFactory.getClass(), tokenFilterFactory.getOriginalArgs());
            }
            return builder.build();
        }
    }

}

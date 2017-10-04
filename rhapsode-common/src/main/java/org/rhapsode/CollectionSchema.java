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

package org.rhapsode;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

class CollectionSchema {
    private String docIdField;
    File origDocsRoot;
    Set<String> ignoreds = new HashSet<>();
    Set<String> favorites = new HashSet<>();


    CollectionSchema(String docIdField, Path origDocsRoot) {
        this.origDocsRoot = origDocsRoot.toFile();
        this.docIdField = docIdField;
    }

    File getOrigDocsRoot() {
        return origDocsRoot;
    }

    void setOrigDocsRoot(File origDocsRoot) {
        this.origDocsRoot = origDocsRoot;
    }


    void addIgnoreds(Set<String> vs) {
        if (ignoreds == null) {
            ignoreds = new HashSet<>();
        }

        for (String s : vs) {
            this.ignoreds.add(s);
        }

    }

    void addFavorites(Set<String> vs) {
        if (favorites == null) {
            favorites = new HashSet<>();
        }
        for (String s : vs) {
            this.favorites.add(s);
        }
    }

    //will return null if ignoreds is null or empty;
    //this builds a SHOULD query!!!  Need to modify as MUST_NOT at top!
    Query buildIgnoredQuery() {
        if (ignoreds == null || ignoreds.size() == 0) {
            return null;

        }
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        for (String s : ignoreds) {
            builder.add(new TermQuery(new Term(docIdField, s)),
                    BooleanClause.Occur.SHOULD);
        }
        return builder.build();
    }


    Query buildFavoritesQuery() {
        if (favorites == null || favorites.size() == 0) {
            //return empty query
            return new BooleanQuery.Builder().build();
        }
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        for (String s : favorites) {
            builder.add(new TermQuery(new Term(docIdField, s)),
                    BooleanClause.Occur.SHOULD);
        }
        return builder.build();
    }

    public void setDocIdField(String docIdField) {
        this.docIdField = docIdField;
    }
}

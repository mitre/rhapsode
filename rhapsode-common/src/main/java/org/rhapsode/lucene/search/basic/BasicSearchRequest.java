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

import org.apache.lucene.search.Sort;
import org.rhapsode.lucene.search.BaseSearchRequest;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class BasicSearchRequest extends BaseSearchRequest {
    private final BasicSearchConfig config;
    private int lastEnd = -1;
    private int resultsPerPage;
    private int lastStart = -1;
    private PagingDirection pagingDirection;
    private Sort sort;


    public BasicSearchRequest(BasicSearchConfig config) {
        this.config = config;
    }

    public int getResultsPerPage() {
        return resultsPerPage;
    }

    public int getLastEnd() {
        return lastEnd;
    }

    public int getLastStart() {
        return lastStart;
    }

    public PagingDirection getPagingDirection() {
        return pagingDirection;
    }

    public int getSnippetsPerResult() {
        return config.getSnippetsPerResult();
    }

    public int getMaxCharsToReadForSnippets() {
        return config.getMaxCharsToReadForSnippets();
    }

    public int getFragmentSize() {
        return config.getFragmentSize();
    }

    public int getMaxSnippetLengthChars() {
        return config.getMaxSnippetLengthChars();
    }

    public boolean shouldGetSnippets() {
        return config.getMaxSnippetLengthChars() > 0;
    }

    public Sort getSort() {
        return sort;
    }

    @Override
    public Set<String> getFields() {
        Set<String> fields = new HashSet<>(super.getFields());
        if (config.getMaxSnippetLengthChars() > 0) {
            fields.add(getContentField());
        }
        return Collections.unmodifiableSet(fields);
    }

    public void setPagingDirection(PagingDirection pagingDirection) {
        this.pagingDirection = pagingDirection;
    }

    public void setStartAndEndResult(int start, int end) {
        lastStart = start;
        lastEnd = end;
    }

    public void setResultsPerPage(int resultsPerPage) {
        this.resultsPerPage = resultsPerPage;
    }


}

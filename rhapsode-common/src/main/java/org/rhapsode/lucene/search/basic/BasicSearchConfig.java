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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.lucene.search.highlight.Highlighter;
import org.rhapsode.lucene.utils.JsonUtil;
import org.rhapsode.util.LanguageDirection;

/**
 * Unchanging parameters for basic search with docMetadataExtractor
 */
public class BasicSearchConfig {

    private int fragmentSize = 200;
    private int maxCharsToReadForSnippets = Highlighter.DEFAULT_MAX_CHARS_TO_ANALYZE;
    private int maxLinkDisplaySizeChars = 32;
    private int maxSnippetLengthChars = 100;
    private int snippetsPerResult = 3;
    private LanguageDirection defaultLanguageDirection = LanguageDirection.LTR;

    public static BasicSearchConfig build(JsonElement el) {
        BasicSearchConfig config = new BasicSearchConfig();
        config.fragmentSize = JsonUtil.getInt(el, "fragmentSize");
        config.maxCharsToReadForSnippets = JsonUtil.getInt(el, "maxCharsToReadForSnippets");
        config.maxLinkDisplaySizeChars = JsonUtil.getInt(el, "maxLinkDisplaySizeChars");
        config.maxSnippetLengthChars = JsonUtil.getInt(el, "maxSnippetLengthChars");
        config.snippetsPerResult = JsonUtil.getInt(el, "snippetsPerResult");
        JsonElement ldEl = ((JsonObject) el).get("defaultLanguageDirection");
        if (ldEl != null) {
            String ldString = ldEl.toString();
            if (ldString.equalsIgnoreCase("rtl")) {
                config.defaultLanguageDirection = LanguageDirection.RTL;
            }
        }

        return config;
    }


    protected BasicSearchConfig() {
    }

    ;

    public int getFragmentSize() {
        return fragmentSize;
    }

    protected void setFragmentSize(int fragmentSize) {
        this.fragmentSize = fragmentSize;
    }

    public int getMaxCharsToReadForSnippets() {
        return maxCharsToReadForSnippets;
    }

    protected void setMaxCharsToReadForSnippets(int maxCharsToReadForSnippets) {
        this.maxCharsToReadForSnippets = maxCharsToReadForSnippets;
    }

    public int getMaxLinkDisplaySizeChars() {
        return maxLinkDisplaySizeChars;
    }

    protected void setMaxLinkDisplaySizeChars(int maxLinkDisplaySizeChars) {
        this.maxLinkDisplaySizeChars = maxLinkDisplaySizeChars;
    }

    public int getMaxSnippetLengthChars() {
        return maxSnippetLengthChars;
    }

    protected void setMaxSnippetLengthChars(int maxSnippetLengthChars) {
        this.maxSnippetLengthChars = maxSnippetLengthChars;
    }

    public int getSnippetsPerResult() {
        return snippetsPerResult;
    }

    protected void setSnippetsPerResult(int snippetsPerResult) {
        this.snippetsPerResult = snippetsPerResult;
    }

    public LanguageDirection getDefaultLanguageDirection() {
        return defaultLanguageDirection;
    }

    public BasicSearchConfig deepCopy() {
        BasicSearchConfig ret = new BasicSearchConfig();
        ret.fragmentSize = this.fragmentSize;
        ret.maxCharsToReadForSnippets = this.maxCharsToReadForSnippets;
        ret.maxLinkDisplaySizeChars = this.maxLinkDisplaySizeChars;
        ret.maxSnippetLengthChars = this.maxSnippetLengthChars;
        ret.snippetsPerResult = this.snippetsPerResult;
        ret.defaultLanguageDirection = this.defaultLanguageDirection;
        return ret;
    }
}

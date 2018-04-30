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

package org.rhapsode.app.decorators;


import org.apache.commons.lang3.StringUtils;
import org.apache.tika.io.IOUtils;
import org.rhapsode.app.RhapsodeSearcherApp;
import org.rhapsode.lucene.search.BaseSearchRequest;
import org.xml.sax.SAXException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

public abstract class BaseLinkWriter {

    final RhapsodeSearcherApp searcherApp;

    public BaseLinkWriter(RhapsodeSearcherApp searcherApp) {
        this.searcherApp = searcherApp;
    }


    public abstract void write(BaseSearchRequest request, Map<String, String> metadata,
                               Map<String, String> searcherSpecificAttributes,
                               RhapsodeXHTMLHandler xhtml) throws SAXException;


    void tryToAddQueryParameters(final String name, final String value, final StringBuilder sb) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        try {
            sb.append(String.format("&%s=%s", name, URLEncoder.encode(value, IOUtils.UTF_8.toString())));
        } catch (UnsupportedEncodingException e) {
            //this should never happen; swallow
        }
    }

    void tryToAddQueryParameters(Map<String, String> searcherSpecificAttributes, StringBuilder sb) {
        for (Map.Entry<String, String> e : searcherSpecificAttributes.entrySet()) {
            tryToAddQueryParameters(e.getKey(), e.getValue(), sb);
        }
    }


    String getDisplayName(Map<String, String> metadata) {
        String displayName = metadata.get(
                searcherApp.getRhapsodeCollection().getIndexSchema().getLinkDisplayField());
        if (displayName == null) {
            throw new RuntimeException("Metadata from document doesn't contain a value for the display field:" +
                    searcherApp.getRhapsodeCollection().getIndexSchema().getLinkDisplayField());
        }
        return displayName;
    }

    String getDocKey(Map<String, String> metadata) {
        String key = metadata.get(searcherApp.getRhapsodeCollection().getIndexSchema().getUniqueDocField());
        if (key == null) {
            throw new IllegalArgumentException("Metadata from document doesn't contain a value for the key field:" +
                    searcherApp.getRhapsodeCollection().getIndexSchema().getUniqueDocField());
        }
        return key;
    }
}

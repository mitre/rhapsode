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

import java.util.Locale;
import java.util.Map;

import org.rhapsode.app.RhapsodeSearcherApp;
import org.rhapsode.app.contants.C;
import org.rhapsode.app.contants.CSS;
import org.rhapsode.app.contants.H;
import org.rhapsode.lucene.search.BaseSearchRequest;
import org.rhapsode.lucene.search.StoredQuery;
import org.xml.sax.SAXException;

public class HTMLHighlighterLinkWriter extends BaseLinkWriter {

    public HTMLHighlighterLinkWriter(RhapsodeSearcherApp config) {
        super(config);
    }


    @Override
    public void write(BaseSearchRequest request, Map<String, String> metadata,
                      Map<String, String> searcherSpecificAttributes,
                      RhapsodeXHTMLHandler xhtml) throws SAXException {

        String key = getDocKey(metadata);
        String displayName = getDisplayName(metadata);
        StringBuilder sb = new StringBuilder();
        StoredQuery storedQuery = request.getComplexQuery().getStoredQuery();
        tryToAddQueryParameters(searcherSpecificAttributes, sb);
        tryToAddQueryParameters(C.DOC_KEY, key, sb);
        tryToAddQueryParameters(C.MAIN_QUERY,
                storedQuery.getMainQueryString(), sb);
        tryToAddQueryParameters(C.FILTER_QUERY,
                storedQuery.getFilterQueryString(), sb);
        tryToAddQueryParameters(C.GEO_QUERY,
                storedQuery.getGeoQueryString(), sb);
        tryToAddQueryParameters(C.GEO_RADIUS,
                storedQuery.getGeoQueryRadiusString(), sb);
        tryToAddQueryParameters(C.LANG_DIR,
                request.getLanguageDirection().toString().toLowerCase(Locale.ENGLISH), sb);
        tryToAddQueryParameters(C.DEFAULT_QUERY_FIELD,
                request.getContentField(), sb);

        xhtml.startElement(H.A,
                H.CLASS, CSS.DOC_LINK,
                H.HREF, "/rhapsode/view_doc?" + sb.toString()
        );
        xhtml.characters(displayName);
        xhtml.endElement(H.A);
    }

}
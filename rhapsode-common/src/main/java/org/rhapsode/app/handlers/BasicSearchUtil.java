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

package org.rhapsode.app.handlers;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.classic.ParseException;
import org.rhapsode.RhapsodeCollection;
import org.rhapsode.app.RhapsodeSearcherApp;
import org.rhapsode.app.contants.H;
import org.rhapsode.app.decorators.IndexedDocURLBuilder;
import org.rhapsode.app.decorators.RhapsodeDecorator;
import org.rhapsode.app.decorators.RhapsodeXHTMLHandler;
import org.rhapsode.app.decorators.SnippetWriter;
import org.rhapsode.app.handlers.admin.CollectionNotLoadedException;
import org.rhapsode.app.session.DynamicParameters;
import org.rhapsode.lucene.search.IndexManager;
import org.rhapsode.lucene.search.basic.BasicSearchRequest;
import org.rhapsode.lucene.search.basic.BasicSearchResult;
import org.rhapsode.lucene.search.basic.BasicSearchResults;
import org.rhapsode.lucene.search.basic.BasicSearcher;
import org.xml.sax.SAXException;

public class BasicSearchUtil {

    public static BasicSearchResults executeSearch(RhapsodeSearcherApp searcherApp,
                                                   BasicSearchRequest basicSearchRequest) throws ParseException, IOException {

        IndexManager indexManager = null;
        BasicSearcher basicSearcher = null;
        RhapsodeCollection rc = searcherApp.getRhapsodeCollection();
        if (rc == null) {
            throw new CollectionNotLoadedException("Collection not loaded");
        } else {
            indexManager = rc.getIndexManager();
            //check for null or index?
            basicSearcher = new BasicSearcher(indexManager, rc.getIndexSchema());
        }

        if (basicSearchRequest.hasQuery()) {
            return basicSearcher.search(basicSearchRequest);
        }
        //return empty results
        return new BasicSearchResults();
    }

    public static void writeBasicResults(RhapsodeSearcherApp searcherApp, BasicSearchRequest request,
                                         BasicSearchResults results, RhapsodeXHTMLHandler xhtml) throws SAXException {

        IndexedDocURLBuilder indexedDocURLBuilder = new IndexedDocURLBuilder(searcherApp);

        xhtml.startElement(H.TABLE, H.BORDER, "2");
        //write result summary
        for (BasicSearchResult r : results.getResults()) {
            xhtml.startElement(H.TR);
            xhtml.element(H.TD, Integer.toString(r.getN()) + ".");
            addDocLink(searcherApp, indexedDocURLBuilder, request, r, xhtml);
            String docKey = r.getMetadata().get(searcherApp.getRhapsodeCollection().getIndexSchema().getUniqueDocField());

            RhapsodeDecorator.writeSelectedDocIdTD(searcherApp.getSessionManager().getDynamicParameterConfig().getBoolean(DynamicParameters.SHOW_SELECTED),
                    request, docKey, xhtml);

            //TODO: addDate(r.getMetadata(), xhtml);
            for (String rowResultColumn : searcherApp
                    .getSessionManager()
                    .getDynamicParameterConfig()
                    .getStringList(DynamicParameters.ROW_VIEWER_DISPLAY_FIELDS)) {
                String val = r.getMetadata().get(rowResultColumn);
                if (StringUtils.isBlank(val)) {
                    val = " ";
                }
                int maxColLen = searcherApp
                        .getSessionManager()
                        .getDynamicParameterConfig()
                        .getInt(DynamicParameters.MAX_EXTRA_COLUMNS_LEN);
                if (val.length() > maxColLen) {
                    val = val.substring(0, maxColLen);
                }
                xhtml.element(H.TD, val);
            }

            xhtml.startElement(H.TD);
            SnippetWriter.write(r.getSnippet(), xhtml);
            xhtml.endElement(H.TD);
            //xhtml.element(H.TD, r.getSnippet());
            xhtml.endElement(H.TR);
        }
        xhtml.endElement(H.TABLE);
    }


    private static void addDocLink(RhapsodeSearcherApp searcherApp, IndexedDocURLBuilder indexedDocURLBuilder, BasicSearchRequest request,
                                   BasicSearchResult result, RhapsodeXHTMLHandler xhtml) throws SAXException {
        String displayName = result.getMetadata()
                .get(
                        searcherApp.getRhapsodeCollection().getIndexSchema().getLinkDisplayField());
        int maxLen = searcherApp.getSessionManager().getDynamicParameterConfig().getInt(DynamicParameters.MAX_FILE_NAME_DISPLAY_LENGTH);
        if (maxLen > -1 && displayName != null && displayName.length() > maxLen) {
            displayName = displayName.substring(0, maxLen);
        }
        String docKey = result.getMetadata().get(searcherApp.getRhapsodeCollection().getIndexSchema().getUniqueDocField());
        xhtml.startElement(H.TD);
        String url = indexedDocURLBuilder.getURL(
                docKey,
                Integer.toString(result.getLuceneDocId()),
                Integer.toString(result.getN()),
                request);
        xhtml.href(url, displayName);
        xhtml.endElement(H.TD);
    }

}

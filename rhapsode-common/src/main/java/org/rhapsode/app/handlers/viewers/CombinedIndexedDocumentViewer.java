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

package org.rhapsode.app.handlers.viewers;

import org.eclipse.jetty.server.Request;
import org.rhapsode.app.config.RhapsodeSearcherApp;
import org.rhapsode.app.decorators.IndexedDocURLBuilder;
import org.rhapsode.app.handlers.search.AbstractSearchHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class CombinedIndexedDocumentViewer extends AbstractSearchHandler {

    private static final Logger LOG = LoggerFactory.getLogger(CombinedIndexedDocumentViewer.class);

    private final static int UNSPECIFIED_FIELD_LENGTH = 1000;
    final RhapsodeSearcherApp searcherApp;

    private final IndexedDocURLBuilder indexedDocURLBuilder;

    public CombinedIndexedDocumentViewer(RhapsodeSearcherApp searchConfig) {
        super("Combined Indexed Document Viewer");
        this.searcherApp = searchConfig;
        indexedDocURLBuilder = new IndexedDocURLBuilder(searchConfig);
    }

    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest,
                       HttpServletResponse response) throws IOException, ServletException {

        try {
            //execute(httpServletRequest, response);
        } catch (Exception e) {
            LOG.error("couldn't handle", e);
            throw new RuntimeException(e);
        }
    }/*
    private void execute(HttpServletRequest httpServletRequest,
                         HttpServletResponse response) throws IOException, SAXException{
        RhapsodeXHTMLHandler xhtml = null;
        try {
            xhtml = initResponse(response,
                    RhapsodeDecorator.generateStyleString(searcherApp.getCommonSearchConfig().getHighlightingStyles()));
        } catch (SAXException e) {
            throw new IOException(e);
        }
        BaseSearchRequest baseRequest = new BaseSearchRequest();
        BaseRequestBuilder baseRequestBuilder = new BaseRequestBuilder();
        Collection<ComplexQuery> complexQueries = new ArrayList<>();
        try {
            baseRequestBuilder.extract(searcherApp, httpServletRequest, baseRequest);
            String storedQueryNames = httpServletRequest.getParameter(C.STORED_QUERY_IDS);
            if (storedQueryNames != null) {
                complexQueries = ComplexQueryUtils.parseAllStoredQueries(storedQueryNames, searcherApp).values();
            } else {
                complexQueries.add(baseRequest.getComplexQuery());
            }
        } catch (ParseException e) {
            logger.warn(e);
            throw new RuntimeException(e);
        }

        String docKey = httpServletRequest.getParameter(C.DOC_KEY);
        if (docKey == null) {
            throw new IllegalArgumentException("Must specify doc key in request: "+C.DOC_KEY);
        }
        int rank = ParamUtil.getInt(httpServletRequest.getParameter(C.RANK), -1);

        int docId = ParamUtil.getInt(httpServletRequest.getParameter(C.LUCENE_DOC_ID), -1);
        DocHighlighter highlighter = new DocHighlighter();
        IndexSchema schema = searcherApp.getRhapsodeCollection().getIndexSchema();
        highlighter.setTableClass(CSS.HIGHLIGHTED);
        SortField sortField = new SortedNumericSortField(schema.getAttachmentSortField(), SortField.Type.INT);
        sortField.setMissingValue(0);
        Sort sort = new Sort(sortField);


        List<Document> docs = DocRetriever.getAllDocsFromAnyDocId(
                docKey,
                schema.getUniqueDocField(),
                schema.getUniqueFileField(),
                sort,
                getFields(searcherApp),
                searcherApp.getRhapsodeCollection().getIndexManager().getSearcher()
        );

        try {
            writeInnerHeader(Integer.toString(rank), xhtml);

            xhtml.br();
            AtomicBoolean alreadyHighlighted = new AtomicBoolean(false);
            highlighter.highlightDocs(baseRequest.getContentField(),
                    schema.getEmbeddedPathField(),
                    searcherApp.getLuceneDocViewerConfig().getListOfFieldsToDisplay(),
                    docs,
                    complexQueries,
                    searcherApp.getRhapsodeCollection().getIndexManager().getSchema().getOffsetAnalyzer(),
                    alreadyHighlighted,
                    xhtml);
            RhapsodeDecorator.addFooter(xhtml);
            xhtml.endDocument();

        } catch (SAXException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
        response.flushBuffer();
    }


    private Set<String> getFields(RhapsodeSearcherApp config) {
        Set<String> fields = new HashSet<>();
        for (String k : config.getLuceneDocViewerConfig().getFieldsToDisplay().keySet()) {
            fields.add(k);
        }
        fields.add(config.getCommonSearchConfig().getDefaultSearchField());
        fields.add(config.getRhapsodeCollection().getIndexSchema().getRelPathField());
        fields.add(config.getRhapsodeCollection().getIndexSchema().getUniqueDocField());
        fields.add(config.getRhapsodeCollection().getIndexSchema().getAttachmentIndexField());
        return fields;
    }

    private void writeInnerHeader(String rankString, RhapsodeXHTMLHandler xhtml) throws SAXException {
        xhtml.br();
        xhtml.characters("RANK: "+rankString);
        xhtml.br();
    }
*/
}

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

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.eclipse.jetty.server.Request;
import org.rhapsode.app.config.RhapsodeSearcherApp;
import org.rhapsode.app.contants.C;
import org.rhapsode.app.contants.CSS;
import org.rhapsode.app.decorators.IndexedDocURLBuilder;
import org.rhapsode.app.decorators.RhapsodeDecorator;
import org.rhapsode.app.decorators.RhapsodeXHTMLHandler;
import org.rhapsode.app.handlers.search.AbstractSearchHandler;
import org.rhapsode.app.handlers.search.BaseRequestBuilder;
import org.rhapsode.app.session.DynamicParameters;
import org.rhapsode.app.utils.ComplexQueryUtils;
import org.rhapsode.app.utils.DocHighlighter;
import org.rhapsode.lucene.search.BaseSearchRequest;
import org.rhapsode.lucene.search.ComplexQuery;
import org.rhapsode.lucene.utils.DocRetriever;
import org.rhapsode.util.ParamUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;


public class IndexedDocumentsViewer extends AbstractSearchHandler {

    private static final Logger LOG = LoggerFactory.getLogger(IndexedDocumentsViewer.class);

    private final static int UNSPECIFIED_FIELD_LENGTH = 1000;
    final RhapsodeSearcherApp searcherApp;

    private final IndexedDocURLBuilder indexedDocURLBuilder;

    public IndexedDocumentsViewer(RhapsodeSearcherApp searchConfig) {
        super("Indexed Documents Viewer");
        this.searcherApp = searchConfig;
        indexedDocURLBuilder = new IndexedDocURLBuilder(searchConfig);
    }

    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest,
                       HttpServletResponse response) throws IOException, ServletException {

        try {
            execute(httpServletRequest, response);
        } catch (Exception e) {
            LOG.error("problem handling", e);
            throw new RuntimeException(e);
        }
    }

    private void execute(HttpServletRequest httpServletRequest,
                         HttpServletResponse response) throws IOException, SAXException {
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
            baseRequestBuilder.extractBase(searcherApp, httpServletRequest, baseRequest);
            String storedQueryNames = httpServletRequest.getParameter(C.STORED_QUERY_IDS);
            if (storedQueryNames != null) {
                complexQueries = ComplexQueryUtils.parseAllStoredQueries(storedQueryNames, searcherApp).values();
            } else {
                baseRequestBuilder.parse(searcherApp, baseRequest);
                complexQueries.add(baseRequest.getComplexQuery());
            }
        } catch (ParseException e) {
            LOG.warn("problem parsing", e);
            throw new RuntimeException(e);
        }

        int attachmentOffset = 0;
        String attachmentOffsetString = httpServletRequest.getParameter(C.ATTACHMENT_OFFSET);
        if (attachmentOffsetString != null) {
            try {
                attachmentOffset = Integer.parseInt(attachmentOffsetString);
            } catch (NumberFormatException e) {
            }
        }

        final String docId = httpServletRequest.getParameter(C.DOC_KEY);
        final String fileId = httpServletRequest.getParameter(C.FILE_KEY);
        if (fileId == null && docId == null) {
            throw new IllegalArgumentException("Must specify doc key: " + C.DOC_KEY + " or file key: " + C.FILE_KEY);
        }
        int rank = ParamUtil.getInt(httpServletRequest.getParameter(C.RANK), -1);

        DocHighlighter highlighter = new DocHighlighter();
        highlighter.setTableClass(CSS.HIGHLIGHTED);
        highlighter.setTDClass(CSS.HIGHLIGHTED);

        SortField sortField = new SortedNumericSortField(
                searcherApp.getRhapsodeCollection().getIndexSchema().getAttachmentSortField(),
                SortField.Type.INT);
        sortField.setMissingValue(0);
        Sort sort = new Sort(sortField);

        List<Document> docs;

        if (fileId != null) {
            docs = DocRetriever.getAllDocsByFileId(
                    searcherApp.getRhapsodeCollection().getIndexSchema().getUniqueFileField(),
                    fileId, sort, getFields(searcherApp),
                    searcherApp.getRhapsodeCollection().getIndexManager().getSearcher());
        } else {
            docs = DocRetriever.getAllDocsFromAnyDocId(docId,
                    searcherApp.getRhapsodeCollection().getIndexSchema().getUniqueDocField(),
                    searcherApp.getRhapsodeCollection().getIndexSchema().getUniqueFileField(),
                    sort, getFields(searcherApp),
                    searcherApp.getRhapsodeCollection().getIndexManager().getSearcher());
        }
        try {
            writeInnerHeader(Integer.toString(rank), xhtml);

            xhtml.br();
            AtomicBoolean alreadyHighlighted = new AtomicBoolean(false);
            highlighter.highlightDocs(baseRequest.getContentField(),
                    searcherApp.getRhapsodeCollection().getIndexSchema().getEmbeddedPathField(),
                    searcherApp.getSessionManager().getDynamicParameterConfig().getStringList(DynamicParameters.FILE_VIEWER_DISPLAY_FIELDS),
                    docs,
                    complexQueries,
                    searcherApp.getRhapsodeCollection().getIndexSchema().getOffsetAnalyzer(),
                    alreadyHighlighted,
                    attachmentOffset,
                    xhtml);
            RhapsodeDecorator.addFooter(xhtml);
            xhtml.endDocument();

        } catch (SAXException e) {
            LOG.error("problem writing", e);
            throw new RuntimeException(e);
        }
        response.flushBuffer();
    }

    private Set<String> getFields(RhapsodeSearcherApp searcherApp) {
        Set<String> fields = new HashSet<>();
        for (String k :
                searcherApp
                        .getSessionManager()
                        .getDynamicParameterConfig()
                        .getStringList(DynamicParameters.FILE_VIEWER_DISPLAY_FIELDS)) {
            fields.add(k);
        }
        fields.add(searcherApp.getStringParameter(DynamicParameters.DEFAULT_CONTENT_FIELD));
        fields.add(searcherApp.getRhapsodeCollection().getIndexSchema().getRelPathField());
        fields.add(searcherApp.getRhapsodeCollection().getIndexSchema().getUniqueDocField());
        fields.add(searcherApp.getRhapsodeCollection().getIndexSchema().getAttachmentIndexField());
        return fields;
    }

    private void writeInnerHeader(String rankString, RhapsodeXHTMLHandler xhtml) throws SAXException {
        if (rankString != null && !rankString.equals("-1")) {
            xhtml.br();
            xhtml.characters("RANK: " + rankString);
            xhtml.br();
        }
    }

}

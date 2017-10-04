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

import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.eclipse.jetty.server.Request;
import org.rhapsode.app.config.RhapsodeSearcherApp;
import org.rhapsode.app.contants.C;
import org.rhapsode.app.contants.CSS;
import org.rhapsode.app.contants.H;
import org.rhapsode.app.decorators.ExtractFileURLBuilder;
import org.rhapsode.app.decorators.IndexedDocURLBuilder;
import org.rhapsode.app.decorators.IndexedDocsURLBuilder;
import org.rhapsode.app.decorators.RawFileURLBuilder;
import org.rhapsode.app.decorators.RhapsodeDecorator;
import org.rhapsode.app.decorators.RhapsodeXHTMLHandler;
import org.rhapsode.app.handlers.search.AbstractSearchHandler;
import org.rhapsode.app.handlers.search.BaseRequestBuilder;
import org.rhapsode.app.session.DynamicParameters;
import org.rhapsode.app.utils.ComplexQueryUtils;
import org.rhapsode.app.utils.DocHighlighter;
import org.rhapsode.lucene.search.BaseSearchRequest;
import org.rhapsode.lucene.search.ComplexQuery;
import org.rhapsode.lucene.search.DocLuceneIdPair;
import org.rhapsode.lucene.search.SearcherUtils;
import org.rhapsode.util.ParamUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;


public class IndividualIndexedDocumentViewer extends AbstractSearchHandler {

    private static final Logger LOG = LoggerFactory.getLogger(IndividualIndexedDocumentViewer.class);

    private final static int UNSPECIFIED_FIELD_LENGTH = 1000;
    final RhapsodeSearcherApp searcherApp;

    private final IndexedDocURLBuilder indexedDocURLBuilder;

    public IndividualIndexedDocumentViewer(RhapsodeSearcherApp searchConfig) {
        super("Indexed Document Viewer");
        this.searcherApp = searchConfig;
        indexedDocURLBuilder = new IndexedDocURLBuilder(searchConfig);
    }

    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest,
                       HttpServletResponse response) throws IOException, ServletException {

        try {
            execute(httpServletRequest, response);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("problem executing", e);
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
            LOG.warn("parser exception", e);
            throw new RuntimeException(e);
        }

        String docKey = httpServletRequest.getParameter(C.DOC_KEY);
        if (docKey == null) {
            throw new IllegalArgumentException("Must specify doc key in request: " + C.DOC_KEY);
        }
        int rank = ParamUtil.getInt(httpServletRequest.getParameter(C.RANK), -1);

        int docId = ParamUtil.getInt(httpServletRequest.getParameter(C.LUCENE_DOC_ID), -1);
        DocHighlighter highlighter = new DocHighlighter();
        highlighter.setTableClass(CSS.HIGHLIGHTED);
        highlighter.setTDClass(CSS.HIGHLIGHTED);

        Document doc = SearcherUtils.getUniqueDocument(searcherApp.getRhapsodeCollection().getIndexManager().getSearcher(), docId,
                searcherApp.getRhapsodeCollection().getIndexSchema().getUniqueDocField(), docKey, getFields(searcherApp));

        try {
            writeInnerHeader(Integer.toString(rank), xhtml);

            writePrevNextLinks(baseRequest, doc,
                    rank, docId, docKey, xhtml);
            xhtml.br();
            AtomicBoolean alreadyHighlighted = new AtomicBoolean(false);
            highlighter.highlightDoc(baseRequest.getContentField(),
                    searcherApp
                            .getSessionManager()
                            .getDynamicParameterConfig()
                            .getStringList(DynamicParameters.FILE_VIEWER_DISPLAY_FIELDS),
                    doc,
                    complexQueries,
                    searcherApp.getRhapsodeCollection().getIndexSchema().getOffsetAnalyzer(),
                    alreadyHighlighted,
                    xhtml);
            RhapsodeDecorator.addFooter(xhtml);
            xhtml.endDocument();

        } catch (Exception e) {
            LOG.error("problem", e);
            throw new RuntimeException(e);
        }
        response.flushBuffer();
    }


    private void writePrevNextLinks(BaseSearchRequest request, Document doc,
                                    int rank, int docId,
                                    String docKey, RhapsodeXHTMLHandler xhtml) throws IOException, SAXException {
        if (rank < 0) {
            return;
        }
        Set<String> fields = new HashSet<>();
        String uniqueDocField = searcherApp.getRhapsodeCollection().getIndexSchema().getUniqueDocField();
        String linkDisplayField = searcherApp.getRhapsodeCollection().getIndexSchema().getLinkDisplayField();
        String relPathField = searcherApp.getRhapsodeCollection().getIndexSchema().getRelPathField();

        String attachmentIndexField = searcherApp.getRhapsodeCollection().getIndexSchema().getAttachmentIndexField();
        String uniqueFileField = searcherApp.getRhapsodeCollection().getIndexSchema().getUniqueFileField();


        fields.add(uniqueDocField);
        fields.add(linkDisplayField);
        fields.add(relPathField);
        fields.add(attachmentIndexField);
        Pair<DocLuceneIdPair, DocLuceneIdPair> pair =
                SearcherUtils.getBeforeAndAfter(searcherApp.getRhapsodeCollection().getIndexManager().getSearcher(),
                        request.getComplexQuery().getRetrievalQuery(),
                        rank, docId,
                        searcherApp.getSessionManager()
                                .getDynamicParameterConfig()
                                .getString(DynamicParameters.DEFAULT_CONTENT_FIELD),
                        docKey, fields);

        String previousURL = getPrePostDocUrl(pair.getLeft(), rank - 1, request);
        String nextURL = getPrePostDocUrl(pair.getRight(), rank + 1, request);
        String originalFileURL = RawFileURLBuilder.build(
                searcherApp.getRhapsodeCollection().getOrigDocsRoot(),
                doc.get(relPathField));
        String fullTikaViewerURL = null;
        if (originalFileURL != null) {
            originalFileURL = URLDecoder.decode(originalFileURL, StandardCharsets.UTF_8.toString());
            fullTikaViewerURL = ExtractFileURLBuilder.build(doc.get(
                    uniqueDocField),
                    doc.get(searcherApp.getRhapsodeCollection().
                            getIndexSchema().getAttachmentIndexField()));
        }
        String indexedDocsURL = null;
        String attachmentOffsetString = doc.get(attachmentIndexField);
        int attachmentOffset = 0;
        try {
            if (attachmentOffsetString != null) {
                attachmentOffset = Integer.parseInt(attachmentOffsetString);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        indexedDocsURL = IndexedDocsURLBuilder.build(doc.get(uniqueFileField),
                attachmentOffset, request);


        xhtml.startElement(H.TABLE,
                H.BORDER, "2");
//                H.CLASS, CSS.DOC_VIEWER_LINKS);
        xhtml.startElement(H.TR);
//                H.CLASS, CSS.DOC_VIEWER_LINKS);
        xhtml.element(H.TD, " ");
        if (originalFileURL != null) {
            xhtml.startElement(H.TD);
            xhtml.href(originalFileURL, "Original File");
            xhtml.endElement(H.TD);
        } else {
            xhtml.element(H.TD, " ");
        }
        xhtml.element(H.TD, " ");
        xhtml.endElement(H.TR);

        xhtml.startElement(H.TR);
        xhtml.startElement(H.TD);//,         H.CLASS, CSS.DOC_VIEWER_LINK_PRE);
        if (previousURL != null) {
            xhtml.href(previousURL, "Previous");
        } else {
            xhtml.characters(" ");
        }
        xhtml.endElement(H.TD);

        xhtml.startElement(H.TD);
        if (indexedDocsURL != null) {
            xhtml.href(indexedDocsURL, "Indexed Document(s)");
        } else {
            xhtml.characters(" ");
        }
        xhtml.endElement(H.TD);

        xhtml.startElement(H.TD);//,         H.CLASS, CSS.DOC_VIEWER_LINK_POST);
        if (nextURL != null) {
            xhtml.href(nextURL, "Next");
        } else {
            xhtml.characters(" ");
        }
        xhtml.endElement(H.TD);
        xhtml.endElement(H.TR);

        xhtml.startElement(H.TR);
        xhtml.element(H.TD, " ");
        if (fullTikaViewerURL != null) {
            xhtml.startElement(H.TD);
            xhtml.href(fullTikaViewerURL, "Full Extract");
            xhtml.endElement(H.TD);
        } else {
            xhtml.characters(" ");
        }
        xhtml.endElement(H.TD);
        xhtml.element(H.TD, " ");
        xhtml.endElement(H.TR);
        xhtml.endElement(H.TABLE);
    }

    private String getPrePostDocUrl(DocLuceneIdPair docLuceneIdPair, int rank, BaseSearchRequest request) {
        if (docLuceneIdPair == null) {
            return null;
        }
        int luceneId = docLuceneIdPair.getLuceneId();
        Document doc = docLuceneIdPair.getDocument();
        if (doc == null) {
            return null;
        }
        String key = doc.get(searcherApp.getRhapsodeCollection().getIndexSchema().getUniqueDocField());
        return indexedDocURLBuilder.getURL(key, Integer.toString(luceneId), Integer.toString(rank), request);
    }

    private Set<String> getFields(RhapsodeSearcherApp config) {
        Set<String> fields = new HashSet<>();
        for (String k : searcherApp
                .getSessionManager()
                .getDynamicParameterConfig()
                .getStringList(DynamicParameters.FILE_VIEWER_DISPLAY_FIELDS)) {
            fields.add(k);
        }
        fields.add(config.getSessionManager().getDynamicParameterConfig().getString(DynamicParameters.DEFAULT_CONTENT_FIELD));
        fields.add(config.getRhapsodeCollection().getIndexSchema().getRelPathField());
        fields.add(config.getRhapsodeCollection().getIndexSchema().getUniqueDocField());
        fields.add(config.getRhapsodeCollection().getIndexSchema().getAttachmentIndexField());
        fields.add(config.getRhapsodeCollection().getIndexSchema().getUniqueFileField());
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

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

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.serialization.JsonMetadataList;
import org.eclipse.jetty.server.Request;
import org.rhapsode.RhapsodeCollection;
import org.rhapsode.app.config.RhapsodeSearcherApp;
import org.rhapsode.app.contants.C;
import org.rhapsode.app.contants.CSS;
import org.rhapsode.app.contants.H;
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
import org.rhapsode.lucene.search.DocLuceneIdPair;
import org.rhapsode.lucene.search.SearcherUtils;
import org.rhapsode.util.ParamUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class ExtractViewer extends AbstractSearchHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ExtractViewer.class);


    private final static String TIKA_CONTENT_KEY = "X-TIKA:content";
    private final static String TIKA_INTERNAL_EMBEDDED_PATH = "X-TIKA:embedded_resource_path";
    private final static int UNSPECIFIED_FIELD_LENGTH = 1000;
    final RhapsodeSearcherApp searcherApp;

    private final IndexedDocURLBuilder indexedDocURLBuilder;

    public ExtractViewer(RhapsodeSearcherApp searchConfig) {
        super("Extract Viewer");
        this.searcherApp = searchConfig;
        indexedDocURLBuilder = new IndexedDocURLBuilder(searchConfig);
    }

    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest,
                       HttpServletResponse response) throws IOException, ServletException {

        RhapsodeXHTMLHandler xhtml = null;
        try {
            xhtml = initResponse(response, null);
        } catch (SAXException e) {
            throw new IOException(e);
        }

        //TODO: clean up this abomination
        BaseSearchRequest baseRequest = new BaseSearchRequest();
        BaseRequestBuilder baseRequestBuilder = new BaseRequestBuilder();
        Collection<ComplexQuery> complexQueries = new ArrayList<>();
        try {
            baseRequestBuilder.extractBase(searcherApp, httpServletRequest, baseRequest);
            String storedQueryNames = httpServletRequest.getParameter(C.STORED_QUERY_IDS);
            if (storedQueryNames != null) {
                complexQueries = ComplexQueryUtils.parseAllStoredQueries(storedQueryNames, searcherApp).values();
            } else {
                complexQueries.add(baseRequest.getComplexQuery());
            }
            baseRequestBuilder.parse(searcherApp, baseRequest);
        } catch (ParseException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        String attachmentOffsetString = httpServletRequest.getParameter(C.ATTACHMENT_OFFSET);
        int attachmentOffset = 0;
        if (attachmentOffsetString != null) {
            try {
                attachmentOffset = Integer.parseInt(attachmentOffsetString);
            } catch (NumberFormatException e) {
                //swallow
            }
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
            writeTable(doc.get(searcherApp.getRhapsodeCollection().getIndexSchema().getRelPathField()), attachmentOffset, xhtml);
            RhapsodeDecorator.addFooter(xhtml);
            xhtml.endDocument();

        } catch (SAXException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        response.flushBuffer();
    }

    private void writeTable(String relPath, int attachmentOffset,
                            RhapsodeXHTMLHandler xhtml) throws IOException, SAXException {
        List<Metadata> metadataList = loadMetadataList(relPath, xhtml);
        if (metadataList == null) {
            return;
        }
        int i = 0;
        for (Metadata m : metadataList) {
            String internalRelPath = m.get(TIKA_INTERNAL_EMBEDDED_PATH);
            if (i > 0) {
                xhtml.startElement(H.H2);
                if (i == attachmentOffset) {
                    xhtml.startElement(H.SPAN, H.ID, H.JUMP_TO_FIRST);
                }
                xhtml.characters("Embedded Document " + i);
                if (internalRelPath != null) {
                    xhtml.characters(": " + internalRelPath);
                }
                if (i == attachmentOffset) {
                    xhtml.endElement(H.SPAN);
                }
                xhtml.endElement(H.H2);
            }
            xhtml.startElement(H.TABLE, H.BORDER, "2");
            if (i == attachmentOffset) {
                xhtml.startElement(H.SPAN, H.ID, H.JUMP_TO_FIRST);
                xhtml.endElement(H.SPAN);
//                xhtml.href("#"+H.JUMP_TO_FIRST, " ");
            }
            if (i > 0) {
                xhtml.element(H.TR, " ");
            }
            i++;
            if (m == null) {
                LOG.debug("metadata is null");
                continue;
            }

            List<String> names = new ArrayList<>(Arrays.asList(m.names()));
            Collections.sort(names);
            for (String n : names) {
                if (n.equals(TIKA_CONTENT_KEY)) {
                    continue;
                }
                String[] values = m.getValues(n);

                xhtml.startElement(H.TR);
                xhtml.startElement(H.TD);
                xhtml.characters(n);
                xhtml.endElement(H.TD);
                xhtml.startElement(H.TD);
                if (values.length == 1) {
                    xhtml.characters(values[0]);
                } else {
                    writeMultivalues(values, xhtml);
                }
                xhtml.endElement(H.TD);
                xhtml.endElement(H.TR);
            }
            String[] values = m.getValues(TIKA_CONTENT_KEY);
            xhtml.startElement(H.TR);
            xhtml.element(H.TD, "Content");
            if (values.length == 0) {
                xhtml.element(H.TD, " ");
            } else if (values.length == 1) {
                xhtml.element(H.TD, values[0]);
            } else {
                xhtml.startElement(H.TD);
                writeMultivalues(values, xhtml);
                xhtml.endElement(H.TD);
            }
            xhtml.endElement(H.TR);
            xhtml.endElement(H.TABLE);
            xhtml.br();
        }
    }

    private void writeMultivalues(String[] values, RhapsodeXHTMLHandler xhtml) throws SAXException {
        for (int i = 0; i < values.length; i++) {
            xhtml.startElement(H.DIV);
            if (i % 2 == 0) {
                xhtml.startElement(H.SPAN,
                        H.STYLE, "background-color: #EFF5FB");
                xhtml.characters(values[i]);
                xhtml.endElement(H.SPAN);
            } else {
                xhtml.characters(values[i]);
            }
            xhtml.endElement(H.DIV);
        }
    }

    private List<Metadata> loadMetadataList(String relPath, RhapsodeXHTMLHandler xhtml) throws SAXException {
        Path cand = getExtractPath(searcherApp.getRhapsodeCollection(), relPath);
        List<Metadata> metadataList = null;
        if (cand != null) {
            try (Reader r = Files.newBufferedReader(cand, StandardCharsets.UTF_8)) {
                metadataList = JsonMetadataList.fromJson(r);
            } catch (IOException | TikaException e) {
                xhtml.characters("Exception trying to open: " + relPath + ".json");
                LOG.warn(relPath, e);
            }
        }
        return metadataList;
    }

    public static Path getExtractPath(RhapsodeCollection rhapsodeCollection, String relPath) {
        for (String suffix : new String[]{".json", ".json.bz2"}) {
            Path cand = rhapsodeCollection.getExtractedTextRoot().resolve(relPath+suffix);
            if (Files.isRegularFile(cand)) {
                return cand;
            }
        }
        LOG.warn("Couldn't find extract file for: " + relPath);
        return null;
    }
    private void writePrevNextLinks(BaseSearchRequest request, Document doc,
                                    int rank, int docId,
                                    String docKey, RhapsodeXHTMLHandler xhtml) throws IOException, SAXException {
        Set<String> fields = new HashSet<>();
        return;
        /*
        if (rank < 0) {
            return;
        }
        Pair<DocLuceneIdPair, DocLuceneIdPair> pair =
                SearcherUtils.getBeforeAndAfter(searcherApp.getRhapsodeCollection().getIndexManager().getSearcher(),
                        request.getComplexQuery().getRetrievalQuery(),
                        rank, docId, searcherApp.getCommonSearchConfig().getDefaultSearchField(), docKey, fields);

        String previousURL = getPrePostDocUrl(pair.getLeft(), rank-1, request);
        String nextURL = getPrePostDocUrl(pair.getRight(), rank+1, request);
        String originalDocURL = RawFileURLBuilder.buildStoredQuery(
                searcherApp.getRhapsodeCollection().getOrigDocsRoot(), doc.get(searcherApp.getRelativePathField()));
        String fullTikaViewerURL = ExtractFileURLBuilder.buildStoredQuery(doc.get(
                        searcherApp.getKeyField()),
                doc.get(searcherApp.getRhapsodeCollection().
                        getIndexSchema().getAttachmentIndexField()));



        xhtml.startElement(H.TABLE,
                H.CLASS, CSS.DOC_VIEWER_LINKS);
        xhtml.startElement(H.TR,
                H.CLASS, CSS.DOC_VIEWER_LINKS);
        xhtml.element(H.TD, " ");
        if (originalDocURL != null) {
            xhtml.href(originalDocURL, "Original Document");
        } else {
            xhtml.element(H.TD, " ");
        }
        xhtml.endElement(H.TR);
        xhtml.startElement(H.TD,
                H.CLASS, CSS.DOC_VIEWER_LINK_PRE);
        if (previousURL != null) {
            xhtml.href(previousURL, "Previous");
        } else {
            xhtml.characters(" ");
        }
        xhtml.endElement(H.TD);
        xhtml.element(H.TD, " ");
        xhtml.startElement(H.TD,
                H.CLASS, CSS.DOC_VIEWER_LINK_POST);
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
            xhtml.href(fullTikaViewerURL, "Full Extract");
        } else {
            xhtml.characters(" ");
        }
        xhtml.endElement(H.TD);
        xhtml.endElement(H.TR);
        xhtml.endElement(H.TABLE);*/
    }

    private String getPrePostDocUrl(DocLuceneIdPair docLuceneIdPair, int rank, BaseSearchRequest request) {
        if (docLuceneIdPair == null) {
            return null;
        }
        int luceneId = docLuceneIdPair.getLuceneId();
        Document doc = docLuceneIdPair.getDocument();
        String key = doc.get(searcherApp.getRhapsodeCollection().getIndexSchema().getUniqueDocField());
        return indexedDocURLBuilder.getURL(key, Integer.toString(luceneId), Integer.toString(rank), request);
    }


    private Set<String> getFields(RhapsodeSearcherApp searcherApp) {
        Set<String> fields = new HashSet<>();

        fields.add(searcherApp.getStringParameter(DynamicParameters.DEFAULT_CONTENT_FIELD));
        fields.add(searcherApp.getRhapsodeCollection().getIndexSchema().getRelPathField());
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

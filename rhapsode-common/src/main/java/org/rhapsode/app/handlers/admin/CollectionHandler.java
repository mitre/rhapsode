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

package org.rhapsode.app.handlers.admin;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.eclipse.jetty.server.Request;
import org.rhapsode.app.RhapsodeSearcherApp;
import org.rhapsode.app.contants.C;
import org.rhapsode.app.contants.H;
import org.rhapsode.app.decorators.RhapsodeDecorator;
import org.rhapsode.app.decorators.RhapsodeXHTMLHandler;
import org.rhapsode.app.session.CollectionsHistory;
import org.rhapsode.app.session.DynamicParameters;
import org.xml.sax.SAXException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class CollectionHandler extends AdminHandler {

    private final RhapsodeSearcherApp searcherApp;

    public CollectionHandler(RhapsodeSearcherApp searcherApp) {
        super("Collection Admin");
        this.searcherApp = searcherApp;
    }

    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest,
                       HttpServletResponse response) throws IOException, ServletException {
        try {
            _handle(s, request, httpServletRequest, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void _handle(String s, Request request, HttpServletRequest httpServletRequest,
                         HttpServletResponse response) throws IOException, ServletException {
        RhapsodeXHTMLHandler xhtml = null;
        try {
            xhtml = initResponse(response, null);
        } catch (SAXException e) {
            throw new IOException(e);
        }

        try {

            if (httpServletRequest.getParameter(C.OPEN_NEW_COLLECTION) != null
                    || httpServletRequest.getParameter(C.REOPEN_COLLECTION) != null) {

                String errMsg = tryToLoadCollection(httpServletRequest, xhtml);
                if (errMsg != null) {
                    xhtml.br();
                    RhapsodeDecorator.writeErrorMessage(errMsg, xhtml);
                    loadCollectionDialogue(xhtml);
                    xhtml.endDocument();
                    return;
                }
            }
            if (searcherApp.getRhapsodeCollection() == null ||
                    httpServletRequest.getParameter(C.OPEN_NEW_COLLECTION_DIALOGUE) != null) {
                String errMsg = loadCollectionDialogue(xhtml);
                if (errMsg != null) {
                    xhtml.br();
                    RhapsodeDecorator.writeErrorMessage(errMsg, xhtml);
                    loadCollectionDialogue(xhtml);
                    xhtml.endDocument();
                }
                return;
            }


            Path p = searcherApp.getRhapsodeCollection().getCollectionPath();
            xhtml.startElement("b");
            xhtml.characters("Current Collection: ");
            xhtml.endElement("b");
            xhtml.characters(p.toAbsolutePath().toString());
            xhtml.br();
            xhtml.startElement(H.P);
            xhtml.startElement("b");
            xhtml.characters("Current Collections Directory: ");
            xhtml.endElement("b");
            xhtml.characters(searcherApp.getStringParameter(DynamicParameters.COLLECTIONS_ROOT));
            xhtml.endElement(H.P);

            if (searcherApp.getRhapsodeCollection().getIndexManager() == null) {
                RhapsodeDecorator.writeErrorMessage("index manager is null?!", xhtml);
                RhapsodeDecorator.addFooter(xhtml);
                xhtml.endDocument();
            }

            if (httpServletRequest.getParameter(C.REFRESH_COLLECTION) != null) {
                searcherApp.getRhapsodeCollection().getIndexManager().maybeRefresh();
            }

            IndexReader r = searcherApp.getRhapsodeCollection().getIndexManager().getSearcher().getIndexReader();
            xhtml.element(H.P, formatDocCount(r.numDocs(), "indexed document"));
            xhtml.element(H.P, formatDocCount(r.numDeletedDocs(), "deleted document"));
            xhtml.element(H.P, formatDocCount(searcherApp.getRhapsodeCollection().getIgnoredSize(), "ignored document"));
            xhtml.element(H.P, formatDocCount(searcherApp.getRhapsodeCollection().getFavoritesSize(), "favorite document"));
            Map<String, Long> m = new HashMap<>();
            for (LeafReaderContext c : r.leaves()) {
                LeafReader reader = c.reader();
                FieldInfos fs = reader.getFieldInfos();
                Iterator<FieldInfo> it = fs.iterator();
                while (it.hasNext()) {
                    FieldInfo info = it.next();
                    String name = info.name;
                    int nbr = reader.getDocCount(name);
                    Long curr = m.get(name);
                    curr = (curr == null) ? 0 : curr;
                    curr += nbr;
                    m.put(name, curr);
                }
            }
            xhtml.br();
            xhtml.br();
            xhtml.characters("Fields:");
            xhtml.startElement(H.TABLE, H.BORDER, "2");
            SortedSet<String> keys = new TreeSet<>(m.keySet());
            xhtml.startElement(H.TR);
            xhtml.element(H.TH, "Field");
            xhtml.element(H.TH, "Number of Documents");
            xhtml.endElement(H.TR);
            for (String k : keys) {
                xhtml.startElement(H.TR);
                xhtml.startElement(H.TD);
                xhtml.characters(k);
                xhtml.endElement(H.TD);
                xhtml.startElement(H.TD);
                xhtml.characters(Long.toString(m.get(k)));
                xhtml.endElement(H.TD);
                xhtml.endElement(H.TR);
            }
            xhtml.endElement(H.TABLE);
            xhtml.br();
            xhtml.startElement(H.FORM, H.METHOD, H.POST);
            xhtml.startElement(H.INPUT,
                    H.TYPE, H.SUBMIT,
                    H.NAME, C.REFRESH_COLLECTION,
                    H.VALUE, "Refresh collection");
            xhtml.endElement(H.INPUT);
            xhtml.characters(" ");
            xhtml.startElement(H.INPUT,
                    H.TYPE, H.SUBMIT,
                    H.NAME, C.OPEN_NEW_COLLECTION_DIALOGUE,
                    H.VALUE, "Load new collection",
                    "default", "");
            xhtml.endElement(H.INPUT);
            xhtml.endElement(H.FORM);

            RhapsodeDecorator.addFooter(xhtml);
            xhtml.endDocument();
        } catch (SAXException e) {
            e.printStackTrace();
        }

    }

    private String formatDocCount(int i, String typeOfDocument) {
        if (i == 1) {
            return "There is one " + typeOfDocument + ".";
        }
        return "There are " + i + " " + typeOfDocument + "s.";
    }

    private String loadCollectionDialogue(RhapsodeXHTMLHandler xhtml) throws SAXException {
        List<Pair<Path, Date>> paths = null;
        //TODO: if there was an error in the last request
        //pull that out of the requestlet
        String lastCollection = "";
        CollectionsHistory ch = searcherApp.getSessionManager().getCollectionsHistory();
        try {
            paths = ch.getPaths();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        xhtml.startElement(H.FORM, H.METHOD, H.POST);
        Path collectionsDir = Paths.get(searcherApp.getStringParameter(DynamicParameters.COLLECTIONS_ROOT));
        if (!Files.isDirectory(collectionsDir)) {
            return "The collections root directory (" + collectionsDir.toAbsolutePath().toString() + ") doesn't exist." +
                    " Please update the 'Collections Directory' via the 'Settings Interface' button.";
        }

        File[] files = collectionsDir.toFile().listFiles();
        if (files == null || files.length == 0) {
            return "I'm sorry, but I couldn't find any collections in the collections root directory (" + collectionsDir.toAbsolutePath().toString() + ") doesn't exist." +
                    " Please update the 'Collections Directory' via the 'Settings Interface' button.";
        }
        xhtml.startElement(H.TABLE);
        for (File f : files) {
            xhtml.startElement(H.TR);
            xhtml.element(H.TD, f.getName());
            xhtml.startElement(H.TD);
            if (searcherApp.getRhapsodeCollection() != null &&
                    searcherApp.getRhapsodeCollection().getCollectionPath() != null &&
                    f.getName().equals(searcherApp.getRhapsodeCollection().getCollectionPath().getFileName().toString())) {
                xhtml.startElement(H.INPUT,
                        H.TYPE, H.RADIO,
                        H.NAME, C.COLLECTION_NAME,
                        H.VALUE, f.getName(),
                        H.CHECKED, H.CHECKED);
            } else {
                xhtml.startElement(H.INPUT,
                        H.TYPE, H.RADIO,
                        H.NAME, C.COLLECTION_NAME,
                        H.VALUE, f.getName());

            }
            xhtml.endElement(H.INPUT);
            xhtml.endElement(H.TD);
            xhtml.endElement(H.TR);
        }
        xhtml.endElement(H.TABLE);
        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.OPEN_NEW_COLLECTION,
                H.VALUE, "Open Collection",
                "default", "");
/*

        if (paths != null && paths.size() > 0) {
            xhtml.br();
            xhtml.characters("Recent collections:");
            xhtml.startElement(H.SELECT,
                    H.NAME, C.COLLECTION_HISTORY_SELECTOR);
            for (int i = 0; i < paths.size(); i++) {
                Path collPath = paths.get(i).getKey();
                String collString = relativize(collPath.toString());
                xhtml.startElement(H.OPTION,
                        H.VALUE, collString);
                xhtml.characters(collString);
                xhtml.endElement(H.OPTION);
            }
            xhtml.endElement(H.SELECT);
        }

        xhtml.startElement(H.INPUT,
                H.TYPE, H.SUBMIT,
                H.NAME, C.REOPEN_COLLECTION,
                H.VALUE, "Reopen Collection");

        xhtml.endElement(H.INPUT);
*/
        xhtml.endElement(H.FORM);
        RhapsodeDecorator.addFooter(xhtml);
        xhtml.endDocument();
        return null;
    }


    private String relativize(String collection) {
        if (StringUtils.isBlank(collection)) {
            return StringUtils.EMPTY;
        }
        Path cP = Paths.get(collection);
        if (cP.toAbsolutePath().startsWith(RhapsodeSearcherApp.CWD)) {
            return RhapsodeSearcherApp.CWD.toAbsolutePath().relativize(cP.toAbsolutePath()).toString();
        }
        return collection;
    }

    private String tryToLoadCollection(HttpServletRequest httpServletRequest, RhapsodeXHTMLHandler xhtml) throws SAXException {
        String collPath = null;
        if (httpServletRequest.getParameter(C.REOPEN_COLLECTION) != null) {
            collPath = httpServletRequest.getParameter(C.COLLECTION_HISTORY_SELECTOR);
        } else {
            collPath = httpServletRequest.getParameter(C.COLLECTION_NAME);
        }
        if (collPath == null) {
            return "Collection path must not be null";
        }
        collPath = collPath.trim();//trailing whitespace not allowed on windows
        Path collectionsRoot = Paths.get(searcherApp.getStringParameter(DynamicParameters.COLLECTIONS_ROOT));
        if (!Files.isDirectory(collectionsRoot)) {
            return "Can't find this directory:" + collectionsRoot.toAbsolutePath();
        }

        Path collectionPath = collectionsRoot.resolve(collPath);
        if (!Files.isDirectory(collectionsRoot)) {
            return "Can't find this directory:" + collectionPath.toAbsolutePath();
        }
        try {
            searcherApp.tryToLoadRhapsodeCollection(collectionPath);
            searcherApp.getSessionManager().getCollectionsHistory().addLoaded(collectionPath);
            searcherApp.getSessionManager().getStoredQueryManager().validateQueries(searcherApp);
            String defaultContentField = searcherApp.getRhapsodeCollection().getIndexSchema().getDefaultContentField();
            if (!StringUtils.isBlank(defaultContentField)) {
                searcherApp.getSessionManager().getDynamicParameterConfig().update(DynamicParameters.DEFAULT_CONTENT_FIELD, defaultContentField);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "IOException " + e.getMessage();
        }
        return null;
    }
}
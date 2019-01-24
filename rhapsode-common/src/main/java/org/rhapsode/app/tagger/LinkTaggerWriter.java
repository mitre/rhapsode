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

package org.rhapsode.app.tagger;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.rhapsode.app.RhapsodeSearcherApp;
import org.rhapsode.app.contants.C;
import org.rhapsode.app.contants.CSS;
import org.rhapsode.app.contants.H;
import org.rhapsode.app.contants.Internal;
import org.rhapsode.app.decorators.RhapsodeDecorator;
import org.rhapsode.app.handlers.admin.ReportRequest;
import org.rhapsode.app.session.DynamicParameters;
import org.rhapsode.app.session.StoredQueryWriter;
import org.rhapsode.app.utils.ComplexQueryUtils;
import org.rhapsode.app.utils.DocHighlighter;
import org.rhapsode.lucene.search.ComplexQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LinkTaggerWriter {

    private static final Logger LOG = LoggerFactory.getLogger(LinkTaggerWriter.class);

    private static final int MAX_HYPERLINKS = 65000;
    final SXSSFWorkbook wb;
    final CellStyle hlinkStyle;
    final CellStyle hlinkFloatStyle;
    final DataFormat dataFormat;
    final CellStyle floatStyle;
    final CreationHelper creationHelper;
    final TaggerRequest request;
    final RhapsodeSearcherApp searcherApp;
    private final Set<String> linkDisplayField;
    int links = 0;
    boolean alreadyCalled = false;

    public LinkTaggerWriter(TaggerRequest request, RhapsodeSearcherApp searcherApp) {
        this.request = request;
        this.searcherApp = searcherApp;
        this.linkDisplayField = new HashSet<>();
        linkDisplayField.add(
                searcherApp.getRhapsodeCollection().getIndexSchema().getLinkDisplayField());


        wb = new SXSSFWorkbook(new XSSFWorkbook(), 100, true, true);
        StoredQueryWriter.updateMetadata(wb.getXSSFWorkbook(),
                searcherApp.getSessionManager()
                        .getDynamicParameterConfig()
                        .getString(DynamicParameters.METADATA_CREATOR),
                "Rhapsode 0.4.0-SNAPSHOT Report"
        );
        wb.getXSSFWorkbook().getProperties().getCoreProperties().setDescription("Rhapsode Report, type: " + request.reportType.toString());

        wb.setCompressTempFiles(true);

        creationHelper = wb.getCreationHelper();
        dataFormat = wb.createDataFormat();
        floatStyle = wb.createCellStyle();
        floatStyle.setDataFormat(dataFormat.getFormat("0.00"));
        Font hlinkFont = wb.createFont();
        hlinkStyle = wb.createCellStyle();
        hlinkFont.setUnderline(Font.U_SINGLE);
        hlinkFont.setColor(IndexedColors.BLUE.getIndex());
        hlinkStyle.setFont(hlinkFont);

        hlinkFloatStyle = wb.createCellStyle();
        hlinkFloatStyle.setFont(hlinkFont);
        hlinkFloatStyle.setDataFormat(dataFormat.getFormat("0.00"));
    }

    public void write(Connection conn) throws IOException, SQLException {
        if (alreadyCalled) {
            throw new IllegalArgumentException("need to instantiate new writer for each write");
        }
        alreadyCalled = true;

        Files.createDirectories(request.reportFile.toAbsolutePath().getParent());
        String sql = "SELECT " + Tagger.QUERY_ID_COL + "," +
                Tagger.Q_NAME_COL + " from " + Tagger.QUERY_NAME_TABLE;
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(sql);
        List<Integer> queryIds = new ArrayList<>();
        List<String> queryNames = new ArrayList<>();
        while (rs.next()) {
            queryIds.add(rs.getInt(1));
            queryNames.add(rs.getString(2));
        }
        rs.close();

        sql = "SELECT s." + Tagger.FILE_ID_COL + "," +
                Tagger.DISPLAY_NAME_COL + "," +
                Tagger.ORIG_REL_PATH + "," +
                Tagger.QUERY_ID_COL + "," +
                Tagger.SCORES_COL + ", " +
                Tagger.TOTAL_SCORE_COL + " FROM " +
                Tagger.SCORES_TABLE + " s " +
//                " s left join " + Tagger.QUERY_NAME_TABLE + " q ON s." + Tagger.QUERY_ID_COL + "=q." + Tagger.QUERY_ID_COL +
                " left join " + Tagger.SCORE_SORT_TABLE + " t on s." + Tagger.FILE_ID_COL + "=t." + Tagger.FILE_ID_COL +
                " left join " + Tagger.NAME_PATH_TABLE + " npt on s." + Tagger.FILE_ID_COL + "=npt." + Tagger.FILE_ID_COL +
                " ORDER BY " + Tagger.TOTAL_SCORE_COL + " DESC, " + Tagger.FILE_ID_COL;
        rs = st.executeQuery(sql);
        Map<Integer, Float> scores = new HashMap<>();
        Sheet sheet = wb.createSheet("Rhapsode Tags");
        int rowCount = 0;
        Row xssfRow = sheet.createRow(rowCount);
        Cell c = xssfRow.createCell(0);
        c.setCellValue("FILE_NAME");
        for (int i = 0; i < queryNames.size(); i++) {
            c = xssfRow.createCell(i + 1);
            c.setCellValue(queryNames.get(i));
        }
        c = xssfRow.createCell(queryIds.size() + 1);
        c.setCellValue("TOTAL");
        rowCount++;
        int cellCount = 0;
        boolean hitMax = false;
        String lastId = null;
        String lastDisplay = null;
        String lastRelPath = null;
        try {
            float total = -1.0f;
            while (rs.next()) {
                String fileId = rs.getString(1);
                if (lastId != null && !fileId.equals(lastId)) {
                    dumpRow(lastId, lastDisplay, lastRelPath, queryIds, scores, total, sheet.createRow(rowCount));
                    rowCount++;
                    cellCount += scores.size();
                    scores.clear();
                }
                if (request.topNCombinedResults > -1 && rowCount > request.topNCombinedResults) {
                    hitMax = true;
                    break;
                }
                scores.put(rs.getInt(4), rs.getFloat(5));
                lastId = fileId;
                lastDisplay = rs.getString(2);
                lastRelPath = rs.getString(3);
                total = rs.getFloat(6);
            }

            if (!hitMax) {
                dumpRow(lastId, lastDisplay, lastRelPath, queryIds, scores, total, sheet.createRow(rowCount));
            }
            LOG.debug("finished writing scores sheet");
            StoredQueryWriter writer = new StoredQueryWriter(searcherApp);
            LOG.debug("adding stored query sheet");
            writer.addStoredQuerySheet(wb);
            LOG.debug("adding stored concept sheet");
            writer.addConceptSheet(wb);
        } catch (IOException e) {
            LOG.warn("IOException while writing", e);
            throw (e);
        } finally {
            LOG.debug("about to write file");
            try (OutputStream os = Files.newOutputStream(request.reportFile)) {
                wb.write(os);
                LOG.debug("finished writing file");
            } finally {
                wb.dispose();
                LOG.debug("disposed");
            }
        }
        rs.close();
        if (request.reportType.equals(ReportRequest.REPORT_TYPE.STATIC_LINKS)) {
            highlightFiles(queryIds, st);
        }
    }

    public void highlightFiles(List<Integer> ids, Statement statement) throws IOException, SQLException {
        Map<Integer, ComplexQuery> parsedQueries = ComplexQueryUtils.parseAllStoredQueries(ids, searcherApp);
        String sql = "SELECT s." + Tagger.FILE_ID_COL + "," + Tagger.QUERY_ID_COL + " FROM " +
                Tagger.SCORES_TABLE + " s" +
//                " left join "+Tagger.QUERY_NAME_TABLE+" q ON s."+Tagger.QUERY_ID_COL +"=q."+Tagger.QUERY_NAME_ID_COL +
                " left join " + Tagger.SCORE_SORT_TABLE + " t on s." + Tagger.FILE_ID_COL + "=t." + Tagger.FILE_ID_COL +

                " ORDER BY " + Tagger.TOTAL_SCORE_COL + " DESC, " + Tagger.FILE_ID_COL;
        ResultSet rs = statement.executeQuery(sql);
        Map<Integer, ComplexQuery> relevantQueries = new HashMap<>();
        String defaultContentField = searcherApp.getRhapsodeCollection().getIndexSchema().getDefaultContentField();
        List<String> fieldsToDisplay = searcherApp.getSessionManager().getDynamicParameterConfig().getStringList(DynamicParameters.FILE_VIEWER_DISPLAY_FIELDS);
        //if the user hasn't set the fields to display, yet, make sure to add the content field.
        if (fieldsToDisplay.size() == 0) {
            fieldsToDisplay.add(defaultContentField);
        }
        Set<String> fieldsToRetrieve = new HashSet<>();
        fieldsToRetrieve.addAll(fieldsToDisplay);
        fieldsToRetrieve.add(searcherApp.getRhapsodeCollection().getIndexSchema().getRelPathField());
        DocHighlighter highlighter = new DocHighlighter();
        highlighter.setTableClass(CSS.HIGHLIGHTED);
        highlighter.setTDClass(CSS.HIGHLIGHTED);

        //TODO: make this multi-threaded!!!
        int docCount = 0;
        String lastId = null;
        boolean hitMax = false;
        while (rs.next()) {
            String id = rs.getString(1);
            Integer storedQueryId = rs.getInt(2);

            if (lastId != null && !id.equals(lastId)) {
                List<Document> docs = searcherApp.getRhapsodeCollection().getAllDocsFromAnyDocId(lastId, fieldsToRetrieve);
                highlighter.highlightDocsToFile(buildPathToStaticColorizedHTMLFile(docs.get(0)),
                        defaultContentField,
                        searcherApp.getRhapsodeCollection().getIndexSchema().getEmbeddedPathField(),
                        searcherApp.getSessionManager().getDynamicParameterConfig().getStringList(DynamicParameters.FILE_VIEWER_DISPLAY_FIELDS),
                        docs,
                        relevantQueries.values(),
                        searcherApp.getRhapsodeCollection().getIndexSchema().getOffsetAnalyzer(),
                        RhapsodeDecorator.generateStyleString(searcherApp.getCommonSearchConfig().getHighlightingStyles()),
                        0
                );
                relevantQueries.clear();
                docCount++;
            }
            if (request.topNCombinedResults > -1 && docCount >= request.topNCombinedResults) {
                hitMax = true;
                break;
            }
            if (!storedQueryId.equals(Internal.MANUALLY_SELECTED_FAVORITES_QUERY_NAME)) {
                relevantQueries.put(storedQueryId, parsedQueries.get(storedQueryId));
            }
            lastId = id;
        }
        List<Document> docs = searcherApp.getRhapsodeCollection().getAllDocsFromAnyDocId(lastId, fieldsToRetrieve);
        if (!hitMax) {
            highlighter.highlightDocsToFile(buildPathToStaticColorizedHTMLFile(docs.get(0)),
                    defaultContentField,
                    searcherApp.getRhapsodeCollection().getIndexSchema().getEmbeddedPathField(),
                    searcherApp.getSessionManager().getDynamicParameterConfig().getStringList(DynamicParameters.FILE_VIEWER_DISPLAY_FIELDS),
                    docs,
                    relevantQueries.values(),
                    searcherApp.getRhapsodeCollection().getIndexSchema().getOffsetAnalyzer(),
                    RhapsodeDecorator.generateStyleString(searcherApp.getCommonSearchConfig().getHighlightingStyles()),
                    0
            );
            docCount++;
        }

        rs.close();
    }


    private void dumpRow(String fileId, String displayName, String relPath,
                         List<Integer> queryIds, Map<Integer, Float> scores, float total,
                         Row row) throws IOException {


        Cell c = row.createCell(0);
        c.setCellValue(displayName);
        if (request.reportType.equals(ReportRequest.REPORT_TYPE.LIVE_LINKS)) {
            Set<Integer> actualSQNames = new HashSet<>(scores.keySet());
            actualSQNames.remove(Internal.MANUALLY_SELECTED_FAVORITES_QUERY_NAME);
            if (actualSQNames.size() > 0) {
                addHyperlink(c, "http://localhost:8092/rhapsode/view_docs?&" + C.FILE_KEY + "=" + fileId + "&" + C.STORED_QUERY_IDS + "=" +
                        StringUtils.join(actualSQNames, ',') + "#" + H.JUMP_TO_FIRST, false);
            } else {
                addHyperlink(c, "http://localhost:8092/rhapsode/view_docs?&" + C.FILE_KEY + "=" + fileId, false);
            }
        } else if (request.reportType.equals(ReportRequest.REPORT_TYPE.STATIC_LINKS)) {
            Path docKey = buildRelativeURLToStaticColorizedHTMLFile(relPath);
            addHyperlink(c, docKey.toString(), false);
        }
        int i = 1;
        for (Integer queryId : queryIds) {
            c = row.createCell(i++);
            Float score = scores.get(queryId);
            if (score != null) {
                c.setCellValue(score);
                c.setCellStyle(floatStyle);
                if (request.reportType.equals(ReportRequest.REPORT_TYPE.LIVE_LINKS)) {
                    if (!(queryId.equals(Internal.MANUALLY_SELECTED_FAVORITES_QUERY_NAME))) {
                        addHyperlink(c, buildPerFilePerQueryLiveURL(queryId, fileId), true);
                    }
                }
            } else {
                c.setCellValue("");
            }
        }
        c = row.createCell(i);
        c.setCellValue(total);
        c.setCellStyle(floatStyle);
    }

    private String buildPerFilePerQueryLiveURL(Integer storedQueryId, String fileId) {
        StringBuilder sb = new StringBuilder();
        sb.append("http://localhost:8092/rhapsode/view_docs/?");//&r=-1
        sb.append("&").append(C.FILE_KEY).append("=").append(fileId);
        sb.append("&").append(C.STORED_QUERY_ID).append("=").append(storedQueryId);
        sb.append("#").append(H.JUMP_TO_FIRST);
        return sb.toString();
    }

    /*
    private String buildPerFileAllQueriesLiveURL(List<String> storedQueryNames, Document doc) {
        StringBuilder sb = new StringBuilder();
        sb.append("http://localhost:8092/rhapsode/view_doc/?&r=-1");
        sb.append("&").append(C.DOC_KEY).append("=").append(doc.get(request.getDocIdFieldName()));
        sb.append("&").append(C.STORED_QUERY_ID).append("=").append(StringUtils.join(storedQueryNames, ','));
        return sb.toString();
    }*/

    private Path buildRelativeURLToStaticColorizedHTMLFile(String originalRelPath) {
        Path htmlFile = request.staticDir.resolve(originalRelPath + ".html");
        Path relative = request.staticDir.toAbsolutePath().getParent().relativize(htmlFile.toAbsolutePath());
        return relative;

    }

    private Path buildPathToStaticColorizedHTMLFile(Document document) {
        Path htmlFile = request.staticDir.resolve(document.get(request.getRelPathFieldName()) + ".html");
        return htmlFile;
    }

    void addHyperlink(Cell cell, String address, boolean decimalFormat) {
        if (address == null) {
            //something went wrong, silently skip
        }
        HyperlinkType linkType = HyperlinkType.NONE;

        if (request.reportType.equals(ReportRequest.REPORT_TYPE.LIVE_LINKS)) {
            linkType = HyperlinkType.URL;
        } else if (request.reportType.equals(ReportRequest.REPORT_TYPE.STATIC_LINKS)) {
            linkType = HyperlinkType.DOCUMENT;
            try {
                address = URLEncoder.encode(address.replaceAll("\\\\", "/"), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            address = address.replaceAll("\\+", "%20");
        } else {
            //no link!
            return;
        }
        if (links < MAX_HYPERLINKS) {

            Hyperlink hyperlink = creationHelper.createHyperlink(linkType);
            links++;
            hyperlink.setAddress(address);
            cell.setHyperlink(hyperlink);
            if (decimalFormat) {
                cell.setCellStyle(hlinkFloatStyle);
            } else {
                cell.setCellStyle(hlinkStyle);
            }
        } else {
            //silently stop adding hyperlinks
        }

    }
}

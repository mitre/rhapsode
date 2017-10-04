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

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.rhapsode.app.io.XLSXTableWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class BasicTaggerWriter {

    public void write(Connection conn, IndexSearcher searcher, Path outputFile) throws IOException, SQLException {
        String sql = "SELECT " + Tagger.Q_NAME_COL + " FROM " + Tagger.QUERY_NAME_TABLE;
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(sql);
        List<String> names = new ArrayList<>();
        while (rs.next()) {
            names.add(rs.getString(1));
        }
        rs.close();

        sql = "SELECT s." + Tagger.FILE_ID_COL + "," + Tagger.Q_NAME_COL + "," + Tagger.SCORES_COL + ", " + Tagger.TOTAL_SCORE_COL + " FROM " +
                Tagger.SCORES_TABLE + " s " +
//                " s left join "+Tagger.QUERY_NAME_TABLE+" q ON s."+Tagger.QUERY_ID_COL +"=q."+Tagger.QUERY_ID_COL +
                " left join " + Tagger.SCORE_SORT_TABLE + " t on s." + Tagger.FILE_ID_COL + "=t." + Tagger.FILE_ID_COL +
                " ORDER BY " + Tagger.TOTAL_SCORE_COL + " DESC, DOC_ID";
        rs = st.executeQuery(sql);
        int lastId = -1;
        Map<String, Float> scores = new HashMap<>();
        //need to write headers
        XLSXTableWriter writer = new XLSXTableWriter(outputFile, "Rhapsode Scores");
        List<String> headers = new ArrayList<>();
        headers.add("FILE_ID");
        for (String n : names) {
            headers.add(n);
        }
        headers.add("TOTAL");
        try {
            writer.writeNext(headers);
            float total = -1.0f;
            while (rs.next()) {
                int id = rs.getInt(1);
                if (id != lastId && lastId > -1) {
                    dumpRow(searcher, lastId, names, scores, total, writer);
                    scores.clear();
                }
                scores.put(rs.getString(2), rs.getFloat(3));
                lastId = id;
                total = rs.getFloat(4);
            }
            dumpRow(searcher, lastId, names, scores, total, writer);
        } catch (IOException e) {

        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e) {

                    //swallow
                }
            }
        }
        rs.close();
    }

    private void dumpRow(IndexSearcher searcher, int docId,
                         List<String> names, Map<String, Float> scores, float total,
                         XLSXTableWriter writer) throws IOException {
        Document doc = searcher.doc(docId);

        List<String> cols = new ArrayList<>();
        //TODO: bad hack
        cols.add(doc.get("file_name"));
        for (String n : names) {
            Float score = scores.get(n);
            if (score != null) {
                cols.add(Float.toString(score));
            } else {
                cols.add(StringUtils.EMPTY);
            }
        }
        cols.add(Float.toString(total));
        writer.writeNext(cols);
    }
}

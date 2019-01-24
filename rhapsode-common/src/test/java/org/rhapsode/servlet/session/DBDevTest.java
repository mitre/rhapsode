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

package org.rhapsode.servlet.session;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Random;
import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;

//used to figure out the right sql call to get count of
//embedded documents with at least one hit
public class DBDevTest extends ServletSessionTestBase {


    @Test
    @Ignore("until we have an actual test")
    public void testBasic() throws Exception {
        Statement st = connection.createStatement();
        st.execute("create table tmp (file_id char(36), doc_id integer, query_id integer)");
        String uuid = UUID.randomUUID().toString();
        int last = 0;
        for (int fileCount = 0; fileCount < 1000; fileCount++) {
            String file_id = UUID.randomUUID().toString();
            for (int i = last; i < last + 20; i++) {
                int doc_id = i;
                for (int q = 0; q < new Random().nextInt(3); q++) {
                    st.execute("insert into tmp values('" + file_id + "', " + doc_id + "," + q + ")");
                }
            }
            last += 20;
        }

        ResultSet rs = st.executeQuery("select * from tmp");
        while (rs.next()) {
            System.out.println(rs.getString(1) + " : " + rs.getInt(2) + " : " + rs.getInt(3));
        }

        System.out.println("SELECTING");
        rs = st.executeQuery("select file_id, count(1) from " +
                "(select file_id, doc_id from tmp group by file_id, doc_id) group by file_id");
        while (rs.next()) {
            System.out.println(rs.getString(1) + " : " + rs.getInt(2));//+ " : "+rs.getInt(2) + " : "+ rs.getInt(3));
        }
    }
}

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

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.Map;

import org.junit.Test;
import org.rhapsode.app.session.DBStoredConceptManager;
import org.rhapsode.app.session.DBStoredQueryManager;
import org.rhapsode.app.session.StoredQueryReader;
import org.rhapsode.lucene.queryparsers.SQPParserPlugin;
import org.rhapsode.lucene.schema.IndexSchema;
import org.rhapsode.lucene.search.StoredQuery;

public class StoredQueryManagerTest extends ServletSessionTestBase {
    static final String DEFAULT_FIELD = "content";
/*
    @Test
    public void testBasic() throws Exception {
        DBStoredQueryManager m = DBStoredQueryManager.load(connection);
        m.addQuery("apples",
                new StoredQuery("apples",
                        new StoredQuery("field", "apple pinklady apples", null, 1, "default"), 100));

        m.addQuery("bananas",
                new StoredQuery("bananas",
                        new StoredQuery("field", "bananas", null, 1, "default"), 100));

        Map<String, StoredQuery> c = m.getStoredQueryMap();

        assertEquals(2, c.size());
        assertEquals("apple pinklady apples", c.get("apples").getMainQueryString());


        try {
            m.addQuery("apples",
                    new StoredQuery("apples",
                            new StoredQuery("field", "apple pinklady apples", null, 1, "default"), 100));
            fail("Should fail to insert unique");
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertEquals(2, c.size());

        m.deleteAllQueries();
        c = m.getStoredQueryMap();
        assertEquals(0, c.size());

        m.addQuery("apples",
                new StoredQuery("apples",
                        new StoredQuery("field", "more apples", null, 1, "default"), 100));

        assertEquals("more apples", m.getStoredQueryMap().get("apples").getMainQueryString());
        m.close();
    }*/


    @Test
    public void testLoadingXLSX() throws Exception {
        DBStoredConceptManager storedConceptManager = DBStoredConceptManager.load(connection);
        DBStoredQueryManager storedQueryManager = DBStoredQueryManager.load(connection);
        IndexSchema indexSchema = IndexSchema.load(this.getClass().getResourceAsStream("/test_index_data/index_schema.json"));
        try (DBStoredQueryManager m = DBStoredQueryManager.load(connection)) {
            StoredQueryReader reader = new StoredQueryReader(storedConceptManager,
                    storedQueryManager, new SQPParserPlugin(indexSchema), indexSchema, connection);
            try (InputStream is = getClass().getResourceAsStream("/test-docs/testQueries.xlsx")) {
                reader.loadBoth(is);
            }
            Map<Integer, StoredQuery> c = m.getStoredQueryMap();
            StoredQuery q = c.get(0);
            assertEquals(-1, q.getMaxHits());
            assertEquals(DEFAULT_FIELD, q.getDefaultField());
            assertEquals("regular", q.getHighlightingStyle());
            assertEquals(-1, q.getPriority());
            System.out.println(c.keySet());
        }
    }

}

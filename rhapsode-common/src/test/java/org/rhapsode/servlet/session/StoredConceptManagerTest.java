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

public class StoredConceptManagerTest extends ServletSessionTestBase {

/*
    @Test
    public void testBasic() throws Exception {
        DBStoredConceptManager m = DBStoredConceptManager.load(connection);
        m.addConcept("apples", "apple apples macintosh");
        m.addConcept("bananas", "banana bananas");
        Map<String, String> c = m.getConceptMap();

        assertEquals(2, c.size());
        assertEquals("apple apples macintosh", c.get("apples"));

        m.updateConcept("apples", "apple pinklady apples");
        c = m.getConceptMap();
        assertEquals("apple pinklady apples", c.get("apples"));

        try {
            m.addConcept("apples", "blah");
            fail("Should fail to insert unique");
        } catch (Exception e) {

        }

        m.deleteConcepts();
        c = m.getConceptMap();
        assertEquals(0, c.size());

        m.addConcept("apples", "more apples");
        c = m.getConceptMap();
        assertEquals("more apples", c.get("apples"));
        m.close();
    }

    @Test
    public void testEscapeInIntegration() throws Exception {
        DBStoredConceptManager m = DBStoredConceptManager.load(connection);
        m.addConcept("apples", "ap'ple' ' '' ''' ? ??");
        Map<String, String> c = m.getConceptMap();
        assertEquals("ap'ple' ' '' ''' ? ??", c.get("apples"));
    }

    @Test
    public void testMapping() throws Exception {
        DBStoredConceptManager m = DBStoredConceptManager.load(connection);
        m.addConcept("apples", "apple apples");
        m.addConcept("fruits", "{apples} oranges");
        String rewritten = m.rewriteQuery("a {apples} b");
        assertEquals("a (apple apples) b", rewritten);

        rewritten = m.rewriteQuery("a {fruits} b");
        assertEquals("a ((apple apples) oranges) b", rewritten);
        m.close();
    }
*/
/*    @Test
    public void testLoadingXLSX() throws Exception {
        System.out.println(Paths.get(".").toAbsolutePath());
        try(DBStoredConceptManager m = DBStoredConceptManager.load(connection)) {
            Path p = Paths.get("src/test/resources/test-docs/testConcepts.xlsx");
            m.load(p, false);
            Map<String, String> c = m.getConceptMap();
            assertEquals("\u062A\u0641\u0627\u062d\u0629", c.get("apple"));
            assertEquals("[0-9]+", c.get("number"));
        }
    }*/
}

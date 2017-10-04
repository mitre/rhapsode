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

package org.rhapsode.lucene.schema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;


public class IndivFieldCopierTest {

    static String LANG_ID_STRING = null;

    @Before
    public void init() throws Exception {

        LANG_ID_STRING = FileUtils.readFileToString(
                Paths.get(this.getClass().getResource("/lang_id_test.txt").toURI()).toFile(),
                "UTF-8");
    }

    @Test
    public void testCapture() {
        IndivFieldMapper c = new CaptureFieldMapper("toField", "(\\d+)", "a $1 b",
                CaptureFieldMapper.FAIL_POLICY.EXCEPTION);
        String[] args = new String[] {
                "q w 123 x yz",
                "456"
        };
        String[] copied = c.map(args);
        assertEquals("a 123 b", copied[0]);
        assertEquals("a 456 b", copied[1]);

        c = new CaptureFieldMapper("toField", "(\\d)(\\d+)", "a $1->$2 b",
                CaptureFieldMapper.FAIL_POLICY.EXCEPTION);
        copied = c.map(args);
        assertEquals("a 1->23 b", copied[0]);
        assertEquals("a 4->56 b", copied[1]);
    }

    @Test
    public void testChain() {
        IndivFieldMapper c1 = new CaptureFieldMapper("toField",
                "(?i)([^a-z \r\n]+)", "$1",
                CaptureFieldMapper.FAIL_POLICY.EXCEPTION);
        IndivFieldMapper c2 = new LangIdMapper("toField", 10, 2000, false);

        IndivFieldMapper mapper = new ChainedFieldMapper("toField", Lists.asList(c1, new IndivFieldMapper[]{c2}));
        String[] args = new String[]{
                LANG_ID_STRING
        };
        String[] ret = mapper.map(args);
        assertEquals("zh-CN", ret[0]);

        //now try the full file without the capture mapper
        ret = c2.map(args);
        assertEquals("en", ret[0]);
    }

    @Test
    public void testLangIdsMapper() {
        IndivFieldMapper mapper = new LangIdsMapper("toField", 10, 2000, false, -1.0);
        String[] args = new String[]{
                LANG_ID_STRING
        };
        String[] ret = mapper.map(args);

        assertEquals(1, ret.length);
        assertTrue(ret[0].startsWith("en :"));

    }

    @Test
    public void testIdentityMapper() {
        IdentityFieldMapper mapper = new IdentityFieldMapper("toField");
        String [] args = new String[] {
                "   the quick    \r\n\r\n\r\n",
                null,
                "brown \r\n\r\n   \r  \n   \r  \n \r\n fox jumped    "
        };
        String[] ret = mapper.map(args);
        assertTrue(ret[0].startsWith("the"));
        assertTrue(ret[0].endsWith("quick"));
        assertTrue(ret[1].contains("brown \n\nfox"));


        IdentityFieldMapper.setTrim(false);
        ret = mapper.map(args);
        assertTrue(ret[0].startsWith("  "));
        assertTrue(ret[0].endsWith("quick    \n\n"));

        IdentityFieldMapper.setCompressNewLines(false);
        ret = mapper.map(args);
        assertTrue(ret[1].contains("\r\n\r\n   \r"));

        IdentityFieldMapper.setFilterNull(false);
        ret = mapper.map(args);
        assertEquals(3, ret.length);
        assertNull(ret[1]);

    }

    @Test
    public void testStackOverflow() {
        StringBuilder sb = new StringBuilder();
        sb.append("a");
        for (int i = 0; i < 100000; i++) {
            sb.append("\r\n\n");
        }
        sb.append("b");

        IdentityFieldMapper mapper = new IdentityFieldMapper("toField");
        String [] args = new String[] {
                "   the quick    \r\n\r\n\r\n",
                null,
                sb.toString(),
        };
        mapper.map(args);
    }
}

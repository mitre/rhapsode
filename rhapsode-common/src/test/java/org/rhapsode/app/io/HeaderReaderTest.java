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
package org.rhapsode.app.io;

import org.junit.Test;
import org.rhapsode.app.io.CSVTableReader;
import org.rhapsode.app.io.HeaderReader;
import org.rhapsode.app.io.XLSTableReader;
import org.rhapsode.app.io.XLSXStreamingTableReader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class HeaderReaderTest {
    @Test
    public void testHeaderReaderXLSX() throws Exception {
        Path p = Paths.get(getClass().getResource("/test-docs/testMissingHeader.xlsx").toURI());
        HeaderReader headerReader = new HeaderReader(true);
        XLSXStreamingTableReader r = new XLSXStreamingTableReader(p, null, headerReader, true);
        r.parse();
        List<String> headers = headerReader.getHeaders();
        assertEquals(3, headers.size());
        assertEquals("COL_1", headers.get(1));
    }

    @Test
    public void testHeaderReaderXLS() throws Exception {
        Path p = Paths.get(getClass().getResource("/test-docs/testMissingHeader.xls").toURI());
        HeaderReader headerReader = new HeaderReader(true);
        XLSTableReader r = new XLSTableReader(p, null, headerReader, true);
        r.parse();
        List<String> headers = headerReader.getHeaders();
        assertEquals(3, headers.size());
        assertEquals("COL_1", headers.get(1));
    }

    @Test
    public void testHeaderReaderTXT() throws Exception {
        Path p = Paths.get(getClass().getResource("/test-docs/testMissingHeader.csv").toURI());
        HeaderReader headerReader = new HeaderReader(true);
        CSVTableReader r = new CSVTableReader(p, headerReader, ',', StandardCharsets.UTF_8, true);
        r.parse();
        List<String> headers = headerReader.getHeaders();
        assertEquals(3, headers.size());
        assertEquals("COL_1", headers.get(1));
    }

}

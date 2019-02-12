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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;


public class TableReaderTest {
    @Test
    public void testHeaderReaderXLSX() throws Exception {
        Path p = Paths.get(getClass().getResource("/test-docs/testGeneral.xlsx").toURI());
        HeaderReader headerReader = new HeaderReader(true);
        XLSXStreamingTableReader r = new XLSXStreamingTableReader(p, null, headerReader, true);
        r.parse();
        List<String> headers = headerReader.getHeaders();
        assertEquals(headers.size(), 46);
        for (int i = 1; i < 47; i++) {
            String h = headers.get(i - 1);
            assertEquals("Column" + i, h);
        }
    }

    @Test
    public void testHeaderReaderXLS() throws Exception {
        Path p = Paths.get(getClass().getResource("/test-docs/testGeneral.xls").toURI());
        HeaderReader headerReader = new HeaderReader(true);
        XLSTableReader r = new XLSTableReader(p, null, headerReader, true);
        r.parse();
        List<String> headers = headerReader.getHeaders();
        assertEquals(headers.size(), 46);
        for (int i = 1; i < 47; i++) {
            String h = headers.get(i - 1);
            assertEquals("Column" + i, h);
        }
    }

    @Test
    public void testBasic() throws Exception {
        Path p = Paths.get(getClass().getResource("/test-docs/testGeneral.xls").toURI());
        XLSTableReader r = new XLSTableReader(p, null, new TruthHandler(), true);
        r.parse();
        r.close();

        p = Paths.get(getClass().getResource("/test-docs/testGeneral.xlsx").toURI());
        XLSXStreamingTableReader rx = new XLSXStreamingTableReader(p, null, new TruthHandler(), true);
        rx.parse();
        rx.close();

    }

    @Test
    public void testIndexing() throws Exception {
        Path p = Paths.get(getClass().getResource("/test-docs/testInput.xlsx").toURI());
        XLSXStreamingTableReader rx = new XLSXStreamingTableReader(p, "Sheet2", new IgnoringHandler(), true);
        rx.parse();
        rx.close();

    }

    private class IgnoringHandler extends RowReader {

        @Override
        public boolean process(Map<String, String> data) throws IOException {
            return true;
        }
    }

    private class DebugHandler extends RowReader {

        @Override
        public boolean process(Map<String, String> data) {
            for (Map.Entry<String, String> e : data.entrySet()) {
                System.out.println("D: >" + e.getKey() + "< : " + e.getValue());
            }
            System.out.println("");
            return true;
        }
    }

    private class TruthHandler extends RowReader {

        private List<Map<String, String>> truth = new ArrayList<>();

        TruthHandler() {
            addTruth(truth, "Column1", "the");
            addTruth(truth, "Column2", "quick");
            addTruth(truth, "Column3", "brown");
            addTruth(truth, "Column4", "fox",
                    "Column10", "jumped",
                    "Column15", "over",
                    "Column20", "the",
                    "Column25", "lazy",
                    "Column30", "brown",
                    "Column40", "dog"
            );
            addTruth(truth, "Column1", "1");
            addTruth(truth, "Column1", "1.00001");
            addTruth(truth, "Column1", "10/21/16");
        }

        private void addTruth(List<Map<String, String>> truth, String... data) {
            Map<String, String> m = new HashMap<>();
            for (int i = 0; i < data.length - 1; i += 2) {
                m.put(data[i], data[i + 1]);
            }
            truth.add(m);
        }

        @Override
        public boolean process(Map<String, String> data) {
            if (truth.size() <= 0) {
                //skip
                return false;
            }
            Map<String, String> t = truth.remove(0);
            assertEquals(t.size(), data.size());
            assertEquals(t, data);
            return true;
        }
    }

}

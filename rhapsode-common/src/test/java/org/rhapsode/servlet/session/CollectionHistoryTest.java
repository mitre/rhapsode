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

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.junit.Test;
import org.rhapsode.app.session.CollectionsHistory;

public class CollectionHistoryTest extends ServletSessionTestBase {

    @Test
    public void basicTest() throws Exception {
        CollectionsHistory ch = CollectionsHistory.load(connection);
        for (int i = 0; i < 15; i++) {
            ch.addLoaded(Paths.get("collection_" + i));
            Thread.sleep(500);
        }
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        assertEquals(10, ch.getPaths().size());
        assertTrue(ch.getPaths().get(0).getLeft().endsWith("collection_14"));

        ch.addLoaded(Paths.get("collection_6"));
        assertEquals(10, ch.getPaths().size());
        assertTrue(ch.getPaths().get(0).getLeft().endsWith("collection_6"));
        assertTrue(ch.getPaths().get(1).getLeft().endsWith("collection_14"));

    }


}

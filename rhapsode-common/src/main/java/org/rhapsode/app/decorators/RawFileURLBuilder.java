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

package org.rhapsode.app.decorators;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;


public class RawFileURLBuilder {
    /**
     * @param rawFilePath
     * @param relPath
     * @return null if raw file path or rel path is null or if actual file doesn't exist
     */
    public static String build(Path rawFilePath, String relPath) {
        //System.err.println("about to build link >" + rawFilePath + "< : >" + relPath + "<");
        if (rawFilePath == null || relPath == null) {
            return null;
        }
        Path fullPath = rawFilePath.resolve(relPath);
        if (Files.isRegularFile(fullPath)) {
            try {
                return "/rhapsode/download/" + URLEncoder.encode(relPath, "UTF-8").replaceAll("\\+", "%20");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return null;
            }
        } else {
//            System.err.println("Not a regular file>" + fullPath + "<\n>" + fullPath.toAbsolutePath() + "<");
            return null;
        }
    }
}

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

package org.rhapsode.lucene;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;

public class CreateTestCorpus {

    public static void main(String[] args) {
        Path outdir = Paths.get("");
        //for (int k = 1; k < 11; k++) {
        int k = 1;
        StringBuilder sb = new StringBuilder();
        for (int i = 100000; i > 0; i--) {
            sb.append(i).append(" ");
//                Path subdir = outdir.resolve(Integer.toString(k));
            Path subdir = outdir.resolve(StringUtils.leftPad(Integer.toString(i), 6, '0').substring(0, 3));
            Path outputFile = subdir.resolve(StringUtils.leftPad(Integer.toString(i), 6, '0') + ".txt");
            try {
                Files.createDirectories(outputFile.getParent());
                Writer w = Files.newBufferedWriter(outputFile);
                w.write(sb.toString());
                w.flush();
                w.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        //}
    }
}

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

package org.rhapsode.io.html;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

public class HTMLTableWriter {
    Writer writer = null;
    private final static String DEFAULT_ENCODING = "UTF-8";


    public HTMLTableWriter(Writer writer, String encoding) throws IOException {
        this.writer = writer;
        init(encoding);
    }

    public HTMLTableWriter(File f) throws IOException {
        this(f, DEFAULT_ENCODING);
    }

    public HTMLTableWriter(File f, String encoding) throws IOException {
        writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), encoding));
        init(encoding);
    }

    private void init(String encoding) throws IOException {
        writer.write("<decorate><head>");
        writer.write("<meta http-equiv=\"Content-Type\" content=\"text/constants;charset=" + encoding + "\">");
        writer.write("</head>");
        writer.write("<body><table border=\"2\">\r\n");
    }

    public void writeRowLiteral(String row) throws IOException {
        writer.write(row);
    }

    public void writeRowLiteral(List<String> items) throws IOException {
        writer.write("<tr height=\"21\">");
        for (String it : items) {
            String txt = (it == null) ? "" : it;
            writer.write("<td>" + txt + "</td>");
        }
        writer.write("</tr>\r\n");
    }

    public void writeRowClean(List<String> items) throws IOException {
        writer.write("<tr height=\"21\">");
        for (String it : items) {
            writer.write("<td>" + HTMLWriterUtil.clean(it) + "</td>");
        }
        writer.write("</tr>\r\n");
    }


    public void close() throws IOException {
        writer.write("</table></body></html>\r\n");
        writer.flush();
        writer.close();
    }
}

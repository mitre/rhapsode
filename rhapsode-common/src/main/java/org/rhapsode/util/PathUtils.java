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

package org.rhapsode.util;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


public class PathUtils {

    public static List<Path> listPaths(Path p) throws IOException {
        if (Files.exists(p) && !Files.isDirectory(p)) {
            throw new IOException("This is a regular file, not a directory!");
        }

        DirectoryStream<Path> ds = Files.newDirectoryStream(p);
        Iterator<Path> it = ds.iterator();
        List<Path> list = new LinkedList<>();
        while (it.hasNext()) {
            list.add(it.next());
        }
        return list;
    }

    public static void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir) && !Files.isDirectory(dir)) {
            throw new IOException("This is a regular file, not a directory!");
        }

        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file,
                                             BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir,
                                                      IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }

        });

    }

    public static String platformIndependentPathQuote(String s) {
        if (s == null) {
            return "";
        }
        if (s.indexOf(" ") > -1) {
            s = "\"" + s + "\"";
        }
        return s;
    }
}

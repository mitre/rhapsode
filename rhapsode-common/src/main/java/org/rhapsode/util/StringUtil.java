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

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class StringUtil {

    private static final Pattern MULTI_NEW_LINES_A = Pattern.compile("(?:\r\n)[ \t]*");
    private static final Pattern MULTI_NEW_LINES_B = Pattern.compile("(?:[\r\n])[ \t]*");
    private static final Pattern MULTI_NEW_LINES_C = Pattern.compile("\n\n+");

    private static final String MULTI_LINE_REPLACEMENT = "\n\n";
    private static final String SINGLE_LINE_REPLACEMENT = "\n";


    public static String compoundNewLines(String s) {
        //do this in multiple steps to avoid stackoverflow (A|B)*
        Matcher m = MULTI_NEW_LINES_A.matcher(s);
        s = m.replaceAll(SINGLE_LINE_REPLACEMENT);
        m = MULTI_NEW_LINES_B.matcher(s);
        s = m.replaceAll(SINGLE_LINE_REPLACEMENT);
        m = MULTI_NEW_LINES_C.matcher(s);
        s = m.replaceAll(MULTI_LINE_REPLACEMENT);
        return s;
    }
}

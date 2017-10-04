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
package org.rhapsode.text.windows;

import org.apache.commons.lang3.StringUtils;

public class WindowUtil {

    private static final String delimiter = " ";

    public String getPreString(String[] terms, int start, int numWords) {
        int end = start - 1;
        start = start - numWords;
        return StringUtils.join(terms, start, end, delimiter);
    }

    public String getPostString(String[] terms, int start, int numWords) {
        start = start + 1;
        int end = start + numWords - 1;
        return StringUtils.join(terms, start, end, delimiter);
    }

    public String getPostString(String s, int start, int numWords) {
        if (start >= s.length())
            return StringUtils.EMPTY;
        int i = start;
        int found = 0;
        boolean inAlphaNum = false;
        boolean tmpAlphaNum = false;
        while (i < s.length() && found < numWords) {
            tmpAlphaNum = Character.isLetterOrDigit(s.charAt(i));
            if (inAlphaNum == true && !tmpAlphaNum) {
                found++;
            }
            inAlphaNum = tmpAlphaNum;
            i++;
        }
        //if you haven't hit the end of the string
        //then the last thing you hit was not a letter or digit, so subtract 1
        if (i < s.length() && i > 0 && i - 1 >= start) {
            i--;
        }
        return new String(s.substring(start, i).trim());
    }

    public String getPreString(String s, int start, int numWords) {

        int i = start;
        if (start < 0)
            return StringUtils.EMPTY;

        int found = 0;
        boolean inAlphaNum = false;
        boolean tmpAlphaNum = false;
        while (i >= 0 && found <= numWords) {
            tmpAlphaNum = Character.isLetterOrDigit(s.charAt(i));
            if (inAlphaNum == true && !tmpAlphaNum) {
                found++;
            }
            inAlphaNum = tmpAlphaNum;
            i--;
        }
        return new String(s.substring(i + 1, start).trim());
    }

}

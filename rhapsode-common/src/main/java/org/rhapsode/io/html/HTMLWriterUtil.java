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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.rhapsode.text.Offset;
import org.rhapsode.text.OffsetOverlapDecider;
import org.rhapsode.text.OffsetStartComparator;
import org.rhapsode.text.PriorityOffset;
import org.rhapsode.text.PriorityOffsetLengthStartComparator;
import org.rhapsode.util.StringUtil;

public class HTMLWriterUtil {

    public final static String BREAK = "<br/>\n";

    public static String simpleElementRaw(HTMLElement element, String contents) {
        return String.format("%s%s%s", element.getStart(), contents, element.getEnd());
    }

    public static String simpleElement(HTMLElement element, String contents) {
        return String.format("%s%s%s", element.getStart(), clean(contents), element.getEnd());
    }

    public static String clean(String s) {
        return clean(s, false);
    }

    public static String clean(String s, boolean convertNewLines) {

        if (null == s)
            return "";

        if (convertNewLines == false) {
            return org.apache.commons.lang3.StringEscapeUtils.escapeHtml4(s);

        }
        s = StringUtil.compoundNewLines(s);
        Matcher m = Pattern.compile("\n\n").matcher(s);
        StringBuilder sb = new StringBuilder();
        int last = 0;
        while (m.find()) {
            //trim initial new lines
            if (m.start() == 0) {
                last = m.end();
                continue;
            }
            String bit = s.substring(last, m.start());
            sb.append(org.apache.commons.lang3.StringEscapeUtils.escapeHtml4(bit));
            sb.append(BREAK).append(BREAK);
            last = m.end();
        }
        sb.append(org.apache.commons.lang3.StringEscapeUtils.escapeHtml4(s.substring(last)));
        return sb.toString();
    }

    public String colorize(String s, List<Offset> offsets) {
        //this requires that offsets be sorted and non-overlapping!
        StringBuilder sb = new StringBuilder();
        int last = 0;
        for (Offset offset : offsets) {
            sb.append(clean(s.substring(last, offset.startOffset())));
            sb.append("<span style=\"color: red; background-color: #FFFF00\">");
            sb.append(clean(s.substring(offset.startOffset(), offset.endOffset())));
            sb.append("</span>");
            last = offset.endOffset();
        }
        sb.append(clean(s.substring(last)));
        return sb.toString();
    }

    public String colorize(String s, String className, List<Offset> offsets) {
        //this requires that offsets be sorted and non-overlapping!
        StringBuilder sb = new StringBuilder();
        int last = 0;
        String classStart = "<span class=\"" + className + "\">";
        String classEnd = "</span>";
        for (Offset offset : offsets) {
            sb.append(clean(s.substring(last, offset.startOffset())));
            sb.append(classStart);
            sb.append(clean(s.substring(offset.startOffset(), offset.endOffset())));
            sb.append(classEnd);
            last = offset.endOffset();
        }
        sb.append(clean(s.substring(last)));
        return sb.toString();
    }

    public String safeMarkup(List<PriorityOffset> offsets, String s, boolean replaceNewLines) {
        if (s == null) {
            return StringUtils.EMPTY;
        }
        //offsets can be in any order and can be overlapping
        OffsetOverlapDecider d = new OffsetOverlapDecider();
        Collections.sort(offsets, new PriorityOffsetLengthStartComparator());
        List<PriorityOffset> nonoverlapping = new ArrayList<PriorityOffset>();
        for (PriorityOffset offset : offsets) {
            if (!d.hasAlreadySeen(offset)) {
                nonoverlapping.add(offset);
                d.add(offset);
            }
        }
        Collections.sort(nonoverlapping, new OffsetStartComparator());
        return dangerousMarkup(nonoverlapping, s, replaceNewLines);
    }

    public String dangerousMarkup(List<PriorityOffset> offsets, String s, boolean replaceNewLines) {
        //offsets must be in order and not overlapping;
        if (offsets == null || offsets.size() == 0) {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        int last = 0;
        for (PriorityOffset offset : offsets) {
            sb.append(clean(s.substring(last, offset.startOffset()), replaceNewLines));
            sb.append("<span class=\"").append(offset.getLabel()).append("\">");
            sb.append(clean(s.substring(offset.startOffset(), offset.endOffset()), replaceNewLines));
            sb.append("</span>");
            last = offset.endOffset();
        }
        sb.append(clean(s.substring(last), replaceNewLines));
        return sb.toString();
    }

}

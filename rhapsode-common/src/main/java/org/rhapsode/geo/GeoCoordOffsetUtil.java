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

package org.rhapsode.geo;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttributeImpl;

public class GeoCoordOffsetUtil {
    public static final String marker = ">";
    public static final int markerLength = marker.length();

    public static String asIfContent(String s) {
        return String.format("%s%s", marker, s);
    }


    public static OffsetAttribute getOffset(String s) {
        //beware, can return null!!!

        if (StringUtils.isBlank(s))
            return null;

        String[] parts = s.split(",");

        if (parts.length < 2)
            return null;

        int start = -1;
        int end = -1;
        try {
            start = Integer.parseInt(parts[0]);
            end = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
        if (start < 0 || end < 0)
            return null;

        OffsetAttribute offset = new OffsetAttributeImpl();
        offset.setOffset(start, end);
        return offset;
    }

    public static String getContent(String s) {
        if (StringUtils.isBlank(s))
            return StringUtils.EMPTY;

        int i = s.indexOf(marker);
        if (i > -1 && markerLength < s.length()) {
            return s.substring(markerLength);
        }
        return StringUtils.EMPTY;
    }
}

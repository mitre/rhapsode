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
import org.rhapsode.text.Offset;

import java.util.ArrayList;
import java.util.List;

public class WindowBuilder {
    /*
            Lightweight builder to build windows.
            Basic assumptions: for the offset offset.start() < offset.end()
     */
    private final static WindowUtil windowUtil = new WindowUtil();

    public List<Window> buildWindows(String[] terms, List<Offset> termOffsets, int pre, int post) {
        List<Window> windows = new ArrayList<Window>();

        for (Offset offset : termOffsets) {
            Window tmp = buildWindow(terms, offset, pre, post);
            if (null != tmp)
                windows.add(tmp);
        }
        return windows;
    }

    public Window buildWindow(String[] terms, Offset offset, int pre, int post) {
        //this does no error checking and can throw an IndexOutOfBoundsException if the offset
        //start is < 0 or if the end is > terms.size()
        String preString = windowUtil.getPreString(terms, offset.startOffset(), pre);
        String targ = StringUtils.join(terms, offset.startOffset(), offset.endOffset(), StringUtils.SPACE);
        String postString = windowUtil.getPostString(terms, offset.endOffset(), post);
        return new Window(preString, targ, postString);

    }

    public Window buildWindow(String string, Offset offset, int pre, int post) {
        return buildWindow(string, offset.startOffset(), offset.endOffset(), pre, post);
    }

    public Window buildWindow(String string, int start, int end, int pre, int post) {
        String preString = windowUtil.getPreString(string, start, pre);
        String postString = windowUtil.getPostString(string, end, post);
        String targ = new String(string.substring(start, end));

        return new Window(preString, targ, postString);
    }
}


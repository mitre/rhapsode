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
package org.rhapsode.text;

import java.io.Serializable;
import java.util.Comparator;

public class PriorityOffsetLengthStartComparator implements Comparator<PriorityOffset>, Serializable {

    private static final long serialVersionUID = 7526472295622776147L;

    @Override
    public int compare(PriorityOffset offsetA, PriorityOffset offsetB) {

        //this should sort priority ascending, length desc and start asc
        if (offsetA.getPriority() < offsetB.getPriority()) {
            return -1;
        } else if (offsetA.getPriority() > offsetB.getPriority()) {
            return 1;
        }
        if (offsetA.length() < offsetB.length()) {
            return 1;
        } else if (offsetA.length() > offsetB.length()) {
            return -1;
            //by here, the length is the same
        } else if (offsetA.startOffset() < offsetB.startOffset()) {
            return -1;
        } else if (offsetA.startOffset() > offsetB.startOffset()) {
            return 1;
        }
        return 0;
    }


}

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

public class PriorityOffset extends Offset {
    private final int priority;
    private final String label;

    public PriorityOffset(int start, int end, int priority, String label) {
        super(start, end);
        this.priority = priority;
        this.label = label;
    }

    public int getPriority() {
        return priority;
    }

    public String getLabel() {
        return label;
    }

    public boolean equals(Object other) {
        if (other == null)
            return false;
        if (this.getClass().equals(other.getClass()) &&
                ((PriorityOffset) other).startOffset() == startOffset() &&
                ((PriorityOffset) other).endOffset() == endOffset() &&
                ((PriorityOffset) other).getPriority() == getPriority() &&
                ((PriorityOffset) other).getLabel().equals(getLabel()))
            return true;
        return false;
    }

    public int hashCode() {
        return super.hashCode() + 1369 * priority;
    }

    @Override
    public String toString() {
        String builder = "PriorityOffset [priority=" +
                priority +
                ", label=" +
                label +
                ", startOffset()=" +
                startOffset() +
                ", endOffset()=" +
                endOffset() +
                ", length()=" +
                length() +
                ", toString()=" +
                super.toString() +
                ", getClass()=" +
                getClass() +
                "]";
        return builder;
    }


}


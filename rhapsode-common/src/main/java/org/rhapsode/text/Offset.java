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

public class Offset {

    /**
     * @param args
     */
    private int start = -1;
    private int end = -1;

    public Offset() {
        //argh!!!
        //need to have mutability for bean function in jackson
    }

    public Offset(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public int startOffset() {
        return start;
    }

    public int endOffset() {
        return end;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public int length() {
        return end - start + 1;

    }

    public boolean equals(Object other) {
        if (other == null)
            return false;
        if (this.getClass().equals(other.getClass()) &&
                ((Offset) other).startOffset() == start &&
                ((Offset) other).endOffset() == end)
            return true;
        return false;
    }

    public int hashCode() {
        int hash = (37 * start) + end;
        return hash;
    }

    public String toString() {
        return start + "->" + end;
    }

    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}

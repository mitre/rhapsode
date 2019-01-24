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

public class Window {

    /**
     * @param args
     */
    protected String pre = new String("");
    protected String target = "";
    protected String post = "";

    public Window(String pre, String target, String post) {
        this.pre = pre;
        this.post = post;
        this.target = target;

    }

    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

    public String toString() {
        //this assumes left to right language
        //use WindowDecorator to present proper order
        return pre + "\t" + target + "\t" + post;
    }

    public String getPre() {
        return pre;
    }

    public String getPost() {
        return post;
    }

    public String getTarget() {
        return target;
    }

    public int getSize() {
        int size = 0;
        if (pre != null) {
            size += pre.length();
        }
        if (target != null) {
            size += target.length();
        }
        if (post != null) {
            size += post.length();
        }
        return size;
    }

}

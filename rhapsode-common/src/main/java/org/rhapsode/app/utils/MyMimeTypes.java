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

package org.rhapsode.app.utils;

import org.eclipse.jetty.http.MimeTypes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MyMimeTypes {
    MimeTypes types = new MimeTypes();

    //expects value, key order as in:
    //application/vnd.ms-word.document.macroEnabled.12 docm
    public MyMimeTypes(File f) {
        Properties props = new Properties();
        InputStream is = null;
        try {
            is = new FileInputStream(f);
            props.load(is);
        } catch (IOException e) {

        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                //swallow
            }
        }
        for (Object k : props.keySet()) {
            String kString = (String) k;
            String v = props.getProperty(kString);
            types.addMimeMapping(v, kString);
        }
    }

    public MimeTypes getTypes() {
        return types;
    }
}

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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PropsUtil {
    public static Properties loadProperties(File f) {

        Properties props = new Properties();
        InputStream is = null;
        try {
            is = new FileInputStream(f);
            props.load(is);
        } catch (Exception e) {
            //swallow
        } finally {
            try {
                if (null != is) {
                    is.close();
                }
            } catch (IOException e) {
                //swallow
            }
        }
        return props;

    }

    public static Matcher getMatcher(Properties props, String key, Matcher defaultMissing) {
        String pat = props.getProperty(key);
        if (pat == null) {
            return defaultMissing;
        }
        return Pattern.compile(pat).matcher("");
    }

    public static Pattern getPattern(Properties props, String key, Pattern defaultMissing) {
        String pat = props.getProperty(key);
        if (pat == null) {
            return defaultMissing;
        }
        return Pattern.compile(pat);
    }


    public static String getString(Properties props, String key, String defaultMissing) {
        //helper class that does nothing
        //created to make interface consistent
        return props.getProperty(key, defaultMissing);
    }

    public static File getFile(Properties props, String key, File defaultMissing) {
        String fName = props.getProperty(key, "");
        File f = null;
        if (fName.equals("")) {
            f = defaultMissing;
        }
        //defensively expand potentially relative file length
        f = new File(new File(fName).getAbsolutePath());

        return f;
    }

    public static boolean getBoolean(Properties props, String key, boolean defaultMissing) {
        String val = props.getProperty(key, "");
        if (val == null || val.equals("")) {
            return defaultMissing;
        } else if (val.toLowerCase().indexOf("y") > -1 || val.toLowerCase().indexOf("true") > -1) {
            return true;
        } else if (val.toLowerCase().indexOf("n") > -1 || val.toLowerCase().indexOf("false") > -1) {
            return false;
        }
        return defaultMissing;

    }

    public static long getLong(Properties props, String key, long defaultMissing) {
        long ret = defaultMissing;
        try {
            String s = props.getProperty(key, "");
            ret = Long.parseLong(s);
        } catch (NumberFormatException e) {
            //swallow
        }
        return ret;
    }

    public static int getInt(Properties props, String key, int defaultMissing) {
        int ret = defaultMissing;
        try {
            String s = props.getProperty(key, "");
            ret = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            //swallow
        }
        return ret;
    }

    public static double getDouble(Properties props, String key, double defaultMissing) {
        double ret = defaultMissing;
        try {
            String s = props.getProperty(key, "");
            ret = Double.parseDouble(s);
        } catch (NumberFormatException e) {
            //swallow
        }
        return ret;

    }
}

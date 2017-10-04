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

import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ParamUtil {

    public static String getString(String key, String defaultMissing) {
        if (key == null)
            return defaultMissing;

        return key;
    }

    public static Path getAbsolutePath(String fName, Path defaultMissing) {
        Path p = null;
        if (StringUtils.isBlank(fName)) {
            p = defaultMissing;
        } else {
            //defensively expand potentially relative file length
            p = Paths.get(fName).toAbsolutePath();
        }
        return p;
    }


    public static boolean getBooleanChecked(String val, boolean defaultMissing) {
        if (val == null) {
            return false;
        }
        return getBoolean(val, defaultMissing);
    }

    public static boolean getBoolean(String val, boolean defaultMissing) {

        if (val == null || val.equals("")) {
            return defaultMissing;
        } else if (val.toLowerCase().indexOf("y") > -1 ||
                val.toLowerCase().indexOf("true") > -1 ||
                val.toLowerCase().indexOf("on") > -1) {
            return true;
        } else if (val.toLowerCase().indexOf("off") > -1 ||
                val.toLowerCase().indexOf("n") > -1 || val.toLowerCase().indexOf("false") > -1) {
            return false;
        } else if (val.equals("1")) {
            return true;
        } else if (val.equals("0")) {
            return false;
        }
        return defaultMissing;

    }

    public static long getLong(String val, long defaultMissing) {
        long ret = defaultMissing;
        if (val == null || val.equals(""))
            return ret;
        try {
            ret = Long.parseLong(val);
        } catch (NumberFormatException e) {
            //swallow
        }
        return ret;
    }

    public static long getLong(String val, long defaultMissing, long min, long max) {
        long ret = getLong(val, defaultMissing);
        return rangeCheckLong(ret, min, max);
    }

    public static long getLongDefault(String val, long defaultMissing, long min, long max) {
        long ret = getLong(val, defaultMissing);
        return rangeCheckLong(ret, defaultMissing, min, max);
    }

    public static long rangeCheckLong(long val, long min, long max) {
        if (val < min)
            return min;
        if (val > max)
            return max;
        return val;
    }

    public static long rangeCheckLong(long val, long defaultOutOfBounds, long min, long max) {
        if (val < min)
            return defaultOutOfBounds;
        if (val > max)
            return defaultOutOfBounds;
        return val;
    }

    public static int getInt(String val, int defaultMissing) {
        int ret = defaultMissing;
        if (val == null || val.equals(""))
            return ret;
        try {
            ret = Integer.parseInt(val);
        } catch (NumberFormatException e) {
            //swallow
        }
        return ret;
    }

    public static int getInt(String val, int defaultMissing, int min, int max) {
        int ret = getInt(val, defaultMissing);
        return rangeCheckInt(ret, min, max);
    }

    public static int getIntDefault(String val, int defaultMissing, int min, int max) {
        int ret = getInt(val, defaultMissing);
        return rangeCheckInt(ret, defaultMissing, min, max);
    }

    public static int rangeCheckInt(int val, int min, int max) {
        if (val < min)
            return min;
        if (val > max)
            return max;
        return val;
    }

    public static int rangeCheckInt(int val, int defaultOutOfBounds, int min, int max) {
        if (val < min)
            return defaultOutOfBounds;
        if (val > max)
            return defaultOutOfBounds;
        return val;
    }

    public static double getDouble(String val, double defaultMissing) {
        double ret = defaultMissing;
        if (val == null || val.equals(""))
            return ret;

        try {
            ret = Double.parseDouble(val);
        } catch (NumberFormatException e) {
            //swallow
        }
        return ret;
    }

    public static double getDouble(String val, double defaultMissing, double min, double max) {
        double ret = getDouble(val, defaultMissing);
        return rangeCheckDouble(ret, min, max);
    }

    public static double getDoubleDefault(String val, double defaultMissing, double min, double max) {
        double ret = getDouble(val, defaultMissing);
        return rangeCheckDouble(ret, defaultMissing, min, max);
    }

    public static double rangeCheckDouble(double val, double min, double max) {
        if (val < min)
            return min;
        if (val > max)
            return max;
        return val;
    }

    public static double rangeCheckDouble(double val, double defaultOutOfBounds, double min, double max) {
        if (val < min)
            return defaultOutOfBounds;
        if (val > max)
            return defaultOutOfBounds;
        return val;
    }
}

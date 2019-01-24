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
package org.rhapsode.lucene.schema;

import java.util.ArrayList;
import java.util.List;

import org.rhapsode.util.StringUtil;

public class IdentityFieldMapper extends IndivFieldMapper {
    private static boolean trim = true;
    private static boolean compressNewLines = true;
    private static boolean filterNull = true;


    public IdentityFieldMapper(String toField) {
        super(toField);
    }

    public static void setTrim(boolean trim) {
        IdentityFieldMapper.trim = trim;
    }

    public static void setCompressNewLines(boolean compressNewLines) {
        IdentityFieldMapper.compressNewLines = compressNewLines;
    }

    public static void setFilterNull(boolean filterNull) {
        IdentityFieldMapper.filterNull = filterNull;
    }

    @Override
    public String[] map(String[] vals) {
        if (vals == null) {
            return vals;
        }
        List<String> ret = new ArrayList<>();
        for (String v : vals) {
            if (v != null) {
                if (trim) {
                    v = v.trim();
                }
                if (compressNewLines) {
                    v = StringUtil.compoundNewLines(v);
                }
                ret.add(v);
            } else {
                if (!filterNull) {
                    ret.add(v);
                }
            }
        }
        return ret.toArray(new String[ret.size()]);
    }

}

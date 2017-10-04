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

package org.rhapsode.app.session;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;


public class StringListDynamicParameter extends DynamicParameter<List<String>> {
    final static Gson GSON = new GsonBuilder().create();


    public StringListDynamicParameter(String displayName, PREFIX prefix, String name, String defaultValue) {
        super(displayName, prefix, name, parseStringToValue(defaultValue));
    }

    public static List<String> parseStringToValue(String val) {
        if (val == null) {
            throw new IllegalArgumentException("Must specify non null value for default value");
        } else if ("".equals(val)) {
            return new ArrayList<String>();
        }
        Type typeOfList = new TypeToken<List<String>>() {
        }.getType();
        return GSON.fromJson(val, typeOfList);
    }


    @Override
    public String getDefaultValueAsString() {
        return GSON.toJson(getDefaultValue());
    }

    @Override
    public List<String> getValueFromString(String s) {
        return parseStringToValue(s);
    }

    public static String valueToString(List<String> strings) {
        return GSON.toJson(strings);
    }
}

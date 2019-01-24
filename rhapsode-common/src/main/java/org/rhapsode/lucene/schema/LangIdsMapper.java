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

import com.optimaize.langdetect.DetectedLanguage;
import org.rhapsode.lucene.utils.LanguageIDWrapper;


public class LangIdsMapper extends LangIdMapper {
    final double minConfidence;

    public LangIdsMapper(String toField, int minChars, int maxChars, boolean langOnly, double minConfidence) {
        super(toField, minChars, maxChars, langOnly);
        this.minConfidence = minConfidence;
    }

    @Override
    public String[] map(String[] vals) {
        String langString = getString(vals);
        if (langString.length() < minChars) {
            return new String[0];
        }

        List<DetectedLanguage> langs = LanguageIDWrapper.getProbabilities(langString);
        List<String> ret = new ArrayList<>();
        StringBuilder langVal = new StringBuilder();
        for (DetectedLanguage lang : langs) {
            if (lang.getProbability() >= minConfidence) {
                langVal.setLength(0);
                if (langOnly) {
                    langVal.append(lang.getLocale().getLanguage());
                } else {
                    langVal.append(lang.getLocale().toString());
                }
                langVal.append(" : ");
                langVal.append(Double.toString(lang.getProbability()));
                ret.add(langVal.toString());
            }
        }
        return ret.toArray(new String[ret.size()]);
    }

}

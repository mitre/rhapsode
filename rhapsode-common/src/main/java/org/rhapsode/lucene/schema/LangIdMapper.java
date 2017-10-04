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

import com.google.common.base.Optional;
import com.optimaize.langdetect.i18n.LdLocale;
import org.apache.commons.lang3.StringUtils;
import org.rhapsode.lucene.utils.LanguageIDWrapper;

import java.io.IOException;


public class LangIdMapper extends IndivFieldMapper {

    static {
        try {
            LanguageIDWrapper.loadBuiltInModels();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    final int minChars;
    final int maxChars;
    final boolean langOnly;//record only the lang: zh-CN -> zh if true

    public LangIdMapper(String toField, int minChars, int maxChars, boolean langOnly) {
        super(toField);
        this.minChars = minChars;
        this.maxChars = maxChars;
        this.langOnly = langOnly;
    }

    @Override
    public String[] map(String[] vals) {
        String s = getString(vals);
        if (minChars > -1 && s.length() < minChars) {
            return new String[0];
        }

        Optional<LdLocale> ldLocale = LanguageIDWrapper.detect(s);
        if (ldLocale.isPresent()) {
            String langVal = "";
            if (langOnly) {
                langVal = ldLocale.get().getLanguage();
            } else {
                langVal = ldLocale.get().toString();
            }
            return new String[]{
                    langVal
            };
        }
        return new String[0];
    }

    String getString(String[] vals) {
        StringBuilder sb = new StringBuilder();
        for (String s : vals) {
            if (maxChars > -1 && sb.length() + s.length() > maxChars) {
                int end = maxChars - (sb.length());
                if (end < s.length()) {
                    sb.append(s.substring(0, end).trim());
                }
                break;
            } else {
                sb.append(s.trim()).append(StringUtils.SPACE);
            }
            if (sb.length() > maxChars) {
                break;
            }
        }
        String s = null;
        if (sb.length() > maxChars) {
            s = sb.substring(0, maxChars);
        } else {
            s = sb.toString();
        }
        return s;
    }

}

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

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.rhapsode.app.contants.C;
import org.rhapsode.util.LanguageDirection;

public class DynamicParameters {

    public final static StringDynamicParameter COLLECTIONS_ROOT = getString(
            "Collections Directory",
            DynamicParameter.PREFIX.COMMON, C.COLLECTION_ROOT, "collections");

    //display order in the app is based on initialization order here
    //order matters!!!
    public final static StringDynamicParameter DEFAULT_CONTENT_FIELD = getString(
            "Default Content Field",
            DynamicParameter.PREFIX.COMMON, C.DEFAULT_CONTENT_FIELD, "content"
    );
    //common parameters
    public final static BooleanDynamicParameter SHOW_LANGUAGE_DIRECTION = getBoolean(
            "Show Language Direction",
            DynamicParameter.PREFIX.COMMON, C.SHOW_LANGUAGE_DIRECTION, true);
    public final static LangDirDynamicParameter DEFAULT_LANGUAGE_DIRECTION = getLangDir(
            "Default Language Direction",
            DynamicParameter.PREFIX.COMMON, C.LANG_DIR, LanguageDirection.LTR);
    public final static BooleanDynamicParameter SHOW_SELECTED = getBoolean(
            "Show Selected",
            DynamicParameter.PREFIX.COMMON, C.SHOW_SELECTED, false);
    public final static IntDynamicParameter MAX_FILE_NAME_DISPLAY_LENGTH = getInt(
            "Maximum character length for file name hyperlinks",
            DynamicParameter.PREFIX.COMMON, C.MAX_FILE_NAME_DISPLAY_LENGTH, 36
    );
    public final static IntDynamicParameter MAX_EXTRA_COLUMNS_LEN = getInt(
            "Maximum character length for metadata in row results",
            DynamicParameter.PREFIX.COMMON, C.MAX_METADATA_COLUMN_LENGTH, 20
    );
    public final static IntDynamicParameter MAX_BOOLEAN_CLAUSES = getInt(
            "Maximum Boolean Clauses",
            DynamicParameter.PREFIX.COMMON, C.MAX_BOOLEAN_CLAUSES, 1024
    );
    public final static StringListDynamicParameter FILE_VIEWER_DISPLAY_FIELDS =
            new StringListDynamicParameter("Indexed Document Viewer Fields",
                    DynamicParameter.PREFIX.COMMON, C.INDEXED_DOC_DISPLAY_FIELDS, "");
    public final static StringListDynamicParameter ROW_VIEWER_DISPLAY_FIELDS =
            new StringListDynamicParameter("Metadata in Row Result Fields",
                    DynamicParameter.PREFIX.COMMON, C.ROW_VIEWER_DISPLAY_FIELDS, "");
    public final static IntDynamicParameter BS_MAIN_QUERY_WIDTH = getInt(
            "Main Query Box Width",
            DynamicParameter.PREFIX.BS, C.BS_MAIN_QUERY_BOX_WIDTH, 80);

    //basic search
/*    public static BooleanDynamicParameter BS_SHOW_STORED_BUTTONS = getBoolean(
            "Show Stored Query/Concept Buttons",
            DynamicParameter.PREFIX.BS, C.BS_SHOW_STORED_BUTTONS, false);
*/
    public final static IntDynamicParameter BS_MAIN_QUERY_HEIGHT = getInt(
            "Main Query Box Height",
            DynamicParameter.PREFIX.BS, C.BS_MAIN_QUERY_BOX_HEIGHT, 1);
    public final static IntDynamicParameter BS_FILTER_QUERY_WIDTH = getInt(
            "Filter Query Box Width",
            DynamicParameter.PREFIX.BS, C.BS_FILTER_QUERY_BOX_WIDTH, 80);
    public final static IntDynamicParameter BS_FILTER_QUERY_HEIGHT = getInt(
            "Filter Query Box Height",
            DynamicParameter.PREFIX.BS, C.BS_FILTER_QUERY_BOX_HEIGHT, 1);
    public final static IntDynamicParameter BS_RESULTS_PER_PAGE = getInt(
            "Results Per Page",
            DynamicParameter.PREFIX.BS, C.RESULTS_PER_PAGE, 10);
    //CONCORDANCE
    public final static IntDynamicParameter CONC_MAIN_QUERY_WIDTH = getInt(
            "Main Query Box Width",
            DynamicParameter.PREFIX.CONC, C.CONC_MAIN_QUERY_BOX_WIDTH, 80);
    public final static IntDynamicParameter CONC_MAIN_QUERY_HEIGHT = getInt(
            "Main Query Box Height",
            DynamicParameter.PREFIX.CONC, C.CONC_MAIN_QUERY_BOX_HEIGHT, 1);
    public final static IntDynamicParameter CONC_FILTER_QUERY_WIDTH = getInt(
            "Filter Query Box Width",
            DynamicParameter.PREFIX.CONC, C.CONC_FILTER_QUERY_BOX_WIDTH, 80);
    public static final BooleanDynamicParameter CONC_ALLOW_CLUSTERING = getBoolean(
            "Allow Clustering", DynamicParameter.PREFIX.CONC,
            C.CONC_ALLOW_CLUSTERING, true);
    public final static IntDynamicParameter CONC_FILTER_QUERY_HEIGHT = getInt(
            "Filter Query Box Height",
            DynamicParameter.PREFIX.CONC, C.CONC_FILTER_QUERY_BOX_HEIGHT, 1);
    public final static IntDynamicParameter CONC_MAX_STORED_WINDOWS = getInt(
            "Max. Stored Windows",
            DynamicParameter.PREFIX.CONC,
            C.RESULTS_PER_PAGE, 1000);
    public final static BooleanDynamicParameter CONC_IGNORE_DUPLICATE_WINDOWS = getBoolean(
            "Ignore Duplicate Windows", DynamicParameter.PREFIX.CONC,
            C.IGNORE_DUPLICATE_WINDOWS, false);
    public final static IntDynamicParameter CONC_WORDS_BEFORE = getInt(
            "Words Before", DynamicParameter.PREFIX.CONC,
            C.WORDS_BEFORE, 10);
    public final static IntDynamicParameter CONC_WORDS_AFTER = getInt(
            "Words After",
            DynamicParameter.PREFIX.CONC,
            C.WORDS_AFTER, 10);
    public final static IntDynamicParameter COOCCUR_MAX_RESULTS = getInt("Max Results",
            DynamicParameter.PREFIX.COOCCUR,
            C.NUM_RESULTS, 20);


    //COOCCUR
    public final static IntDynamicParameter COOCCUR_MIN_NGRAM = getInt("Min. Phrase Size",
            DynamicParameter.PREFIX.COOCCUR,
            C.MIN_NGRAM, 1);
    public final static IntDynamicParameter COOCCUR_MAX_NGRAM = getInt("Max Phrase Size",
            DynamicParameter.PREFIX.COOCCUR,
            C.MAX_NGRAM, 1);
    public final static IntDynamicParameter COOCCUR_MIN_TERM_FREQ = getInt("Min. Co-Occurrences",
            DynamicParameter.PREFIX.COOCCUR,
            C.MIN_TERM_FREQ, 5);
    //stored query admin
    public final static BooleanDynamicParameter SQ_SHOW_MAIN_QUERY_TRANSLATION = getBoolean(
            "Show Main Query Translation", DynamicParameter.PREFIX.STORED_QUERY,
            C.SQ_SHOW_MAIN_QUERY_TRANSLATION, false);
    public final static BooleanDynamicParameter SQ_SHOW_FILTER_QUERY = getBoolean(
            "Show Filter Query", DynamicParameter.PREFIX.STORED_QUERY,
            C.SQ_SHOW_FILTER_QUERY, true);
    public final static BooleanDynamicParameter SQ_SHOW_FILTER_QUERY_TRANSLATION = getBoolean(
            "Show Filter Query Translation", DynamicParameter.PREFIX.STORED_QUERY,
            C.SQ_SHOW_FILTER_QUERY_TRANSLATION, false);
    public final static BooleanDynamicParameter SQ_SHOW_MAX_HITS = getBoolean(
            "Show Max Hits", DynamicParameter.PREFIX.STORED_QUERY,
            C.SQ_SHOW_MAX_HITS, false);
    public final static BooleanDynamicParameter SQ_SHOW_DEFAULT_FIELD = getBoolean(
            "Show Default Field", DynamicParameter.PREFIX.STORED_QUERY,
            C.SQ_SHOW_DEFAULT_FIELD, false);
    public final static BooleanDynamicParameter SQ_SHOW_STYLES = getBoolean(
            "Show Styles", DynamicParameter.PREFIX.STORED_QUERY,
            C.SQ_SHOW_STYLES, false);
    public final static BooleanDynamicParameter SQ_SHOW_PRIORITY = getBoolean(
            "Show Priority", DynamicParameter.PREFIX.STORED_QUERY,
            C.SQ_SHOW_PRIORITY, false);
    public final static BooleanDynamicParameter SQ_SHOW_NOTES = getBoolean(
            "Show Notes", DynamicParameter.PREFIX.STORED_QUERY,
            C.SQ_SHOW_NOTES, false);
    public final static StringDynamicParameter TFI_TABLE_DIRECTORY = getString("Directory for Table Files",
            DynamicParameter.PREFIX.TABLE_FILE_INDEXER, C.TABLE_INPUT_DIRECTORY, "input");

    //table indexer
    //link writer
    public final static StringDynamicParameter METADATA_CREATOR = getString("Metadata Creator",
            DynamicParameter.PREFIX.REPORT_WRITER,
            "dc:creator", "Tim Allison");
    final static Map<String, DynamicParameter> params = new LinkedHashMap<>();

    static {

        Field[] declaredFields = DynamicParameters.class.getDeclaredFields();
        for (Field field : declaredFields) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                try {
                    Object val = field.get(null);
                    if (val instanceof DynamicParameter) {
                        params.put(((DynamicParameter) val).getFullName(), (DynamicParameter) val);
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    throw new RuntimeException("Something went horribly wrong while trying to load DynamicParameters");
                }
            }
        }
    }

    static BooleanDynamicParameter getBoolean(String displayName, DynamicParameter.PREFIX prefix,
                                              String name, boolean defaultValue) {
        return new BooleanDynamicParameter(displayName, prefix, name, defaultValue);
    }

    static IntDynamicParameter getInt(String displayName, DynamicParameter.PREFIX prefix,
                                      String name, int defaultValue) {
        return new IntDynamicParameter(displayName, prefix, name, defaultValue);
    }

    static LangDirDynamicParameter getLangDir(String displayName, DynamicParameter.PREFIX prefix,
                                              String name, LanguageDirection defaultValue) {
        return new LangDirDynamicParameter(displayName, prefix, name, defaultValue);
    }

    static StringDynamicParameter getString(String displayName, DynamicParameter.PREFIX prefix,
                                            String name, String defaultValue) {
        return new StringDynamicParameter(displayName, prefix, name, defaultValue);
    }

    public static Map<String, DynamicParameter> getParams() {
        return Collections.unmodifiableMap(params);
    }


}

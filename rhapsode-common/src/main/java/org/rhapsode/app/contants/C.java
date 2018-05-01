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

package org.rhapsode.app.contants;

//constants used in html parameters and url queries
public class C {
    public static final String EXPORT = "ex";
    public static final String NEXT = "nx";
    public static final String PREVIOUS = "pr";
    public static final String SELECT_ALL = "sa";
    public static final String DESELECT_ALL = "da";


    public static final String RESULTS_PER_PAGE = "rp";


    public static final String MAIN_QUERY = "q";
    public static final String MAIN_QUERY_TRANSLATION = "qt";
    public static final String FILTER_QUERY = "fq";
    public static final String FILTER_QUERY_TRANSLATION = "fqt";
    public static final String GEO_QUERY = "gq";
    public static final String GEO_RADIUS = "gr";

    public static final String FILE_KEY = "fk";//initial file
    public static final String DOC_KEY = "dk";//lucene document (can be multiple lucene docs per file key)


    public static final String LANG_DIR = "ld";

    public static final String LAST_START = "ls";
    public static final String LAST_END = "le";
    public static final String PAGING_DIRECTION = "pd";
    public static final String SEARCH = "s";
    public static final String RANK = "r";
    public static final String LUCENE_DOC_ID = "i";
    public static final String DEFAULT_QUERY_FIELD = "fs";
    public static final String MAX_VISITED_WINDOWS = "mvw";
    public static final String MAX_STORED_WINDOWS = "msw";
    public static final String NORMALIZE_TARGET = "nt";
    public static final String NUM_RESULTS = "nr";
    public static final String SHOW_CODE_POINTS = "scp";
    public static final String SIMPLE_SINGLE_TERM_SEARCH = "ssts";
    public static final String CONCEPT_NAME = "cn";
    public static final String CONCEPT_QUERY = "cq";
    public static final String ADD_CONCEPT = "ca";
    public static final String DELETE_CONCEPTS = "cdel";
    public static final String ADD_CONCEPT_DIALOGUE = "cdia";
    public static final String UPDATE_DOCUMENT_COUNTS = "udc";
    public static final String CANCEL = "cx";
    public static final String COLLECTION_PATH = "cp";
    public static final String OPEN_NEW_COLLECTION = "onc";
    public static final String OPEN_NEW_COLLECTION_DIALOGUE = "oncd";
    public static final String COLLECTION_HISTORY_SELECTOR = "chs";
    public static final String REOPEN_COLLECTION = "ronc";
    public static final String REFRESH_COLLECTION = "uc";
    public static final String ADD_STORED_QUERY = "asq";
    public static final String ADD_STORED_QUERY_DIALOGUE = "asqd";
    public static final String DELETE_ALL_STORED_QUERIES = "dsq";
    public static final String STORED_QUERY_ID = "sqid";
    public static final String STORED_QUERY_IDS = "sqids";
    public static final String REPORT_NAME = "rf";
    public static final String ABS_MAX_REPORT_RESULTS = "amrr";
    public static final String MAX_SEARCH_RESULTS = "msr";
    public static final String QUERIES_FOR_REPORT = "qfr";
    public static final String CALC_VALS_IN_REPORT = "cvr";
    public static final String WRITE_REPORT = "wr";

    public static final String REPORT_NORM_TYPE = "rnt";
    public static final String REPORT_NORM_ONE = "rno";
    public static final String REPORT_NORM_INVERSE_RANK = "rnir";
    public static final String REPORT_NORM_WEIGHTED_INVERSE_RANK = "rnwir";

    public static final String REPORT_TYPE = "losr";
    public static final String HIGHLIGHT_FILES_FOR_REPORT = "hfr";
    public static final String HIGHLIGHT_FILES_FOR_REPORT_TRUE = "hfrt";
    public static final String HIGHLIGHT_FILES_FOR_REPORT_FALSE = "hfrf";
    public static final String LOAD_STORED_QUERIES_DIALOGUE = "lsqd";
    public static final String LOAD_STORED_QUERIES = "lsq";
    public static final String STORED_QUERIES_PATH = "sqp";
    public static final String ATTACHMENT_OFFSET = "ao";
    public static final String SHOW_LANGUAGE_DIRECTION = "sld";
    public static final String DEFAULT_LANG_DIRECTION = "dld";
    public static final String SHOW_SELECTED = "ss";
    public static final String IGNORE_DUPLICATE_WINDOWS = "idu";
    public static final String MIN_NGRAM = "minn";
    public static final String MAX_NGRAM = "maxn";
    public static final String WORDS_BEFORE = "wpre";
    public static final String WORDS_AFTER = "wpost";
    public static final String MIN_TERM_FREQ = "mtf";
    public static final String CONC_SORT_ORDER_DOC = "csod";
    public static final String CONC_SORT_ORDER_PRE = "pre";
    public static final String CONC_SORT_ORDER_POST = "post";
    public static final String CONC_SORT_ORDER_TARGET_PRE = "tpre";
    public static final String CONC_SORT_ORDER_TARGET_POST = "tpost";
    public static final String CONC_SORT_ORDER_SELECTOR = "cso";
    public static final String FALSE = "false";
    public static final String TRUE = "true";
    public static final String HIGHLIGHT_STYLE = "hs";
    public static final String STORED_QUERY_PRIORITY = "sqpr";
    public static final String STORED_QUERY_NOTES = "sqnts";
    public static final String TOP_N_COMBINED_REPORT_RESULTS = "tnrr";
    public static final String REPORT_TYPE_LIVE = "rptl";
    public static final String REPORT_TYPE_STATIC = "rpts";
    public static final String REPORT_TYPE_NO_LINKS = "rptn";
    public static final String SETTINGS_REQUEST_UPDATE = "sru";
    public static final String SELECTED_DOC_IDS = "sids";
    public static final String ADD_SELECTED_TO_FAVORITES = "astf";
    public static final String ADD_SELECTED_TO_IGNORE = "asti";
    public static final String SELECTED_VIEW_FAVORITES = "svf";
    public static final String SELECTED_VIEW_IGNORED = "svi";
    public static final String USE_IGNORE_QUERY = "uiq";
    public static final String USE_FAVORITES_QUERY = "ufq";
    public static final String CLEAR_SELECTED_IGNORED = "csli";
    public static final String SELECT_ALL_IGNORED = "sai";
    public static final String SELECT_ALL_FAVORITES = "saf";
    public static final String CLEAR_SELECTED_FAVORITES = "csf";
    public static final String REPORT_INCLUDE_FAVORITES = "rif";

    public static final String SELECT_QUERIES_FOR_UPDATE = "squp";
    public static final String UPDATE_SELECTED_QUERY = "squps";
    public static final String SAVE_STORED_QUERIES_DIALOGUE = "sqsd";
    public static final String SAVE_STORED_QUERIES = "sqss";
    public static final String CONCEPT_TRANSLATION = "sct";
    public static final String CONCEPT_NOTES = "scn";
    public static final String SELECTED_STORED_QUERY = "sqsel";
    public static final String DELETE_SELECTED_QUERIES = "sqdel";
    public static final String UPDATE_SELECTED_QUERY_DIALOGUE = "squpd";
    public static final String STORED_QUERY_NAME = "sqn";
    public static final String SQ_SHOW_MAIN_QUERY_TRANSLATION = "sqsmqt";
    public static final String SQ_SHOW_FILTER_QUERY = "sqsfq";
    public static final String SQ_SHOW_FILTER_QUERY_TRANSLATION = "sqsfqt";
    public static final String SQ_SHOW_NOTES = "sqsn";
    public static final String SQ_SHOW_PRIORITY = "sqsp";
    public static final String SQ_SHOW_MAX_HITS = "sqmh";
    public static final String SQ_SHOW_STYLES = "sqsst";
    public static final String SQ_SHOW_DEFAULT_FIELD = "sqsdf";
    public static final String BS_SHOW_STORED_BUTTONS = "bsssb";
    public static final String BS_MAIN_QUERY_BOX_WIDTH = "bsmqbw";
    public static final String BS_MAIN_QUERY_BOX_HEIGHT = "bsmqbh";
    public static final String BS_FILTER_QUERY_BOX_WIDTH = "bsfqbw";
    public static final String BS_FILTER_QUERY_BOX_HEIGHT = "bsfqbh";
    public static final String CONC_MAIN_QUERY_BOX_WIDTH = "cmqbw";
    public static final String CONC_MAIN_QUERY_BOX_HEIGHT = "cmqbh";
    public static final String CONC_FILTER_QUERY_BOX_WIDTH = "cfqbw";
    public static final String CONC_FILTER_QUERY_BOX_HEIGHT = "cfqbh";
    public static final String MAX_FILE_NAME_DISPLAY_LENGTH = "mfndl";
    public static final String MAX_BOOLEAN_CLAUSES = "mbc";
    public static final String KMEANS = "cl_kmeans";
    public static final String LINGO = "cl_lingo";
    public static final String STC = "cl_stc";
    public static final String CLUSTERING_NUM_CLUSTERS = "cl_n_cl";
    public static final String CLUSTER_ALGO_SELECTOR = "cl_algo";
    public static final String NO_CLUSTERING = "cl_n";
    public static final String CONC_ALLOW_CLUSTERING = "conc_cl";
    public static final String TABLE_INPUT_DIRECTORY = "ti";
    public static final String SET_TABLE_INPUT_DIRECTORY = "tsi";
    public static final String TABLE_FILE_NAME = "tfn";
    public static final String SELECT_TABLE_FILE = "tsf";
    public static final String SELECT_TABLE_COLUMNS = "tsc";
    public static final String COLLECTION_NAME = "cln";
    public static final String COLLECTION_ROOT = "cr";
    public static final String DEFAULT_CONTENT_FIELD = "dcf";
    public static final String DEFAULT_LINK_FIELD = "dlf";
    public static final String TABLE_COL_IS_CONTENT = "tcic";
    public static final String TABLE_COL_IS_LINK = "tcil";
    public static final String INDEXED_DOC_DISPLAY_FIELDS = "iddf";
    public static final String ROW_VIEWER_DISPLAY_FIELDS = "rvdf";
    public static final String MAX_METADATA_COLUMN_LENGTH = "mmcl";
    public static final String RESTART_TABLE_LOADING_DIALOGUE = "rtld";
    public static final String TABLE_ENCODING = "tenc";
    public static final String TABLE_DELIMITER = "tdel";
    public static final String TABLE_WORKSHEET_NAME = "tws";
    public static final String SELECT_COLLECTION_NAME = "scon";
    public static final String SELECT_ENCODING_DELIMITER = "sed";
    public static final String SELECT_WORKSHEET = "sws";
    public static final String START_INDEXER = "sind";
    public static final String SELECT_ENCODING_DELIMITER_WORKSHEET = "sedw";
    public static final String TABLE_HAS_HEADERS = "thh";
    public static final String W2V_POSITIVE = "w2vp";
    public static final String W2V_NEGATIVE = "w2vn";
}

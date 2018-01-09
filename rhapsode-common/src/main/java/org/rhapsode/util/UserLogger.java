package org.rhapsode.util;


import org.rhapsode.app.contants.C;
import org.rhapsode.lucene.search.ComplexQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class UserLogger {
    private static final String TOOL = "t";
    private static final String QUERY = "q";
    private static final String FILTER_QUERY = "fq";
    private static final String NUM_HITS = "hits";
    private static final String MILLIS = "ms";
    private static final String EXCEPTION = "ex";
    private static final String EXCEPTION_MSG = "ex_msg";

    private static final Logger LOG = LoggerFactory.getLogger(UserLogger.class);
    private static volatile boolean SHOULD_LOG = false;

    public static void setShouldLog(boolean shouldLog) {
        SHOULD_LOG = shouldLog;
    }

    public static void log(String toolName, ComplexQuery complexQuery, int totalHits, long elapsed) {
        if (! SHOULD_LOG) {
            return;
        }
        Map<String, String> map = new LinkedHashMap<>();
        map.put(TOOL, toolName);
        map.put(QUERY, complexQuery.getStoredQuery().getMainQueryString());
        map.put(FILTER_QUERY, complexQuery.getStoredQuery().getFilterQueryString());
        map.put(NUM_HITS, Integer.toString(totalHits));
        map.put(MILLIS, Long.toString(elapsed));
        LOG.info(urlEncode(map));
    }

    private static String urlEncode(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : map.entrySet()) {
            String value = e.getValue();
            value = (value == null) ? "" : value;
            try {
                sb.append("&");
                sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8.name()));
                sb.append("=");
                sb.append(URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }
        }
        return sb.toString();
    }

    public static void logParseException(String toolName, String msg, HttpServletRequest httpServletRequest) {
        if (! SHOULD_LOG) {
            return;
        }
        String mainQuery = httpServletRequest.getParameter(C.MAIN_QUERY);
        String filterQuery = httpServletRequest.getParameter(C.FILTER_QUERY);
        Map<String, String> map = new LinkedHashMap<>();
        map.put(TOOL, toolName);
        if (mainQuery != null) {
            map.put(QUERY, mainQuery);
        }
        if (filterQuery != null) {
            map.put(FILTER_QUERY, filterQuery);
        }
        map.put(EXCEPTION, "true");
        map.put(EXCEPTION_MSG, msg);
        LOG.info(urlEncode(map));
    }
}

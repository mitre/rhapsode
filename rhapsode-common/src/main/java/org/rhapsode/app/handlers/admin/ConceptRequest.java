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

package org.rhapsode.app.handlers.admin;

import org.apache.commons.lang3.StringUtils;
import org.rhapsode.app.contants.C;
import org.rhapsode.lucene.search.SCField;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

public class ConceptRequest {

    public Map<SCField, String> getConceptFields() {
        return storedConceptFields;
    }

    enum ActionType {
        DEFAULT,
        ADD_CONCEPT,
        ADD_DIALOGUE,
        DELETE,
        UPDATE_CONCEPT,
        SAVE_DIALOGUE,
        SAVE_CONCEPTS,
        UPDATE_DOC_COUNTS
    }

    ;

    public static ConceptRequest build(HttpServletRequest httpServletRequest) {
        ConceptRequest r = new ConceptRequest();
        r.t = ActionType.DEFAULT;
        if (httpServletRequest.getParameter(C.ADD_CONCEPT) != null) {
            r.t = ActionType.ADD_CONCEPT;
        } else if (httpServletRequest.getParameter(C.ADD_CONCEPT_DIALOGUE) != null) {
            r.t = ActionType.ADD_DIALOGUE;
        } else if (httpServletRequest.getParameter(C.DELETE_CONCEPTS) != null) {
            r.t = ActionType.DELETE;
        } else if (httpServletRequest.getParameter(C.UPDATE_DOCUMENT_COUNTS) != null) {
            r.t = ActionType.UPDATE_DOC_COUNTS;
        }

        r.storedConceptFields = getConceptFields(httpServletRequest);

        return r;
    }

    private static Map<SCField, String> getConceptFields(HttpServletRequest httpServletRequest) {
        Map<SCField, String> fields = new HashMap<>();
        addField(SCField.NAME,
                httpServletRequest.getParameter(C.CONCEPT_NAME), fields);
        addField(SCField.CONCEPT_QUERY,
                httpServletRequest.getParameter(C.CONCEPT_QUERY), fields);
        addField(SCField.CONCEPT_TRANSLATION,
                httpServletRequest.getParameter(C.CONCEPT_TRANSLATION), fields);
        addField(SCField.NOTES,
                httpServletRequest.getParameter(C.CONCEPT_NOTES), fields);
        return fields;
    }

    private static void addField(SCField field, String value, Map<SCField, String> fields) {
        if (!StringUtils.isBlank(value)) {
            fields.put(field, value);
        }
    }

    private Map<SCField, String> storedConceptFields;
    private ActionType t;

    public ActionType getActionType() {
        return t;
    }
}

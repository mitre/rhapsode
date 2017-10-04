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
package org.rhapsode.lucene.search;

import org.apache.lucene.document.Document;
import org.rhapsode.util.LanguageDirection;
import org.tallison.lucene.search.concordance.classic.DocMetadataExtractor;
import org.tallison.lucene.search.concordance.classic.impl.SimpleDocMetadataExtractor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BaseSearchRequest {


    public enum ActionType {
        SEARCH,
        NEXT, //ditto below
        PREVIOUS,//actually only used by basic search...refactor
        DESELECT_ALL,
        SELECT_ALL,
        ADD_SELECTED_TO_FAVORITES,
        ADD_SELECTED_TO_IGNORE
    }

    private StoredQuery storedQuery = null;
    private LanguageDirection languageDirection;
    private ComplexQuery complexQuery;
    private Set<String> fields = new HashSet<>();//fields to return from search
    private DocMetadataExtractor docMetadataExtractor = null;
    private Set<String> selectedDocIds;
    private ActionType actionType;
    private boolean useIgnoreQuery = false;
    private boolean useFavoritesQuery = false;


    /**
     * This also builds the doc metadata extractor.  This has to be called by the
     * builder!!!!
     *
     * @param fields
     */
    public void setFields(Set<String> fields) {
        this.fields = Collections.unmodifiableSet(fields);
        this.docMetadataExtractor = new SimpleDocMetadataExtractor(fields);
    }

    /**
     * This needs to be set by the builder!!!
     *
     * @param complexQuery
     */
    public void setComplexQuery(ComplexQuery complexQuery) {
        this.complexQuery = complexQuery;
    }


    public Map<String, String> extractMetadata(Document d) {
        return docMetadataExtractor.extract(d);
    }


    public String getContentField() {
        return getStoredQuery().getDefaultField();
    }

    public LanguageDirection getLanguageDirection() {
        return languageDirection;
    }

    public void setLanguageDirection(LanguageDirection languageDirection) {
        this.languageDirection = languageDirection;
    }

    public boolean hasStoredQuery() {
        return storedQuery != null;
    }

    public void setStoredQuery(StoredQuery sq) {
        this.storedQuery = sq;
    }

    public boolean hasQuery() {
        return complexQuery != null && complexQuery.getRetrievalQuery() != null;
    }


    public ComplexQuery getComplexQuery() {
        return complexQuery;
    }


    public Set<String> getFields() {
        return docMetadataExtractor.getFieldSelector();
    }

    public StoredQuery getStoredQuery() {
        return storedQuery;
    }

    public DocMetadataExtractor getDocMetadataExtractor() {
        return docMetadataExtractor;
    }

    public boolean isSelectAll() {
        return actionType.equals(ActionType.SELECT_ALL);
    }

    public Set<String> getSelectedDocIds() {
        return selectedDocIds;
    }

    public void setSelectedDocIds(Set<String> selectedDocIds) {
        this.selectedDocIds = selectedDocIds;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setUseIgnoreQuery(boolean useIgnoreQuery) {
        this.useIgnoreQuery = useIgnoreQuery;
    }

    public boolean getUseIgnoreQuery() {
        return useIgnoreQuery;
    }

    public boolean getUseFavoritesQuery() {
        return useFavoritesQuery;
    }

    public void setUseFavoritesQuery(boolean useFavoritesQuery) {
        this.useFavoritesQuery = useFavoritesQuery;
    }

}

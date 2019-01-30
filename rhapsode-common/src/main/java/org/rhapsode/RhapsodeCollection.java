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

package org.rhapsode;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.rhapsode.lucene.schema.IndexSchema;
import org.rhapsode.lucene.search.IndexManager;
import org.rhapsode.lucene.utils.DocRetriever;
import org.rhapsode.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RhapsodeCollection {

    private static final Logger LOG = LoggerFactory.getLogger(RhapsodeCollection.class);

    //constant names of subdirectories
    public static final String LUCENE_INDEX_SUBDIR = "index";
    public static final String TIKA_EXTRACT_SUBDIR = "extracted_text";
    public static final String COLLECTION_TRASH_SUBDIR = "trash";
    public static final String COLLECTION_STORED_EXPORT_SUBDIR = "exports";

    //constant name of collection's schema
    public static final String INDEX_SCHEMA_FILE_NAME = "index_schema.json";
    public static final String COLLECTION_SCHEMA_FILE_NAME = "collection_schema.json";

    //TODO: allow configuration of some subdirs


    Path collectionPath;
    IndexSchema schema;
    CollectionSchema collectionSchema;
    private boolean loaded = false;
    private IndexManager indexManager;
    private Query favoritesQuery = null;
    private Query ignoredQuery = null;

    //disallow
    private RhapsodeCollection() {
        throw new IllegalArgumentException("MUST INCLUDE COLLECTION ROOT");
    }

    private RhapsodeCollection(Path collectionRoot) {
        collectionPath = collectionRoot;
    }

    /**
     * Builds a new collection directory with all required subdirectories.
     * Serializes indexSchema into the correct location.
     *
     * @param origDocsRoot
     * @param collectionRoot
     * @param indexSchemaPath
     * @return
     * @throws IOException
     */
    public static RhapsodeCollection build(Path origDocsRoot, Path collectionRoot, Path indexSchemaPath) throws IOException {
        if (Files.isDirectory(collectionRoot) && PathUtils.listPaths(collectionRoot).size() > 0) {
            //do nothing
        } else {
            Files.createDirectories(collectionRoot);
        }
        RhapsodeCollection rc = new RhapsodeCollection(collectionRoot);
        rc.loaded = false;

        Files.copy(indexSchemaPath, rc.getIndexSchemaPath());
        Files.createDirectories(rc.getCollectionExportsPath());
        Files.createDirectories(rc.getCollectionTrashPath());
        Files.createDirectories(rc.getExtractedTextRoot());

        rc.schema = IndexSchema.load(rc.getIndexSchemaPath());
        rc.collectionSchema = new CollectionSchema(rc.schema.getUniqueDocField(),
                origDocsRoot);
        Gson gson = new Gson();
        try (Writer writer = Files.newBufferedWriter(rc.getCollectionSchemaPath(),
                StandardCharsets.UTF_8)) {
            gson.toJson(rc.collectionSchema, writer);
        }

        rc.loaded = true;
        return rc;
    }

    ;

    /**
     * Loads an existing collection.  This requires that the following must exist:
     * collectionRoot
     * trashRoot
     * extractedTextRoot
     * indexSchema
     *
     * @param collectionRoot
     * @return
     * @throws IOException
     */
    public static RhapsodeCollection loadExisting(Path collectionRoot) throws IOException {
        RhapsodeCollection rc = new RhapsodeCollection(collectionRoot);
        rc.loaded = false;
        rc.schema = IndexSchema.load(rc.getIndexSchemaPath());

        Gson gson = new Gson();
        try (Reader reader = Files.newBufferedReader(rc.getCollectionSchemaPath(),
                StandardCharsets.UTF_8)) {
            rc.collectionSchema = gson.fromJson(reader, CollectionSchema.class);
        }
        rc.collectionSchema.setDocIdField(rc.schema.getUniqueDocField());
        IndexManager.load(rc);
        rc.loaded = true;
        return rc;
    }

    public static boolean isProbablyACompleteCollection(Path p) {
        if (!Files.isDirectory(p)) {
            LOG.warn("not a directory: "+p);
            return false;
        }
        if (!Files.isRegularFile(p.resolve(INDEX_SCHEMA_FILE_NAME))) {
            LOG.warn("no index schema file in: "+p);
            return false;
        }
        if (!Files.isRegularFile(p.resolve(COLLECTION_SCHEMA_FILE_NAME))) {
            LOG.warn("no collection schema file in: "+p);
            return false;
        }
        if (!Files.isDirectory(p.resolve(LUCENE_INDEX_SUBDIR))) {
            LOG.warn("no lucene index directory in: "+p);
            return false;
        }
        File[] files = p.resolve(LUCENE_INDEX_SUBDIR).toFile().listFiles();
        if (files.length < 2) {
            LOG.warn("too few files in lucene index directory ("+(files.length+1)
                    +") in "+p);
            return false;
        }
        return true;
    }

    public void emptyTrash() throws IOException {
        PathUtils.deleteDirectory(getCollectionTrashPath());
    }

    public Path getCollectionPath() {
        return collectionPath;
    }

    public Path getExtractedTextRoot() {
        return collectionPath.resolve(TIKA_EXTRACT_SUBDIR);
    }

    public Path getIndexSchemaPath() {
        return collectionPath.resolve(INDEX_SCHEMA_FILE_NAME);
    }

    public Path getLuceneIndexPath() {
        return collectionPath.resolve(LUCENE_INDEX_SUBDIR);
    }

    public Path getCollectionTrashPath() {
        return collectionPath.resolve(COLLECTION_TRASH_SUBDIR);
    }

    public Path getCollectionExportsPath() {
        return collectionPath.resolve(COLLECTION_STORED_EXPORT_SUBDIR);
    }

    public Path getCollectionSchemaPath() {
        return collectionPath.resolve(COLLECTION_SCHEMA_FILE_NAME);
    }

    public Path getOrigDocsRoot() {
        return Paths.get(collectionSchema.getOrigDocsRoot().toString());
    }

    //each time this is set, it writes to disk
    public void setOrigDocsRoot(Path origDocsRoot) throws IOException {
        this.collectionSchema.setOrigDocsRoot(origDocsRoot.toFile());
        updateCollectionSchema();

    }

    public IndexSchema getIndexSchema() {
        return schema;
    }

    public IndexManager getIndexManager() {
        return indexManager;
    }

    public void setIndexManager(IndexManager indexManager) {
        this.indexManager = indexManager;
    }

    public boolean isLoaded() {
        return loaded;
    }

    /**
     * @param id
     * @param fieldsToRetrieve
     * @return
     * @throws IOException
     */
    public List<Document> getAllDocsFromAnyDocId(int id, Set<String> fieldsToRetrieve)
            throws IOException {

        String fileIdField = getIndexSchema().getUniqueFileField();
        Sort sort = new Sort(new SortedNumericSortField(getIndexSchema().getAttachmentIndexField(), SortField.Type.INT));
        return DocRetriever.getAllDocsFromAnyDocId(id, fileIdField, sort, fieldsToRetrieve, getIndexManager().getSearcher());
    }

    public List<Document> getAllDocsByFileId(String fileId, Set<String> fieldsToRetrieve) throws IOException {
        Sort sort = new Sort(new SortedNumericSortField(getIndexSchema().getAttachmentIndexField(), SortField.Type.INT));

        return DocRetriever.getAllDocsByFileId(
                getIndexSchema().getUniqueFileField(),
                fileId,
                sort, fieldsToRetrieve, getIndexManager().getSearcher());
    }

    public List<Document> getAllDocsFromAnyDocId(String docId, Set<String> fieldsToRetrieve) throws IOException {
        Sort sort = new Sort(new SortedNumericSortField(getIndexSchema().getAttachmentSortField(), SortField.Type.INT));
        return DocRetriever.getAllDocsFromAnyDocId(docId,
                getIndexSchema().getUniqueDocField(),
                getIndexSchema().getUniqueFileField(),
                sort,
                fieldsToRetrieve,
                getIndexManager().getSearcher());
    }

    private void updateCollectionSchema() throws IOException {
        try (Writer w = Files.newBufferedWriter(getCollectionSchemaPath(),
                StandardCharsets.UTF_8)) {
            Gson gson = new GsonBuilder().create();
            gson.toJson(collectionSchema, w);
        }
    }

    public Query getIgnoredQuery() {
        if (ignoredQuery == null) {
            ignoredQuery = collectionSchema.buildIgnoredQuery();
        }
        return ignoredQuery;
    }

    public Query getFavoritesQuery() throws IOException {

        if (favoritesQuery == null) {
            favoritesQuery = collectionSchema.buildFavoritesQuery();
        }
        return favoritesQuery;
    }

    public void addFavorites(Set<String> ids) {
        favoritesQuery = null;//must rebuild favorites query
        collectionSchema.addFavorites(ids);
        try {
            updateCollectionSchema();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addIgnoreds(Set<String> ids) {
        ignoredQuery = null;//sign that query must be rebuilt
        collectionSchema.addIgnoreds(ids);
        try {
            updateCollectionSchema();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getIgnoredSize() {
        if (collectionSchema.ignoreds != null) {
            return collectionSchema.ignoreds.size();
        }
        return 0;
    }

    public int getFavoritesSize() {
        if (collectionSchema.favorites != null) {
            return collectionSchema.favorites.size();
        }
        return 0;
    }


    public void removeIgnoreds(Set<String> docIds) {
        ignoredQuery = null;
        if (collectionSchema.ignoreds == null) {
            return;
        }
        for (String id : docIds) {
            collectionSchema.ignoreds.remove(id);
        }
        try {
            updateCollectionSchema();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeFavorites(Set<String> docIds) {
        favoritesQuery = null;
        if (collectionSchema.favorites == null) {
            return;
        }
        for (String id : docIds) {
            collectionSchema.favorites.remove(id);
        }
        try {
            updateCollectionSchema();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public boolean hasIndex() {
        if (indexManager != null && indexManager.hasSearcher()) {
            return true;
        }
        return false;
    }
}

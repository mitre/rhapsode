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

package org.rhapsode.app;


import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.lucene.search.MultiTermQuery;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.rhapsode.RhapsodeCollection;
import org.rhapsode.app.session.BooleanDynamicParameter;
import org.rhapsode.app.session.SessionManager;
import org.rhapsode.app.session.StringDynamicParameter;
import org.rhapsode.app.tasks.RhapsodeTask;
import org.rhapsode.app.tasks.RhapsodeTaskStatus;
import org.rhapsode.geo.GeoConfig;
import org.rhapsode.lucene.queryparsers.ClassicQParserPlugin;
import org.rhapsode.lucene.queryparsers.ComplexQParserPlugin;
import org.rhapsode.lucene.queryparsers.ParserPlugin;
import org.rhapsode.lucene.queryparsers.SQPParserPlugin;
import org.rhapsode.lucene.search.CommonSearchConfig;
import org.rhapsode.lucene.search.basic.BasicSearchConfig;
import org.rhapsode.lucene.search.concordance.ConcordanceSearchConfig;
import org.rhapsode.lucene.search.cooccur.CooccurConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RhapsodeSearcherApp {


    public static final Path CWD = Paths.get("").toAbsolutePath();
    private static final Logger LOG = LoggerFactory.getLogger(RhapsodeSearcherApp.class);
    private final Object lock = new Object();
    GeoConfig geoConfig;
    CommonSearchConfig commonSearchConfig;
    BasicSearchConfig basicSearchConfig;
    ConcordanceSearchConfig concordanceSearchConfig;
    CooccurConfig cooccurConfig;
    long serverIdleTimeoutMillis;
    RhapsodeCollection rhapsodeCollection;
    ConcordanceSearchConfig cooccurSearchConfig;
    SessionManager sessionManager;
    //need this to update the doc base if the collection changes
    ResourceHandler resourceHandler;
    RhapsodeTask task;
    ExecutorService executorService;
    ExecutorCompletionService executorCompletionService;
    //TODO: refactor these into child managers
    int maxBooleanClauses;
    boolean treatStoredQueryLineAsPhrase;
    ParserType queryParserType;
    private RhapsodeTaskStatus lastTaskStatus;

    public static RhapsodeSearcherApp load(Path propsFile) throws IOException {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeHierarchyAdapter(RhapsodeSearcherApp.class, new RhapsodeSearcherAppDeserializer());
        Gson gson = builder.create();
        RhapsodeSearcherApp config = null;
        try (Reader r = Files.newBufferedReader(propsFile, StandardCharsets.UTF_8)) {
            config = gson.fromJson(r, RhapsodeSearcherApp.class);
        }
        return config;
    }

    public void tryToLoadRhapsodeCollection(Path p) throws IOException {
        //add locking!
        rhapsodeCollection = RhapsodeCollection.loadExisting(p);
        if (resourceHandler != null) {
            resourceHandler.setResourceBase(rhapsodeCollection.getOrigDocsRoot().toAbsolutePath().toString());
        }
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public ParserPlugin getQueryParser(ParserPlugin.PARSERS parser) {
        switch (parser) {
            case CLASSIC:
                return new ClassicQParserPlugin(getRhapsodeCollection().getIndexSchema());
            case COMPLEX:
                return new ComplexQParserPlugin(getRhapsodeCollection().getIndexSchema());
            default:
                return new SQPParserPlugin(getRhapsodeCollection().getIndexSchema());
        }
    }

    public int getMaxBooleanClauses() {
        return maxBooleanClauses;
    }

    public long getServerIdleTimeoutMillis() {
        return serverIdleTimeoutMillis;
    }

    public boolean getTreatStoredQueryLineAsPhrase() {
        return treatStoredQueryLineAsPhrase;
    }

    public CommonSearchConfig getCommonSearchConfig() {
        return commonSearchConfig;
    }

    public ConcordanceSearchConfig getConcordanceSearchConfig() {
        return concordanceSearchConfig;
    }

    public CooccurConfig getCooccurSearchConfig() {
        return cooccurConfig;
    }

    public void setResourceHandler(ResourceHandler resourceHandler) {
        this.resourceHandler = resourceHandler;
    }

    public void setCollection(RhapsodeCollection collection) {
        this.rhapsodeCollection = collection;
    }

    public ParserPlugin getParserPlugin(MultiTermQuery.RewriteMethod rewriteMethod) {
        //TODO: figure out how to make this configurable
        //check for collection
        return new SQPParserPlugin(getRhapsodeCollection().getIndexSchema(), rewriteMethod);
    }

    public ParserPlugin getParserPlugin() {
        //TODO: figure out how to make this configurable
        //check for collection
        return new SQPParserPlugin(getRhapsodeCollection().getIndexSchema());
    }

    public boolean hasCollection() {
        return rhapsodeCollection != null && rhapsodeCollection.isLoaded();
    }

    public Path getReportsDirectory() {
        return Paths.get("reports");
    }

    ;

    public RhapsodeCollection getRhapsodeCollection() {
        return rhapsodeCollection;
    }

    public GeoConfig getGeoConfig() {
        return geoConfig;
    }

    public BasicSearchConfig getBasicSearchConfig() {
        return basicSearchConfig;
    }

    public String getStringParameter(StringDynamicParameter parameter) {
        return getSessionManager().getDynamicParameterConfig().getString(parameter);
    }

    public boolean getBooleanParameter(BooleanDynamicParameter parameter) {
        return getSessionManager().getDynamicParameterConfig().getBoolean(parameter);
    }

    public RhapsodeTaskStatus getLastTaskStatus() {
        return lastTaskStatus;
    }

    public RhapsodeTaskStatus getTaskStatus() {
        synchronized (lock) {
            if (task == null) {
                return null;
            }

            try {
                Future<RhapsodeTaskStatus> future = executorCompletionService.poll(500, TimeUnit.MILLISECONDS);
                if (future == null) {
                    return task.getIntermediateResult();
                } else {
                    endTask(future);
                    return lastTaskStatus;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public void startTask(RhapsodeTask task) {
        synchronized (lock) {
            if (this.task != null) {
                throw new IllegalStateException("can't set a task if one is currenlty running");
            }
            this.task = task;
            this.executorService = Executors.newSingleThreadExecutor();
            this.executorCompletionService = new ExecutorCompletionService<RhapsodeTaskStatus>(executorService);
            executorCompletionService.submit(task);
        }

    }

    private void endTask(Future<RhapsodeTaskStatus> future) {
        try {
            lastTaskStatus = future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        executorService.shutdownNow();
        if (!executorService.isTerminated()) {
            LOG.warn("executor service has not terminated.  Might want to restart Rhapsode");
        }
        task = null;
        executorCompletionService = null;
        executorService = null;
        return;
    }

    public enum ParserType {
        COMPLEX,
        CLASSIC,
        SPAN_QUERY
    }
}

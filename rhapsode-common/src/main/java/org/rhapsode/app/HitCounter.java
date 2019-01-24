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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TotalHitCountCollector;

public class HitCounter {

    private static final Integer POISON = Integer.MIN_VALUE;

    /**
     * @param queries
     * @param indexSearcher
     * @param numSearchingThreads
     * @param maxSeconds
     * @return map of storedqueryId and number of hits
     * @throws IOException
     */
    public Map<Integer, Integer> count(Map<Integer, Query> queries, IndexSearcher indexSearcher,
                                       int numSearchingThreads, long maxSeconds) throws IOException {
        Map<Integer, Integer> results = new HashMap<>();
        if (queries.size() == 0) {
            return results;
        }
        int numThreads = Math.min(queries.size(), numSearchingThreads);
        ArrayBlockingQueue<Pair<Integer, Query>> qq = new ArrayBlockingQueue<>(queries.size() +
                numThreads);
        for (Map.Entry<Integer, Query> e : queries.entrySet()) {
            if (e.getKey().equals(POISON)) {
                //silently skip
                continue;
            }
            qq.add(Pair.of(e.getKey(), e.getValue()));
        }
        for (int i = 0; i < numThreads; i++) {
            qq.add(Pair.of(POISON, null));
        }

        ExecutorService ex =
                Executors.newFixedThreadPool(Math.min(queries.size(), numThreads));
        CompletionService<Map<Integer, Integer>> completionService =
                new ExecutorCompletionService<>(ex);
        for (int i = 0; i < numThreads; i++) {
            completionService.submit(new Counter(qq, indexSearcher));
        }

        long start = System.currentTimeMillis();
        long elapsed = -1;
        int completed = 0;
        while (elapsed < maxSeconds && completed < numThreads) {
            try {
                Future<Map<Integer, Integer>> result = completionService.poll(100,
                        TimeUnit.MILLISECONDS);
                if (result != null) {
                    Map<Integer, Integer> rr = result.get(1000, TimeUnit.MILLISECONDS);
                    if (rr != null) {
                        completed++;
                        results.putAll(rr);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new IOException(e.getMessage());
            } catch (ExecutionException e) {
                e.printStackTrace();
                throw new IOException(e);
            } catch (TimeoutException e) {
                e.printStackTrace();
                throw new IOException(e);
            }
            elapsed = System.currentTimeMillis() - start;
        }
        ex.shutdownNow();
        return results;
    }

    private class Counter implements Callable<Map<Integer, Integer>> {
        final ArrayBlockingQueue qq;
        final IndexSearcher indexSearcher;
        Exception exception = null;

        public Counter(ArrayBlockingQueue<Pair<Integer, Query>> qq, IndexSearcher indexSearcher) {
            this.qq = qq;
            this.indexSearcher = indexSearcher;
        }

        @Override
        public Map<Integer, Integer> call() throws InterruptedException {
            Map<Integer, Integer> ret = new HashMap<>();
            while (true) {
                Pair<Integer, Query> p = (Pair<Integer, Query>) qq.poll(1000, TimeUnit.SECONDS);
                if (p == null) {
                    //something went horribly wrong;
                    //TODO: warn somehow?
                    return ret;
                }
                if (p.getKey().equals(POISON)) {
                    return ret;
                }
                TotalHitCountCollector thcc = new TotalHitCountCollector();
                //consider wrapping in a timelimitingcollector

                try {
                    indexSearcher.search(p.getValue(), thcc);
                    ret.put(p.getKey(), thcc.getTotalHits());
                } catch (IOException e) {
                    exception = e;
                }
            }
        }
    }
}

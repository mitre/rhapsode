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
package org.rhapsode.app.tasks;

import java.util.Date;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Tasker {

    private static final long SLEEP_AFTER_EXCEPTION_MILLIS = 500;


    public enum STATE {
        NOT_STARTED,
        PROCESSING,
        COMPLETED
    }

    public enum REASON_FOR_COMPLETION {
        TOO_LONG,
        INTERRUPTION_EXCEPTION,
        EXCEPTION,
        USER_INTERRUPTION,
        SUCCESS,
        NA;
    }

    STATE currentState = STATE.NOT_STARTED;
    REASON_FOR_COMPLETION reasonForCompletion = null;
    Date completed = null;
    volatile boolean pleaseStop = false;
    ExecutorService executor = null;
    ExecutorCompletionService<RhapsodeTaskStatus> completionService;
    final long maxMillis;
    final static Object lock = new Object();
    final RhapsodeTask task;
    RhapsodeTaskStatus result = null;

    public Tasker(RhapsodeTask task, long maxMillis) {
        this.task = task;
        this.maxMillis = maxMillis;
    }

    public void start() {
        currentState = STATE.PROCESSING;
        executor = Executors.newSingleThreadExecutor();
        completionService =
                new ExecutorCompletionService<>(executor);
        completionService.submit(task);
        long start = System.currentTimeMillis();
        long elapsed = -1;

        Future<RhapsodeTaskStatus> resultFuture = null;

        while (reasonForCompletion == null) {
            try {
                resultFuture = completionService.poll(1, TimeUnit.SECONDS);
                if (resultFuture != null) {
                    result = resultFuture.get();
                    reasonForCompletion = REASON_FOR_COMPLETION.SUCCESS;
                    break;
                }
            } catch (InterruptedException e) {
                reasonForCompletion = REASON_FOR_COMPLETION.INTERRUPTION_EXCEPTION;
                e.printStackTrace();
                break;
            } catch (Throwable t) {
                t.printStackTrace();
                reasonForCompletion = REASON_FOR_COMPLETION.EXCEPTION;
                break;
            }
            if (maxMillis > -1 && elapsed > maxMillis) {
                reasonForCompletion = REASON_FOR_COMPLETION.TOO_LONG;
                break;
            }
            elapsed = System.currentTimeMillis() - start;
        }
        //now go through the shutdown process
        if (!reasonForCompletion.equals(REASON_FOR_COMPLETION.SUCCESS)) {
            task.pleaseStop();

            try {
                Thread.sleep(SLEEP_AFTER_EXCEPTION_MILLIS);
            } catch (InterruptedException e) {
            }
        }
        executor.shutdown();
        executor.shutdownNow();

        synchronized (lock) {
            currentState = STATE.COMPLETED;
            completed = new Date();
        }
        //thread leak can happen...executor might not be terminated yet!!!
    }


    public STATE getState() {
        return currentState;
    }


    public RhapsodeTaskStatus getResult() {
        synchronized (lock) {
            return result;
        }
    }

    public Date getCompletedTime() {
        return completed;
    }

    public void pleaseStop() {
        reasonForCompletion = REASON_FOR_COMPLETION.USER_INTERRUPTION;
        task.pleaseStop();
    }

    public boolean hasTerminated() throws InterruptedException {
        return executor.awaitTermination(500, TimeUnit.MILLISECONDS);
    }

    public REASON_FOR_COMPLETION getReasonForCompletion() {
        return reasonForCompletion;
    }


}

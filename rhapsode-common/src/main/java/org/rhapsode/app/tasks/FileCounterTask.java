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

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

public class FileCounterTask extends RhapsodeTask {

    final Path root;
    volatile AtomicLong currentCount = new AtomicLong(0);


    FileCounterTask(Path root) {
        super("File Counter");
        this.root = root;
    }

    @Override
    public FileCounterStatus call() {
        Tasker.REASON_FOR_COMPLETION reasonForCompletion = null;
        Date lastCompleted = null;
        try {
            Files.walkFileTree(root, new FileCounter());
            reasonForCompletion = Tasker.REASON_FOR_COMPLETION.SUCCESS;
        } catch (IOException e) {
            reasonForCompletion = Tasker.REASON_FOR_COMPLETION.EXCEPTION;
        } finally {
            lastCompleted = new Date();
        }
        return new FileCounterStatus(
                Tasker.STATE.COMPLETED,
                reasonForCompletion,
                lastCompleted, Long.toString(currentCount.longValue()),
                currentCount.get());
    }

    @Override
    public RhapsodeTaskStatus getIntermediateResult() {
        long val = currentCount.get();
        return new FileCounterStatus(
                Tasker.STATE.PROCESSING,
                Tasker.REASON_FOR_COMPLETION.NA,
                new Date(), Long.toString(val), val);
    }

    class FileCounter extends SimpleFileVisitor<Path> {

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            currentCount.incrementAndGet();
            if (pleaseStop || Thread.interrupted()) {
                return FileVisitResult.TERMINATE;
            }
            return FileVisitResult.CONTINUE;
        }
    }
}

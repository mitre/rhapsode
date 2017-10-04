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


import org.rhapsode.app.config.RhapsodeSearcherApp;
import org.rhapsode.app.tagger.Tagger;
import org.rhapsode.app.tagger.TaggerRequest;

import java.util.Date;

public class TaggerTask extends RhapsodeTask {


    final TaggerRequest request;
    final RhapsodeSearcherApp searcherApp;
    private final Tagger tagger;

    public TaggerTask(TaggerRequest r, RhapsodeSearcherApp searcherApp) {
        super("Stored Query Tagger");
        this.request = r;
        this.searcherApp = searcherApp;
        tagger = new Tagger(request, searcherApp);
    }

    @Override
    public RhapsodeTaskStatus getIntermediateResult() {
        //should add processing step in taggertaskresult
        String message = tagger.getStatusMessage();
        return new TaggerTaskStatus(
                Tasker.STATE.PROCESSING,
                Tasker.REASON_FOR_COMPLETION.NA, new Date(), message, request);
    }

    @Override
    public RhapsodeTaskStatus call() throws Exception {
        tagger.execute();
        return new TaggerTaskStatus(
                Tasker.STATE.COMPLETED, Tasker.REASON_FOR_COMPLETION.SUCCESS, new Date(),
                tagger.getStatusMessage(), request);
    }
}

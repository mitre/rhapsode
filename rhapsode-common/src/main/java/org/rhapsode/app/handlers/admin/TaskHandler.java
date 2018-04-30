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

import org.eclipse.jetty.server.Request;
import org.rhapsode.app.RhapsodeSearcherApp;
import org.rhapsode.app.decorators.RhapsodeXHTMLHandler;
import org.rhapsode.app.tasks.RhapsodeTaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class TaskHandler extends AdminHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TaskHandler.class);

    private final RhapsodeSearcherApp searcherApp;

    public TaskHandler(RhapsodeSearcherApp searcherApp) {
        super("Task Status Checker");
        this.searcherApp = searcherApp;
    }


    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest,
                       HttpServletResponse response) throws IOException, ServletException {
        RhapsodeXHTMLHandler xhtml = null;
        try {
            xhtml = initResponse(response, null);
            RhapsodeTaskStatus status = searcherApp.getTaskStatus();
            if (status == null) {
                xhtml.element("p", "There is currently no running task");
            } else {
                xhtml.element("p", status.toString());
            }
            RhapsodeTaskStatus lastTaskStatus = searcherApp.getLastTaskStatus();
            if (lastTaskStatus == null) {
                xhtml.element("p", "There is currently no last task");
            } else {
                xhtml.element("p", lastTaskStatus.toString());
            }
            xhtml.endDocument();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}

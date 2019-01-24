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

package org.rhapsode.app.handlers;

import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.tika.io.IOUtils;
import org.apache.tika.sax.ToHTMLContentHandler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.rhapsode.app.decorators.RhapsodeXHTMLHandler;
import org.xml.sax.SAXException;


public abstract class AbstractRhapsodeHandler extends AbstractHandler {
    protected static final String hrefRoot = "/rhapsode";//(hrefRoot == null) ? StringUtils.EMPTY : hrefRoot;
    private final String toolName;

    public AbstractRhapsodeHandler(String toolName) {
        this.toolName = toolName;
    }

    public void init(Request request, HttpServletResponse httpServletResponse) {

        try {
            request.setCharacterEncoding(IOUtils.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("Java doesn't have UTF-8?");
        }
        httpServletResponse.setBufferSize(1028);//searcherApp.getResponseWriterBufferSize());
    }

    public RhapsodeXHTMLHandler initResponse(HttpServletResponse response,
                                             String optionalStyle) throws IOException, SAXException {
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);

        RhapsodeXHTMLHandler handler =
                new RhapsodeXHTMLHandler(new ToHTMLContentHandler(response.getOutputStream(),
                        IOUtils.UTF_8.name()));
        handler.startDocument();
        addHeader(handler, optionalStyle);
        return handler;
    }

    protected abstract void addHeader(RhapsodeXHTMLHandler handler, String optionalStyle) throws SAXException;

    public String getToolName() {
        return toolName;
    }


}

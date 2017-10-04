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

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.rhapsode.app.config.RhapsodeSearcherApp;
import org.rhapsode.lucene.search.IndexManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * might make sense to use at some point???
 */
public class DelegatorHandler extends AbstractHandler {

    private final IndexManager indexManager;
    private final RhapsodeSearcherApp config;

    public DelegatorHandler(IndexManager indexManager, RhapsodeSearcherApp config) {
        this.indexManager = indexManager;
        this.config = config;
    }

    public void handle(String target, Request simpleRequest, HttpServletRequest request,
                       HttpServletResponse response) throws IOException, ServletException {
            /*
			String contextPath = request.getContextPath();
			if (contextPath.endsWith("/basic")){
				BasicSearchBackend searcherBackend = new BasicSearchBackend();
				searcherBackend.handle(request, response, indexManager, searcherApp, storedQueryManager);
			} else if (contextPath.endsWith("/concordance")){
				ConcordanceSearchBackend handler = new ConcordanceSearchBackend();
				handler.handle(request, response, indexManager, searcherApp, storedQueryManager);
			} else if (contextPath.endsWith("/concordance_tfidf")){
				ConcordanceCooccurBackend cooccur = new ConcordanceCooccurBackend();
				cooccur.handle(request, response, indexManager, searcherApp, storedQueryManager);
      } else if (contextPath.endsWith("/variant_counter")){
        VariantCounterBackend counter = new VariantCounterBackend(searcherApp);
        counter.handle(request, response, indexManager, searcherApp, storedQueryManager);
      } else if (contextPath.endsWith("/analyzer")){
				AnalyzerTestBackend aTester = new AnalyzerTestBackend();
				//querymanager is never called, this is required because of RhasodeBaseconfig.
				aTester.handle(request, response, indexManager, searcherApp, storedQueryManager);
			} else if (contextPath.endsWith("/view_doc")){
			      RhapsodeHTMLDocHighlighter highlighter = new RhapsodeHTMLDocHighlighter();
			      highlighter.handle(request, response, indexManager, searcherApp, storedQueryManager);
			}
			((Request)request).setHandled(true);  
			
	}
*/
    }

}

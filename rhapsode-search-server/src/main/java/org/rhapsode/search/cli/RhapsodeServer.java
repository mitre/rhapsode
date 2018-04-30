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

package org.rhapsode.search.cli;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.rhapsode.app.RhapsodeSearcherApp;
import org.rhapsode.app.handlers.search.AbstractSearchHandler;
import org.rhapsode.app.handlers.search.BasicSearchHandler;
import org.rhapsode.app.handlers.search.ConcordanceSearchHandler;
import org.rhapsode.app.handlers.search.CooccurHandler;
import org.rhapsode.app.handlers.search.TargetCounterHandler;
import org.rhapsode.app.handlers.search.VariantTermHandler;
import org.rhapsode.app.handlers.viewers.ExtractViewer;
import org.rhapsode.app.handlers.viewers.IndexedDocumentsViewer;
import org.rhapsode.app.handlers.viewers.IndividualIndexedDocumentViewer;
import org.rhapsode.app.utils.MyMimeTypes;

public class RhapsodeServer {

    static Options OPTIONS = new Options()
            .addOption(Option.builder("p")
                    .longOpt("port")
                    .hasArg(true)
                    .required(false)
                    .desc("port")
                    .build())
            .addOption(Option.builder("h")
                    .required(false)
                    .hasArg(true)
                    .desc("host name")
                    .longOpt("host")
                    .build())
            .addOption(Option.builder("c")
                    .required(true)
                    .hasArg(true)
                    .desc("collection path")
                    .longOpt("collection")
                    .build())
            .addOption(Option.builder("s")
                    .required(false)
                    .hasArg(true)
                    .desc("search_config.json file")
                    .longOpt("searchConfig")
                    .build()
    );

    final static String LOCAL_HOST = "127.0.0.1";
    final static int DEFAULT_PORT = 8092;

    public static String getVersion() {
        return "Rhapsode Prototype, v0.3.2-SNAPSHOT";
    }

    public void execute(Path propsFile, int port, String host, Path collectionPath) throws Exception {
        AbstractSearchHandler.INCLUDE_ADMIN_LINK = false;
        RhapsodeSearcherApp searcherApp = RhapsodeSearcherApp.load(propsFile);
        searcherApp.tryToLoadRhapsodeCollection(collectionPath);
        System.out.println("Finished loading config file.");
        //BooleanQuery.setMaxClauseCount(searcherApp.getMaxBooleanClauses());

        //configure server
        Server server = new Server();

        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        connector.setIdleTimeout(searcherApp.getServerIdleTimeoutMillis());


        //	   connector.setRequestHeaderSize(8192);
        connector.setName("rhapsode");
        //Step 1 to lockdown jetty to only LOCAL_HOST
        if (host != null) {
            connector.setHost(host);
        }
        server.setConnectors(new Connector[]{ connector});

        ContextHostWrapper handlerWrapper = new ContextHostWrapper();

        //configure BasicSearcher
        AbstractHandler basicSearchHandler = handlerWrapper.wrap(
                new BasicSearchHandler(searcherApp), "/rhapsode/basic");
        AbstractHandler concordanceHandler = handlerWrapper.wrap(
                new ConcordanceSearchHandler(searcherApp), "/rhapsode/concordance");
        AbstractHandler cooccurHandler = handlerWrapper.wrap(
                new CooccurHandler(searcherApp), "/rhapsode/cooccur");
        AbstractHandler targetCounterHandler = handlerWrapper.wrap(
                new TargetCounterHandler(searcherApp), "/rhapsode/target_counter");
        AbstractHandler variantTermCounterHandler = handlerWrapper.wrap(
                new VariantTermHandler(searcherApp), "/rhapsode/variant_term_counter");

        AbstractHandler extractViewerHandler = handlerWrapper.wrap(new ExtractViewer(searcherApp),
                "/rhapsode/view_extract");


        AbstractHandler docViewerHandler =
                handlerWrapper.wrap(new IndividualIndexedDocumentViewer(searcherApp),
                        "/rhapsode/view_doc");
        AbstractHandler docsViewerHandler =
                handlerWrapper.wrap(new IndexedDocumentsViewer(searcherApp),
                        "/rhapsode/view_docs");

        //configure resourceHandler
        MimeTypes mimeTypes = new MyMimeTypes(new File("resources/config/extra_mimes.properties")).getTypes();

        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setMimeTypes(mimeTypes);
        resourceHandler.setDirectoriesListed(true);
        //TODO: toString() right?
        //figure this one out...doh!
        if (searcherApp.getRhapsodeCollection() != null) {
            resourceHandler.setResourceBase(searcherApp.getRhapsodeCollection().getOrigDocsRoot().toString());
        }
        searcherApp.setResourceHandler(resourceHandler);
        AbstractHandler wrappedResourceHandler = handlerWrapper.wrap(resourceHandler, "/rhapsode/download");

        ResourceHandler helloHandler = new ResourceHandler();
        helloHandler.setDirectoriesListed(false);
        helloHandler.setWelcomeFiles(new String[]{"index.html"});
        String webDir = RhapsodeServer.class.getClassLoader().getResource("WEB-INF").toExternalForm();

        helloHandler.setResourceBase(webDir);
        //        AbstractHandler wrappedHelloHandler = handlerWrapper.wrap(helloHandler, "/", LOCAL_HOST);
        AbstractHandler wrappedHelloHandler = handlerWrapper.wrap(helloHandler, "/rhapsode");


        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] {
                concordanceHandler,
                cooccurHandler,
                variantTermCounterHandler,
                targetCounterHandler,
                basicSearchHandler,
                docViewerHandler,
                docsViewerHandler,
                wrappedResourceHandler,
                wrappedHelloHandler,
                extractViewerHandler,
        });

        server.setHandler(handlers);
        server.start();
        String hostString = (host == null) ? "localhost" : host;
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
        System.out.println("The search server has been successfully started.");
        System.out.println("Open a browser and navigate to:\nhttp://"+hostString+":" + port + "/rhapsode/index.html");

    }

    public static void main(String[] args) throws Exception {
        CommandLineParser commandlineParser = new DefaultParser();
        CommandLine commandLine = commandlineParser.parse(OPTIONS, args);
        Path propsFile = null;
        if (commandLine.hasOption("s")) {
            propsFile = Paths.get(commandLine.getOptionValue("s"));
        } else {
            propsFile = Paths.get("resources/config/search_config.json");
        }

        if (! Files.isRegularFile(propsFile)) {
            throw new RuntimeException("Couldn't find search_config.json file: "+propsFile.toAbsolutePath().toString());
        }
        int port = DEFAULT_PORT;
        if (commandLine.hasOption("p")) {
            port = Integer.parseInt(commandLine.getOptionValue("p"));
        }

        String host = null;
        if (commandLine.hasOption("h")) {
            host = commandLine.getOptionValue("h");
        }

        Path collectionPath = Paths.get(commandLine.getOptionValue("c"));
        if (!Files.isDirectory(collectionPath)) {
            throw new RuntimeException("Couldn't find collection: "+collectionPath.toAbsolutePath().toString());
        }

        RhapsodeServer server = new RhapsodeServer();
        try {
            server.execute(propsFile, port, host, collectionPath);
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
            try {
                Thread.sleep(120000);
            } catch (InterruptedException e2) {
                System.err.println("Wow, things are really going wrong today.");
            }
        }
    }
}

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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.rhapsode.app.RhapsodeSearcherApp;
import org.rhapsode.app.handlers.admin.CollectionHandler;
import org.rhapsode.app.handlers.admin.ConceptHandler;
import org.rhapsode.app.handlers.admin.ReportHandler;
import org.rhapsode.app.handlers.admin.SelectedDocumentHandler;
import org.rhapsode.app.handlers.admin.SettingsHandler;
import org.rhapsode.app.handlers.admin.StoredQueryHandler;
import org.rhapsode.app.handlers.admin.TaskHandler;
import org.rhapsode.app.handlers.indexer.TableFileHandler;
import org.rhapsode.app.handlers.search.BasicSearchHandler;
import org.rhapsode.app.handlers.search.ConcordanceSearchHandler;
import org.rhapsode.app.handlers.search.CooccurHandler;
import org.rhapsode.app.handlers.search.TargetCounterHandler;
import org.rhapsode.app.handlers.search.VariantTermHandler;
import org.rhapsode.app.handlers.search.Word2VecHandler;
import org.rhapsode.app.handlers.viewers.ExtractViewer;
import org.rhapsode.app.handlers.viewers.IndexedDocumentsViewer;
import org.rhapsode.app.handlers.viewers.IndividualIndexedDocumentViewer;
import org.rhapsode.app.utils.MyMimeTypes;
import org.rhapsode.util.UserLogger;

public class RhapsodeDesktopServlet {

    public final static int DEFAULT_PORT = 8092;
    private final static String LOCAL_HOST = "127.0.0.1";
    private static Options OPTIONS;

    static {
        OPTIONS = new Options();
        OPTIONS.addOption("log", "user-log", false, "log user queries (default: false")
                .addOption("c", "config", true, "search config json file")
                .addOption("p", "port", true, "port on which to run application, default is 8092");

    }

    public static String getVersion() {
        return "Rhapsode Prototype, v0.4.0-SNAPSHOT";
    }

    public static void main(String[] args) {
        DefaultParser defaultCLIParser = new DefaultParser();
        CommandLine commandLine = null;
        try {
            commandLine = defaultCLIParser.parse(OPTIONS, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            USAGE();
            return;
        }


        Path propsFile = null;
        if (commandLine.hasOption("c")) {
            propsFile = Paths.get(commandLine.getOptionValue("c"));
        } else {
            propsFile = Paths.get("resources/config/search_config.json");
        }

        if (commandLine.hasOption("log")) {
            UserLogger.setShouldLog(true);
        }

        int port = DEFAULT_PORT;
        if (commandLine.hasOption("p")) {
            port = Integer.parseInt(commandLine.getOptionValue("p"));
        }

        RhapsodeDesktopServlet servlet = new RhapsodeDesktopServlet();
        try {
            servlet.execute(propsFile, port);
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

    private static void USAGE() {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp(
                80,
                "java -cp resources/jars/rhapsode/* org.rhapsode.search.cli.RhapsodeDesktopServlet -log (log user queries) -c my_search_config.json",
                "starts the Rhapsode desktop servlet",
                OPTIONS,
                "");

    }

    public void execute(Path propsFile, int port) throws Exception {
        RhapsodeSearcherApp searchApp = RhapsodeSearcherApp.load(propsFile);
        System.out.println("Finished loading config file.");
        //BooleanQuery.setMaxClauseCount(searchApp.getMaxBooleanClauses());

        //configure server
        Server server = new Server();

        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        connector.setIdleTimeout(searchApp.getServerIdleTimeoutMillis());

        //	   connector.setRequestHeaderSize(8192);
        connector.setName("rhapsode");
        //Step 1 to lockdown jetty to only LOCAL_HOST
        connector.setHost(LOCAL_HOST);

        server.setConnectors(new Connector[]{connector});

        LocalHostOnlyContextHostWrapper handlerWrapper = new LocalHostOnlyContextHostWrapper();

        //index selector
      /*	    AbstractHandler indexSelectorHandler = handlerWrapper.wrap(
	    		new IndexSelectorHandler(config, storedQueryManager),
	    		"/rhapsode/index_selector", LOCAL_HOST);

       */
        //configure BasicSearcher
        AbstractHandler basicSearchHandler = handlerWrapper.wrap(
                new BasicSearchHandler(searchApp), "/rhapsode/basic", LOCAL_HOST);
        AbstractHandler concordanceHandler = handlerWrapper.wrap(
                new ConcordanceSearchHandler(searchApp), "/rhapsode/concordance", LOCAL_HOST);
        AbstractHandler cooccurHandler = handlerWrapper.wrap(
                new CooccurHandler(searchApp), "/rhapsode/cooccur", LOCAL_HOST);
        AbstractHandler targetCounterHandler = handlerWrapper.wrap(
                new TargetCounterHandler(searchApp), "/rhapsode/target_counter", LOCAL_HOST);
        AbstractHandler variantTermCounterHandler = handlerWrapper.wrap(
                new VariantTermHandler(searchApp), "/rhapsode/variant_term_counter", LOCAL_HOST);
        AbstractHandler indexHandler = handlerWrapper.wrap(
                new CollectionHandler(searchApp), "/rhapsode/admin/collection", LOCAL_HOST);
        AbstractHandler conceptHandler = handlerWrapper.wrap(new ConceptHandler(searchApp),
                "/rhapsode/admin/concepts", LOCAL_HOST);
        AbstractHandler storedQueryHandler = handlerWrapper.wrap(new StoredQueryHandler(searchApp),
                "/rhapsode/admin/queries", LOCAL_HOST);
        AbstractHandler settingsHandler = handlerWrapper.wrap(new SettingsHandler(searchApp),
                "/rhapsode/admin/settings", LOCAL_HOST);
        AbstractHandler selectedHandler = handlerWrapper.wrap(new SelectedDocumentHandler(searchApp),
                "/rhapsode/admin/selecteds", LOCAL_HOST);

        AbstractHandler reportHandler = handlerWrapper.wrap(new ReportHandler(searchApp),
                "/rhapsode/admin/reports", LOCAL_HOST);
        AbstractHandler extractViewerHandler = handlerWrapper.wrap(new ExtractViewer(searchApp),
                "/rhapsode/view_extract", LOCAL_HOST);

        //   AbstractHandler analyzerTestHandler = handlerWrapper.wrap(delegator, "/rhapsode/analyzer", LOCAL_HOST);

        AbstractHandler docViewerHandler =
                handlerWrapper.wrap(new IndividualIndexedDocumentViewer(searchApp),
                        "/rhapsode/view_doc", LOCAL_HOST);
        AbstractHandler docsViewerHandler =
                handlerWrapper.wrap(new IndexedDocumentsViewer(searchApp),
                        "/rhapsode/view_docs", LOCAL_HOST);

        //configure resourceHandler
        MimeTypes mimeTypes = new MyMimeTypes(new File("resources/config/extra_mimes.properties")).getTypes();

        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setMimeTypes(mimeTypes);
        resourceHandler.setDirectoriesListed(true);
        //TODO: toString() right?
        //figure this one out...doh!
        if (searchApp.getRhapsodeCollection() != null) {
            resourceHandler.setResourceBase(searchApp.getRhapsodeCollection().getOrigDocsRoot().toString());
        }
        searchApp.setResourceHandler(resourceHandler);
        AbstractHandler wrappedResourceHandler = handlerWrapper.wrap(resourceHandler, "/rhapsode/download",
                LOCAL_HOST);

        ResourceHandler helloHandler = new ResourceHandler();
        helloHandler.setDirectoriesListed(false);
        helloHandler.setWelcomeFiles(new String[]{"index.html"});
        String webDir = RhapsodeDesktopServlet.class.getClassLoader().getResource("WEB-INF").toExternalForm();

        helloHandler.setResourceBase(webDir);
        //        AbstractHandler wrappedHelloHandler = handlerWrapper.wrap(helloHandler, "/", LOCAL_HOST);
        AbstractHandler wrappedHelloHandler = handlerWrapper.wrap(helloHandler, "/rhapsode", LOCAL_HOST);

        ResourceHandler reportsHandler = new ResourceHandler();
        reportsHandler.setMimeTypes(mimeTypes);
        reportsHandler.setDirectoriesListed(true);
        reportsHandler.setResourceBase("reports");
        reportsHandler.setMinMemoryMappedContentLength(-1);
        reportsHandler.setMinAsyncContentLength(-1);
        AbstractHandler wrappedReportsHandler = handlerWrapper.wrap(reportsHandler, "/rhapsode/reports", LOCAL_HOST);

        TableFileHandler tableFileHandler = new TableFileHandler(searchApp);
        AbstractHandler wrappedTableFileHandler = handlerWrapper.wrap(tableFileHandler, "/rhapsode/indexer", LOCAL_HOST);
        AbstractHandler wrappedTaskHandler = handlerWrapper.wrap(new TaskHandler(searchApp),
                "/rhapsode/tasks", LOCAL_HOST);

        AbstractHandler wrappedW2VHandler = handlerWrapper.wrap(new Word2VecHandler(searchApp),
                "/rhapsode/w2v", LOCAL_HOST);
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[]{
                indexHandler,
                conceptHandler,
                storedQueryHandler,
//                analyzerTestHandler,
                concordanceHandler,
                cooccurHandler,
                variantTermCounterHandler,
                targetCounterHandler,
                basicSearchHandler,
                docViewerHandler,
                docsViewerHandler,
                wrappedResourceHandler,
                wrappedHelloHandler,
                reportHandler,
                extractViewerHandler,
                settingsHandler,
                selectedHandler,
                wrappedReportsHandler,
                wrappedTableFileHandler,
                wrappedTaskHandler,
                wrappedW2VHandler
        });

        server.setHandler(handlers);
        tryToLoadLastCollection(searchApp);
        server.start();
        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
        System.out.println("The search server has been successfully started.");
        System.out.println("Open a browser and navigate to:\nhttp://localhost:" + port + "/rhapsode/index.html");
        System.out.print("\n\n\n\n\n\n\n\n\n\n\n\n\n\nWhen you have finished using the Rhapsode search server,\n" +
                "type [Enter] to stop the application.");
        System.out.println("\n\n\n");
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
            String str = "";
            while (str != null) {

                str = in.readLine();
                // if (str.length() > 0){
                server.stop();
                break;
                //}
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("The server has successfully shut down.");
        server.destroy();
        System.out.println("The server has been completely turned off");
        try {
            searchApp.getSessionManager().getConnection().close();
            System.out.println("The session manager db has been closed");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (searchApp.getRhapsodeCollection() != null &&
                searchApp.getRhapsodeCollection().getIndexManager() != null) {
            searchApp.getRhapsodeCollection().getIndexManager().close();
        }
        System.out.println("The search app has closed the index.");
        System.exit(0);
        System.out.println("You should never see this");

    }

    private void tryToLoadLastCollection(RhapsodeSearcherApp searchConfig) {
        List<Pair<Path, Date>> collections = null;
        try {
            collections = searchConfig.getSessionManager().getCollectionsHistory().getPaths();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (collections != null && collections.size() > 0) {
            Path lastCollectionPath = collections.get(0).getKey();
            try {
                searchConfig.tryToLoadRhapsodeCollection(lastCollectionPath);
                System.out.println("Successfully loaded collection at: " + lastCollectionPath);
            } catch (IOException e) {
                System.err.println("tried to load collection at path: " + lastCollectionPath);
                e.printStackTrace();
            }
        }

    }
}

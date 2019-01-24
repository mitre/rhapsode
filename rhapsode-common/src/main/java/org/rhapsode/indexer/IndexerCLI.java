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


package org.rhapsode.indexer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.tika.batch.BatchProcessDriverCLI;
import org.rhapsode.RhapsodeCollection;
import org.rhapsode.lucene.search.IndexManager;
import org.rhapsode.util.ParamUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexerCLI {

    private static final Logger LOG = LoggerFactory.getLogger(IndexerCLI.class);


    private final static Path DEFAULT_SCHEMA_PATH = Paths.get("resources/config/default_index_schema.json");

    public static Options getOptions() {
        Options opts = new Options();
        opts.addOption("i", "input", true, "Path to the original documents' root directory");
        opts.addOption("s", "defaultSchema", true, "Schema to use for first load of a collection");
        opts.addOption("c", "collection", true, "Path to the collection");
        opts.addOption("e", "extractOnly", false, "Run the extraction process only");
        opts.addOption("l", "indexOnly", false, "Run the (L)ucene indexer only");
        opts.addOption("m", "mergeOnly", false, "Merge the Lucene index only");
        opts.addOption("o", "overwriteIndex", true, "Delete index before running the indexer");
        opts.addOption("a", "appendToIndex", true, "If an index exists, append to it");
        opts.addOption("x", "JXmx", true, "use this heap space -Xmx for child processes: -x 2g");
        opts.addOption("rt", "maxRestartsTika", true, "maximum number of times that Tika can restart");
        opts.addOption("df", "duplicateField", true, "specify a field to use to delete documents that have a duplicate value");
        opts.addOption("n", "numConsumers", true, "number of threads");
        opts.addOption("merge", true, "number of index segments to merge to");
        opts.addOption("tArgs", true, "jvm args for the Tika extraction process; no dash, semicolon delimited, e.g. " +
                "'Xmx4g;Xss512m'");
        opts.addOption("lArgs", true, "jvm args for the Lucene indexing process; no dash, semicolon delimited, e.g. " +
                "'Xmx4g;Xss512m'");
        return opts;
    }

    private static void usage(String msg) {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp(msg, getOptions());
        System.exit(-1);
    }

    public static void main(String[] args) throws Exception {
        CommandLineParser p = new DefaultParser();
        IndexerCLI cli = new IndexerCLI();
        cli.execute(p.parse(getOptions(), args));
    }

    public void execute(CommandLine cl) throws Exception {

        Path origDocsPath = ParamUtil.getAbsolutePath(cl.getOptionValue('i'), null);
        Path collectionPath = ParamUtil.getAbsolutePath(cl.getOptionValue('c'), null);
        Path defaultSchemaPath = ParamUtil.getAbsolutePath(cl.getOptionValue('s'), null);
        if (cl.hasOption('e') && cl.hasOption('l')) {
            usage("Can't specify both extract only and index only");
        }
        boolean shouldExtract = cl.hasOption('e') || (!cl.hasOption('l') && !cl.hasOption('m'));
        boolean shouldIndex = cl.hasOption('l') || (!cl.hasOption('e') && !cl.hasOption('m'));
        boolean shouldDeleteDupes = cl.hasOption("df");
        boolean shouldMerge = cl.hasOption('m') || cl.hasOption("merge");

        int numConsumers = -1;
        if (cl.hasOption('n')) {
            String v = cl.getOptionValue('n');
            numConsumers = Integer.parseInt(v);
        }

        if (collectionPath == null) {
            System.err.println("User didn't specify a collection '-c' option, defaulting to: collections/collection1");
            collectionPath = Paths.get("collections/collection1");
        }
        Path collectionSchemaPath = collectionPath.resolve(RhapsodeCollection.INDEX_SCHEMA_FILE_NAME);
        if (Files.isRegularFile(
                collectionPath) &&
                collectionSchemaPath != null) {
            usage("Can't specify a new index schema when one already exists");
        }

        if (defaultSchemaPath == null) {
            defaultSchemaPath = DEFAULT_SCHEMA_PATH;
            System.err.println("Didn't specify a default index schema; backing off to the default:" + DEFAULT_SCHEMA_PATH.toAbsolutePath());
        }
        if (collectionSchemaPath == null || (
                !Files.isRegularFile(collectionSchemaPath) && defaultSchemaPath == null)) {
            usage("Must either have an existing schema in the collection or " +
                    "specify 's' a default schema to start with");
        }

        if (cl.hasOption('l') && cl.hasOption('i')) {
            usage("Don't specify a raw input directory 'i' if you are just indexing 'l'.\n" +
                    "I worry that you might be misunderstanding the options.");
        }

        if (origDocsPath == null) {
            System.err.println("Didn't specify an input directory '-i', defaulting to 'input/'");
            origDocsPath = Paths.get("input");
        }
        RhapsodeCollection rc = null;
        //TODO make this more robust...do other types of tests
        //to make sure we aren't overwriting anything
        if (!Files.isRegularFile(collectionSchemaPath)) {
            rc = RhapsodeCollection.build(origDocsPath, collectionPath, defaultSchemaPath);
        } else {
            rc = RhapsodeCollection.loadExisting(collectionPath);
        }

        if (shouldExtract) {
            System.out.println("extracting");
            runExtraction(rc, cl, numConsumers);
        }

        if (shouldIndex) {
            System.out.println("indexing");
            runIndexer(rc, cl, numConsumers);
        }
        if (shouldDeleteDupes) {
            System.out.println("deleting duplicates");
            if (rc.getIndexManager() == null) {
                IndexManager.load(rc);
            }
            try {
                rc.getIndexManager().deleteDuplicates(rc, cl.getOptionValue("df"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (shouldMerge) {
            int segs = Integer.parseInt(cl.getOptionValue("merge"));
            if (rc.getIndexManager() == null) {
                IndexManager.load(rc);
            }
            rc.getIndexManager().merge(rc, segs);
        }

    }


    private void runIndexer(RhapsodeCollection rc, CommandLine cl, int numConsumers) throws Exception {
        List<String> args = new ArrayList<>();
        //make most of this more configurable!!!, jars, etc
        //java -Xmx128m -cp "%CP%" org.apache.tika.batch.fs.FSBatchProcessCLI -bc resources/config/batch-indexer-config.xml -inputDir resources/collection1
        args.add("java");
        args.add("-maxRestarts");
        args.add("0");
//        args.add("-waitAfterShutdownMillis");
        //      args.add("4000000");//roughly one hour

        if (cl.hasOption('x')) {
            args.add("-Xmx" + cl.getOptionValue('x'));
        }
        addJVMArgs(cl.getOptionValue("lArgs"), args, "-");
//        args.add("-Xmx128m");
        args.add("-cp");
        args.add("resources/jars/rhapsode/*");
        args.add("org.apache.tika.batch.fs.FSBatchProcessCLI");
        args.add("-bc");
        args.add("resources/config/batch-indexer-config.xml");
        args.add("-inputDir");
        args.add(rc.getExtractedTextRoot().toAbsolutePath().toString());
        args.add("-collectionPath");
        args.add(rc.getCollectionPath().toAbsolutePath().toString());
        if (numConsumers > 0) {
            args.add("-numConsumers");
            args.add(Integer.toString(numConsumers));
        }
        logCommandLine("lucene", args);
        BatchProcessDriverCLI driverCLI = new BatchProcessDriverCLI(args.toArray(new String[args.size()]));
        driverCLI.execute();
    }

    private void logCommandLine(String processName, List<String> args) {
        LOG.info("commandline for " + processName + ": " + StringUtils.join(args, "\n"));
    }

    private void addJVMArgs(String argString, List<String> args, String prefix) {
        if (argString == null) {
            return;
        }

        for (String arg : argString.split(";")) {
            String a = prefix + arg;
            args.add(a);
        }
    }

    private void runExtraction(RhapsodeCollection rc, CommandLine userOptions, int numConsumers) throws Exception {
        List<String> commandLine = new ArrayList<>();
        commandLine.add("java");
        if (userOptions.hasOption("rt")) {
            commandLine.add("-maxRestarts");
            commandLine.add(userOptions.getOptionValue("rt"));
        }
        commandLine.add("-cp");
        commandLine.add("resources/jars/tika/*");
        addJVMArgs(userOptions.getOptionValue("tArgs"), commandLine, "-");
        commandLine.add("org.apache.tika.batch.fs.FSBatchProcessCLI");
        commandLine.add("-bc");
        commandLine.add("resources/config/tika-batch-config.xml");
        commandLine.add("-inputDir");
        commandLine.add(rc.getOrigDocsRoot().toAbsolutePath().toString());
        commandLine.add("-outputDir");
        commandLine.add(rc.getExtractedTextRoot().toAbsolutePath().toString());
        //make this configurable

        if (numConsumers > 0) {
            commandLine.add("-numConsumers");
            commandLine.add(Integer.toString(numConsumers));
        }
        logCommandLine("tika", commandLine);
        BatchProcessDriverCLI driverCLI = new BatchProcessDriverCLI(commandLine.toArray(new String[commandLine.size()]));
        driverCLI.execute();
    }
}

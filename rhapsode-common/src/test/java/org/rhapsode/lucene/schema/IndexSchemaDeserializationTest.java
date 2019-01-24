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

package org.rhapsode.lucene.schema;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cjk.CJKBigramFilterFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.fa.PersianCharFilterFactory;
import org.apache.lucene.analysis.icu.ICUFoldingFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;
import org.junit.Test;

public class IndexSchemaDeserializationTest {

    //TODO: add more tests!!

    @Test
    public void basicReadTest() throws Exception {
        Path p = Paths.get(this.getClass().getResource("/schema.json").toURI());
        IndexSchema indexSchema = IndexSchema.load(p);
        assertEquals(12, indexSchema.getDefinedFields().size());


        Analyzer persian = indexSchema.getAnalyzerByName("persian_icu");
        assertTrue(persian instanceof CustomAnalyzer);

        List<TokenFilterFactory> tokenFilterFactories = ((CustomAnalyzer) persian).getTokenFilterFactories();
        assertTrue(tokenFilterFactories.get(0) instanceof ICUFoldingFilterFactory);
        assertTrue(tokenFilterFactories.get(1) instanceof CJKBigramFilterFactory);

        assertTrue(((CustomAnalyzer) persian).getTokenizerFactory() instanceof StandardTokenizerFactory);

        assertTrue(((CustomAnalyzer) persian).getCharFilterFactories().get(0)
                instanceof PersianCharFilterFactory);


        assertEquals(6, indexSchema.getFieldMapper().getTikaFields().size());
        List<IndivFieldMapper> ms = indexSchema.getFieldMapper().get("tika_batch_fs:relative_path");
        assertEquals(2, ms.size());
        IndivFieldMapper m0 = ms.get(0);
        IndivFieldMapper m1 = ms.get(1);
        assertEquals("rel_path", m0.getToField());
        assertEquals("display_name", m1.getToField());
        String[] tst = m0.map(
                new String[]{"the/quick/brown/fox1234.txt", "jumped/over.doc"}
        );
        assertArrayEquals(new String[]{
                "the/quick/brown/fox1234.txt", "jumped/over.doc"
        }, tst);

        tst = m1.map(
                new String[]{"the/quick/brown/fox1234.txt", "jumped/over.doc"}
        );
        assertArrayEquals(new String[]{
                "1234", "over"
        }, tst);

        ms = indexSchema.getFieldMapper().get("X-TIKA:content");
        assertEquals(3, ms.size());
        assertEquals(IdentityFieldMapper.class, ms.get(0).getClass());
        assertEquals(LangIdMapper.class, ms.get(1).getClass());
        assertEquals(LangIdsMapper.class, ms.get(2).getClass());
    }

    @Test
    public void testSerializing() throws Exception {
        IndexSchema schema2 = new IndexSchema();
        Path tmpFile = Files.createTempFile("test-schema", "");
        try (OutputStream os = Files.newOutputStream(tmpFile)) {
            IndexSchema.write(schema2, os);
        } finally {
            Files.delete(tmpFile);
        }
    }

    @Test
    public void testUpdating() throws Exception {
        IndexSchema schema = IndexSchema.load(this.getClass().getResourceAsStream("/default_table_index_schema.json"));
        FieldType ft = new FieldType();
        ft.setStored(true);
        ft.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        ft.setTokenized(true);
        FieldDef fieldDef = new FieldDef("content", false, ft);
        fieldDef.setAnalyzers(new NamedAnalyzer("text", schema.getAnalyzerByName("text")), null, null, null);
        schema.addField("content", fieldDef);
        Path tmpFile = Files.createTempFile("test-schema", "");
        try (OutputStream os = Files.newOutputStream(tmpFile)) {
            IndexSchema.write(schema, os);
            os.flush();
        }

        IndexSchema reloaded = IndexSchema.load(tmpFile);
    }

    @Test
    public void testUpdated() throws Exception {
        //use this to test modifications to the schema/turn this into a real test. :)
        IndexSchema schema = IndexSchema.load(this.getClass().getResourceAsStream("/test_index_schema.json"));

    }
}

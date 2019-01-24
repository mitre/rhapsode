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

package org.rhapsode.lucene.search;

import java.lang.reflect.Field;
import java.sql.Types;

import org.rhapsode.lucene.utils.SqlUtil;

public class StoredConceptBuilder {

    final private StoredConcept sc;

    public StoredConceptBuilder(String queryName) {
        sc = new StoredConcept(queryName);
    }

    public StoredConcept build() throws IllegalStateException {
        //add sanity checks
        return sc;
    }

    public StoredConceptBuilder add(SCField scField, String value) {
        if (value == null) {
            return this;
        }
        try {
            Field field = sc.getClass().getDeclaredField(scField.getObjFieldName());
            switch (scField.getType()) {
                case Types.VARCHAR:
                    if (value.length() > scField.getMaxLength()) {
                        throw new IllegalArgumentException("For stored query '" +
                                sc.getConceptName() + "' the length for field '" +
                                scField.getXlsxName() + "' must be < " + scField.getMaxLength());
                    }
                    field.set(sc, value);
                    break;
                default:
                    throw new RuntimeException("Need to add type: " + SqlUtil.getTypeName(scField.getType()));
            }
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        return this;
    }
}

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

import java.io.IOException;
import java.util.Random;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.search.LeafFieldComparator;
import org.apache.lucene.search.Scorable;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;

/**
 * modified from http://stackoverflow.com/questions/7201638/lucene-2-9-2-how-to-show-results-in-random-order
 */
public class RandomSort {


    public Sort getRandomSort() {
        return new Sort(
                new SortField("",
                        new RandomFieldComparatorSource())
        );
    }

    private static class RandomLeafFieldComparator implements LeafFieldComparator {
        Random r = new Random();

        @Override
        public void setBottom(int i) {

        }

        @Override
        public int compareBottom(int i) throws IOException {
            return r.nextInt();
        }

        @Override
        public int compareTop(int i) throws IOException {
            return r.nextInt();
        }

        @Override
        public void copy(int i, int i1) throws IOException {
            //noop
        }

        @Override
        public void setScorer(Scorable scorable) throws IOException {
            //noop
        }

    }

    private class RandomFieldComparatorSource extends FieldComparatorSource {
        @Override
        public FieldComparator<?> newComparator(String arg0, int arg1, int arg2,
                                                boolean arg3) {
            return new RandomFieldComparator();
        }
    }

    private class RandomFieldComparator extends FieldComparator<Integer> {
        Random rand = new Random();

        @Override
        public int compare(int arg0, int arg1) {
            return rand.nextInt();
        }


        @Override
        public Integer value(int arg0) {
            return rand.nextInt();
        }

        @Override
        public LeafFieldComparator getLeafComparator(LeafReaderContext leafReaderContext) throws IOException {
            return null;
        }

        @Override
        public void setTopValue(Integer arg0) {

        }
    }
}

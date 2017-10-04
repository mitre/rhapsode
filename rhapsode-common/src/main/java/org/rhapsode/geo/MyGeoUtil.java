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
package org.rhapsode.geo;


import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Query;
import org.apache.lucene.spatial.SpatialStrategy;
import org.apache.lucene.spatial.prefix.TermQueryPrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.spatial.query.SpatialArgs;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.distance.DistanceCalculator;
import org.locationtech.spatial4j.distance.DistanceUtils;
import org.locationtech.spatial4j.distance.GeodesicSphereDistCalc;
import org.locationtech.spatial4j.shape.Circle;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Shape;
import org.locationtech.spatial4j.shape.SpatialRelation;
import org.locationtech.spatial4j.shape.impl.CircleImpl;
import org.locationtech.spatial4j.shape.impl.PointImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MyGeoUtil {

    private final static String COMMA = ",";
    private final static int DEFAULT_PRECISION = 9;

    public static SpatialContext getDefaultSpatialContext() {
        DistanceCalculator dc = new GeodesicSphereDistCalc.Haversine();
        SpatialContext ctx = new SpatialContext(true, dc, null);

        return ctx;
    }

    public static SpatialStrategy getDefaultStrategy(String field, int maxLevels) {
        SpatialContext ctx = getDefaultSpatialContext();
        SpatialPrefixTree grid = new GeohashPrefixTree(ctx, maxLevels);
        return new TermQueryPrefixTreeStrategy(grid, field);
    }


    public static Circle getCircle(double lat, double lng, double distanceInKM, Point p, SpatialContext ctx) {
        double degrees = DistanceUtils.dist2Degrees(distanceInKM, DistanceUtils.EARTH_EQUATORIAL_RADIUS_KM);

        p = getPoint(lat, lng, p, ctx);
        return new CircleImpl(p, degrees, ctx);
    }

    public static Circle getCircle(Point p, double distanceInKM, SpatialContext ctx) {
        //can return null!
        if (p == null || distanceInKM < 0.0)
            return null;

        double degrees = DistanceUtils.dist2Degrees(distanceInKM, DistanceUtils.EARTH_EQUATORIAL_RADIUS_KM);
        return new CircleImpl(p, degrees, ctx);
    }

/*  query builders
 * 	
 */


    public static Query buildQuery(String field, double lat, double lng,
                                   double distanceInKM, Point p, int maxLevels, SpatialContext ctx) {
        Circle queryShape = getCircle(lat, lng, distanceInKM, p, ctx);
        SpatialStrategy strategy = getDefaultStrategy(field, maxLevels);

        return strategy.makeQuery(new SpatialArgs(SpatialOperation.Intersects, queryShape));
    }

    public static Query buildQuery(String fieldName, Shape queryShape) {
        SpatialStrategy strategy = getDefaultStrategy(fieldName, DEFAULT_PRECISION);

        return strategy.makeQuery(new SpatialArgs(SpatialOperation.Intersects, queryShape));

    }

    public static List<PointOffsetStringPair> getPointOffsetStrings(Document d, String offsetFieldName,
                                                                    String geoCoordFieldName,
                                                                    Shape queryShape,
                                                                    SpatialContext ctx) throws IOException {

        //if query shape is null, return all

        IndexableField[] offsetRecords = d.getFields(offsetFieldName);
        IndexableField[] geoCoords = d.getFields(geoCoordFieldName);
        List<PointOffsetStringPair> offsets = new ArrayList<PointOffsetStringPair>();
        //if no geocoord fields

        if (offsetRecords.length < 1) {
            return offsets;
        }

        Point p = null;
        for (int i = 0; i < geoCoords.length; i++) {

            p = getPoint(geoCoords[i], p, ctx);
            if (p == null) {
                //error in parsing string
                //dangerous, silently swallow
                continue;
            }
            if (queryShape == null || intersects(queryShape, p, ctx)) {
                IndexableField tmp = offsetRecords[i];

                if (tmp != null) {
                    offsets.add(new PointOffsetStringPair(deepCopy(p, ctx), tmp.stringValue()));
                }
                //TODO: this should not be silent!
            }
        }
        return offsets;
    }

    public static String toString(Point p) {
        return Double.toString(p.getY()) + COMMA + Double.toString(p.getX());
    }

    public static Point deepCopy(Point p, SpatialContext ctx) {
        Point np = new PointImpl(p.getX(), p.getY(), ctx);
        return np;
    }

    public static Point getPoint(double lat, double lng, Point p, SpatialContext ctx) {

        if (p == null) {
            p = new PointImpl(lng, lat, ctx);
        } else {
            p.reset(lng, lat);
        }
        return p;
    }

    public static Point getPoint(String pString, Point p, SpatialContext ctx) {
        //can return null!!!
        if (pString == null)
            return null;

        try {
            String[] ps = pString.split(COMMA);
            if (ps.length < 2) {
                return null;
            }
            double lat = Double.parseDouble(ps[0]);
            double lng = Double.parseDouble(ps[1]);
            return getPoint(lat, lng, p, ctx);
        } catch (NumberFormatException e) {
            //swallow
        }
        return null;
    }

    public static Point getPoint(IndexableField field, Point p, SpatialContext ctx) {
        return getPoint(field.stringValue(), p, ctx);
    }

    public static Point getPoint(IndexableField field, SpatialContext ctx) {
        return getPoint(field, null, ctx);
        //return GeohashUtils.decode(field.stringValue(), ctx);
    }

    public static boolean intersects(Shape s1, Shape s2, SpatialContext ctx) {
        if (s1.relate(s2).equals(SpatialRelation.DISJOINT))
            return false;

        return true;
    }

    public static IndexableField buildPointField(String geocoordsFieldName,
                                                 Point point) {

        return new StringField(geocoordsFieldName, toString(point), Field.Store.YES);
    }

}

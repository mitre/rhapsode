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


import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Query;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Shape;

public class GeoQueryBuilder {


    //	private static final double EARTHS_RADIUS = 6378.1370;//wgs-84 in km
    //lazy initialization of xcoord.  Be careful!!!
//    private XCoord xcoord = null;

    private final GeoConfig geoConfig;

    public GeoQueryBuilder(GeoConfig geoConfig) {
        this.geoConfig = geoConfig;
    }

    /*   private void initXCoord() throws ParseException{
           try{
               Logger logger = Logger.getLogger(XCoord.class);
               logger.setLevel(Level.OFF);
               Logger.getLogger(org.mitre.xcoord.PatternManager.class).setLevel(Level.OFF);
               xcoord = new XCoord();

               if (geoConfig.getXCoordConfig() != null){

                   xcoord.configure(geoConfig.getXCoordConfig().toURI().toURL());
               } else {
                   System.err.println("backing off to jarred xcoord config file");
                   URL xCoordConfigURL = GeoQueryBuilder.class.getResource("geocoord_regex.cfg");
                   xcoord.configure(xCoordConfigURL);
               }
               //}// catch (XCoordException e){
           } catch (Exception e){
               throw new ParseException("couldn't open xcoord config file");
           }
       }
   */
    public Query buildGeoQuery(String fieldName, Shape shape) {//Circle circleQuery){
        return MyGeoUtil.buildQuery(fieldName, shape);

    }

    public BooleanClause buildGeoClause(String fieldName, String coordinateString, String radiusKMString) throws ParseException {
        if (coordinateString == null)
            return null;
        BooleanClause.Occur occur = BooleanClause.Occur.SHOULD;

        if (coordinateString.startsWith("AND ")) {
            coordinateString = coordinateString.substring(4);
            occur = BooleanClause.Occur.FILTER;
        } else if (coordinateString.startsWith("NOT ")) {
            coordinateString = coordinateString.substring(4);
            occur = BooleanClause.Occur.MUST_NOT;
        }

        Shape queryShape = buildQueryShape(coordinateString, radiusKMString);
        Query query = MyGeoUtil.buildQuery(fieldName, queryShape);
        return new BooleanClause(query, occur);
    }

    public Shape buildQueryShape(String coordinateString, String radiusKMString) throws ParseException {
        //snip and ignore initial "and" and "not"
        if (coordinateString.startsWith("AND ") || coordinateString.startsWith("NOT ")) {
            coordinateString = coordinateString.substring(4);
        }

        if (coordinateString.trim().equals("*/*")) {
            Shape rect = MyGeoUtil.getDefaultSpatialContext().getWorldBounds();
            return rect;
        }
        Point point = null;
        point = trySimpleLatLong(coordinateString, point);
        if (point == null) {
            point = getFirstCoord(coordinateString, point);
        }

        if (point == null) {
            throw new ParseException("I couldn't parse the geo coordinate: " + coordinateString);
        }

        double radiusKM = -1.0;
        try {
            radiusKM = Double.parseDouble(radiusKMString);
        } catch (NumberFormatException e) {
            throw new ParseException("I couldn't parse the radius size in KM");
        }
        if (radiusKM <= 0.0) {
            throw new ParseException("I couldn't parse the radius size in KM");
        }

        return MyGeoUtil.getCircle(point, radiusKM, geoConfig.getSpatialContext());
    }

    private Point trySimpleLatLong(final String s, Point point) {
        //modifies the value of point! or not
        String[] bits = s.trim().split("[ ,]+");
        double lat = -361.0;
        double lng = -361.0;
        if (bits.length > 1) {
            try {
                lat = Double.parseDouble(bits[0]);
                lng = Double.parseDouble(bits[1]);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (Math.abs(lat) <= 90.0 && Math.abs(lng) <= 180.0) {
            point = MyGeoUtil.getPoint(lat, lng, point, geoConfig.getSpatialContext());
            return point;
        }
        return null;
    }

    public Point getFirstCoord(String s, Point p) throws ParseException {
        return trySimpleLatLong(s, p);
/*        //dangerous! lazy initialization
        //can return null
        if (xcoord == null){
            initXCoord();
        }

        TextMatchResultSet result = xcoord.extract_coordinates(s, "empty");
        for (TextMatch match : result.matches){
            GeocoordMatch gmatch = (GeocoordMatch)match;
            p = MyGeoUtil.getPoint(gmatch.latitude, gmatch.longitude, p, geoConfig.getSpatialContext());
            return p;
        }
        return null;*/
    }

}

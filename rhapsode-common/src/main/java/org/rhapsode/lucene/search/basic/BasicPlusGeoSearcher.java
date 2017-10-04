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
package org.rhapsode.lucene.search.basic;


/**
 * This was needed for highlighting geo entities that were pulled out
 * free text.
 */

public class BasicPlusGeoSearcher {
    //	private final static int ABSOLUTE_MAX_CHARS_FOR_HIGHLIGHTING = 1000000;
/*

  public BasicSearchResults search(IndexSearcher searcher, Query highlightingQuery,
      Query retrievalQuery, Analyzer analyzer, 
      BasicSearchConfig config, DocMetadataExtractor mEx,
      Shape queryShape, String geoCoordsField,
      String geoOffsetsField, SpatialContext ctx) throws ParseException, IOException {
   
      int howMany = BasicSearcher.calcHowMany(config);
      
      Sort sort = config.getSort();
      
      TopDocs topDocs = null;
      try{
         //according to the javadocs (v4.5.1, including Sort() slightly increases overhead)
         if (sort == null){
            topDocs = searcher.search(retrievalQuery, howMany);
         } else {
            topDocs = searcher.search(retrievalQuery, howMany, sort);
         }
      } catch (BooleanQuery.TooManyClauses e){
         throw new ParseException("Too many clauses in boolean query");
      }
      
      ScoreDoc[] scoreDocs = topDocs.scoreDocs;
      //System.out.println("LEN of Hits: " + scoreDocs.length);
      int start = -1;
      if (config.getBsPagingDirection() == PagingDirection.NEXT){
         start = config.getBsLastEnd()+1;
      } else if (config.getBsPagingDirection().equals(PagingDirection.PREVIOUS)){
         start = config.getBsLastStart()-config.getBsResultsPerPage();
      } else if (config.getBsPagingDirection() == PagingDirection.SAME){
         start = config.getBsLastStart();
      }
      start = (start > -1) ? start : 0;
      int end = start + config.getBsResultsPerPage()-1;
      List<BasicSearchResult> results = new ArrayList<BasicSearchResult>();
      IndexReader reader = searcher.getIndexReader();
      Set<String> userSelected = mEx.getFieldSelector();
      Set<String> selected = new HashSet<String>(userSelected);
      if (config.getBsMaxSnippetLengthChars() > 0 && config.getBsSnippetsPerResult() > 0){
         selected.add(config.getContentField());
      }
      selected.add(geoCoordsField);
      selected.add(geoOffsetsField);

      for (int i = start; i <= end && i < scoreDocs.length; i++){
         Document d = reader.document(scoreDocs[i].doc, selected);
         String snippetString = StringUtils.EMPTY;
         if (config.getBsMaxSnippetLengthChars() > 0 && config.getBsSnippetsPerResult() > 0){
           
            String[] txt = d.getValues(config.getContentField());

            //first try content
            StringBuilder sb = BasicSearcher.tryToGetSnippetFromContent(config, highlightingQuery, txt, analyzer);

            snippetString = sb.toString();
            //then try geo
            if (StringUtils.isEmpty(snippetString) && queryShape != null){
               List<String> geoStrings = getQueriedGeoStrings(d, ctx, queryShape, geoCoordsField, geoOffsetsField);
               snippetString = tryToGetGeoSnippet(geoStrings, txt, config);
            }
            //last ditch, just take first x characters
            if (StringUtils.isEmpty(snippetString)){
               if (txt.length > 0 && txt[0] != null)  {
                 int tmpEnd = Math.min(config.getBsMaxSnippetLengthChars(), txt[0].length());
                 if (tmpEnd > 0){
                   snippetString = HTMLWriterUtil.clean(txt[0].substring(0, tmpEnd));
                 }
               }
            }
         }
         BasicSearchResult result = new BasicSearchResult();
         result.setSnippet(snippetString);
         result.setMetadata(mEx.extract(d));
         result.setN(i+1);
         results.add(result);
      }
      return new BasicSearchResults(results, start, end, topDocs.totalHits);
   }

   private String tryToGetGeoSnippet(List<String> geoStrings, String[] strings, BasicSearchConfig config){
      //TODO: for now assume that strings is single valued!!!
      String content = strings[0];
      //this assumes that only matching geo strings are in the list
      StringBuilder sb = new StringBuilder();
      WindowBuilder windowBuilder = null;
      //TODO: add snippet configuration info to the calculations
      int maxHits = config.getBsSnippetsPerResult();
      
      int hits = 0;
      for (int i = 0; i < geoStrings.size() && hits < maxHits; i++){
         String offsetString = geoStrings.get(i);
         OffsetAttribute offset = GeoCoordOffsetUtil.getOffset(offsetString);
         if (offset == null){
            String asIfContent = GeoCoordOffsetUtil.getContent(offsetString);
            if (! StringUtils.isEmpty(asIfContent)){
               sb.append(" <span style=\"background: #F2FA06; \">");
               sb.append(HTMLWriterUtil.clean(asIfContent));
               sb.append("</span> ");
               if (hits > 0 && hits < maxHits-1){
                  sb.append("...");
               }
               hits++;
            }
         } else {
            //lazy initialization
            if (windowBuilder == null){
               windowBuilder = new WindowBuilder();
            }
            Window w = windowBuilder.buildWindow(content, offset.startOffset(), offset.endOffset(), 10, 10);
            sb.append(HTMLWriterUtil.clean(w.getPre()));
            sb.append(" <span style=\"background: #F2FA06; \">");
            sb.append(HTMLWriterUtil.clean(w.getTarget()));
            sb.append("</span> ");
            sb.append(HTMLWriterUtil.clean(w.getPost()));
            if (hits > 0 && hits < maxHits-1){
               sb.append(BasicSearcher.ELLIPSE);
            }
            hits++;
         }
      }
      return sb.toString();
   }
   private List<String> getQueriedGeoStrings(Document d,
         SpatialContext ctx,
         Shape queryShape, 
         String geoCoordsFieldName, String geoOffsetFieldName) {
      List<String> geoStrings = new ArrayList<String>();

      IndexableField[] geoCoords = d.getFields(geoCoordsFieldName);
      IndexableField[] geoOffsets = d.getFields(geoOffsetFieldName);
      for (int i = 0; i < geoCoords.length; i++){
         IndexableField geoCoordsField = geoCoords[i];
         Point p = MyGeoUtil.getPoint(geoCoordsField, ctx);
         if (p != null && MyGeoUtil.intersects(queryShape,p, ctx)){
            String v = (geoOffsets[i] == null) ? StringUtils.EMPTY : geoOffsets[i].stringValue();
            geoStrings.add(v);
         }
      }
      return geoStrings;
   }
*/
}
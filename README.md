Rhapsode
========
Advanced* desktop search/corpus exploration prototype

News
----
Initial release 0.3.2-BETA is now available.

Quick Start
-----------
Prerequisite:
Java >= 8 needs to be installed and callable from the command line


1) Unzip the latest [release](https://github.com/mitre/rhapsode/releases).
2) Put documents to search in the "input" directory.
2) Run 01_buildIndex.(bat|sh).
3) Once that finishes, close out command window and run 02_startRhapsodeDesktop.(bat|sh).
4) Open a browser and navigate to http://localhost:8092/rhapsode/admin/collection
5) Select "collection1" and click open.
6) Click on "Search Tools".

Enjoy!

Much more work remains. :)

Background
----------
The vast majority of search -- web, site and intranet -- is focused on helping users find the most 
relevant document, the best piece of information or the best product to meet their need.  Learning
to Rank** and other machine learning methods are revolutionizing relevance ranking for 
intranet and site search.

There are other types of search, which I'll broadly categorize here as exploratory search, 
that don't appear to be well supported among some of the mainstream search tools.
In exploratory search, the goal is to make sense of what is in a document set -- while it would 
be useful for a patent examiner to find the existing patents most relevant to the one 
under consideration, s/he really does need to go through all existing patents that 
contain related and relevant terms/concepts. Legal analysts, journalists, linguists, 
literary scholars and many other analytical fields often require tools for this 
type of search, and I list several good ones below.

Another key differentiator between traditional search and exploratory search
is that exploratory search may include making sense of very long
documents.  While the three best snippets might be useful to determine if a document
is relevant, it would be really useful for explorers to be able to see every time
their search term appears even in lengthy documents -- with enough context, perhaps
they don't even need to open the document.

Another differentiator is the user's interest and capability in crafting complex
queries. In traditional search, thanks to Google, many intranet searchers don't
even want to bother with double-quotes or boolean operators.  In exploratory search,
users (or knowledge managers behind the scenes) are willing to construct some 
pretty elaborate queries.

In traditional search, the system should help the user find "the right spelling", 
because authoritative/desired sites typically spell things correctly.
In exploratory search, the user wants to find all variants, even in noisy OCR.

Goal of Rhapsode
----------------
The goal of Rhapsode and of open-sourcing Rhapsode is not to corner the market for
this type of search or even, frankly, to build a community around it.  

The goal is to demonstrate the utility of the concordance as well as the 
results matrix in the hope that these ideas
and code (?) might make it into other libraries and other tools.

As a first step, adoption into Apache Lucene/Solr and Elasticsearch would be great.

Other exploratory types of tools might also benefit from adopting some capabilities
available in Rhapsode:

* [Open Semantic Search](https://www.opensemanticsearch.org/)
* [ContentMine](http://contentmine.org/)
* [Overview Documents](https://www.overviewdocs.com/)
* [Project Blacklight](http://projectblacklight.org/)
* [ICIJ](https://github.com/ICIJ)
* [Stevedore](https://github.com/newsdev/stevedore)

and... please help me fill out this list!

Search consultants and developers, such as [Lucidworks](https://lucidworks.com/), 
[Basis Technology](https://www.basistech.com/), [Flax](http://www.flax.co.uk/) and 
[OpenSourceConnections](http://opensourceconnections.com/), might find these capabilities
 useful for _specific_ (and likely rare) clients.

E-Discovery tools including...?

In short, Rhapsode is not the solution for exploratory search, rather a prototype
to communicate the ideas for others to adopt.  Nevertheless, it can be useful on its own
as is.

Features
--------
* **SpanQueryParser** -- allows for very complex nested queries with wildcards, fuzziness and all
of the features available in Apache Lucene's SpanQuery capabilities.  Note the caveat on 
[LUCENE-7398](https://issues.apache.org/jira/browse/LUCENE-7398) below, and 
**PLEASE** help fix LUCENE-7398.  For the history, see [LUCENE-5205](https://issues.apache.org/jira/browse/LUCENE-5205),
for the jar in maven central see 
[lucene-5205](https://mvnrepository.com/artifact/org.tallison.lucene/lucene-5205) and
[solr-5410](https://mvnrepository.com/artifact/org.tallison.solr/solr-5410).
* **Basic Search** -- out of the box, Lucene search with Lucene highlighted snippets.
* **Concordance** -- shows _every_ time your search term appears.  Allows you to sort on words
before your search term (to see what's modifying your term) or words after (to see what your term is modifying).
For the history, see [LUCENE-5317](https://issues.apache.org/jira/browse/LUCENE-5317);
for the jar in maven central see 
[lucene-5317](https://mvnrepository.com/artifact/org.tallison.lucene/lucene-5317).
* **Co-Occurrence Counter** -- calculates tf*idf of terms within _x_ words of your search terms; very useful
as a low-cost, search-time term recommender***.  For the history see 
[LUCENE-5318](https://issues.apache.org/jira/browse/LUCENE-5318); included with
[lucene-5317](https://mvnrepository.com/artifact/org.tallison.lucene/lucene-5317) in maven central.
* **Target Counter** -- identify spelling variants within your corpus; use this
to experiment with slop or other fuzzy queries to craft the query for your
document set that has a good balance of precision/recall -- `mackintosh~2`
brings back the right spelling variants, but `mackintosh~3` brings back too
many false positives.
* **Stored Concepts** -- let's say you have an "apple" query `(macintosh "red delicious")`,
you can call that at search time with curly brackets, e.g. `{apple}`. You can use that 
with all the regular query operators; for example, `"{apple} peeler"~10` retrieves
documents with one of the `apple` words 
w/in 10 words of `"peeler"`. And, you can combine stored concepts so you can 
have a `"fruit"` concept `({apple} {orange})` and query  that as `{fruit}`.
* **Stored Queries** -- this is a combination of the main query and the filter query,
and it allows you to get quick counts of how many documents in your set or a new
set have hits for your queries.  Let's say you're really interested in fruit, and 
you have a new batch of documents. YOu can load the documents and see how many documents
have `apples` and how many have `oranges`.
* **Report writer** -- writes a table where each column represents a different stored
query, and each row records which documents have hits for each stored query, 
sorted by descending order of the number of matched stored queries in the document.
  This allows you to quickly focus on which documents have both `apples` and `oranges`.

Caveat
------
For too long, nested SpanQueries have been buggy.  See [LUCENE-7398](https://issues.apache.org/jira/browse/LUCENE-7398)
and please help solve that.

Documentation/References
------------------------
See an initial draft of a Users Guide under [here](https://github.com/mitre/rhapsode/tree/master/documentation).

See a preprint of "Collaborative Exploratory Search for Information Filtering and Large-Scale Information Triage", our upcoming JASIST
[article](https://www.mitre.org/publications/technical-papers/collaborative-exploratory-search-for-information-filtering-and-large)
/ [pdf](https://www.mitre.org/sites/default/files/publications/pr-16-1413-collaborative-exploratory-search-nformation-filtering-preprint.pdf).


License
-------
Basically, [Apache Software License 2.0](https://github.com/mitre/rhapsode/blob/master/LICENSE.txt).

Other
-----
A [rhapsode](https://en.wikipedia.org/wiki/Rhapsode) was a bard in ancient Greece, 
who wove together elements from tradition to tell a new(ish) story.  
Exploratory searchers weave together disparate pieces of information to carry out 
analysis and develop new insights.

Notes
-----
\* Advanced -- well, right, no fancy deep learning with blockchain 
convnets, but some tools that are useful if you're trying to do more with a collection 
of documents than finding the best one for your need.

\** Bloomberg's Learning to Rank module in Apache Solr, Doug Turnbull/OpenSource Connection's
 [Elasticsearch LTR plugin](https://github.com/o19s/elasticsearch-learning-to-rank) and 
[Lucidworks](https://lucidworks.com)' Fusion platform, among others.

\*** TF*IDF of co-occurring terms is a low-cost way of identifying important 
collocations/co-occurrences. Currently looking into integrating word2vec...who isn't? :)
# Rhapsode
Advanced* desktop search/corpus exploration prototype

##News
Initial release 0.3.2-BETA is now available.

##Quick Start
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

##Background
The vast majority of search -- web and intranet -- is focused on helping users find the most 
relevant document, the best piece of information or product to meet their need.

There's an entirely different type of search -- exploratory search -- that doesn't appear to be well supported
among the mainstream search tools because the market is far smaller.  In exploratory search,
the goal is to make sense of what is in a document set -- while it
would be useful for a patent examiner to find the existing patents most relevant to
the one under consideration, s/he really does need to go through all existing patents
and find even potentially distant matches.  Legal analysts, journalists, investigators,
linguists, literature scholars and many other analytical fields often require tools for 
this type of search, and I list several good ones below.

Another key differentiator between traditional search and exploratory search
is that exploratory search may include making sense of very long
documents.  While the three best snippets might be useful to determine if a document
is relevant, it would be really useful for explorers to be able to see every time
their search term appears even in lengthy documents -- with enoungh context, perhaps
they don't even need to open the document.

Another differentiator is the user's interest and capability in crafting complex
queries. In traditional search, thanks to Google, many intranet searchers don't
even want to bother with double-quotes or boolean operators.  In exploratory search,
users (or knowledge managers behind the scenes) are willing to construct some 
pretty elaborate queries.

##Goal of Rhapsode
The goal of Rhapsode and of open-sourcing Rhapsode is not to corner the market for
this type of search or even, frankly, to build a community around it.  
The goal is to demonstrate the utility of the concordance as well as the 
results matrix in the hope that these ideas
and potential code (via [LUCENE-5205](https://issues.apache.org/jira/browse/LUCENE-5205), 
[LUCENE-5317](https://issues.apache.org/jira/browse/LUCENE-5317) and 
[LUCENE-5318](https://issues.apache.org/jira/browse/LUCENE-5205)) might make
it into other libraries and other tools.

As a first step, adoption into Lucene/Solr and Elastic Search would be great.

Other exploratory types of tools might also benefit from adopting some capabilities
available in Rhapsode:

* [Open Semantic Search](https://www.opensemanticsearch.org/)
* [Overview Documents](https://www.overviewdocs.com/)
* [Project Blacklight](http://projectblacklight.org/)
* [ICIJ](https://github.com/ICIJ)
* [Stevedore](https://github.com/newsdev/stevedore)

and... please help me fill out this list!

Search consultants and developers, such as [Lucidworks](https://lucidworks.com/), 
[Basis Technology](https://www.basistech.com/), [Flax](http://www.flax.co.uk/) and 
[OpenSourceConnections](http://opensourceconnections.com/), might find these capabilities
 useful for _specific_ (and likely rare) clients.

 
##Documentation/References
See an initial draft of a Users Guide under [here](https://github.com/mitre/rhapsode/tree/master/documentation).

See a preprint of "Collaborative Exploratory Search for Information Filtering and Large-Scale Information Triage", our upcoming JASIST
[article](https://www.mitre.org/publications/technical-papers/collaborative-exploratory-search-for-information-filtering-and-large)
/ [pdf](https://www.mitre.org/sites/default/files/publications/pr-16-1413-collaborative-exploratory-search-nformation-filtering-preprint.pdf).

## Notes
\* Advanced -- well, right, no fancy clustering, word2vec or deep blockchained learning, but some tools that are useful if you're trying to do more with a 
bunch of documents than finding the best one for your need.

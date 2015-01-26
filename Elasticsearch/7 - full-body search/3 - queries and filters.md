## 查询和过滤器 ##

Although we refer to the query DSL, in reality there are two DSLs: the query DSL and the filter DSL. Query clauses and filter clauses are similar in nature, but have slightly different purposes.

A filter asks a yes|no question of every document and is used for fields that contain exact values:

- Is the created date in the range 2013 - 2014?
- Does the status field contain the term published?
- Is the lat_lon field within 10km of a specified point?

A query is similar to a filter, but also asks the question: How well does this document match?

A typical use for a query is to find documents

- Best matching the words full text search
- Containing the word run, but maybe also matching runs, running, jog, or sprint
- Containing the words quick, brown, and fox—the closer together they are, the more relevant the document
- Tagged with lucene, search, or java—the more tags, the more relevant the document
- 
A query calculates how relevant each document is to the query, and assigns it a relevance _score, which is later used to sort matching documents by relevance. This concept of relevance is well suited to full-text search, where there is seldom a completely “correct” answer.

### 性能差异 ###

The output from most filter clauses—a simple list of the documents that match the filter—is quick to calculate and easy to cache in memory, using only 1 bit per document. These cached filters can be reused efficiently for subsequent requests.

Queries have to not only find matching documents, but also calculate how relevant each document is, which typically makes queries heavier than filters. Also, query results are not cachable.

Thanks to the inverted index, a simple query that matches just a few documents may perform as well or better than a cached filter that spans millions of documents. In general, however, a cached filter will outperform a query, and will do so consistently.

The goal of filters is to reduce the number of documents that have to be examined by the query.

### 如何选择 ###

As a general rule, use query clauses for full-text search or for any condition that should affect the relevance score, and use filter clauses for everything else.
# full-body搜索 #

Search lite—a [query-string search](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/search-lite.html)—is useful for ad hoc queries from the command line. To harness the full power of search, however, you should use the request body search API, so called because most parameters are passed in the HTTP request body instead of in the query string.

Request body search—henceforth known as search—not only handles the query itself, but also allows you to return highlighted snippets from your results, aggregate analytics across all results or subsets of results, and return did-you-mean suggestions, which will help guide your users to the best results quickly.
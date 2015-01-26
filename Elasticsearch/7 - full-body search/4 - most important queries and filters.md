## 最重要的查询和过滤器 ##

While Elasticsearch comes with many queries and filters, you will use just a few frequently. We discuss them in much greater detail in [Search in Depth](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/search-in-depth.html) but next we give you a quick introduction to the most important queries and filters.

### term过滤器 ###

The term filter is used to filter by exact values, be they numbers, dates, Booleans, or not_analyzed exact-value string fields:

```json
{ "term": { "age":    26           }}
{ "term": { "date":   "2014-09-01" }}
{ "term": { "public": true         }}
{ "term": { "tag":    "full_text"  }}
```

### terms过滤器 ###

The terms filter is the same as the term filter, but allows you to specify multiple values to match. If the field contains any of the specified values, the document matches:

```json
{ "terms": { "tag": [ "search", "full_text", "nosql" ] }}
```

### range过滤器 ###

The range filter allows you to find numbers or dates that fall into a specified range:

```json
{
    "range": {
        "age": {
            "gte":  20,
            "lt":   30
        }
    }
}
```

The operators that it accepts are as follows:

- gt
	Greater than
- gte
	Greater than or equal to
- lt
	Less than
- lte
	Less than or equal to

### exists和missing过滤器 ###

The exists and missing filters are used to find documents in which the specified field either has one or more values (exists) or doesn’t have any values (missing). It is similar in nature to IS_NULL (missing) and NOT IS_NULL (exists)in SQL:

```json
{
    "exists":   {
        "field":    "title"
    }
}
```

These filters are frequently used to apply a condition only if a field is present, and to apply a different condition if it is missing.

### bool过滤器 ###

The bool filter is used to combine multiple filter clauses using Boolean logic. It accepts three parameters:

- must
	These clauses must match, like and.
- must_not
	These clauses must not match, like not.
- should
	At least one of these clauses must match, like or.

Each of these parameters can accept a single filter clause or an array of filter clauses:

```json
{
    "bool": {
        "must":     { "term": { "folder": "inbox" }},
        "must_not": { "term": { "tag":    "spam"  }},
        "should": [
                    { "term": { "starred": true   }},
                    { "term": { "unread":  true   }}
        ]
    }
}
```

### match_all查询 ###

The match_all query simply matches all documents. It is the default query that is used if no query has been specified:

```json
{ "match_all": {}}
```

This query is frequently used in combination with a filter—for instance, to retrieve all emails in the inbox folder. All documents are considered to be equally relevant, so they all receive a neutral _score of 1.

### match查询 ###

The match query should be the standard query that you reach for whenever you want to query for a full-text or exact value in almost any field.

If you run a match query against a full-text field, it will analyze the query string by using the correct analyzer for that field before executing the search:

```json
{ "match": { "tweet": "About Search" }}
```

If you use it on a field containing an exact value, such as a number, a date, a Boolean, or a not_analyzed string field, then it will search for that exact value:

```json
{ "match": { "age":    26           }}
{ "match": { "date":   "2014-09-01" }}
{ "match": { "public": true         }}
{ "match": { "tag":    "full_text"  }}
```

> **TIP**
> 
> For exact-value searches, you probably want to use a filter instead of a query, as a filter will be cached.

Unlike the query-string search that we showed in [Search Lite](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/search-lite.html), the match query does not use a query syntax like +user_id:2 +tweet:search. It just looks for the words that are specified. This means that it is safe to expose to your users via a search field; you control what fields they can query, and it is not prone to throwing syntax errors.

### multi_match查询 ###

The multi_match query allows to run the same match query on multiple fields:

```json
{
    "multi_match": {
        "query":    "full text search",
        "fields":   [ "title", "body" ]
    }
}
```

### bool查询 ###

The bool query, like the bool filter, is used to combine multiple query clauses. However, there are some differences. Remember that while filters give binary yes/no answers, queries calculate a relevance score instead. The bool query combines the _score from each must or should clause that matches. This query accepts the following parameters:

- must
	Clauses that must match for the document to be included.
- must_not
	Clauses that must not match for the document to be included.
- should
	If these clauses match, they increase the _score; otherwise, they have no effect. They are simply used to refine the relevance score for each document.

The following query finds documents whose title field matches the query string how to make millions and that are not marked as spam. If any documents are starred or are from 2014 onward, they will rank higher than they would have otherwise. Documents that match both conditions will rank even higher:

```json
{
    "bool": {
        "must":     { "match": { "title": "how to make millions" }},
        "must_not": { "match": { "tag":   "spam" }},
        "should": [
            { "match": { "tag": "starred" }},
            { "range": { "date": { "gte": "2014-01-01" }}}
        ]
    }
}
```

> **TIP**
> 
> If there are no must clauses, at least one should clause has to match. However, if there is at least one must clause, no should clauses are required to match.


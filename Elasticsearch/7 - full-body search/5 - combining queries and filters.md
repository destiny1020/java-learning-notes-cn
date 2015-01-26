## 结合查询和过滤器 ##

Queries can be used in query context, and filters can be used in filter context. Throughout the Elasticsearch API, you will see parameters with query or filter in the name. These expect a single argument containing either a single query or filter clause respectively. In other words, they establish the outer context as query context or filter context.

Compound query clauses can wrap other query clauses, and compound filter clauses can wrap other filter clauses. However, it is often useful to apply a filter to a query or, less frequently, to use a full-text query as a filter.

To do this, there are dedicated query clauses that wrap filter clauses, and vice versa, thus allowing us to switch from one context to another. It is important to choose the correct combination of query and filter clauses to achieve your goal in the most efficient way.

### 过滤一个查询 ###

Let’s say we have this query:

```json
{ "match": { "email": "business opportunity" }}
```

We want to combine it with the following term filter, which will match only documents that are in our inbox:

```json
{ "term": { "folder": "inbox" }}
```

The search API accepts only a single query parameter, so we need to wrap the query and the filter in another query, called the filtered query:

```json
{
    "filtered": {
        "query":  { "match": { "email": "business opportunity" }},
        "filter": { "term":  { "folder": "inbox" }}
    }
}
```

We can now pass this query to the query parameter of the search API:

```json
GET /_search
{
    "query": {
        "filtered": {
            "query":  { "match": { "email": "business opportunity" }},
            "filter": { "term": { "folder": "inbox" }}
        }
    }
}
```

### 仅使用过滤器 ###

While in query context, if you need to use a filter without a query (for instance, to match all emails in the inbox), you can just omit the query:

```json
GET /_search
{
    "query": {
        "filtered": {
            "filter":   { "term": { "folder": "inbox" }}
        }
    }
}
```

If a query is not specified it defaults to using the match_all query, so the preceding query is equivalent to the following:

```json
GET /_search
{
    "query": {
        "filtered": {
            "query":    { "match_all": {}},
            "filter":   { "term": { "folder": "inbox" }}
        }
    }
}
```

### 查询作为过滤器 ###

Occasionally, you will want to use a query while you are in filter context. This can be achieved with the query filter, which just wraps a query. The following example shows one way we could exclude emails that look like spam:

```json
GET /_search
{
    "query": {
        "filtered": {
            "filter":   {
                "bool": {
                    "must":     { "term":  { "folder": "inbox" }},
                    "must_not": {
                        "query": { 
                            "match": { "email": "urgent business proposal" }
                        }
                    }
                }
            }
        }
    }
}
```

Note the query filter, which is allowing us to use the match query inside a bool filter.

> **NOTE**
> 
> You seldom need to use a query as a filter, but we have included it for completeness' sake. The only time you may need it is when you need to use full-text matching while in filter context.
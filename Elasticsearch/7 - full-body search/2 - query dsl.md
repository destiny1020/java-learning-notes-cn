## 查询DSL ##

The query DSL is a flexible, expressive search language that Elasticsearch uses to expose most of the power of Lucene through a simple JSON interface. It is what you should be using to write your queries in production. It makes your queries more flexible, more precise, easier to read, and easier to debug.

To use the Query DSL, pass a query in the query parameter:

```json
GET /_search
{
    "query": YOUR_QUERY_HERE
}
```

The empty search—{}—is functionally equivalent to using the match_all query clause, which, as the name suggests, matches all documents:

```json
GET /_search
{
    "query": {
        "match_all": {}
    }
}
```

### 一条查询语句的结构 ###

A query clause typically has this structure:

```json
{
    QUERY_NAME: {
        ARGUMENT: VALUE,
        ARGUMENT: VALUE,...
    }
}
```

If it references one particular field, it has this structure:

```json
{
    QUERY_NAME: {
        FIELD_NAME: {
            ARGUMENT: VALUE,
            ARGUMENT: VALUE,...
        }
    }
}
```

For instance, you can use a match query clause to find tweets that mention elasticsearch in the tweet field:

```json
{
    "match": {
        "tweet": "elasticsearch"
    }
}
```

The full search request would look like this:

```json
GET /_search
{
    "query": {
        "match": {
            "tweet": "elasticsearch"
        }
    }
}
```

### 合并多条语句 ###

Query clauses are simple building blocks that can be combined with each other to create complex queries. Clauses can be as follows:

- Leaf clauses (like the match clause) that are used to compare a field (or fields) to a query string.
- Compound clauses that are used to combine other query clauses. For instance, a bool clause allows you to combine other clauses that either must match, must_not match, or should match if possible:

```json
{
    "bool": {
        "must":     { "match": { "tweet": "elasticsearch" }},
        "must_not": { "match": { "name":  "mary" }},
        "should":   { "match": { "tweet": "full text" }}
    }
}
```

It is important to note that a compound clause can combine any other query clauses, including other compound clauses. This means that compound clauses can be nested within each other, allowing the expression of very complex logic.

As an example, the following query looks for emails that contain business opportunity and should either be starred, or be both in the Inbox and not marked as spam:

```json
{
    "bool": {
        "must": { "match":      { "email": "business opportunity" }},
        "should": [
             { "match":         { "starred": true }},
             { "bool": {
                   "must":      { "folder": "inbox" }},
                   "must_not":  { "spam": true }}
             }}
        ],
        "minimum_should_match": 1
    }
}
```

Don't worry about the details of this example yet; we will explain in full later. The important thing to take away is that a compound query clause can combine multiple clauses—both leaf clauses and other compound clauses—into a single query.
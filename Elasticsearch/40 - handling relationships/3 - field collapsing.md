## 字段折叠(Field Collapsing)

A common requirement is the need to present search results grouped by a particular field. We might want to return the most relevant blog posts grouped by the user’s name. Grouping by name implies the need for a terms aggregation. To be able to group on the user’s whole name, the name field should be available in its original not_analyzed form, as explained in [Aggregations and Analysis](https://www.elastic.co/guide/en/elasticsearch/guide/current/aggregations-and-analysis.html):
一个常见的需求是通过对某个特定的字段分组来展现搜索结果。我们或许希望通过对用户名分组来返回最相关的博文。对用户名分组意味着我们需要使用到terms聚合。为了对用户的全名进行分组，name字段需要有not_analyzed的原始值，如[聚合和分析](https://www.elastic.co/guide/en/elasticsearch/guide/current/aggregations-and-analysis.html)中解释的那样。

```json
PUT /my_index/_mapping/blogpost
{
  "properties": {
    "user": {
      "properties": {
        "name": {   (1)
          "type": "string",
          "fields": {
            "raw": {   (2)
              "type":  "string",
              "index": "not_analyzed"
            }
          }
        }
      }
    }
  }
}
```

(1) The user.name field will be used for full-text search. user.name字段用来支持全文搜索。
(2) The user.name.raw field will be used for grouping with the terms aggregation. user.name.raw字段用来支持terms聚合来完成分组。

Then add some data:
然后添加一些数据：

```json
PUT /my_index/user/1
{
  "name": "John Smith",
  "email": "john@smith.com",
  "dob": "1970/10/24"
}

PUT /my_index/blogpost/2
{
  "title": "Relationships",
  "body": "It's complicated...",
  "user": {
    "id": 1,
    "name": "John Smith"
  }
}

PUT /my_index/user/3
{
  "name": "Alice John",
  "email": "alice@john.com",
  "dob": "1979/01/04"
}

PUT /my_index/blogpost/4
{
  "title": "Relationships are cool",
  "body": "It's not complicated at all...",
  "user": {
    "id": 3,
    "name": "Alice John"
  }
}
```

Now we can run a query looking for blog posts about relationships, by users called John, and group the results by user, thanks to the [top_hits aggregation](http://bit.ly/1CrlWFQ):
现在我们可以运行一个查询来获取关于relationships的博文，通过用户名为John对结果进行分组。这都要感谢[top_hits聚合](http://bit.ly/1CrlWFQ):

```json
GET /my_index/blogpost/_search?search_type=count (1)
{
  "query": { (2)
    "bool": {
      "must": [
        { "match": { "title":     "relationships" }},
        { "match": { "user.name": "John"          }}
      ]
    }
  },
  "aggs": {
    "users": {
      "terms": {
        "field":   "user.name.raw",      (3) 
        "order": { "top_score": "desc" } (4)
      },
      "aggs": {
        "top_score": { "max":      { "script":  "_score"           }},  (5)
        "blogposts": { "top_hits": { "_source": "title", "size": 5 }}   (6)
      }
    }
  }
}
```

(1) The blog posts that we are interested in are returned under the blogposts aggregation, so we can disable the usual search hits by setting the search_type=count. 我们感兴趣的博文在blogposts聚合中被返回了，因此我们可以通过设置`search_type=count`来禁用通常的搜索结果。

(2) The query returns blog posts about relationships by users named John. 该查询返回用户名为John，title匹配relationships的博文。

(3) The terms aggregation creates a bucket for each user.name.raw value. terms聚合为每个user.name.raw值创建一个桶。

(4)(5) The top_score aggregation orders the terms in the users aggregation by the top-scoring document in each bucket. 在users聚合中，使用top_score聚合通过每个桶中拥有最高分值的文档进行排序。

(6) The top_hits aggregation returns just the title field of the five most relevant blog posts for each user. top_hits聚合只返回每个用户的5篇最相关博文的title字段。

The abbreviated response is shown here:
部分响应如下所示：

```json
...
"hits": {
  "total":     2,
  "max_score": 0,
  "hits":      []    (1)
},
"aggregations": {
  "users": {
     "buckets": [
        {
           "key":       "John Smith",    (2)
           "doc_count": 1,
           "blogposts": {
              "hits": {    (3)
                 "total":     1,
                 "max_score": 0.35258877,
                 "hits": [
                    {
                       "_index": "my_index",
                       "_type":  "blogpost",
                       "_id":    "2",
                       "_score": 0.35258877,
                       "_source": {
                          "title": "Relationships"
                       }
                    }
                 ]
              }
           },
           "top_score": {    (4)
              "value": 0.3525887727737427
           }
        },
...
```

(1) The hits array is empty because we set search_type=count. hits数组为空因为我们设置了`search_type=count`。

(2) There is a bucket for each user who appeared in the top results. 针对每个用户都有一个对应的桶。

(3) Under each user bucket there is a blogposts.hits array containing the top results for that user. 在每个用户的桶中，有一个blogposts.hits数组，它包含了该用户的相关度最高的搜索结果。

(4) The user buckets are sorted by the user’s most relevant blog post. 用户桶通过用户最相关的博文进行排序。

Using the top_hits aggregation is the equivalent of running a query to return the names of the users with the most relevant blog posts, and then running the same query for each user, to get their best blog posts. But it is much more efficient.
使用top_hits聚合等效于运行获取最相关博文及其对应用户的查询，然后针对每个用户运行同样的查询，来得到每个用户最相关的博文。可见使用top_hits更高效。

The top hits returned in each bucket are the result of running a light mini-query based on the original main query. The mini-query supports the usual features that you would expect from search such as highlighting and pagination.
每个桶中返回的top hits是通过运行一个基于原始主查询的迷你查询而来。该迷你查询也同样支持高亮(Highlighting)以及分页(Pagination)。
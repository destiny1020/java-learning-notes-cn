## 通过查询结构调整相关度 ##

ES提供的查询DSL是相当灵活的。你可以通过将单独的查询子句在查询层次中上下移动来让它更重要/更不重要。比如，下面的查询：

> quick OR brown OR red OR fox

我们可以使用一个bool查询，对所有词条一视同仁：

```json
GET /_search
{
  "query": {
    "bool": {
      "should": [
        { "term": { "text": "quick" }},
        { "term": { "text": "brown" }},
        { "term": { "text": "red"   }},
        { "term": { "text": "fox"   }}
      ]
    }
  }
}
```

但是这个查询会给一份含有quick，red及brown的文档和一份含有quick，red及fox的文档完全相同的分数，然而在[合并查询(Combining Queries)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/bool-query.html)中，我们知道bool查询不仅能够决定一份文档是否匹配，同时也能够知道该文档的匹配程度。

下面是更好的查询方式：

```json
GET /_search
{
  "query": {
    "bool": {
      "should": [
        { "term": { "text": "quick" }},
        { "term": { "text": "fox"   }},
        {
          "bool": {
            "should": [
              { "term": { "text": "brown" }},
              { "term": { "text": "red"   }}
            ]
          }
        }
      ]
    }
  }
}
```

现在，red和brown会在同一层次上相互竞争，而quick，fox以及red或者brown则是在顶层上相互对象的词条。

我们已经讨论了match，multi_match，term，book以及dis_max是如何对相关度分值进行操作的。在本章的剩余部分，我们会讨论和相关度分值有关的另外三种查询：boosting查询，constant_score查询以及function_score查询。


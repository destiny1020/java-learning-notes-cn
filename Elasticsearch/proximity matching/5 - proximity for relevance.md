## 使用邻近度来提高相关度 ##

尽管邻近度查询(Proximity Query)管用，但是所有的词条都必须出现在文档的这一要求显的过于严格了。这个问题和我们在[全文搜索(Full-Text Search)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/full-text-search.html)一章的[精度控制(Controlling Precision)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/match-multi-word.html#match-precision)一节中讨论过的类似：如果7个词条中有6个匹配了，那么该文档也许对于用户而言已经足够相关了，但是match_phrase查询会将它排除在外。

相比将邻近度匹配作为一个绝对的要求，我们可以将它当做一个信号(Signal) - 作为众多潜在匹配中的一员，会对每份文档的最终分值作出贡献(参考[多数字段(Most Fields)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/most-fields.html))。

我们需要将多个查询的分值累加这一事实表示我们应该使用bool查询将它们合并。

我们可以使用一个简单的match查询作为一个must子句。该查询用于决定哪些文档需要被包含到结果集中。可以通过minimum_should_match参数来去除长尾(Long tail)。然后我们以should子句的形式添加更多特定查询。每个匹配了should子句的文档都会增加其相关度。

```json
GET /my_index/my_type/_search
{
  "query": {
    "bool": {
      "must": {
        "match": { 
          "title": {
            "query":                "quick brown fox",
            "minimum_should_match": "30%"
          }
        }
      },
      "should": {
        "match_phrase": { 
          "title": {
            "query": "quick brown fox",
            "slop":  50
          }
        }
      }
    }
  }
}
```

毫无疑问我们可以向should子句中添加其它的查询，每个查询都用来增加特定类型的相关度。






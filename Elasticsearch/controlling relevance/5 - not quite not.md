## 不完全的不(Not Quite Not) ##

在互联网上搜索"苹果"也许会返回关于公司，水果或者各种食谱的结果。我们可以通过排除pie，tart，crumble和tree这类单词，结合bool查询中的must_not子句，将结果范围缩小到只剩苹果公司：

```json
GET /_search
{
  "query": {
    "bool": {
      "must": {
        "match": {
          "text": "apple"
        }
      },
      "must_not": {
        "match": {
          "text": "pie tart fruit crumble tree"
        }
      }
    }
  }
}
```

但是有谁敢说排除了tree或者crumble不会将一份原本和苹果公司非常相关的文档也排除在外了呢？有时，must_not过于严格了。

### boosting查询 ###

[boosting查询](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/not-quite-not.html#boosting-query)能够解决这个问题。它允许我们仍然将水果或者食谱相关的文档考虑在内，只是会降低它们的相关度 - 将它们的排序更靠后：

```json
GET /_search
{
  "query": {
    "boosting": {
      "positive": {
        "match": {
          "text": "apple"
        }
      },
      "negative": {
        "match": {
          "text": "pie tart fruit crumble tree"
        }
      },
      "negative_boost": 0.5
    }
  }
}
```

它接受一个positive查询和一个negative查询。只有匹配了positive查询的文档才会被包含到结果集中，但是同时匹配了negative查询的文档会被降低其相关度，通过将文档原本的_score和negative_boost参数进行相乘来得到新的_score。

因此，negative_boost参数必须小于1.0。在上面的例子中，任何包含了指定负面词条的文档的_score都会是其原本_score的一半。
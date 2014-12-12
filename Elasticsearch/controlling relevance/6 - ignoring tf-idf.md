## 忽略TD/IDF ##

有时我们不需要TD/IDF。我们想知道的只是一个特定的单词是否出现在了字段中。比如我们正在搜索度假酒店，希望它拥有的卖点越多越好：

- WiFi
- 花园(Garden)
- 泳池(Pool)

而关于度假酒店的文档类似下面这样：

```json
{ "description": "A delightful four-bedroomed house with ... " }
```

可以使用一个简单的match查询：

```json
GET /_search
{
  "query": {
    "match": {
      "description": "wifi garden pool"
    }
  }
}
```

然而，我们需要的并不是真正的全文搜索。此时TF/IDF只会碍手碍脚。我们不在意wifi是否是一个常见的词条，也不在意它在文档中出现的是否频繁。我们在意的只是它是否出现了。实际上，我们只是想通过卖点来对这些度假酒店进行排序 - 越多越好。如果拥有一个卖点，那么它的分值就是1，如果没有它的分值就是0。

### constant_score查询 ###

首先介绍[constant_score查询](http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl-constant-score-query.html)。该查询能够包含一个查询或者一个过滤器，所有匹配文档的相关度分值都为1，不考虑TF/IDF：

```json
GET /_search
{
  "query": {
    "bool": {
      "should": [
        { "constant_score": {
          "query": { "match": { "description": "wifi" }}
        }},
        { "constant_score": {
          "query": { "match": { "description": "garden" }}
        }},
        { "constant_score": {
          "query": { "match": { "description": "pool" }}
        }}
      ]
    }
  }
}
```

大概并不是所有的卖点都同等重要 - 其中的某些更有价值。如果最看中的卖点是泳池，那么我们可以对它进行相应提升：

```json
GET /_search
{
  "query": {
    "bool": {
      "should": [
        { "constant_score": {
          "query": { "match": { "description": "wifi" }}
        }},
        { "constant_score": {
          "query": { "match": { "description": "garden" }}
        }},
        { "constant_score": {
          "boost":   2 
          "query": { "match": { "description": "pool" }}
        }}
      ]
    }
  }
}
```

> **NOTE**
> 
> 每个结果的最终分值并不是将所有匹配子句的分值累加而得到。[Coordination因子](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/practical-scoring-function.html#coord)和[查询归约因子(Query Normalization Factor)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/practical-scoring-function.html#query-norm)仍然会被考虑在内。

我们可以在度假酒店的文档中添加一个not_analyzed类型的features字段：

```json
{ "features": [ "wifi", "pool", "garden" ] }
```

默认情况下，一个not_analyzed字段的[字段长度归约(Field-length Norm)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/scoring-theory.html#field-norm)是被禁用的，同时其index_options也会被设置为docs，从而禁用[词条频度(Term Frequencies)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/scoring-theory.html#tf)，但是问题还是存在：每个词条的[倒排文档频度(Inverse Document Frequency)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/scoring-theory.html#idf)仍然会被考虑。

仍然使用constant_score查询：

```json
GET /_search
{
  "query": {
    "bool": {
      "should": [
        { "constant_score": {
          "query": { "match": { "features": "wifi" }}
        }},
        { "constant_score": {
          "query": { "match": { "features": "garden" }}
        }},
        { "constant_score": {
          "boost":   2
          "query": { "match": { "features": "pool" }}
        }}
      ]
    }
  }
}
```

实际上，每个卖点都应该被视为一个过滤器。度假酒店要么有该卖点，要么没有 - 使用过滤器似乎是更自然的选择。并且如果我们使用了过滤器，还可以得益于过滤器缓存这一功能。

不使用过滤器的根源在于：过滤器不会计算相关度分值。我们需要的是一座用来连接过滤器和查询的桥梁。而function_score查询就能够做到这一点，并且它也提供了更多的功能。
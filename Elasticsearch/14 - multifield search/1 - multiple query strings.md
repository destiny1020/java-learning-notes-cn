## 多个查询字符串(Multiple Query Strings) ##

处理字段查询最简单的方法是将搜索词条对应到特定的字段上。如果我们知道*战争与和平*是标题，而Leo Tolstoy是作者，那么我们可以简单地将每个条件当做一个`match`子句，然后通过[`bool`查询](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/bool-query.html)将它们合并：

```json
GET /_search
{
  "query": {
    "bool": {
      "should": [
        { "match": { "title":  "War and Peace" }},
        { "match": { "author": "Leo Tolstoy"   }}
      ]
    }
  }
}
```

`bool`查询采用了一种"匹配越多越好(More-matches-is-better)"的方法，因此每个`match`子句的分值会被累加来得到文档最终的`_score`。匹配两个子句的文档相比那些只匹配一个子句的文档的分值会高一些。

当然，你并不是只能使用`match`子句：`bool`查询可以包含任何其他类型的查询，包括其它的`bool`查询。我们可以添加一个子句来指定我们希望的译者：

```json
GET /_search
{
  "query": {
    "bool": {
      "should": [
        { "match": { "title":  "War and Peace" }},
        { "match": { "author": "Leo Tolstoy"   }},
        { "bool":  {
          "should": [
            { "match": { "translator": "Constance Garnett" }},
            { "match": { "translator": "Louise Maude"      }}
          ]
        }}
      ]
    }
  }
}
```

我们为什么将译者的查询子句放在一个单独的`bool`查询中？所有的4个`match`查询都是`should`子句，那么为何不将译者的查询子句和标题及作者的查询子句放在同一层次上呢？

答案在于分值是如何计算的。`bool`查询会运行每个`match`查询，将它们的分值相加，然后乘以匹配的查询子句的数量，最后除以所有查询子句的数量。相同层次的每个子句都拥有相同的权重。在上述查询中，`bool`查询中包含的译者查询子句只占了总分值的三分之一。如果我们将译者查询子句放到和标题及作者相同的层次上，就会减少标题和作者子句的权重，让它们各自只占四分之一。

### 设置子句优先级 ###

上述查询中每个子句占有三分之一的权重也许并不是我们需要的。相比译者字段，我们可能对标题和作者字段更有兴趣。我们对查询进行调整来让标题和作者相对更重要。

在所有可用措施中，我们可以采用的最简单的方法是`boost`参数。为了增加`title`和`author`字段的权重，我们可以给它们一个大于`1`的`boost`值：

```json
GET /_search
{
  "query": {
    "bool": {
      "should": [
        { "match": { 
            "title":  {
              "query": "War and Peace",
              "boost": 2
        }}},
        { "match": { 
            "author":  {
              "query": "Leo Tolstoy",
              "boost": 2
        }}},
        { "bool":  { 
            "should": [
              { "match": { "translator": "Constance Garnett" }},
              { "match": { "translator": "Louise Maude"      }}
            ]
        }}
      ]
    }
  }
}
```
以上的`title`和k字段的`boost`值为`2`。
嵌套的`bool`查询自居的默认`boost`值为k。

通过试错(Trial and Error)的方式可以确定"最佳"的`boost`值：设置一个`boost`值，执行测试查询，重复这个过程。一个合理`boost`值的范围在`1`和`10`之间，也可能是`15`。比它更高的值的影响不会起到很大的作用，因为分值会被[规范化(Normalized)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/_boosting_query_clauses.html#boost-normalization)。

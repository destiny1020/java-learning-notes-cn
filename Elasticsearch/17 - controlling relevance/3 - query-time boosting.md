## 查询期间提升(Query-time Boosting) ##

在[调整查询子句优先级(Prioritizing Clauses)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/multi-query-strings.html#prioritising-clauses)一节中，我们已经介绍过如何在搜索期间使用boost参数为一个查询子句增加权重。比如：

```json
GET /_search
{
  "query": {
    "bool": {
      "should": [
        {
          "match": {
            "title": {
              "query": "quick brown fox",
              "boost": 2 
            }
          }
        },
        {
          "match": { 
            "content": "quick brown fox"
          }
        }
      ]
    }
  }
}
```

查询期间提升是用来调优相关度的主要工具。任何类型的查询都接受boost参数。将boost设为2并不是简单地将最终的_score加倍；确切的提升值会经过规范化以及一些内部优化得到。但是，它也意味着一个提升值为2的子句比一个提升值为1的子句要重要两倍。

实际上，没有任何公式能够决定对某个特定的查询子句，"正确的"提升值应该是多少。它是通过尝试来得到的。记住boost仅仅是相关度分值中的一个因素；它需要和其它因素竞争。比如在上面的例子中，title字段相对于content字段，大概已经有一个"自然的"提升了，该提升来自[字段长度归约(Field-length Norm)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/scoring-theory.html#field-norm)(因为标题通常会比相关内容要短一些)，因此不要因为你认为某个字段应该被提升而盲目地对它进行提升。适用一个提升值然后检查得到的结果，再进行修正。

### 提升索引(Boosting an Index) ###

当在多个索引中搜索时，你可以通过indices_boost参数对整个索引进行提升。在下面的例子中，会给予最近索引中的文档更多的权重：

```json
GET /docs_2014_*/_search 
{
  "indices_boost": { 
    "docs_2014_10": 3,
    "docs_2014_09": 2
  },
  "query": {
    "match": {
      "text": "quick brown fox"
    }
  }
}
```

该多索引搜索(Multi-index Search)会查询所有以docs_2014_开头的索引。
索引docs_2014_10中的文档的提升值为3，索引docs_2014_09中的文档的提升值为2，其它索引中的文档的提升值为默认值1。

### t.getBoost() ###

这些提升值在[Lucene的Practical Scoring Function](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/practical-scoring-function.html)中通过t.getBoost()元素表达。提升并不是其在查询DSL出现的地方被适用的。相反，任何的提升值都会被合并然后传递到每个词条上。t.getBoost()方法返回的是适用于词条本身上的提升值，或者是适用于上层查询的提升值。

> **TIP**
> 
> 实际上，阅读[解释API](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/relevance-intro.html#explain)的输出本身比上述的说明更复杂。你在解释中根本看不到boost值或者t.getBoost()。提升被融合到了适用于特定词条上的[queryNorm](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/practical-scoring-function.html#query-norm)中。尽管我们说过queryNorm对任何词条都是相同的，但是对于提升过的词条而言，queryNorm会更高一些。






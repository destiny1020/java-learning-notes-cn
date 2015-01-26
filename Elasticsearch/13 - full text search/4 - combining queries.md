## 合并查询(Combining Queries) ##

在[合并过滤器](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/combining-filters.html)中我们讨论了使用`bool`过滤器来合并多个过滤器以实现`and`，`or`和`not`逻辑。`bool`查询也做了类似的事，但有一个显著的不同。

过滤器做出一个二元的决定：这份文档是否应该被包含在结果列表中？而查询，则更加微妙。它们不仅要决定是否包含一份文档，还需要决定这份文档有多相关。

和过滤器类似，`bool`查询通过`must`，`must_not`以及`should`参数来接受多个查询。比如：


```json
GET /my_index/my_type/_search
{
  "query": {
    "bool": {
      "must":     { "match": { "title": "quick" }},
      "must_not": { "match": { "title": "lazy"  }},
      "should": [
                  { "match": { "title": "brown" }},
                  { "match": { "title": "dog"   }}
      ]
    }
  }
}
```

`title`字段中含有词条`quick`，且不含有词条`lazy`的任何文档都会被作为结果返回。目前为止，它的工作方式和`bool`过滤器十分相似。

差别来自于两个`should`语句，它表达了这种意思：一份文档不被要求需要含有词条`brown`或者`dog`，但是如果它含有了，那么它的相关度应该更高。

```json
{
  "hits": [
     {
        "_id":      "3",
        "_score":   0.70134366, 
        "_source": {
           "title": "The quick brown fox jumps over the quick dog"
        }
     },
     {
        "_id":      "1",
        "_score":   0.3312608,
        "_source": {
           "title": "The quick brown fox"
        }
     }
  ]
}
```
文档3的分值更高因为它包含了`brown`以及`dog`。

### 分值计算(Score Calculation) ###

`bool`查询通过将匹配的`must`和`should`语句的`_score`相加，然后除以`must`和`should`语句的总数来得到相关度分值`_score`。

`must_not`语句不会影响分值；它们唯一的目的是将不需要的文档排除在外。

### 控制精度(Controlling Precision) ###

所有的`must`语句都需要匹配，而所有的`must_not`语句都不能匹配，但是`should`语句需要匹配多少个呢？默认情况下，`should`语句一个都不要求匹配，只有一个特例：如果查询中没有`must`语句，那么至少要匹配一个`should`语句。

正如我们可以[控制`match`查询的精度](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/match-multi-word.html#match-precision)，我们也能够通过`minimum_should_match`参数来控制`should`语句需要匹配的数量，该参数可以是一个绝对数值或者一个百分比：

```json
GET /my_index/my_type/_search
{
  "query": {
    "bool": {
      "should": [
        { "match": { "title": "brown" }},
        { "match": { "title": "fox"   }},
        { "match": { "title": "dog"   }}
      ],
      "minimum_should_match": 2 
    }
  }
}
```

以上查询的而结果仅包含以下文档：

`title`字段包含：
`"brown" AND "fox"` 或者 `"brown" AND "dog"` 或者 `"fox" AND "dog"`

如果一份文档含有所有三个词条，那么它会被认为更相关。



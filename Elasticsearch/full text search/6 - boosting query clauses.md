## 提升查询子句(Boosting Query Clause) ##

当然，`bool`查询并不是只能合并简单的单词(One-word)`match`查询。它能够合并任何其它的查询，包括其它的`bool`查询。它通常被用来通过合并数个单独的查询的分值来调优每份文档的相关度`_score`。

假设我们需要搜索和"full-text search"相关的文档，但是我们想要给予那些提到了"Elasticsearch"或者"Lucene"的文档更多权重。更多权重的意思是，对于提到了"Elasticsearch"或者"Lucene"的文档，它们的相关度`_score`会更高，即它们会出现在结果列表的前面。

一个简单的bool查询能够让我们表达较为复杂的逻辑：

```json
GET /_search
{
    "query": {
        "bool": {
            "must": {
                "match": {
                    "content": { 
                        "query":    "full text search",
                        "operator": "and"
                    }
                }
            },
            "should": [ 
                { "match": { "content": "Elasticsearch" }},
                { "match": { "content": "Lucene"        }}
            ]
        }
    }
}
```

1. `content`字段必须含有`full`，`text`和`search`这三个词条
2. 如果`content`字段也含有了词条`Elasticsearch`或者`Lucene`，那么该文档会有一个较高的`_score`

`should`查询子句的匹配数量越多，那么文档的相关度就越高。目前为止还不错。

但是如果我们想给含有`Lucene`的文档多一些权重，同时给含有`Elasticsearch`的文档更多一些权重呢？

我们可以通过指定一个`boost`值来控制每个查询子句的相对权重，该值默认为`1`。一个大于`1`的`boost`会增加该查询子句的相对权重。因此我们可以将上述查询重写如下：

```json
GET /_search
{
    "query": {
        "bool": {
            "must": {
                "match": {  
                    "content": {
                        "query":    "full text search",
                        "operator": "and"
                    }
                }
            },
            "should": [
                { "match": {
                    "content": {
                        "query": "Elasticsearch",
                        "boost": 3 
                    }
                }},
                { "match": {
                    "content": {
                        "query": "Lucene",
                        "boost": 2 
                    }
                }}
            ]
        }
    }
}
```

> **NOTE**
> 
> `boost`参数被用来增加一个子句的相对权重(当`boost`大于`1`时)，或者减小相对权重(当`boost`介于`0`到`1`时)，但是增加或者减小不是线性的。换言之，`boost`设为`2`并不会让最终的`_score`加倍。
> 
> 相反，新的`_score`会在适用了`boost`后被归一化(Normalized)。每种查询都有自己的归一化算法(Normalization Algorithm)，算法的细节超出了本书的讨论范围。但是能够说一个高的`boost`值会产生一个高的`_score`。
> 
> 如果你在实现你自己的不基于TF/IDF的相关度分值模型并且你需要对提升过程拥有更多的控制，你可以使用[`function_score`查询](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/function-score-query.html)，它不通过归一化步骤对文档的`boost`进行操作。

在下一章中，我们会介绍其它的用于合并查询的方法，[多字段查询(Multifield Search)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/multi-field-search.html)。但是，首先让我们看看查询的另一个重要特定：文本分析(Text Analysis)。
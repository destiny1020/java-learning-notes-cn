## 索引期间的即时搜索(Index-time Search-as-you-type) ##

建立索引期间即时搜索的第一步就是定义你的分析链(Analysis Chain)(在[配置解析器](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/configuring-analyzers.html)中讨论过)，在这里我们会详细阐述这些步骤：

### 准备索引 ###

第一步是配置一个自定义的edge_ngram词条过滤器，我们将它称为autocomplete_filter：

```json
{
    "filter": {
        "autocomplete_filter": {
            "type":     "edge_ngram",
            "min_gram": 1,
            "max_gram": 20
        }
    }
}
```

以上配置的作用是，对于此词条过滤器接受的任何词条，它都会产生一个最小长度为1，最大长度为20的边缘ngram(Edge ngram)。

然后我们将该词条过滤器配置在自定义的解析器中，该解析器名为autocomplete。

```json
{
    "analyzer": {
        "autocomplete": {
            "type":      "custom",
            "tokenizer": "standard",
            "filter": [
                "lowercase",
                "autocomplete_filter" 
            ]
        }
    }
}
```

以上的解析器会使用standard分词器将字符串划分为独立的词条，将它们变成小写形式，然后为它们生成边缘ngrams，这要感谢autocomplete_filter。

创建索引，词条过滤器和解析器的完整请求如下所示：

```json
PUT /my_index
{
    "settings": {
        "number_of_shards": 1, 
        "analysis": {
            "filter": {
                "autocomplete_filter": { 
                    "type":     "edge_ngram",
                    "min_gram": 1,
                    "max_gram": 20
                }
            },
            "analyzer": {
                "autocomplete": {
                    "type":      "custom",
                    "tokenizer": "standard",
                    "filter": [
                        "lowercase",
                        "autocomplete_filter" 
                    ]
                }
            }
        }
    }
}
```

你可以通过下面的analyze API来确保行为是正确的：

```json
GET /my_index/_analyze?analyzer=autocomplete
quick brown
```

返回的词条说明解析器工作正常：

- q
- qu
- qui
- quic
- quick
- b
- br
- bro
- brow
- brown

为了使用它，我们需要将它适用到字段中，通过update-mapping API：

```json
PUT /my_index/_mapping/my_type
{
    "my_type": {
        "properties": {
            "name": {
                "type":     "string",
                "analyzer": "autocomplete"
            }
        }
    }
}
```

现在，让我们索引一些测试文档：

```json
POST /my_index/my_type/_bulk
{ "index": { "_id": 1            }}
{ "name": "Brown foxes"    }
{ "index": { "_id": 2            }}
{ "name": "Yellow furballs" }
```

### 查询该字段 ###

如果你使用一个针对"brown fo"的简单match查询：

```json
GET /my_index/my_type/_search
{
    "query": {
        "match": {
            "name": "brown fo"
        }
    }
}
```

你会发现两份文档都匹配了，即使Yellow furballs既不包含brown，也不包含fo：

```json
{

  "hits": [
     {
        "_id": "1",
        "_score": 1.5753809,
        "_source": {
           "name": "Brown foxes"
        }
     },
     {
        "_id": "2",
        "_score": 0.012520773,
        "_source": {
           "name": "Yellow furballs"
        }
     }
  ]
}
```

通过validate-query API来发现问题：

```json
GET /my_index/my_type/_validate/query?explain
{
    "query": {
        "match": {
            "name": "brown fo"
        }
    }
}
```

得到的解释说明了查询会寻找查询字符串中每个单词的边缘ngrams：

name:b name:br name:bro name:brow name:brown name:f name:fo

name:f这一条件满足了第二份文档，因为furballs被索引为f，fu，fur等。因此，得到以上的结果也没什么奇怪的。autocomplete解析器被同时适用在了索引期间和搜索期间，通常而言这都是正确的行为。但是当前的场景是为数不多的不应该使用该规则的场景之一。

我们需要确保在倒排索引中含有每个单词的边缘ngrams，但是仅仅匹配用户输入的完整单词(brown和fo)。我们可以通过在索引期间使用autocomplete解析器，而在搜索期间使用standard解析器来达到这个目的。直接在查询中指定解析器就是一种改变搜索期间分析器的方法：

```json
GET /my_index/my_type/_search
{
    "query": {
        "match": {
            "name": {
                "query":    "brown fo",
                "analyzer": "standard" 
            }
        }
    }
}
```

另外，还可以在name字段的映射中分别指定index_analyzer和search_analyzer。因为我们只是想修改search_analyzer，所以可以在不对数据重索引的前提下对映射进行修改：

```json
PUT /my_index/my_type/_mapping
{
    "my_type": {
        "properties": {
            "name": {
                "type":            "string",
                "index_analyzer":  "autocomplete", 
                "search_analyzer": "standard" 
            }
        }
    }
}
```

此时再通过validate-query API得到的解释如下：

name:brown name:fo

重复执行查询后，也仅仅会得到Brown foxes这份文档。

因为大部分的工作都在索引期间完成了，查询需要做的只是查找两个词条：brown和fo，这比使用match_phrase_prefix来寻找所有以fo开头的词条更加高效。

> **完成建议(Completion Suggester)**
> 
> 使用边缘ngrams建立的即时搜索是简单，灵活和迅速的。然而，有些时候它还是不够快。延迟的影响不容忽略，特别当你需要提供实时反馈时。有时最快的搜索方式就是没有搜索。
> 
> ES中的[完成建议](http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search-suggesters-completion.html)采用了一种截然不同的解决方案。通过给它提供一个完整的可能完成列表(Possible Completions)来创建一个有限状态转换器(Finite State Transducer)，该转换器是一个用来描述图(Graph)的优化数据结构。为了搜索建议，ES会从图的起始处开始，对用户输入逐个字符地沿着匹配路径(Matching Path)移动。一旦用户输入被检验完毕，它就会根据当前的路径产生所有可能的建议。
> 
> 该数据结构存在于内存中，因此对前缀查询而言是非常迅速的，比任何基于词条的查询都要快。使用它来自动完成名字和品牌(Names and Brands)是一个很不错的选择，因为它们通常都以某个特定的顺序进行组织，比如"Johnny Rotten"不会被写成"Rotten Johnny"。
> 
> 当单词顺序不那么容易被预测时，边缘ngrams就是相比完成建议更好的方案。

### 边缘ngrams和邮政编码 ###

边缘ngrams这一技术还可以被用在结构化数据上，比如本章[前面提到过的邮政编码](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/prefix-query.html)。当然，postcode字段也许需要被设置为analyzed，而不是not_analyzed，但是你仍然可以通过为邮政编码使用keyword分词器来让它们和not_analyzed字段一样。

> **TIP**
> 
> keyword分词器是一个没有任何行为(no-operation)的分词器。它接受的任何字符串会被原样输出为一个词条。所以对于一些通常被当做not_analyzed字段，然而需要某些处理(如转换为小写)的情况下，是有用处的。

这个例子使用keyword分词器将邮政编码字符串转换为一个字符流，因此我们就能够利用边缘ngram词条过滤器了：

```json
{
    "analysis": {
        "filter": {
            "postcode_filter": {
                "type":     "edge_ngram",
                "min_gram": 1,
                "max_gram": 8
            }
        },
        "analyzer": {
            "postcode_index": { 
                "tokenizer": "keyword",
                "filter":    [ "postcode_filter" ]
            },
            "postcode_search": { 
                "tokenizer": "keyword"
            }
        }
    }
}
```







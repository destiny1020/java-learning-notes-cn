## 多数字段(Most Fields) ##

全文搜索是一场召回率(Recall) - 返回所有相关的文档，以及准确率(Precision) - 不返回无关文档，之间的战斗。目标是在结果的第一页给用户呈现最相关的文档。

为了提高召回率，我们会广撒网 - 不仅包括精确匹配了用户搜索词条的文档，还包括了那些我们认为和查询相关的文档。如果一个用户搜索了"quick brown fox"，一份含有fast foxes的文档也可以作为一个合理的返回结果。

如果我们拥有的相关文档仅仅是含有fast foxes的文档，那么它会出现在结果列表的顶部。但是如果我们有100份含有quick brown fox的文档，那么含有fast foxes的文档的相关性就会变低，我们希望它出现在结果列表的后面。在包含了许多可能的匹配后，我们需要确保相关度高的文档出现在顶部。

一个用来调优全文搜索相关性的常用技术是将同样的文本以多种方式索引，每一种索引方式都提供了不同相关度的信号(Signal)。主要字段(Main field)中含有的词条的形式是最宽泛的(Broadest-matching)，用来尽可能多的匹配文档。比如，我们可以这样做：

- 使用一个词干提取器来将jumps，jumping和jumped索引成它们的词根：jump。然后当用户搜索的是jumped时，我们仍然能够匹配含有jumping的文档。
- 包含同义词，比如jump，leap和hop。
- 移除变音符号或者声调符号：比如，ésta，está和esta都会以esta被索引。

但是，如果我们有两份文档，其中之一含有jumped，而另一份含有jumping，那么用户会希望第一份文档的排序会靠前，因为它含有用户输入的精确值。

我们可以通过将相同的文本索引到其它字段来提供更加精确的匹配。一个字段可以包含未被提取词干的版本，另一个则是含有变音符号的原始单词，然后第三个使用了shingles，用来提供和[单词邻近度](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/proximity-matching.html)相关的信息。这些其它字段扮演的角色就是信号(Signals)，它们用来增加每个匹配文档的相关度分值。能够匹配的字段越多，相关度就越高。

如果一份文档能够匹配具有最宽泛形式的主要字段(Main field)，那么它就会被包含到结果列表中。如果它同时也匹配了信号字段，它会得到一些额外的分值用来将它移动到结果列表的前面。

我们会在本书的后面讨论同义词，单词邻近度，部分匹配以及其他可能的信号，但是我们会使用提取了词干和未提取词干的字段的简单例子来解释这个技术。

### 多字段映射(Multifield Mapping) ###

第一件事就是将我们的字段索引两次：一次是提取了词干的形式，一次是未提取词干的形式。为了实现它，我们会使用多字段(Multifields)，在[字符串排序和多字段](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/multi-fields.html)中我们介绍过：

```json
DELETE /my_index

PUT /my_index
{
    "settings": { "number_of_shards": 1 }, 
    "mappings": {
        "my_type": {
            "properties": {
                "title": { 
                    "type":     "string",
                    "analyzer": "english",
                    "fields": {
                        "std":   { 
                            "type":     "string",
                            "analyzer": "standard"
                        }
                    }
                }
            }
        }
    }
}
```

title字段使用了english解析器进行词干提取。
title.std字段则使用的是standard解析器，因此它没有进行词干提取。

下一步，我们会索引一些文档：

```json
PUT /my_index/my_type/1
{ "title": "My rabbit jumps" }

PUT /my_index/my_type/2
{ "title": "Jumping jack rabbits" }
```

以下是一个简单的针对title字段的match查询，它查询jumping rabbits：

```json
GET /my_index/_search
{
   "query": {
        "match": {
            "title": "jumping rabbits"
        }
    }
}
```

它会变成一个针对两个提干后的词条jump和rabbit的查询，这要得益于english解析器。两份文档的title字段都包含了以上两个词条，因此两份文档的分值是相同的：

```json
{
  "hits": [
     {
        "_id": "1",
        "_score": 0.42039964,
        "_source": {
           "title": "My rabbit jumps"
        }
     },
     {
        "_id": "2",
        "_score": 0.42039964,
        "_source": {
           "title": "Jumping jack rabbits"
        }
     }
  ]
}
```

如果我们只查询title.std字段，那么只有文档2会匹配。但是，当我们查询两个字段并将它们的分值通过bool查询进行合并的话，两份文档都能够匹配(title字段也匹配了)，而文档2的分值会更高一些(匹配了title.std字段)：

```json
GET /my_index/_search
{
   "query": {
        "multi_match": {
            "query":  "jumping rabbits",
            "type":   "most_fields", 
            "fields": [ "title", "title.std" ]
        }
    }
}
```

在上述查询中，由于我们想合并所有匹配字段的分值，因此使用的类型为most_fields。这会让multi_match查询将针对两个字段的查询子句包含在一个bool查询中，而不是包含在一个dis_max查询中。

```json
{
  "hits": [
     {
        "_id": "2",
        "_score": 0.8226396, 
        "_source": {
           "title": "Jumping jack rabbits"
        }
     },
     {
        "_id": "1",
        "_score": 0.10741998, 
        "_source": {
           "title": "My rabbit jumps"
        }
     }
  ]
}
```

此时，文档2的分值比文档1的高许多。

我们使用了拥有宽泛形式的title字段来匹配尽可能多的文档 - 来增加召回率(Recall)，同时也使用了title.std字段作为信号来让最相关的文档能够拥有更靠前的排序(译注：增加了准确率(Precision))。

每个字段对最终分值的贡献可以通过指定boost值进行控制。比如，我们可以提升title字段来让该字段更加重要，这也减小了其它信号字段的影响：

```json
GET /my_index/_search
{
   "query": {
        "multi_match": {
            "query":       "jumping rabbits",
            "type":        "most_fields",
            "fields":      [ "title^10", "title.std" ] 
        }
    }
}
```







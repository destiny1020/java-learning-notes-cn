## 多词查询(Multi-word Queries) ##

如果我们一次只能搜索一个词，那么全文搜索就会显得相当不灵活。幸运的是，通过`match`查询来实现多词查询也同样简单：

```json
GET /my_index/my_type/_search
{
    "query": {
        "match": {
            "title": "BROWN DOG!"
        }
    }
}
```

以上的查询会返回所有的四份文档：

```json
{
  "hits": [
     {
        "_id":      "4",
        "_score":   0.73185337, 
        "_source": {
           "title": "Brown fox brown dog"
        }
     },
     {
        "_id":      "2",
        "_score":   0.47486103, 
        "_source": {
           "title": "The quick brown fox jumps over the lazy dog"
        }
     },
     {
        "_id":      "3",
        "_score":   0.47486103, 
        "_source": {
           "title": "The quick brown fox jumps over the quick dog"
        }
     },
     {
        "_id":      "1",
        "_score":   0.11914785, 
        "_source": {
           "title": "The quick brown fox"
        }
     }
  ]
}
```

文档4的相关度最高因为它包含了`"brown"`两次和`"dog"`一次。
文档2和文档3都包含了`"brown"`和`"dog"`一次，同时它们的`title`字段拥有相同的长度，因此它们的分值相同。
文档1只包含了`"brown"`。

因为`match`查询需要查询两个词条 - `["brown","dog"]` - 在内部它需要执行两个`term`查询，然后将它们的结果合并来得到整体的结果。因此，它会将两个`term`查询通过一个`bool`查询组织在一起，我们会在[合并查询](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/bool-query.html)一节中详细介绍。

从上面的例子中需要吸取的经验是，文档的`title`字段中只需要**包含至少一个指定的词条**，就能够匹配该查询。如果匹配的词条越多，也就意味着该文档的相关度就越高。

### 提高精度(Improving Precision) ###

匹配任何查询词条就算作匹配的话，会导致最终结果中有很多看似无关的匹配。它是一个霰弹枪式的策略(Shotgun Approach)。我们大概只想要显示包含了所有查询词条的文档。换言之，相比`brown OR dog`，我们更想要的结果是`brown AND dog`。

`match`查询接受一个`operator`参数，该参数的默认值是`"or"`。你可以将它改变为`"and"`来要求所有的词条都需要被匹配：

```json
GET /my_index/my_type/_search
{
    "query": {
        "match": {
            "title": {      
                "query":    "BROWN DOG!",
                "operator": "and"
            }
        }
    }
}
```

`match`查询的结构需要被稍稍改变来容纳`operator`参数。

这个查询的结果会将文档1排除在外，因为它只包含了一个查询词条。

### 控制精度(Controlling Precision) ###

在**all**和**any**中选择有种非黑即白的感觉。如果用户指定了5个查询词条，而一份文档只包含了其中的4个呢？将`"operator"`设置成`"and"`会将它排除在外。

有时候这正是你想要的，但是对于大多数全文搜索的使用场景，你会希望将相关度高的文档包含在结果中，将相关度低的排除在外。换言之，我们需要一种介于两者中间的方案。

`match`查询支持`minimum_should_match`参数，它能够让你指定有多少词条必须被匹配才会让该文档被当做一个相关的文档。尽管你能够指定一个词条的绝对数量，但是通常指定一个百分比会更有意义，因为你无法控制用户会输入多少个词条：

```json
GET /my_index/my_type/_search
{
  "query": {
    "match": {
      "title": {
        "query":                "quick brown dog",
        "minimum_should_match": "75%"
      }
    }
  }
}
```

当以百分比的形式指定时，`minimum_should_match`会完成剩下的工作：在上面拥有3个词条的例子中，`75%`会被向下舍入到`66.6%`，即3个词条中的2个。无论你输入的是什么，至少有2个词条被匹配时，该文档才会被算作最终结果中的一员。

> `minimum_should_match`参数非常灵活，根据用户输入的词条的数量，可以适用不同的规则。具体可以参考[`minimum_should_match`参数的相关文档](http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.4//query-dsl-minimum-should-match.html)。

为了更好地了解`match`查询是如何处理多词查询的，我们需要看看`bool`查询是如何合并多个查询的。
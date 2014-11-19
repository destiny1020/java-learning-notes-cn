## 配置解析器 ##

第三个重要的索引设置就是解析(Analysis)，可以利用已经存在的解析器(Analyzer)进行配置，或者是为你的索引定制新的解析器。

在[解析和解析器](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/analysis-intro.html)中，我们介绍了一些内置的解析器，它们用来将全文字符串转换成适合搜索的倒排索引(Inverted Index)。

对于全文字符串字段默认使用的是`standard`解析器，它对于多数西方语言而言是一个不错的选择。它包括：

- `standard`分词器。它根据词语的边界进行分词。
- `standard` token过滤器。用来整理上一步分词器得到的tokens，但是目前是一个空操作(no-op)。
- `lowercase` token过滤器。将所有tokens转换为小写。
- `stop` token过滤器。移除所有的stopwords，比如a，the，and，is等

默认下stopwords过滤器没有被使用。可以通过创建一个基于`standard`解析器的解析器并设置`stopwords`参数来启用。要么提供一个stopwords的列表或者告诉它使用针对某种语言预先定义的stopwords列表。

在下面的例子中，我们创建了一个名为`es_std`的解析器，它使用了预先定义的西班牙语中的stopwords列表：

```
PUT /spanish_docs
{
    "settings": {
        "analysis": {
            "analyzer": {
                "es_std": {
                    "type":      "standard",
                    "stopwords": "_spanish_"
                }
            }
        }
    }
}
```

`es_std`解析器不是全局的 - 它只作用于`spanish_docs`索引。可以通过制定索引名，使用`analyze` API进行测试：

```
GET /spanish_docs/_analyze?analyzer=es_std
{
    El veloz zorro marrón"
}
```

下面的部分结果显示了西班牙语中的stopword `El`已经被正确地移除了：

```
{
  "tokens" : [
    { "token" :    "veloz",   "position" : 2 },
    { "token" :    "zorro",   "position" : 3 },
    { "token" :    "marrón",  "position" : 4 }
  ]
}
```
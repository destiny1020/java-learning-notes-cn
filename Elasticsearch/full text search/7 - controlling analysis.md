## 控制分析(Controlling Analysis) ##

查询只能摘到真实存在于倒排索引(Inverted Index)中的词条(Term)，因此确保相同的分析过程会被适用于文档的索引阶段和搜索阶段的查询字符串是很重要的，这样才能够让查询中的词条能够和倒排索引中的词条匹配。

尽管我们说的是文档(Document)，解析器(Analyzer)是因字段而异的(Determined per Field)。每个字段都能够拥有一个不同的解析器，通过为该字段配置一个特定的解析器或者通过依赖类型(Type)，索引(Index)或者节点(Node)的默认解析器。在索引时，一个字段的值会被该字段的解析器解析。

比如，让我们为`my_index`添加一个新字段：

```json
PUT /my_index/_mapping/my_type
{
    "my_type": {
        "properties": {
            "english_title": {
                "type":     "string",
                "analyzer": "english"
            }
        }
    }
}
```

现在我们就能够通过`analyze` API来比较`english_title`字段和`title`字段在索引时的解析方式，以`Foxes`这一单词为例：

```json
GET /my_index/_analyze?field=my_type.title   
Foxes

GET /my_index/_analyze?field=my_type.english_title 
Foxes
```

对于`title`字段，它使用的是默认的`standard`解析器，它会返回词条`foxes`。
对于`english_title`字段，它使用的是`english`解析器，它会返回词条`fox`。

这说明当我们为词条`fox`执行一个低级的`term`查询时，`english_title`字段能匹配而`title`字段不能。

类似`match`查询的高阶查询能够理解字段映射(Field Mappings)，同时能够为查询的每个字段适用正确的解析器。我们可以通过`validate-query` API来验证这一点：

```json
GET /my_index/my_type/_validate/query?explain
{
    "query": {
        "bool": {
            "should": [
                { "match": { "title":         "Foxes"}},
                { "match": { "english_title": "Foxes"}}
            ]
        }
    }
}
```

它会返回这个`explanation`：

> (title:foxes english_title:fox)

`match`查询会为每个字段适用正确的解析器，来确保该字段的查询词条的形式是正确的。

// TODO
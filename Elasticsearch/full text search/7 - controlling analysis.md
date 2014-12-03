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


### 默认解析器(Default Analyzers) ###

尽管我们能够为字段指定一个解析器，但是当没有为字段指定解析器时，如何决定字段会使用哪个解析器呢？

解析器可以在几个级别被指定。ES会依次检查每个级别直到它找到了一个可用的解析器。在索引期间，检查的顺序是这样的：

- 定义在字段映射中的`analyzer`
- *文档的`_analyzer`字段中定义的解析器*
- `type`默认的`analyzer`，它的默认值是
- 在索引设置(Index Settings)中名为`default`的解析器，它的默认值是
- 节点上名为`default`的解析器，它的默认值是
- `standard`解析器

在搜索期间，顺序稍微有所不同：

- *直接定义在查询中的`analyzer`*
- 定义在字段映射中的`analyzer`
- `type`默认的`analyzer`，它的默认值是
- 在索引设置(Index Settings)中名为`default`的解析器，它的默认值是
- 节点上名为`default`的解析器，它的默认值是
- `standard`解析器

> **NOTE**
> 
> 以上两个斜体表示的项目突出显示了索引期间和搜索期间顺序的不同之处。_analyzer字段允许你能够为每份文档指定一个默认的解析器(比如，`english`，`french`，`spanish`)，而查询中的analyzer参数则让你能够指定查询字符串使用的解析器。然而，这并不是在一个索引中处理多语言的最佳方案，因为在[处理自然语言](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/languages.html)中提到的一些陷阱。

偶尔，在索引期间和查询期间使用不同的解析器是有意义的。比如，在索引期间我们也许希望能够索引同义词(Synonyms)(比如，对每个出现的`quick`，同时也索引`fast`，`rapid`和`speedy`)。但是在查询期间，我们必须要搜索以上所有的同义词。相反我们只需要查询用户输入的单一词汇，不管是`quick`，`fast`，`rapid`或者`speedy`。

为了实现这个区别，ES也支持`index_analyzer`和`search_analyzer`参数，以及名为`default_index`和`default_search`的解析器。

将这些额外的参数也考虑进来的话，索引期间查找解析器的完整顺序是这样的：

- 定义在字段映射中的`index_analyzer`
- 定义在字段映射中的`analyzer`
- 定义在文档`_analyzer`字段中的解析器
- `type`的默认`index_analyzer`，它的默认值是
- `type`的默认`analyzer`，它的默认值是
- 索引设置中`default_index`对应的解析器，它的默认值是
- 索引设置中`default`对应的解析器，它的默认值是
- 节点上`default_index`对应的解析器，它的默认值是
- 节点上`default`对应的解析器，它的默认值是
- `standard`解析器

而查询期间的完整顺序则是：

- 直接定义在查询中的`analyzer`
- 定义在字段映射中的`search_analyzer`
- 定义在字段映射中的`analyzer`
- `type`的默认`search_analyzer`，它的默认值是
- `type`的默认`analyzer`，它的默认值是
- 索引设置中的`default_search`对应的解析器，它的默认值是
- 索引设置中的`default`对应的解析器，它的默认值是
- 节点上`default_search`对应的解析器，它的默认值是
- 节点上`default`对应的解析器，它的默认值是
- `standard`解析器

### 配置解析器 ###

能够指定解析器的地方太多也许会吓到你。但是实际上，它是非常简单的。

**使用索引设置，而不是配置文件**

第一件需要记住的是，即使你是因为一个单一的目的或者需要为一个例如日志的应用而使用ES的，很大可能你在将来会发现更多的用例，因此你会在相同的集群上运行多个独立的应用。每个索引都需要是独立的并且被独立地配置。

所以这就排除了在节点上配置解析器的必要。另外，在节点上配置解析器会改变每个节点的配置文件并且需要重启每个节点，这是维护上的噩梦。让ES持续运行并且只通过API来管理设置是更好的主意。

**保持简单(Keep it simple)**

多数时候，你都能提前知道你的文档会包含哪些字段。最简单的办法是在创建索引或者添加类型映射时为每个全文字段设置解析器。尽管该方法稍微有些繁琐，它能够让你清晰地看到每个字段上使用的解析器。

典型地，大多数字符串字段都会是精确值类型的`not_analyzed`字段，比如标签或者枚举，加上一些会使用`standard`或者`english`以及其他语言解析器的全文字段。然后你会有一两个字段需要自定义解析：比方`title`字段的索引方式需要支持"输入即时搜索(Find-as-you-type)"。

你可以在索引中设置`default`解析器，用来将它作为大多数全文字段的解析器，然后对某一两个字段配置需要的解析器。如果在你的建模中，你需要为每个类型使用一个不同的默认解析器，那么就在类型级别上使用`analyzer`设置。

**NOTE**

对于日志这类基于时间的数据，一个常用的工作流是每天即时地创建一个新的索引并将相应数据索引到其中。尽管这个工作流会让你无法预先创建索引，你仍然可以使用[索引模板(Index Templates)](http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/indices-templates.html)来为一个新的索引指定其设置和映射。

## 类型和映射(Types and Mappings) ##

在ES中的类型(Type)代表的是一类相似的文档。一个类型包含了一个名字(Name) - 比如`user`或者`blogpost` - 以及一个映射(Mapping)。映射就像数据库的模式那样，描述了文档中的字段或者属性，和每个字段的数据类型 - `string`，`integer`，`date`等 - 这些字段是如何被Lucene索引和存储的。 

在[什么是文档](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/document.html)中，我们说一个类型就好比关系数据库中的一张表。尽管一开始这样思考有助于理解，但是对类型本身进行更细致的解释 - 它们到底是什么，它们是如何在Lucene的基础之上实现的 - 仍然是有价值的。

### Lucene是如何看待文档的 ###

Lucene中的文档包含的是一个简单field-value对的列表。一个字段至少要有一个值，但是任何字段都可以拥有多个值。类似的，一个字符串值也可以通过解析阶段而被转换为多个值。Lucene不管值是字符串类型，还是数值类型或者什么别的类型 - 所有的值都会被同等看做一些不透明的字节(Opaque bytes)。

当我们使用Lucene对文档进行索引时，每个字段的值都会被添加到倒排索引(Inverted Index)的对应字段中。原始值也可以被选择是否会不作修改的被保存到索引中，以此来方便将来的获取。

### 类型是如何实现的 ###

ES中的type是基于以下简单的基础进行实现的。一个索引中可以有若干个类型，每个类型又有它自己的mapping，然后类型下的任何文档可以存储在同一个索引中。

可是Lucene中并没有文档类型这一概念。所以在具体实现中，类型信息通过一个元数据字段`_type`记录在文档中。当我们需要搜索某个特定类型的文档时，ES会自动地加上一个针对_type字段的过滤器来保证返回的结果都是目标类型上的文档。

同时，Lucene中也没有映射的概念。映射是ES为了对复杂JSON文档进行扁平化(可以被Lucene索引)而设计的一个中间层。

比如，`user`类型的`name`字段可以定义成一个`string`类型的字段，而它的值则应该被`whitespace`解析器进行解析，然后再被索引到名为`name`的倒排索引中。

```
"name": {
    "type":     "string",
    "analyzer": "whitespace"
}
```

### 避免类型中的陷阱 ###

由于不同类型的文档能够被添加到相同的索引中，产生了一些意想不到的问题。

比如在我们的索引中，存在两个类型：`blog_en`用来保存英文的博文，`blog_es`用来保存西班牙文的博文。这两种类型中都有一个`title`字段，只不过它们使用的解析器分别是`english`和`spanish`。

问题可以通过下面的查询反映：

```
GET /_search
{
    "query": {
        "match": {
            "title": "The quick brown fox"
        }
    }
}
```

我们在两个类型中搜索`title`字段。查询字符串(Query String)需要被解析，但是应该使用哪个解析器：是`spanish`还是`english`？答案是会利用首先找到的`title`字段对应的解析器，因此对于部分文档这样做是正确的，对于另一部分则不然。

我们可以通过将字段命名地不同 - 比如`title_en`和`title_es` - 或者通过显式地将类型名包含在字段名中，然后对每个字段独立查询来避免这个问题：


```
GET /_search
{
    "query": {
        "multi_match": { 
            "query":    "The quick brown fox",
            "fields": [ "blog_en.title", "blog_es.title" ]
        }
    }
}
```

`multi_match`查询会对指定的多个字段运行`match`查询，然后合并它们的结果。

以上的查询中对`blog_en.title`字段使用`english`解析器，对`blog_es.title`字段使用`spanish`解析器，然后对两个字段的搜索结果按照相关度分值进行合并。

这个解决方案能够在两个域是相同数据类型时起作用，但是考虑下面的场景，当向相同索引中添加两份文档时会发生什么：

**类型user**
```
{ "login": "john_smith" }
```

**类型event**

```
{ "login": "2014-06-01" }
```

Lucene本身不在意类型一个字段是字符串类型，而另一个字段是日期类型 - 它只是愉快地将它们当做字节数据进行索引。

但是当我们试图去针对`event.login`字段进行排序的时候，ES需要将`login`字段的值读入到内存中。根据[Fielddata](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/fielddata-intro.html)提到的，ES会将索引中的所有文档都读入，无论其类型是什么。

取决于ES首先发现的`login`字段的类型，它会试图将这些值当做字符串或者日期类型读入。因此，这会产生意料外的结果或者直接失败。

> **Tip**
> 为了避免发生这些冲突，建议索引中，每个类型的同名字段都使用相同的映射方式。










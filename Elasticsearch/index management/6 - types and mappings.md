## 类型和映射 ##

在ES中的type代表的是一类相似的文档。就好像之前介绍的那样，type就像RDBMS中的table一样。

一个type包含了name信息以及mapping信息。对于mapping，它表达了这个type中的各种字段具有的属性，比如字段的类型是string，integer还是date等，以及这些字段是如何被Lucene索引和存储的。

### Lucene是如何看待文档的 ###

Lucene中的文档包含的是一个简单的field-value列表。一个字段至少要有一个值，但是任何字段都可以拥有多个值。类似的，一个字符串值也可以通过解析阶段而被转换为多个值。Lucene不管值是字符串类型，还是数值类型及什么别的类型 - 所有的值都会被同等对待为 "opaque bytes"。

当我们使用Lucene对文档进行索引时，每个字段的值都会被添加到倒排索引的对应字段中。原始值也可以被选择是否不作修改的而被保存到索引中，以此来方便后续获取。

### type是如何实现的 ###

ES中的type是基于以下进行实现的：一个索引中可以有若干个types，每个type又有它自己的mapping，然后type下的任何文档可以存储在同一个索引中。

可是Lucene中并没有文档type这一概念。所以在具体实现中，type信息通过一个元数据字段_type记录在文档中。当我们需要搜索某个特定type的文档时，ES会自动地加上一个针对_type字段的过滤器。

同时，Lucene中也没有mappings的概念。mappings是ES为了对复杂JSON文档进行扁平化(可以被Lucene索引)而设计的一个中间层。

比如：

```
"name": {
    "type":     "string",
    "analyzer": "whitespace"
}
```

在将name字段写入到倒排索引中前，需要使用whitespace类型的解析器对该字段进行解析。

### 避免type中的陷阱 ###

比如在同一个index中，存在两个types：blog_en用来保存英文的博文信息，blog_es用来保存西班牙文的博文信息。这两种types中都有一个title字段，只不过它们使用的analyzer是不同的，分别用的english和spanish analyzer。

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

当使用上面的查询时，query string中的title字段究竟应该使用哪种解析器呢，是english还是spanish？
实际上，它会使用第一个title字段对应的analyzer。所以这就会造成问题。

我们可以通过两种方式解决这个问题：

1. 使用不同的字段名称 - 分别使用title_en和title_es
2. 显式地声明需要查询的type名称：

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

multi_match查询会对指定的多个fields进行match查询并合并它们的结果。

然而，还有一种情况，就是同一index下，不同types下同名的字段类型不同：

```
# type user
{ "login": "john_smith" }

# type event
{ "login": "2014-06-01" }
```

Lucene本身是不在意类型信息，它只是将它们当做字符串进行保存而已。

但是当我们试图去针对event.login进行排序的时候，根据提到过的Fielddata，ES会将index中的所有文档都读入，无论类型是什么。所以这种情况下：

It will either try to load these values as a string or as a date, depending on which login field it sees first. This will either produce unexpected results or fail outright.

**Tip**
为了避免这种问题，建议index中的每个type中的同名字段都使用相同的映射方式，即字段类型和index和其mappings属性都相同。










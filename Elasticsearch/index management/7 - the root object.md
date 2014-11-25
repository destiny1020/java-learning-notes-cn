## 根对象(Root Object) ##

映射的最顶层被称为根对象。它包含了：

- 属性区域(Properties Section)，列举了文档中包含的每个字段的映射信息。
- 各种元数据(Metadata)字段，它们都以_开头，比如`_type`，`_id`，`_source`。
- 控制用于新字段的动态探测(Dynamic Detection)的设置，如`analyzer`，`dynamic_date_formats`和`dynamic_templates`。
- 其它的可以用在根对象和`object`类型中的字段上的设置，如`enabled`，`dynamic`和`include_in_all`。

### 属性(Properties) ###

我们已经在[核心简单字段类型(Core Simple Field Type)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/mapping-intro.html#core-fields)和[复杂核心字段类型(Complex Core Field Type)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/complex-core-fields.html)中讨论了对于文档字段或属性最为重要的三个设置：

- `type`：字段的数据类型，比如`string`或者`date`。
- `index`：一个字段是否需要被当做全文(Full text)进行搜索(`analyzed`)，被当做精确值(Exact value)进行搜索('not_analyzed')，或者不能被搜索(`no`)。
- `analyzer`：全文字段在索引时(Index time)和搜索时(Search time)使用的`analyzer`。

我们会在后续章节中合适的地方讨论诸如`ip`，`geo_point`和`geo_shape`等其它字段类型。

### 元数据：`_source`字段 ###

默认，ES会将表示文档正文的JSON字符串保存为`_source`字段。和其它存储的字段一样，`_source`字段也会在保存到磁盘上之前被压缩。

这个功能几乎是总被需要的，因为它意味着：

- 完整的文档在搜索结果中直接就是可用的 - 不需要额外的请求来得到完整文档
- `_source`字段让部分更新请求(Partial Update Request)成为可能
- 当映射发生变化而需要对数据进行重索引(Reindex)时，你可以直接在ES中完成，而不需要从另外一个数据存储(Datastore)(通常较慢)中获取所有文档
- 在你不需要查看整个文档时，可以从`_source`直接抽取出个别字段，通过`get`或者`search`请求返回
- 调试查询更容易，因为可以清楚地看到每个文档包含的内容，而不需要根据一个ID列表来对它们的内容进行猜测

即便如此，存储`_store`字段确实会占用磁盘空间。如果以上的任何好处对你都不重要，你可以使用以下的映射来禁用`_source`字段：

```json
PUT /my_index
{
    "mappings": {
        "my_type": {
            "_source": {
                "enabled":  false
            }
        }
    }
}
```

在一个搜索请求中，你可以只要求返回部分字段，通过在请求正文(Request body)中指定`_source`参数：

```json
GET /_search
{
    "query":   { "match_all": {}},
    "_source": [ "title", "created" ]
}
```

这些字段的值会从`_source`字段中被抽取出来并返回，而不是完整的`_source`。

> **存储字段(Stored fields)**
> 
> 除了将一个字段的值索引外，你还可以选择将字段的原始值(Original field value)进行`store`来方便将来的获取。有过使用Lucene经验的用户会使用存储字段来选择在搜索结果中能够被返回的字段。实际上，`_source`字段就是一个存储字段。
> 
> 在ES中，设置个别的文档字段为存储字段通常都是一个错误的优化。整个文档已经通过`_source`字段被保存了。使用`_source`参数来指定需要抽取的字段几乎总是更好的方案。

### 元数据：`_all`字段 ###

在[简化搜索(Search Lite)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/search-lite.html)中我们介绍了`_all`字段：它是一个特殊的字段，将其它所有字段的值当做一个大的字符串进行索引。`query_string`查询语句(以及`?q=john`这种形式的查询)在没有指定具体字段的时候，默认搜索的就是`_all`字段。

`_all`字段在一个新应用的探索阶段有用处，此时你对文档的最终结构还不太确定。你可以直接使用任何搜索字符串，并且也能够得到需要的结果：

```json
GET /_search
{
    "match": {
        "_all": "john smith marketing"
    }
}
```

随着你的应用逐渐成熟，对搜索要求也变的更加精确，你就会越来越少地使用`_all`字段。
`_all`字段是一种搜索的霰弹枪策略(Shotgun approach)。通过查询个别字段，你可以对搜索结果有更灵活，强大和细粒度的控制，来保证结果是最相关的。

> 在[相关度算法(Relevance Algorithm)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/relevance-intro.html)中一个重要的考量因素是字段的长度：字段越短，那么它就越重要。一个出现在较短的`title`字段中的词条会比它出现在较长的`content`字段中时要更重要。而这个关于字段长度的差别在`_all`字段中时不存在的。

如果你决定不再需要`_all`字段了，那么可以通过下面的映射设置来禁用它：

```json
PUT /my_index/_mapping/my_type
{
    "my_type": {
        "_all": { "enabled": false }
    }
}
```

可以使用`include_in_all`设置来对每个字段进行设置，是否需要将它包含到`_all`字段中，默认值是`true`。在一个对象(或者在根对象上)设置`include_in_all`会改变其中所有字段的默认设置。

如果你只需要将部分字段添加到`_all`字段中，比如`title`，`overview`，`summary`，`tags`等，用来方便地进行全文搜索。那么相比完全禁用`_all`，你可以将`include_in_all`默认设置为对所有字段禁用，然后对你选择的字段启用：

```json
PUT /my_index/my_type/_mapping
{
    "my_type": {
        "include_in_all": false,
        "properties": {
            "title": {
                "type":           "string",
                "include_in_all": true
            },
            ...
        }
    }
}
```

需要记住的是，`_all`字段也只不过是一个被解析过的`string`字段。它使用默认的解析器来解析其值，无论来源字段中设置的是什么解析器。和任何`string`字段一样，你也可以配置`_all`字段应该使用的解析器：

```json
PUT /my_index/my_type/_mapping
{
    "my_type": {
        "_all": { "analyzer": "whitespace" }
    }
}
```

### 元数据：文档ID(Document Identity) ###

和文档ID相关的有四个元数据字段：

- `_id`：文档的字符串ID
- `_type`：文档的类型
- `_index`：文档属于的索引
- `_uid`：`_type`和`_id`的结合，`type#id`

默认情况下，`_uid`字段会被保存和索引。意味着它可以被获取，也可以被搜索。`_type`字段会被索引但不会被保存。`_id`和`_index`既不会被索引也不会被保存，也就是说它们实际上是不存在的。

尽管如此，你还是能够查询`_id`字段，就好像它是一个实实在在的字段一样。ES使用`_uid`字段来得到`_id`。尽管你可以为这些字段修改`index`和`store`设置，但是你几乎不需要这么做。

`_id`字段有一个你也许会用到的设置：`path`，它用来告诉ES：文档应该从某个字段中抽取一个值来作为它自身的`_id`。

```json
PUT /my_index
{
    "mappings": {
        "my_type": {
            "_id": {
                "path": "doc_id" 
            },
            "properties": {
                "doc_id": {
                    "type":   "string",
                    "index":  "not_analyzed"
                }
            }
        }
    }
}
```
以上请求设置`_id`来源于`doc_id`字段。

然后，当你索引一份文档：

```json
POST /my_index/my_type
{
    "doc_id": "123"
}
```

得到的结果是这样的：

```json
{
    "_index":   "my_index",
    "_type":    "my_type",
    "_id":      "123", 
    "_version": 1,
    "created":  true
}
```

> **警告**
> 
> 这样做虽然很方便，但是它对于`bulk`请求(参考[为什么选择有趣的格式](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/distrib-multi-doc.html#bulk-format))有一些性能影响。处理请求的节点不能够利用优化的批处理格式：仅通过解析元数据行来得知哪个分片(Shard)应该接受该请求。相反，它需要解析文档正文部分。




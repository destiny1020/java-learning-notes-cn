mapping的最上层被称为root object，它包含了：

- properties区域，列举了每个字段的mapping信息、
- 各种元数据字段，使用_开头，如_type，_id，_source
- 控制用于新字段的dynamic detection的设置，如analyzer，dynamic_date_formats和dynamic_templates
- other settings which can be applied both to the root object and to fields of type object, such as enabled, dynamic, and include_in_all

### 属性 ###

三个对于文档最重要的字段：

- type：字段类型
- index：analyzed，not_analyzed，no
- analyzer：对于index为analyzed的字段使用，同时用在索引阶段和搜索阶段

### 元数据：_source字段 ###

默认，ES会将表示文档body的JSON字符串存储到_source字段。和其他stored字段一样，_source字段也会被压缩然而保存到disk上。

它有几个功能：

- full文档在搜索结果中直接可用，不需要一个单独的request来得到full文档
- _source字段让partial update request成为可能
- when your mapping changes and you need to reindex your data, you can do so directly from Elasticsearch instead of having to retrieve all of your documents from another (usually slower) datastore
- 可以从_source直接抽取出需要的字段，通过get或者search requests
- debug更容易，因为可以清楚地看到每个文档

_source会占用部分磁盘空间。所以可以选择禁用它：

```
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

从_source抽取部分字段：

```
GET /_search
{
    "query":   { "match_all": {}},
    "_source": [ "title", "created" ]
}
```

### 元数据：_all字段 ###

_all字段：一个用来索引其它所有字段的big string。query_string查询语句会默认查询_all字段，如果没有指定需要被查询的字段的话。

The _all field is useful during the **exploratory phase** of a new application, while you are still unsure about the final structure that your documents will have. You can just throw any query string at it and you have a good chance of finding the document you’re after:

```
GET /_search
{
    "match": {
        "_all": "john smith marketing"
    }
}
```

相关度算法中一个重要的因素就是字段长度。随着字段长度的增加，意味着其中的词条的权重会降低。然而，在使用_all字段的时候，这个因素就不会被考虑了。禁用_all：

```
PUT /my_index/_mapping/my_type
{
    "my_type": {
        "_all": { "enabled": false }
    }
}
```

也可以在每个字段上进行配置，是否将该字段包含到_all中：

```
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

定义在object或者root object上的include_in_all属性会对以下的字段进行统一设置，当然每个属性也可以进行覆盖，就像上面显示的那样。

对于_all字段，它使用默认的analyzer来解析其值。无论每个字段本身设置的是什么解析器。当然，可以对它使用的analyzer设置：

```
PUT /my_index/my_type/_mapping
{
    "my_type": {
        "_all": { "analyzer": "whitespace" }
    }
}
```

### 元数据：文档ID ###

和文档ID相关的有四个元数据字段：

- _id：文档的字符串ID
- _type：文档的类型
- _index：文档属于的索引
- _uid：_type和_id的结合，type#id

_uid字段会被stored和indexed，意味着它可以被获取，也可以被搜索。
_type字段会被索引但不会被保存。
_id和_index既不会被索引也不会被保存。

尽管如此，你还是可以搜索到_id字段，因为_uid字段源于_id。

_id字段有一个设置你或许需要使用：path。它用于告诉ES，_id应该来源于文档中的那个字段：

```
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

POST /my_index/my_type
{
    "doc_id": "123"
}

GET /my_index/my_type/123
```

**警告**

这样做虽然很方便，但是它对于bulk请求有一些性能影响：

The node handling the request can no longer make use of the optimized bulk format to parse just the metadata line in order to decide which shard should receive the request. Instead, it has to parse the document body as well.




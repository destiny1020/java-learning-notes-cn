## 创建索引 ##

到现在为止，我们已经通过索引一份文档来完成了新索引的创建。这个索引是使用默认的设置创建的，新的域通过动态映射(Dynamic Mapping)的方式被添加到了类型映射(Type Mapping)中。

现在我们对这个过程拥有更多的控制：我们需要确保索引被创建时拥有合适数量的主分片(Primary Shard)，并且在索引任何数据之前，我们需要设置好解析器(Analyzers)以及映射(Mappings)。

因此我们需要手动地去创建索引，将任何需要的设置和类型映射传入到请求正文中，就像下面这样：

```
PUT /my_index
{
    "settings": { ... any settings ... },
    "mappings": {
        "type_one": { ... any mappings ... },
        "type_two": { ... any mappings ... },
        ...
    }
}
```

事实上，如果你想阻止索引被自动创建，可以通过添加下面的设置到每个节点的`config/elasticsearch.yml`文件中：

```
action.auto_create_index: false
```

> 将来，我们会讨论如何使用[索引模板(Index Template)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/index-templates.html)来预先定义自动生成的索引。这个功能在索引日志数据的时候有用武之地：索引的名字中会包含日期，每天都有一个有着合适配置的索引被自动地生成。




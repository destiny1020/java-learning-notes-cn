## 索引别名和零停机时间(Index Alias and Zero Downtime) ##

重索引的问题在于你需要更新你的应用让它使用新的索引名。而索引别名可以解决这个问题。

一个索引别名就好比一个快捷方式(Shortcut)或一个符号链接(Symbolic Link)，索引别名可以指向一个或者多个索引，可以在任何需要索引名的API中使用。使用别名可以给我们非常多的灵活性。它能够让我们：

- 在一个运行的集群中透明地从一个索引切换到另一个索引
- 让多个索引形成一个组，比如`last_three_months`
- 为一个索引中的一部分文档创建一个视图(View)

我们会在本书的后面讨论更多关于别名的其它用途。现在我们要解释的是如何在零停机时间的前提下，使用别名来完成从旧索引切换到新索引。

有两个用来管理别名的端点(Endpoint)：`_alias`用来完成单一操作，`_aliases`用来原子地完成多个操作。

在这个场景中，我们假设你的应用正在使用一个名为`my_index`的索引。实际上，`my_index`是一个别名，它指向了当前正在使用的真实索引。我们会在真实索引的名字中包含一个版本号码：`my_index_v1`，`my_index_v2`等。

首先，创建索引`my_index_v1`，然后让别名`my_index`指向它：

```json
PUT /my_index_v1 
PUT /my_index_v1/_alias/my_index 
```

可以通过下面的请求得到别名指向的索引：

```json
GET /*/_alias/my_index
```

或者查询指向真实索引的有哪些别名：

```json
GET /my_index_v1/_alias/*
```

它们都会返回：

```json
{
    "my_index_v1" : {
        "aliases" : {
            "my_index" : { }
        }
    }
}
```

然后，我们决定要为索引中的一个字段更改其映射。当然，我们是不能修改当前的映射的，因此我们只好对数据进行重索引。此时我们创建了拥有新的映射的索引`my_index_v2`：

```json
PUT /my_index_v2
{
    "mappings": {
        "my_type": {
            "properties": {
                "tags": {
                    "type":   "string",
                    "index":  "not_analyzed"
                }
            }
        }
    }
}
```

紧接着，我们会根据[数据重索引](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/reindex.html)中的流程将`my_index_v1`中的数据重索引到`my_index_v2`。一旦我们确定了文档已经被正确地索引，我们就能够将别名切换到新的索引上了。

一个别名能够指向多个索引，因此当我们将别名指向新的索引时，我们还需要删除别名原来到旧索引的指向。这个改变需要是原子的，即意味着我们需要使用`_aliases`端点：

```json
POST /_aliases
{
    "actions": [
        { "remove": { "index": "my_index_v1", "alias": "my_index" }},
        { "add":    { "index": "my_index_v2", "alias": "my_index" }}
    ]
}
```

现在你的应用就在零停机时间的前提下，实现了旧索引到新索引的透明切换。

> **TIP**
> 
> 即使你认为当前的索引设计是完美的，将来你也会发现有一些部分需要被改变，而那个时候你的索引已经在生产环境中被使用了。
> 
> 在应用中使用索引别名而不是索引的真实名称，这样你就能够在任何需要的时候执行重索引操作。应该充分地使用别名。






## 搜索选项(Search Options) ##

有一些搜索字符串参数能够影响搜索过程：

### preference ###

该参数能够让你控制哪些分片或者节点会用来处理搜索请求。它能够接受：`_primary`，`_primary_first`，`_local`，`_only_node:xyz`，`_prefer_node:xyz`以及`_shards:2,3`这样的值。这些值的含义在[搜索preference的文档](http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.4//search-request-preference.html)中有详细解释。

但是，最常用的值是某些任意的字符串，用来避免结果跳跃问题(Bouncing Result Problem)。

> **结果跳跃(Bouncing Results)**
> 
> 比如当你使用一个`timestamp`字段对结果进行排序，有两份文档拥有相同的timestamp。因为搜索请求是以一种循环(Round-robin)的方式被可用的分片拷贝进行处理的，因此这两份文档的返回顺序可能因为处理的分片不一样而不同，比如主分片处理的顺序和副本分片处理的顺序就可能不一样。
> 
> 这就是结果跳跃问题：每次用户刷新页面都会发现结果的顺序不一样。
> 
> 这个问题可以通过总是为相同用户指定同样的分片来避免：将`preference`参数设置为一个任意的字符串，比如用户的会话ID(Session ID)。

### timeout ###

默认情况下，协调节点会等待所有分片的响应。如果一个节点有麻烦了，那么会让所有搜索请求的响应速度变慢。

`timeout`参数告诉协调节点它在放弃前要等待多长时间。如果放弃了，它会直接返回当前已经有的结果。返回一部分结果至少比什么都不返回要强。

在搜索请求的响应中，有用来表明搜索是否发生了超时的字段，同时也有多少分片成功响应的字段：

```
...
    "timed_out":     true,  
    "_shards": {
       "total":      5,
       "successful": 4,
       "failed":     1 
    },
...
```

### routing ###

在分布式文档存储(Distributed Document Store)一章中的路由文档到分片(Routing a document to a shard)一小节中，我们已经解释了自定义的`routing`参数可以在索引时被提供，来确保所有相关的文档，比如属于同一用户的文档，都会被保存在一个分片上。在搜索时，相比搜索索引中的所有分片，你可以指定一个或者多个`routing`值来限制搜索的范围到特定的分片上：

```
GET /_search?routing=user_1,user2
```

该技术在设计非常大型的搜索系统有用处，在[Designing for scale](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/scale.html)中我们会详细介绍。

### search_type ###

除了`query_then_fetch`是默认的搜索类型外，还有其他的搜索类型可以满足特定的目的，比如：

```
GET /_search?search_type=count
```

**count**

`count`搜索类型只有查询阶段。当你不需要搜索结果的时候可以使用它，它只会返回匹配的文档数量或者聚合(Aggregation)结果。

**query_and_fetch**

`query_and_fetch`搜索类型会将查询阶段和获取阶段合并成一个阶段。当搜索请求的目标只有一个分片时被使用，比如指定了`routing`值时，是一种内部的优化措施。尽管你可以选择使用该搜索类型，但是这样做几乎是没有用处的。

**dfs_query_then_fetch和dfs_query_and_fetch**

`dfs`搜索类型有一个查询前阶段(Pre-query phase)用来从相关分片中获取到词条频度(Term Frequencies)来计算群安居的词条频度。我们会在[相关性错了(Relevance is broken)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/relevance-is-broken.html)一节中进行讨论。

**scan**

`scan`搜索类型会和`scroll` API一起使用来高效的获取大量的结果。它通过禁用排序来完成。在下一小节中会进行讨论。






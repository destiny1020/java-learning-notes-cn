# 分布式的搜索 #

本文翻译自Elasticsearch官方指南的[Distributed Search Execution](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/distributed-search.html)一章。

在继续之前，我们将绕一段路来谈谈在分布式环境中，搜索是如何执行的。和在分布式文档存储(Distributed Document Store)中讨论的基本CRUD操作相比，这个过程会更加复杂一些。

一个CRUD操作会处理一个文档，该文档有唯一的_index，_type和路由值(Routing Value，它默认情况下就是文档的_id)组合。这意味着我们能够知道该文档被保存在集群中的哪个分片(Shard)上。

然而，搜索的执行模型会复杂的多，因为我们无法知道哪些文档会匹配 - 匹配的文档可以在集群中的任何分片上。因此搜索请求需要访问索引中的每个分片来得知它们是否有匹配的文档。

但是，找到所有匹配的文档只完成了一半的工作。从多个分片中得到的结果需要被合并和排序以得到一个完整的结果，然后search API才能够将结果返回。正因为如此，搜索由两个阶段组成(Two-phase Process) - 查询和获取(Query then Fetch)。

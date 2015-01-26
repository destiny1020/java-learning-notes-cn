## 多文档模式(Multi-Document Patterns) ##

mget和bulk API的行为模式和单个的文档操作是类似的。区别主要在于请求节点知道每份文档被保存在那个分片上，因此就能够将一个多文档请求(Multi-Document Request)给拆分为针对每个分片的多文档请求，然后并行地将这些请求转发到对应的节点上。

一旦它从每个节点上获得了答案，它会将这些答案整理成一个单独的响应并返回给客户端。

![](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/images/04-05_mget.png)

使用一个mget请求来获取多份文档的步骤如下：

1. 客户端发送一个mget请求到节点1。
2. 节点1为每个分片(可以是主要分片或者副本分片)创建一个mget请求，然后将它们并行地转发到其他分片所在的节点。一旦节点1获得了所有的结果，就会将结果组装成响应最后返回给客户端。

每份文档都可以设置routing参数，通过传入一个docs数组来完成。

![](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/images/04-06_bulk.png)

使用一个bulk请求来完成对多份文档的创建，索引，删除及更新的步骤如下：

1. 客户端发送一个bulk请求到节点1。
2. 节点1为每个分片(只能是主要分片)创建一个bulk请求，然后将它们并行地转发到其他主要分片所在的节点。
3. 主要分片会逐个执行bulk请求中出现的指令。当每个指令成功完成后，主要分片会将新的文档(或者删除的)并行地转发到它关联的所有副本分片上，然后执行下一条指令。一旦所有的副本分片对所有的指令都确定其成功了，那么当前节点就会向请求节点(Requesting Node)发送成功的响应，最后请求节点会整理所有的响应并最终发送响应给客户端。

bulk API也能够在整个请求的顶部接受`replication`和`consistency`参数，在每个具体的请求中接受`routing`参数。




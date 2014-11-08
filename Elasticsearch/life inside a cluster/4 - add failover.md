## 增加故障转移(Failover)功能 ##

只运行一个节点意味着可能存在着单点失败(Single point of failure)的问题 - 因为没有冗余。幸运的是，解决这个问题我们只需要启动另一个节点。

> ### 启动第二个节点 ###
> 
> 为了试验当你添加第二节点时会发生什么，你需要像启动第一个节点那样启动第二个节点(参见[运行ES](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/running-elasticsearch.html))，可以在同一个目录下 - 多个节点能够共享相同的目录。
> 
> 只要第二个节点也拥有和第一个节点相同的`cluster.name`(参见`./config/elasticsearch.yml`文件)，它就能够自动地被识别和添加到第一个节点所在的集群中。如果不是这样的话，检查日志来得到错误信息。错误原因可能是你的网络禁用了多播(Multicast)，或者存在防火墙阻止了节点间的通信。

如果我们启动了第二个节点，现在的集群会像下面展示的那样：

![](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/images/02-03_two_nodes.png)

现在第二个节点加入到了集群中，并且三个副本分片也被分配到了该节点上 - 这三个副本分别是三个主分片的副本。这意味着，此时我们如果失去了两个节点中的任何一个，都不会丢失数据。

任何被索引的文档首先都会被保存到主分片上，然后通过并行地方式被拷贝到相关联的副本分片上。这保证了该文档能够通过主分片或者任意一个副本分片获取。

这个时候集群的健康状态会显示为`green`，表示所有的6个分片(3个主分片和3个副本分片)都是处于活动状态的：

```
{
   "cluster_name":          "elasticsearch",
   "status":                "green", 
   "timed_out":             false,
   "number_of_nodes":       2,
   "number_of_data_nodes":  2,
   "active_primary_shards": 3,
   "active_shards":         6,
   "relocating_shards":     0,
   "initializing_shards":   0,
   "unassigned_shards":     0
}
```

此时，我们的集群不仅能够执行正常的功能，也具备了高可用性。









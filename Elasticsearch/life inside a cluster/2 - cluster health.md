## 集群健康指标(Cluster Health) ##

在一个ES集群中，有很多可以被监测的统计数据，但是其中最重要的是集群健康指标，它会以green，yellow和red来报告集群的健康状态。

```
# Retrieve the cluster health
GET /_cluster/health
```

当集群中没有任何索引时，它会返回如下信息：

```
{
   "cluster_name": "elasticsearch",
   "status": "green",
   "timed_out": false,
   "number_of_nodes": 1,
   "number_of_data_nodes": 1,
   "active_primary_shards": 0,
   "active_shards": 0,
   "relocating_shards": 0,
   "initializing_shards": 0,
   "unassigned_shards": 0
}
```

status字段提供的值反应了集群整体的健康程度。它的值的意义如下：

- green：所有的主分片(Primary Shard)和副本分片(Replica Shard)都处于活动状态
- yellow：所有的主分片都处于活动状态，但是并不是所有的副本分片都处于活跃状态
- red：不是所有的主分片都处于活动状态

在本章剩下的部分中，我们会解释什么是主分片和副本分片，以及以上的几种颜色状态信息所带来的实际影响。

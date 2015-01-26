## 主要分片(Primary Shard)和副本分片(Replica Shard)是如何交互的 ##

为了解释这个问题，假设我们有一个包含3个节点(Node)的集群(Cluster)。它含有一个拥有2个主要分片的名为blogs的索引。每个主要分片有2个副本分片。拥有相同数据的两个分片绝不会被分配到同一个节点上，所以这个集群的构成可能会像下图这样：

![](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/images/04-01_index.png)

我们可以向集群中的任意一个节点发送请求。每个节点都有足够的能力来处理请求。每个节点都知道集群中的每份文档的位置，因此能够将请求转发到相应的节点。在下面的例子中，我们会将所有的请求都发送到节点1上，这个节点被称为请求节点(Requesting Node)。

**TIP**
当发送请求时，最好采用一种循环(Round-robin)的方式来将请求依次发送到每个节点上，从而做到分担负载。
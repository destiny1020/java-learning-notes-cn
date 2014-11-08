因为自动刷新会每秒钟创建一个新的区块(Segment)，所以不用花多久就会产生大量的区块文件。区块文件的数量太多是一个问题。每个区块需要消耗文件句柄，内存，CPU这些资源。更重要的是，每个搜索请求都需要依次检查每个区块 - 那么随着区块数量的增加，搜索的速度也会变慢。

ES通过在后台合并这些区块来解决这个问题。小的区块会被合并成为大的区块，而大的区块则会被合并成为更大的区块。

这对区块进行合并的时候，也是被标记为删除的文档真正从文件系统中清除的时候。被标记为删除的文档(或者已经更新的文档的较老版本)是不会被拷贝到新的区块中去的。

你不需要做任何事去启用合并功能 - 合并会在索引和搜索时自动发生。它是这样工作的：

1. 在索引时，刷新操作(Refresh)会创建出新的区块并且读入，以便让它们对搜索可见。
2. 合并操作会选取一些大小相似的区块，在后台将它们合并成一个新的大一些的区块。因为是后台操作，所以不会妨碍索引和搜索。

下面是两个已经处于提交(Committed)状态的区块和一个未提交(Uncommitted)的区块进行合并的示意图：

![](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/images/075_60_merge.png)

3. 当合并完成时：
	- 一个新的区块会被写入到磁盘中
	- 一个新的提交点(Commit Point)会被写入，它包括了新的区块信息，老的被合并的区块则不再被包含在内
	- 新的区块会被读入以便搜索
	- 老的区块会被删除

下图是合并完成，区块状态和提交点的示意图：

![](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/images/075_60_post_merge.png)

对大的区块进行合并的时候会消耗许多I/O和CPU资源，如果不对它进行有效控制的话就会对搜索性能造成影响。默认情况下，ES会对合并过程进行控制，以便腾出足够的资源供搜索功能使用。

**小窍门**
可以参考[区块合并](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/indexing-performance.html#segments-and-merging)这一节，来得到针对具体用例的建议。

### optimize API ###

optimize API最好被描述为强制合并的API。它会强制分片的合并，最后分片的数量会达到指定的max_num_segments参数。它的目的是减少分片的数量(最后通常只有1个分片)来提升搜索的性能。

**警告**
在正在被更新的动态索引(Dynamic Index)中不应该使用optimize API。因为此时有后台的合并进程用来处理区块的合并问题，不需要再使用optimze API来强制合并。

在某些特殊的场合下，optimize API是有帮助的。典型的用例是日志记录，日志通常都会在每天，每周，每月被记录到索引中。老的日志索引本质上是只读的 - 它们通常不会发生变化。

在这种情况下，对于老的索引可以使用optimize API来将其中的区块合并成1个大的区块 - 此时它需要消耗的资源会减少，同时搜索也会更快：

```
POST /logstash-2014-10/_optimize?max_num_segments=1 
```

**注意**
值得注意的是由optimize API所触发的合并是不会被限制其资源的使用的。它可以消耗节点上的所有I/O资源，让搜索和其他必要的功能无法使用，从而也可能让你的集群失去响应。如果你计划在一个索引上使用optimize API，你应该使用分片分配(Shard Allocation，参考[迁移老的索引](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/retiring-data.html#migrate-indices)这一节)，首先将目标索引移动到一个安全的节点上。
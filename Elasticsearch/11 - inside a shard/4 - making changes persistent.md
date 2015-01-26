## 将更改持久化 ##

如果没有fsync来保证数据会从文件系统缓存(File System Cache)同步到磁盘，我们就不能确保在经历了电力故障，甚至在应用正常退出之后，数据会被保存起来。为了让ES更加可靠，它必须确保更改会被持久化到磁盘上。

在动态可更新的索引一节中，我们提到过一次完整提交(Full Commit)会将区块(Segment)全部写入到磁盘，同时也会写入一个提交点(Commit Point)，它会列出所有已知的区块信息。ES会在启动阶段或者重新打开一个索引时通过提交点来决定哪些区块属于当前的分片(Shard)。

尽管我们通过每秒刷新一次来实现近乎实时的搜索功能，我们还是需要经常性地通过完整提交(Full Commit)来保证灾难恢复的功能。但是如果有文档在两次提交之间发生了更改呢？我们也不想丢失那部分数据。

ES中有一个translog(Transaction Log，事务日志)，它记录了ES中发生的每一个操作。有了translog，索引过程就变成下面这样了：

1. 当一个文档被索引时，它被添加到内存缓冲区(In-Memory Buffer)的同时也会被附加到translog，如下图：

![](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/images/075_50_pre_refresh.png)

2. 每秒分片都会被刷新：
	- 内存缓冲区中的文档会被写入到一个新的区块中，这个过程不会使用fsync(译注：因此该区块仍然存在于内存中，只不过从内存缓冲区这个内存区域中移动到了另外一个内存区域)
	- 该区块会被读入从而对于搜索可见
	- 内存缓冲区被清空

此时的状态如下图所示，刷新后缓冲区被清空了但是translog不会被清空：

![](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/images/075_50_post_refresh.png)

3. 随着以上的步骤继续进行，又有一些文档被添加内存缓冲区，同时也记录到translog中。就像下图展示的那样：

![](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/images/075_50_pre_flush.png)

4. 当translog变的太大时，索引会被写入：一个新的translog会被建立，同时会执行一次完全提交：
	- 内存缓冲区的中和文档都会被写入到一个新的区块中
	- 缓冲区被清空
	- 一个提交点会被写入到磁盘
	- 文件系统缓存会通过fsync写入到磁盘
	- 旧的translog会被删除

如下所示：

![](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/images/075_50_post_flush.png)

translog是一个持久化了的关于所有操作的记录，这些操作的内容还没有被写入到磁盘。在启动阶段，ES会利用最后一个提交点从磁盘恢复已知的区块，同时重放translog中的记录来添加最后一个提交点之后进行的所有操作。

translog还被用来提供实时的CRUD。当你试图通过文档的ID进行相应的获取/更新/删除操作时，会首先检查translog，然后才会尝试从相关的区块中获取文档。这样做可以确保文档的最新版本总是能被实时访问。

### flush API ###

在ES中，执行一次完全提交和截断(Truncating)tranlog的操作被称为一次flush。分片会每30分钟被自动flush一次，或者当translog太大了也会进行flush操作。可以参考[translog文档](http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.4/index-modules-translog.html)来了解相关的阈值设置。

通过flush API也可以完成手动flush：

```
POST /blogs/_flush 

POST /_flush?wait_for_ongoing 
```

第一个POST请求会flush blogs这个索引。
第二个POST请求会对所有的索引进行flush操作，并且等待操作完成才会返回响应。

很少需要你去手动地执行flush操作，通常情况下自动flush就够了。

即便如此，在重启一个节点或者关闭一个索引之前去执行flush操作还是可取的。当ES试图去恢复或者重新打开一个索引的时候，它需要重放translog中记录的所有操作，所以当translog中的记录越少时，恢复的速度就越快。

> **translog有多安全？**
> 
> translog的目的是保证操作不会被丢失。而这取决于一个问题：translog有多安全。
> 
> 写入到文件中的数据在没有通过fsync到磁盘时，重启之后就会丢失。默认情况下，translog会每5秒钟被fsync一次。这也意味着，如果我们仅仅依赖于translog作为应急处理的机制时，我们可能会丢失5秒钟中产生的数据。
> 
> 幸运的是，translog只个更大系统中的一个部分而已。需要记住的是，一个索引请求只有当它在主分片(Primary Shard)和其他副本分片(Replica Shard)都完成了之后才会被算作成功。即使持有主分片的节点遭遇了灾难性的故障，也不太可能对其他持有副本分片的节点造成影响。
> 
> 虽然我们可以对translog更加频繁地执行fsync(以损失索引性能为代价)，可是这样做也不见得就能够提高可靠性。


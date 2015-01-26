下一个要解决的问题就是：如何在保证不变性的前提下，让倒排索引(Inverted Index, II)可以更新呢？
答案是：不止使用一个索引。

不是重写整个II，而是添加新的Index来反映近期的变化。每个II都会被查询，从最老的开始 - 最后结果会被合并。

在Lucene中引入了一种per-segment搜索技术。一个segment本身就是一个II，但是在Lucene中，index的意义更宽泛：它代表了一个segment集合加上一个commit point - 该文件会列出所有已知的segments。

Lucene的index如下所示：

![](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/images/075_30_index.png)

### Index vs Shard ###

在ES中，我们将Lucene的Index称为一个Shard，这又让人更迷糊了。

在ES中，index代表的是一个shards集合。当ES搜索一个index时，它会将查询发送到所有shard的一个拷贝上，然后将shards得到结果进行归约得到最终的global result set。

Lucene索引中含有包含新文档的in-memory buffer，准备commit：

![](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/images/075_30_pre_commit.png)

per-segment搜索技术的工作方式如下：

1. 新文档会被收集到一个内存索引缓冲区(in-memory indexing buffer)中，如上图。
2. 缓冲区时常需要被提交(committed)：
	- 一个新的segment - 即一个追加的(supplementary)II，会被写入到磁盘。
	- 一个新的commit point会被写入到磁盘，它包含了新的segment的名字。
	- 磁盘同步。所有在文件系统缓存中等待写入的内容会被写入到磁盘，保证所有的内容都会真正地被保存到磁盘上。
3. 新的segment中的文档对搜索可见。
4. 内存索引缓冲区(in-memory indexing buffer)会被清空，用来保存新的文档，如下图。

![](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/images/075_30_post_commit.png)

当一个查询被执行时，所有的segments都会依次被查询。词条的统计信息(Term statistics)会被累计从而保证每个词条和文档的相关度被精确地计算。通过这种方式，新文档可以以一种相对简单地方式呗添加到索引中。

### 删除和更新 ###

正因为segment是不可变的，所以其中的文档不能被移除，也不能被更新。代替的做法是，每个commit point中都会包含一个.del文件用来列出哪些文档被删除了。

当一个文档被删除了，实际上它只是在.del文件中被标注为删除了。一个已经被删除的文档还能够作为查询的匹配结果，只不过它在返回的最终结果中被移除了。

文档更新的方式和上述删除方式类似：当一个文档被更新了，旧版本的文档会被标注为删除，新版本的文档会被新的segment索引。这时候也许两个新旧两个版本的文档都会匹配某个查询，但是旧版本的文档同样会在返回最终结果前被移除。

在Segment merging中，我们会介绍被标记为删除的文档是如何真正地从文件系统中被删除的。


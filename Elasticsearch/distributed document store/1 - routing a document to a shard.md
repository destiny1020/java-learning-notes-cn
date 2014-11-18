## 路由一份文档(Document)到一个分片(Shard) ##

当你索引一份文档时，它会被保存到一个主要分片(Primary Shard)上。那么ES是如何知道该文档应该被保存到哪个分片上呢？当我们创建了一份新文档，ES是如何知道它究竟应该保存到分片1或者分片2上的呢？

这个过程不能是随机的，因为将来我们或许还需要获取该文档。实际上，这个过程是通过一个非常简单的公式决定的：

> shard = hash(routing) % number_of_primary_shards

以上的routing的值是一个任意的字符串，它默认被设置成文档的_id字段，但是也可以被设置成其他指定的值。这个routing字符串会被传入到一个哈希函数(Hash Function)来得到一个数字，然后该数字会和索引中的主要分片数进行模运算来得到余数。这个余数的范围应该总是在0和`number_of_primary_shards - 1`之间，它就是一份文档被存储到的分片的号码。

这就解释了为什么索引中的主要分片数量只能在索引创建时被指定，并且将来都不能在被更改：如果主要分片数量在索引创建后改变了，那么之前的所有路由结果都会变的不正确，从而导致文档不能被正确地获取。

> 用户有时会认为将主要分片数量固定下来会让将来对索引的水平扩展(Scale Out)变的困难。实际上，有些技术能够让你根据需要方便地进行水平扩展。我们会在[Designing for scale](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/scale.html)中介绍这些技术。

所有的文档API(get, index, delete, buli, update和mget)都接受一个routing参数，它用来定制从文档到分片的映射。一个特定的routing值能够确保所有相关文档 - 比如属于相同用户的所有文档 - 都会被存储在相同的分片上。我们会在[Designing for scale](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/scale.html)中详细介绍为什么你可能会这样做。
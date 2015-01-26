随着per-segment搜索技术的日渐成熟，索引一个文档和让它对搜索可见之间的延迟也有很明显的减小。新的文档可以在几分钟之内就可以被搜索到，但是这还不够快。

瓶颈在于磁盘。每次提交一个新的segment到磁盘是都需要执行一次fsync来保证segment真的被写入到了磁盘，从而保证数据即使在发生硬件断电时也不会丢失。但是fsync的代价是很高的，不可能在没有性能损耗的前提下去为每个新建文档执行它。

现在需要的是一种更加轻量级的方法来让新的文档对于搜索可见，也就是说需要放弃使用fsync。

在ES和磁盘之间的是文件系统缓存(File System Cache)。一个新的segment会首先被写入到文件系统缓存中，这个操作的代价很小。然后在将来的某一时刻，缓存中的内容会被写入到磁盘，这个操作的代价很大。但是当一个文件在缓存中时，就像任何其他文件那样，它是能够被打开和读取的。

内存缓冲区中含有新文档的Lucene索引，如下所示：

![](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/images/075_40_pre_refresh.png)

Lucene允许新的segment被写入和打开，这意味着segments中的文档可以被搜索到，它不需要执行一次full commit。因此它相比于commit而言更加的轻量级，从而更加适合被频繁的利用。

缓冲区中的内容被写入到了segment中，从而可以被搜索。但是它们并没有被commit，如下所示：

![](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/images/075_40_post_refresh.png)

### refresh API ###

在ES中，这个写入和打开新的segment的轻量级操作被称为refresh。默认情况下，每个shard都会每秒被自动地refresh一次。所以这就回答了为什么ES能够拥有近乎实时搜索的能力：文档的变化不会立即对搜索可见，但是它们会在一秒后可见。

这会让新的用户费解：用户也许索引了一个文档，然后立即尝试去搜索它，却发现得不到期待的结果。解决这个问题的办法是执行一次手动refresh，通过refresh API：

```
POST /_refresh 
POST /blogs/_refresh
```

第一个请求用来刷新所有的索引。
第二个请求用来刷新blogs索引。

**TIP**

尽管refresh相比commit而言更加轻量级，但是它还是有一定的性能开销的。手动refresh在执行测试的时候有用，但是在生产环境中不要每次在索引了一份文档后就执行一次手动refresh。相反地，你需要了解ES这种近乎实时搜索的特性并体谅它。

并不是所有的用例都需要每秒都执行refresh。也许你正在使用ES来索引百万计的日志文件，相比于近乎实时搜索的能力，你更在意的是如何优化文件的索引速度。这时你可以通过索引上的refresh_interval属性设置刷新的频度：

```
PUT /my_logs
{
  "settings": {
    "refresh_interval": "30s" 
  }
}
```

这个设置是可以动态更新的。当你在建立一个巨大的索引时，你可以暂时关闭自动刷新的功能，然后在生成环境中使用该索引时再将自动刷新打开：

```
POST /my_logs/_settings
{ "refresh_interval": -1 } 

POST /my_logs/_settings
{ "refresh_interval": "1s" } 
```

**注意**

refresh_interval接受一个"期间"作为参数，比如1s(1秒钟)或者2m(2分钟)。如果不带单位的话 - 如1，则代表的是1毫秒。如果这样设置的话，你的集群必须会跪：（ (译注：但是当设置为-1的时候表示关闭自动刷新功能)







## 获取阶段(Fetch Phase) ##

查询阶段能够辨识出哪些文档能够匹配搜索请求，但是我们还需要获取文档本身。这就是获取阶段完成的工作。此阶段如下图所示：

![](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/images/06-02_fetch.png)

1. 协调节点(Coordinating Node)会首先辨识出哪些文档需要被获取，然后发出一个multi GET请求到相关的分片。
2. 每个分片会读取相关文档并根据要求对它们进行润色(Enrich)，最后将它们返回给协调节点。
3. 一旦所有的文档都被获取了，协调节点会将结果返回给客户端。

协调节点首先决定到底哪些文档是真的需要被获取的。比如，如果在查询中指定了`{ "from": 90, "size": 10 }`，那么头90条结果都会被丢弃(Discarded)，只有剩下的10条结果所代表的文档需要被获取。这些文档可能来自一个或者多个分片。

协调节点会为每个含有目标文档的分片构造一个[multi GET请求](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/distrib-multi-doc.html)，然后将该请求发送给在查询阶段中参与过的分片拷贝。

分片会加载文档的正文部分 - 即_source字段 - 如果被要求的话，还会对结果进行润色，比如元数据和[搜索片段高亮(Search Snippet Highlighting)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/highlighting-intro.html)。一旦协调节点获取到了所有的结果，它就会将它们组装成一个响应并返回给客户端。

> **深度分页(Deep Pagination)**
> 
> 查询并获取(Query then Fetch)的过程支持通过传入的`from`和`size`参数来完成分页功能。但是该功能存在限制。不要忘了每个分片都会在本地保存一个大小为`from + size`的优先队列，该优先队列中的所有内容都需要被返回给协调节点。然后协调节点需要对`number_of_shards * (from + size)`份文档进行排序来保证最后能够得到正确的`size`份文档。
> 
> 根据文档的大小，分片的数量以及你正在使用的硬件，对10000到50000个结果进行分页(1000到5000页)是可以很好地完成的。但是当`from`值大的一定程度，排序就会变成非常消耗CPU，内存和带宽等资源的行为。因为如此，我们强烈地建议你不要使用深度分页。
> 
> 实践中，深度分页返回的页数实际上是不切实际的。用户往往会在浏览了2到3页后就会修改他们的搜索条件。罪魁祸首往往都是网络爬虫这种无休止地页面进行抓取的程序，它们会让你的服务器不堪重负。
> 
> 如果你确实需要从集群中获取大量的文档，那么你可以使用禁用了排序功能的scan搜索类型来高效地完成该任务。我们会在稍后的Scan和Scroll一节中进行讨论。




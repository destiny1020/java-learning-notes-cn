本文翻译自Elasticsearch官方指南的[distributed document store](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/distributed-docs.html)一章。

# 分布式文档存储 #

在上一章中，我们一直在介绍索引数据和获取数据的方法。但是我们省略了很多关于数据是如何在集群中被分布(Distributed)和获取(Fetched)的技术细节。这实际上是有意为之 - 你真的不需要了解数据在ES中是如何被分布的。它能工作就足够了。

在本章中，我们将会深入到这些内部技术细节中，来帮助你了解你的数据是如何被存储在一个分布式系统中的。



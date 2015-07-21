# 对你的数据建模(Modeling Your Data)

Elasticsearch is a different kind of beast, especially if you come from the world of SQL. It comes with many benefits: performance, scale, near real-time search, and analytics across massive amounts of data. And it is easy to get going! Just download and start using it.
ES是一头不同寻常的野兽，尤其是当你来自SQL的世界时。它拥有很多优势：性能，可扩展性，准实时的搜索，以及对大数据的分析能力。并且，它很容易上手！只需要下载就能够开始使用它了。

But it is not magic. To get the most out of Elasticsearch, you need to understand how it works and how to make it work for your needs.
但是它也不是魔法。为了更好的利用ES，你需要了解它从而让它能够满足你的需求。

Handling relationships between entities is not as obvious as it is with a dedicated relational store. The golden rule of a relational database—normalize your data—does not apply to Elasticsearch. In [Handling Relationships](https://www.elastic.co/guide/en/elasticsearch/guide/current/relations.html), [Nested Objects](https://www.elastic.co/guide/en/elasticsearch/guide/current/nested-objects.html), and [Parent-Child Relationship](https://www.elastic.co/guide/en/elasticsearch/guide/current/parent-child.html) we discuss the pros and cons of the available approaches.
在ES中，处理实体之间的关系并不像关系型存储那样明显。在关系数据库中的黄金准则 - 数据规范化，在ES中并不适用。在[处理关联关系](https://www.elastic.co/guide/en/elasticsearch/guide/current/relations.html)，[嵌套对象](https://www.elastic.co/guide/en/elasticsearch/guide/current/nested-objects.html)和[父子关联关系](https://www.elastic.co/guide/en/elasticsearch/guide/current/parent-child.html)中，我们会讨论几种可行方案的优点和缺点。

Then in [Designing for Scale](https://www.elastic.co/guide/en/elasticsearch/guide/current/scale.html) we talk about the features that Elasticsearch offers that enable you to scale out quickly and flexibly. Scale is not one-size-fits-all. You need to think about how data flows through your system, and design your model accordingly. Time-based data like log events or social network streams require a very different approach than more static collections of documents.
紧接着在[为可扩展性而设计](https://www.elastic.co/guide/en/elasticsearch/guide/current/scale.html)中，我们会讨论ES提供的一些用来快速灵活实现扩展的特性。对于扩展，并没有一个可以适用于所有场景的解决方案。你需要考虑数据是如何在你的系统中流转的，从而恰当地对你的数据进行建模。针对基于时间的数据比如日志事件或者社交数据流的方案比相对静态的文档集合的方案是十分不同的。

And finally, we talk about the one thing in Elasticsearch that doesn’t scale.
最后，我们会讨论一样在ES中不会扩展的东西。

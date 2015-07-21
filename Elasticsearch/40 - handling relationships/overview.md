# 处理关联关系(Handling Relationships)

In the real world, relationships matter: blog posts have comments, bank accounts have transactions, customers have bank accounts, orders have order lines, and directories have files and subdirectories.
在真实的世界中，关联关系很重要：博客文章有评论，银行账户有交易，客户有银行账户，订单有行项目，目录也拥有文件和子目录。

Relational databases are specifically designed—and this will not come as a surprise to you—to manage relationships:
在关系数据库中，处理关联关系的方式让你不会感到意外：

- Each entity (or row, in the relational world) can be uniquely identified by a primary key. 每个实体(或者行，在关系世界中)可以通过一个主键唯一标识。
- Entities are normalized. The data for a unique entity is stored only once, and related entities store just its primary key. Changing the data of an entity has to happen in only one place. 实体是规范化了的。对于一个唯一的实体，它的数据仅被存储一次，而与之关联的实体则仅仅保存它的主键。改变一个实体的数据只能发生在一个地方。
- Entities can be joined at query time, allowing for cross-entity search. 在查询期间，实体可以被联接(Join)，它让跨实体查询成为可能。
- Changes to a single entity are atomic, consistent, isolated, and durable. (See [ACID Transactions](http://en.wikipedia.org/wiki/ACID_transactions) for more on this subject.) 对于单个实体的修改是原子性，一致性，隔离性和持久性的。(参考[ACID事务](http://en.wikipedia.org/wiki/ACID_transactions)获取更多相关信息。)
- Most relational databases support ACID transactions across multiple entities. 绝大多数的关系型数据库都支持针对多个实体的ACID事务。

But relational databases do have their limitations, besides their poor support for full-text search. Joining entities at query time is expensive—more joins that are required, the more expensive the query. Performing joins between entities that live on different hardware is so expensive that it is just not practical. This places a limit on the amount of data that can be stored on a single server.
但是关系型数据库也有它们的局限，除了在全文搜索领域它们拙劣的表现外。在查询期间联接实体是昂贵的 - 联接的实体越多，那么查询的代价就越大。对不同硬件上的实体执行联接操作的代价太大以至于它甚至是不切实际的。这就为在单个服务器上能够存储的数据量设下了一个限制。

Elasticsearch, like most NoSQL databases, treats the world as though it were flat. An index is a flat collection of independent documents. A single document should contain all of the information that is required to decide whether it matches a search request.
ES，像多数NoSQL数据库那样，将世界看作是平的。一个索引就是一系列独立文档的扁平集合。一个单一的文档应该包括用来判断它是否符合一个搜索请求的所有信息。

While changing the data of a single document in Elasticsearch is [ACIDic](http://en.wikipedia.org/wiki/ACID_transactions), transactions involving multiple documents are not. There is no way to roll back the index to its previous state if part of a transaction fails.
虽然在ES中改变一份文档的数据是符合[ACIDic](http://en.wikipedia.org/wiki/ACID_transactions)的，涉及到多份文档的事务就不然了。在ES中，当事务失败后是没有办法将索引回滚到它之前的状态的。

This FlatWorld has its advantages:
这个扁平化的世界有它的优势：

- Indexing is fast and lock-free. 索引是迅速且不需要上锁的。
- Searching is fast and lock-free. 搜索是迅速且不需要上锁的。
- Massive amounts of data can be spread across multiple nodes, because each document is independent of the others. 大规模的数据可以被分布到多个节点上，因为每份文档之间是独立的。

But relationships matter. Somehow, we need to bridge the gap between FlatWorld and the real world. Four common techniques are used to manage relational data in Elasticsearch:
但是关联关系很重要。我们需要以某种方式将扁平化的世界和真实的世界连接起来。在ES中，有4中常用的技术来管理关联数据：

- [应用端联接(Application-side joins)](https://www.elastic.co/guide/en/elasticsearch/guide/current/application-joins.html)
- [数据非规范化(Data denormalization)](https://www.elastic.co/guide/en/elasticsearch/guide/current/denormalization.html)
- [嵌套对象(Nested objects)](https://www.elastic.co/guide/en/elasticsearch/guide/current/nested-objects.html)
- [父子关联关系(Parent/child relationships)](https://www.elastic.co/guide/en/elasticsearch/guide/current/parent-child.html)

Often the final solution will require a mixture of a few of these techniques.
通常最终的解决方案会结合这些方案的几种。
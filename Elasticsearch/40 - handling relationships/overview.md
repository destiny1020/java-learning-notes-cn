# 处理关联关系(Handling Relationships)

In the real world, relationships matter: blog posts have comments, bank accounts have transactions, customers have bank accounts, orders have order lines, and directories have files and subdirectories.

Relational databases are specifically designed—and this will not come as a surprise to you—to manage relationships:

- Each entity (or row, in the relational world) can be uniquely identified by a primary key.
- Entities are normalized. The data for a unique entity is stored only once, and related entities store just its primary key. Changing the data of an entity has to happen in only one place.
- Entities can be joined at query time, allowing for cross-entity search.
- Changes to a single entity are atomic, consistent, isolated, and durable. (See [ACID Transactions](http://en.wikipedia.org/wiki/ACID_transactions) for more on this subject.)
- Most relational databases support ACID transactions across multiple entities.

But relational databases do have their limitations, besides their poor support for full-text search. Joining entities at query time is expensive—more joins that are required, the more expensive the query. Performing joins between entities that live on different hardware is so expensive that it is just not practical. This places a limit on the amount of data that can be stored on a single server.

Elasticsearch, like most NoSQL databases, treats the world as though it were flat. An index is a flat collection of independent documents. A single document should contain all of the information that is required to decide whether it matches a search request.

While changing the data of a single document in Elasticsearch is [ACIDic](http://en.wikipedia.org/wiki/ACID_transactions), transactions involving multiple documents are not. There is no way to roll back the index to its previous state if part of a transaction fails.

This FlatWorld has its advantages:

- Indexing is fast and lock-free.
- Searching is fast and lock-free.
- Massive amounts of data can be spread across multiple nodes, because each document is independent of the others.

But relationships matter. Somehow, we need to bridge the gap between FlatWorld and the real world. Four common techniques are used to manage relational data in Elasticsearch:

- [Application-side joins](https://www.elastic.co/guide/en/elasticsearch/guide/current/application-joins.html)
- [Data denormalization](https://www.elastic.co/guide/en/elasticsearch/guide/current/denormalization.html)
- [Nested objects](https://www.elastic.co/guide/en/elasticsearch/guide/current/nested-objects.html)
- [Parent/child relationships](https://www.elastic.co/guide/en/elasticsearch/guide/current/parent-child.html)

Often the final solution will require a mixture of a few of these techniques.
# 应用端联接(Application-side Joins)

We can (partly) emulate a relational database by implementing joins in our application. For instance, let’s say we are indexing users and their blog posts. In the relational world, we would do something like this:
我们可以通过在应用中实现联接来(部分)模拟一个关系型数据库。比如，当我们想要索引用户和他们的博客文章时。在关系型的世界中，我们可以这样做：

```json
PUT /my_index/user/1  (1)
{
  "name":     "John Smith",
  "email":    "john@smith.com",
  "dob":      "1970/10/24"
}

PUT /my_index/blogpost/2   (2)
{
  "title":    "Relationships",
  "body":     "It's complicated...",
  "user":     1   (3)
}
```

(1)(2) The index, type, and id of each document together function as a primary key. 索引，类型以及每份文档的ID一起构成了主键。

(3) The blogpost links to the user by storing the user’s id. The index and type aren’t required as they are hardcoded in our application. 博文通过保存了用户的ID来联接到用户。由于索引和类型是被硬编码到了应用中的，所以这里并不需要。

Finding blog posts by user with ID 1 is easy:
通过用户ID等于1来找到对应的博文很容易：

```json
GET /my_index/blogpost/_search
{
  "query": {
    "filtered": {
      "filter": {
        "term": { "user": 1 }
      }
    }
  }
}
```

To find blogposts by a user called John, we would need to run two queries: the first would look up all users called John in order to find their IDs, and the second would pass those IDs in a query similar to the preceding one:
为了找到用户John的博文，我们可以执行两条查询：第一条查询用来得到所有名为John的用户的IDs，第二条查询通过这些IDs来得到对应文章：

```json
GET /my_index/user/_search
{
  "query": {
    "match": {
      "name": "John"
    }
  }
}

GET /my_index/blogpost/_search
{
  "query": {
    "filtered": {
      "filter": {
        "terms": { "user": [1] }   (1)
      }
    }
  }
}
```

(1) The values in the terms filter would be populated with the results from the first query. 传入到terms过滤器的值是第一条查询的结果。

The main advantage of application-side joins is that the data is normalized. Changing the user’s name has to happen in only one place: the user document. The disadvantage is that you have to run extra queries in order to join documents at search time.
应用端联接最大的优势在于数据是规范化了的。改变用户的名字只需要在一个地方操作：用户对应的文档。劣势在于你需要在搜索期间运行额外的查询来联接文档。

In this example, there was only one user who matched our first query, but in the real world we could easily have millions of users named John. Including all of their IDs in the second query would make for a very large query, and one that has to do millions of term lookups.
在这个例子中，只有一位用户匹配了第一条查询，但是在实际应用中可能轻易就得到了数以百万计的名为John的用户。将所有的IDs传入到第二个查询中会让该查询非常巨大，它需要执行百万计的term查询。

This approach is suitable when the first entity (the user in this example) has a small number of documents and, preferably, they seldom change. This would allow the application to cache the results and avoid running the first query often.
这种方法在第一个实体的文档数量较小并且它们很少改变时合适(这个例子中实体指的是用户)。这就使得通过缓存结果来避免频繁查询成为可能。
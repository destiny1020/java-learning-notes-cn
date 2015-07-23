## 反规范化你的数据(Denormalizing Your Data)

The way to get the best search performance out of Elasticsearch is to use it as it is intended, by [denormalizing](http://en.wikipedia.org/wiki/Denormalization) your data at index time. Having redundant copies of data in each document that requires access to it removes the need for joins.
让ES达到最好的搜索性能的方法是采用更直接的办法，通过在索引期间[反规范化](http://en.wikipedia.org/wiki/Denormalization)你的数据。通过在每份文档中包含冗余数据来避免联接。

If we want to be able to find a blog post by the name of the user who wrote it, include the user’s name in the blog-post document itself:
如果我们需要通过作者的名字来搜索博文，可以在博文对应的文档中直接包含该作者的名字：

```json
PUT /my_index/user/1
{
  "name":     "John Smith",
  "email":    "john@smith.com",
  "dob":      "1970/10/24"
}

PUT /my_index/blogpost/2
{
  "title":    "Relationships",
  "body":     "It's complicated...",
  "user":     {
    "id":       1,
    "name":     "John Smith" 
  }
}
```

Now, we can find blog posts about relationships by users called John with a single query:
现在，我们可以通过一条查询来得到用户名为John的博文了：

```json
GET /my_index/blogpost/_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "title":     "relationships" }},
        { "match": { "user.name": "John"          }}
      ]
    }
  }
}
```

The advantage of data denormalization is speed. Because each document contains all of the information that is required to determine whether it matches the query, there is no need for expensive joins.
对数据的反规范化的优势在于速度。因为每份文档包含了用于判断是否匹配查询的所有数据，不需要执行代价高昂的联接操作。
## 反规范化你的数据(Denormalizing Your Data)

The way to get the best search performance out of Elasticsearch is to use it as it is intended, by [denormalizing](http://en.wikipedia.org/wiki/Denormalization) your data at index time. Having redundant copies of data in each document that requires access to it removes the need for joins.

If we want to be able to find a blog post by the name of the user who wrote it, include the user’s name in the blog-post document itself:

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
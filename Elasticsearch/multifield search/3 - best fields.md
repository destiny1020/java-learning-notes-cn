## 最佳字段(Best Fields) ##

假设我们有一个让用户搜索博客文章的网站，就像这两份文档一样：

```json
PUT /my_index/my_type/1
{
    "title": "Quick brown rabbits",
    "body":  "Brown rabbits are commonly seen."
}

PUT /my_index/my_type/2
{
    "title": "Keeping pets healthy",
    "body":  "My quick brown fox eats rabbits on a regular basis."
}
```

用户输入了"Brown fox"，然后按下了搜索键。我们无法预先知道用户搜索的词条会出现在博文的`title`或者`body`字段中，但是用户是在搜索和他输入的单词相关的内容。以上的两份文档中，文档2似乎匹配的更好一些，因为它包含了用户寻找的两个单词。

让我们运行下面的`bool`查询：

```json
{
    "query": {
        "bool": {
            "should": [
                { "match": { "title": "Brown fox" }},
                { "match": { "body":  "Brown fox" }}
            ]
        }
    }
}
```

然后我们发现文档1的分值更高：

```json
{
  "hits": [
     {
        "_id":      "1",
        "_score":   0.14809652,
        "_source": {
           "title": "Quick brown rabbits",
           "body":  "Brown rabbits are commonly seen."
        }
     },
     {
        "_id":      "2",
        "_score":   0.09256032,
        "_source": {
           "title": "Keeping pets healthy",
           "body":  "My quick brown fox eats rabbits on a regular basis."
        }
     }
  ]
}
```

要理解原因，想想`bool`查询是如何计算得到其分值的：

1. 运行`should`子句中的两个查询
2. 相加查询返回的分值
3. 将相加得到的分值乘以匹配的查询子句的数量
4. 除以总的查询子句的数量

文档1在两个字段中都包含了`brown`，因此两个`match`查询都匹配成功并拥有了一个分值。文档2在`body`字段中包含了`brown`以及`fox`，但是在`title`字段中没有出现任何搜索的单词。因此对`body`字段查询得到的高分加上对`title`字段查询得到的零分，然后在乘以匹配的查询子句数量1，最后除以总的查询子句数量2，导致整体分值比文档1的低。

在这个例子中，`title`和`body`字段是互相竞争的。我们想要找到一个最佳匹配(Best-matching)的字段。

如果我们不是合并来自每个字段的分值，而是使用最佳匹配字段的分值作为整个查询的整体分值呢？这就会让包含有我们寻找的两个单词的字段有更高的权重，而不是在不同的字段中重复出现的相同单词。

### dis_max查询 ###

相比使用`bool`查询，我们可以使用`dis_max`查询(Disjuction Max Query)。Disjuction的意思"OR"(而Conjunction的意思是"AND")，因此Disjuction Max Query的意思就是返回匹配了任何查询的文档，并且分值是产生了最佳匹配的查询所对应的分值：

```json
{
    "query": {
        "dis_max": {
            "queries": [
                { "match": { "title": "Brown fox" }},
                { "match": { "body":  "Brown fox" }}
            ]
        }
    }
}
```

它会产生我们期望的结果：

```json
{
  "hits": [
     {
        "_id":      "2",
        "_score":   0.21509302,
        "_source": {
           "title": "Keeping pets healthy",
           "body":  "My quick brown fox eats rabbits on a regular basis."
        }
     },
     {
        "_id":      "1",
        "_score":   0.12713557,
        "_source": {
           "title": "Quick brown rabbits",
           "body":  "Brown rabbits are commonly seen."
        }
     }
  ]
}
```
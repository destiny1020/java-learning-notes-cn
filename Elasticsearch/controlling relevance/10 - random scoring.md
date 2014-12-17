## 随机分值计算(Random Scoring) ##

你可能会好奇什么是随机分值计算，或者为何要使用它。前面的例子提供了一个较好的用例。该例子的所有结果的最终_score是1，2，3，4或5。也许只有很少的度假酒店能够拥有5分，但我们可以假定会有很多酒店的分值为2或3。

作为一个网站的拥有者，你希望给你的广告投放商尽可能多的机会来展示他们的内容。使用当前的查询，拥有相同的_score的结果每次的返回顺序都是相同的。此时引入一定程度的随机性会更好，来保证拥有相同分值的文档都能有同等的展示机会。

我们希望每个用户都能看到一个不同的随机顺序，但是对于相同的用户，当他点击第二页，第三页或者后续页面时，看到的顺序应该是相同的。这就是所谓的一致性随机(Consistently Random)。

random_score函数，它的输出是一个介于0到1之间的数字，当给它提供相同的seed值时，它能够产生一致性随机的结果，这个seed值可以是用户的会话(Session)ID：

```json
GET /_search
{
  "query": {
    "function_score": {
      "filter": {
        "term": { "city": "Barcelona" }
      },
      "functions": [
        {
          "filter": { "term": { "features": "wifi" }},
          "weight": 1
        },
        {
          "filter": { "term": { "features": "garden" }},
          "weight": 1
        },
        {
          "filter": { "term": { "features": "pool" }},
          "weight": 2
        },
        {
          "random_score": { 
            "seed":  "the users session id" 
          }
        }
      ],
      "score_mode": "sum",
    }
  }
}
```

random_score子句不包含任何的filter，因此它适用于所有文档。

当然，如果你索引了能匹配查询的新文档，无论你是否使用了一致性随机，结果的顺序都会有所改变。

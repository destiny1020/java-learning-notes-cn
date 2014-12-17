## 根据过滤子集来提升(Boosting Filtered Subsets) ##

回到在[忽略TF/IDF(Ignoring TF/IDF)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/ignoring-tfidf.html)中处理的那个问题，我们需要根据每个度假酒店拥有的卖点数量来对它们的相关度分值进行计算。我们希望利用缓存的过滤器来影响分值，而function_score则正好可以实现该目标。

在目前的例子中，我们为所有的文档都使用了一个函数。现在我们希望使用过滤器将结果分成子集(一个卖点对应一个过滤器)，然后对每个子集适用一个不同的函数。

我们使用的函数名为weight，它和查询中接受的boost参数类似。区别在于weight不会被Lucene规范化成某个浮点数；它会被原样使用。

查询的结构需要改变来容纳多个函数：

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
        }
      ],
      "score_mode": "sum", 
    }
  }
}
```

上述例子中出现的新特性会在下面的小节中进行解释：

### 过滤器vs查询 ###

首先，我们在function_score中使用的是filter，而不是query。在上例中，我们没有必要使用全文搜索。我们只是想得到在city字段中出现了Barcelona的所有文档，而该逻辑使用过滤器表达更合适。由过滤器得到的所有文档的_score都是1。function_score会接受一个查询或是一个过滤器。如果什么都没有指定，那么默认使用的是match_all查询。

### 函数(Functions) ###

functions数组用来指定一系列需要适用的函数。数组中的每个函数还能够接受一个可选的过滤器，只有满足了该过滤器要求的文档才会被函数适用。上例中，对所有匹配的文档，weight被设为1(对泳池而言是2)。

### score_mode ###

每个函数都会返回一个结果，我们需要某种方法将多个结果归约成一个，然后将它合并到原始的_score中去。score_mode参数指定了该归约操作，它可以取下面的值：

- multiply: 函数结果会相乘(默认行为)
- sum：函数结果会累加
- avg：得到所有函数结果的平均值
- max：得到最大的函数结果
- min：得到最小的函数结果
- first：只使用第一个函数的结果，该函数可以有过滤器，也可以没有

上例中，我们希望对每个函数的结果进行相加来得到最终的分值，因此使用的是score_mode是sum。

没有匹配任何过滤器的文档会保留它们原本的_score，即为1。





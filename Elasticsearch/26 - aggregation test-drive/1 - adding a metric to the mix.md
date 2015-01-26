##添加一个指标(Metric)

从前面的例子中，我们可以知道每个桶中的文档数量。但是，通常我们的应用会需要基于那些文档的更加复杂的指标(Metric)。比如，每个桶中的汽车的平均价格是多少？

为了得到该信息，我们得告诉ES需要为哪些字段计算哪些指标。这需要将指标嵌套到桶中。指标会基于桶中的文档的值来计算相应的统计信息。

让我们添加一个计算平均值的指标：

```json
GET /cars/transactions/_search?search_type=count
{
   "aggs": {
      "colors": {
         "terms": {
            "field": "color"
         },
         "aggs": { 
            "avg_price": { 
               "avg": {
                  "field": "price" 
               }
            }
         }
      }
   }
}
```

我们添加了一个新的aggs层级来包含该指标。然后给该指标起了一个名字：avg_price。最后定义了该指标作用的字段为price。.

正如你所看到的，我们向前面的例子中添加了一个新的aggs层级。这个新的聚合层级能够让我们将avg指标嵌套在terms桶中。这意味着我们能为每种颜色都计算一个平均值。

同样的，我们需要给指标起一个名(avg_price)来让我们能够在将来得到其值。最后，我们指定了指标本身(avg)以及该指标作用的字段(price)：

```json
{
...
   "aggregations": {
      "colors": {
         "buckets": [
            {
               "key": "red",
               "doc_count": 4,
               "avg_price": { 
                  "value": 32500
               }
            },
            {
               "key": "blue",
               "doc_count": 2,
               "avg_price": {
                  "value": 20000
               }
            },
            {
               "key": "green",
               "doc_count": 2,
               "avg_price": {
                  "value": 21000
               }
            }
         ]
      }
   }
...
}
```

现在，在响应中多了一个avg_price元素。

尽管得到的响应只是稍稍有些变化，但是获得的数据增加的了许多。之前我们只知道有4辆红色汽车。现在我们知道了红色汽车的平均价格是32500刀。这些数据你可以直接插入到报表中。
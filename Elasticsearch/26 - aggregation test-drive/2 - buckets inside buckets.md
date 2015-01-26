##桶中的桶(Buckets inside Buckets)

当你开始使用不同的嵌套模式时，聚合强大的能力才会显现出来。在前面的例子中，我们已经知道了如何将一个指标嵌套进一个桶的，它的功能已经十分强大了。

但是真正激动人心的分析功能来源于嵌套在其它桶中的桶。现在，让我们来看看如何找到每种颜色的汽车的制造商分布信息：

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
            },
            "make": { 
                "terms": {
                    "field": "make" 
                }
            }
         }
      }
   }
}
```

此时发生了一些有意思的事情。首先，你会注意到前面的avg_price指标完全没有变化。一个聚合的每个层级都能够拥有多个指标或者桶。avg_price指标告诉了我们每种汽车颜色的平均价格。为每种颜色创建的桶和指标是各自独立的。

这个性质对你的应用而言是很重要的，因为你经常需要收集一些互相关联却又完全不同的指标。聚合能够让你对数据遍历一次就得到所有需要的信息。

另外一件重要的事情是添加了新聚合make，它是一个terms类型的桶(嵌套在名为colors的terms桶中)。这意味着我们会根据数据集创建不重复的(color, make)组合。

让我们来看看得到的响应(有省略，因为响应太长了)：

```json
{
...
   "aggregations": {
      "colors": {
         "buckets": [
            {
               "key": "red",
               "doc_count": 4,
               "make": { 
                  "buckets": [
                     {
                        "key": "honda", 
                        "doc_count": 3
                     },
                     {
                        "key": "bmw",
                        "doc_count": 1
                     }
                  ]
               },
               "avg_price": {
                  "value": 32500 
               }
            },

...
}
```

该响应告诉了我们如下信息：

- 有4辆红色汽车。
- 红色汽车的平均价格是32500美刀。
- 红色汽车中的3辆是Honda，1辆是BMW。
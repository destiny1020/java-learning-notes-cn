##最后的一个修改(One Final Modification)

在继续讨论新的话题前，为了把问题讲清楚让我们对该例子进行最后一个修改。为每个制造商添加两个指标来计算最低和最高价格：

```json
GET /cars/transactions/_search?search_type=count
{
   "aggs": {
      "colors": {
         "terms": {
            "field": "color"
         },
         "aggs": {
            "avg_price": { "avg": { "field": "price" }
            },
            "make" : {
                "terms" : {
                    "field" : "make"
                },
                "aggs" : { 
                    "min_price" : { "min": { "field": "price"} }, 
                    "max_price" : { "max": { "field": "price"} } 
                }
            }
         }
      }
   }
}
```

我们需要添加另一个aggs层级来进行对min和max的嵌套。

得到的响应如下(仍然有省略)：

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
                        "doc_count": 3,
                        "min_price": {
                           "value": 10000 
                        },
                        "max_price": {
                           "value": 20000 
                        }
                     },
                     {
                        "key": "bmw",
                        "doc_count": 1,
                        "min_price": {
                           "value": 80000
                        },
                        "max_price": {
                           "value": 80000
                        }
                     }
                  ]
               },
               "avg_price": {
                  "value": 32500
               }
            },
...
```

在每个make桶下，多了min和max的指标。

此时，我们可以得到如下信息：

- 有4辆红色汽车。
- 红色汽车的平均价格是32500美刀。
- 红色汽车中的3辆是Honda，1辆是BMW。
- 红色Honda汽车中，最便宜的价格为10000美刀。
- 最贵的红色Honda汽车为20000美刀。
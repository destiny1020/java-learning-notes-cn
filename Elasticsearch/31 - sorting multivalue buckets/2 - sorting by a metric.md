##根据指标排序(Sorting by a Metric)

有时候你会发现你想要根据某个指标的值来排序。对于我们的汽车销售分析仪表板而言，我们也许会创建一个针对汽车颜色的条形图，但是对条(Bars)按照其平均价格的升序进行排序。

我们可以向桶中添加一个指标来实现，然后在order参数中引用该指标：

```json
GET /cars/transactions/_search?search_type=count
{
    "aggs" : {
        "colors" : {
            "terms" : {
              "field" : "color",
              "order": {
                "avg_price" : "asc" 
              }
            },
            "aggs": {
                "avg_price": {
                    "avg": {"field": "price"} 
                }
            }
        }
    }
}
```

通过对指标的名字进行引用，可以让你使用任何指标来覆盖排序规则。对于有些生成多个值的指标，比如extended_stats：它提供了6个独立的指标。

如果你需要根据一个多值指标排序，你可以使用点操作符来指定：

```json
GET /cars/transactions/_search?search_type=count
{
    "aggs" : {
        "colors" : {
            "terms" : {
              "field" : "color",
              "order": {
                "stats.variance" : "asc" 
              }
            },
            "aggs": {
                "stats": {
                    "extended_stats": {"field": "price"}
                }
            }
        }
    }
}
```

在这个例子中，我们对每个桶按其方差来排序，因此拥有较小方差的桶会出现在拥有较大方差的桶之前。
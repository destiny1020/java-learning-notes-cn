##filtered查询

如果你想要找到所有售价高于10000美刀的车，同时也对这些车计算其平均价格，那么可以使用一个filtered查询：

```json
GET /cars/transactions/_search?search_type=count
{
    "query" : {
        "filtered": {
            "filter": {
                "range": {
                    "price": {
                        "gte": 10000
                    }
                }
            }
        }
    },
    "aggs" : {
        "single_avg_price": {
            "avg" : { "field" : "price" }
        }
    }
}
```

从本质上而言，使用filtered查询和使用match查询并无区别，正如我们在上一章所讨论的那样。该查询(包含了一个过滤器)返回文档的一个特定子集，然后聚合工作在该子集上。
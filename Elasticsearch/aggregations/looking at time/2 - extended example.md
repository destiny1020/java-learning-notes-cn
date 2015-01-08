##另外的例子

我们已经看到过很多次，为了实现更复杂的行为，桶可以嵌套在桶中。为了说明这一点，我们会创建一个用来显示每个季度，所有制造商的总销售额的聚合。同时，我们也会在每个季度为每个制造商单独计算其总销售额，因此我们能够知道哪种汽车创造的收益最多：

```json
GET /cars/transactions/_search?search_type=count
{
   "aggs": {
      "sales": {
         "date_histogram": {
            "field": "sold",
            "interval": "quarter", 
            "format": "yyyy-MM-dd",
            "min_doc_count" : 0,
            "extended_bounds" : {
                "min" : "2014-01-01",
                "max" : "2014-12-31"
            }
         },
         "aggs": {
            "per_make_sum": {
               "terms": {
                  "field": "make"
               },
               "aggs": {
                  "sum_price": {
                     "sum": { "field": "price" } 
                  }
               }
            },
            "total_sum": {
               "sum": { "field": "price" } 
            }
         }
      }
   }
}
```

可以发现，interval参数被设成了quarter。

得到的响应如下(删除了很多)：

```json
{
....
"aggregations": {
   "sales": {
      "buckets": [
         {
            "key_as_string": "2014-01-01",
            "key": 1388534400000,
            "doc_count": 2,
            "total_sum": {
               "value": 105000
            },
            "per_make_sum": {
               "buckets": [
                  {
                     "key": "bmw",
                     "doc_count": 1,
                     "sum_price": {
                        "value": 80000
                     }
                  },
                  {
                     "key": "ford",
                     "doc_count": 1,
                     "sum_price": {
                        "value": 25000
                     }
                  }
               ]
            }
         },
...
}
```

我们可以将该响应放入到一个图形中，使用一个线图(Line Chart)来表达总销售额，一个条形图来显示每个制造商的销售额(每个季度)，如下所示：

![](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/images/elas_29in02.png)
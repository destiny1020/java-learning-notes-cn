##返回空桶

发现在上面的响应中的奇怪之处了吗？

Yep, that’s right. We are missing a few months! By default, the date_histogram (and histogram too) returns only buckets that have a nonzero document count.
是的，我们缺失了几个月！默认情况下，date_histogram(以及histogram)只会返回文档数量大于0的桶。

这意味着得到的histogram响应是最小的。但是有些时候该行为并不是我们想要的。对于很多应用而言，你需要将得到的响应直接置入到一个图形库中，而不需要任何额外的处理。

因此本质上，我们需要返回所有的桶，哪怕其中不含有任何文档。我们可以设置两个额外的参数来实现这一行为：

```json
GET /cars/transactions/_search?search_type=count
{
   "aggs": {
      "sales": {
         "date_histogram": {
            "field": "sold",
            "interval": "month",
            "format": "yyyy-MM-dd",
            "min_doc_count" : 0, 
            "extended_bounds" : { 
                "min" : "2014-01-01",
                "max" : "2014-12-31"
            }
         }
      }
   }
}
```

以上的min_doc_count参数会强制返回空桶，extended_bounds参数会强制返回一整年的数据。

这两个参数会强制返回该年中的所有月份，无论它们的文档数量是多少。min_doc_count的意思很容易懂：它强制返回哪怕为空的桶。

extended_bounds参数需要一些解释。min_doc_count会强制返回空桶，但是默认ES只会返回在你的数据中的最小值和最大值之间的桶。

因此如果你的数据分布在四月到七月，你得到的桶只会表示四月到七月中的几个月(可能为空，如果使用了min_doc_count=0)。为了得到一整年的桶，我们需要告诉ES需要得到的桶的范围。

extended_bounds参数就是用来告诉ES这一范围的。一旦你添加了这两个设置，得到的响应就很容易被图形生成库处理而最终得到下图：

![](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/images/elas_29in01.png)


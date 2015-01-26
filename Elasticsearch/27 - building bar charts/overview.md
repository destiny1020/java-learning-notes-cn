#创建条形图(Building Bar Charts)

聚合的一个令人激动的性质是它能够很容易地被转换为图表和图形。在本章中，我们会使用前面的样本数据集来创建出各种分析案例。我们也会展示聚合能够支持的种类。

柱状图桶(Histogram Bucket)非常有用。柱状图在本质上就是条形图，如果你创建过一份报告或者分析面板(Analytics Dashboard)，毫无疑问其中会有一些条形图。柱状图通过指定一个间隔(Interval)来工作。如果我们使用柱状图来表示销售价格，你或许会指定一个值为20000的间隔。因此每20000美刀会创建一个桶。然后文档会被分配到桶中。

对于我们的仪表板，我们想要知道每个价格区间中有多少辆车。同时我们也想知道该价格桶中产生了多少收入。这是通过将该间隔中所有的车的售价累加而计算得到的。

为了达到这一目的，我们使用了一个histogram类型的聚合然后在其中嵌套了一个sum指标：

```json
GET /cars/transactions/_search?search_type=count
{
   "aggs":{
      "price":{
         "histogram":{ 
            "field": "price",
            "interval": 20000
         },
         "aggs":{
            "revenue": {
               "sum": { 
                 "field" : "price"
               }
             }
         }
      }
   }
}
```

正如你能看到的那样，我们的查询是围绕着价格聚合而建立的，该聚合包含了一个柱状图桶。该桶需要一个数值字段以及一个间隔值来进行计算。间隔用来定义每个桶有“多宽”。间隔为20000意味着我们能够拥有区间[0-19999, 20000-39999, 等]。

接下来，我们在柱状图中定义了一个嵌套的指标。它是一个sum类型的指标，会将该区间中的文档的price字段进行累加。这就得到了每个价格区间中的收入，因此我们就能够从中看出是普通车还是豪华车赚的更多。

以下是得到的响应：

```json
{
...
   "aggregations": {
      "price": {
         "buckets": [
            {
               "key": 0,
               "doc_count": 3,
               "revenue": {
                  "value": 37000
               }
            },
            {
               "key": 20000,
               "doc_count": 4,
               "revenue": {
                  "value": 95000
               }
            },
            {
               "key": 80000,
               "doc_count": 1,
               "revenue": {
                  "value": 80000
               }
            }
         ]
      }
   }
}
```

The response is fairly self-explanatory, but it should be noted that the histogram keys correspond to the lower boundary of the interval. The key 0 means 0-19,999, the key 20000 means 20,000-39,999, and so forth.
响应是能够对其意义进行解释的，但是值得注意的是histogram键对应的是间隔的下边界。键值0表示的是0-19999，键值20000表示的是20000-39999，以此类推。

>**NOTE 缺失了空桶**
>
>你也许会注意到40000-60000美刀这一个间隔没有出现在响应中。histogram桶默认会省略它，因为包含空桶可能会造成输出过大，而这可能并不是我们想要的结果。
>
>在下一节中我们会讨论如何包含空桶，[返回空桶](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/_returning_empty_buckets.html)

从图形上，你可以将前面的数据表示如下：

![](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/images/elas_28in01.png)

当然，你可以使用任何生成类别和统计信息的聚合来创建条形图，并不仅限于使用histogram桶。让我们创建一个受欢迎的汽车制造商的条形图，其中包含了它们的平均价格和标准误差(Standard Error)。需要使用的而是terms桶以及一个extended_stats指标：

```json
GET /cars/transactions/_search?search_type=count
{
  "aggs": {
    "makes": {
      "terms": {
        "field": "make",
        "size": 10
      },
      "aggs": {
        "stats": {
          "extended_stats": {
            "field": "price"
          }
        }
      }
    }
  }
}
```

它会返回一个制造商列表(根据受欢迎程度排序)以及针对每个制造商的一些列统计信息。其中，我们对stats.avg，stats.count以及stats.std_deviation感兴趣。有了这一信息，我们能够计算出标准误差：

> std_err = std_deviation / count

得到的图形如下所示：

![](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/images/elas_28in02.png)







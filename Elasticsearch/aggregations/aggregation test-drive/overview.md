#聚合的测试驱动(Aggregation Test-Drive)

我们将学习各种聚合以及它们的语法，但是最好的学习方法还是通过例子。一旦你了解了如何思考聚合以及如何对它们进行合适的嵌套，那么语法本身是不难的。

让我们从一个例子开始。我们会建立一个也许对汽车交易商有所用处的聚合。数据是关于汽车交易的：汽车型号，制造商，销售价格，销售时间以及一些其他的相关数据。

首先，通过批量索引(Bulk-Index)来添加一些数据：

```json
POST /cars/transactions/_bulk
{ "index": {}}
{ "price" : 10000, "color" : "red", "make" : "honda", "sold" : "2014-10-28" }
{ "index": {}}
{ "price" : 20000, "color" : "red", "make" : "honda", "sold" : "2014-11-05" }
{ "index": {}}
{ "price" : 30000, "color" : "green", "make" : "ford", "sold" : "2014-05-18" }
{ "index": {}}
{ "price" : 15000, "color" : "blue", "make" : "toyota", "sold" : "2014-07-02" }
{ "index": {}}
{ "price" : 12000, "color" : "green", "make" : "toyota", "sold" : "2014-08-19" }
{ "index": {}}
{ "price" : 20000, "color" : "red", "make" : "honda", "sold" : "2014-11-05" }
{ "index": {}}
{ "price" : 80000, "color" : "red", "make" : "bmw", "sold" : "2014-01-01" }
{ "index": {}}
{ "price" : 25000, "color" : "blue", "make" : "ford", "sold" : "2014-02-12" }
```

现在我们有了一些数据，来创建一个聚合吧。一个汽车交易商也许希望知道哪种颜色的车卖的最好。这可以通过一个简单的聚合完成。使用terms桶：

```json
GET /cars/transactions/_search?search_type=count 
{
    "aggs" : { 
        "colors" : { 
            "terms" : {
              "field" : "color" 
            }
        }
    }
}
```

因为我们并不关心搜索结果，使用的search_type是count，它的速度更快。
聚合工作在顶层的aggs参数下(当然你也可以使用更长的aggregations)。
然后给这个聚合起了一个名字：colors。
最后，我们定义了一个terms类型的桶，它针对color字段。

聚合是以搜索结果为上下文而执行的，这意味着它是搜索请求(比如，使用/_search端点)中的另一个顶层参数(Top-level Parameter)。聚合可以和查询同时使用，这一点我们在后续的[范围聚合(Scoping Aggregations)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/_scoping_aggregations.html)中介绍。

接下来我们为聚合起一个名字。命名规则是有你决定的; 聚合的响应会被该名字标记，因此在应用中你就能够根据名字来得到聚合结果，并对它们进行操作了。

然后，我们开始定义聚合本身。比如，我们定义了一个terms类型的桶。terms桶会动态地为每一个它遇到的不重复的词条创建一个新的桶。因为我们针对的是color字段，那么terms桶会动态地为每种颜色创建一个新桶。

让我们执行该聚合来看看其结果：

```json
{
...
   "hits": {
      "hits": [] 
   },
   "aggregations": {
      "colors": { 
         "buckets": [
            {
               "key": "red", 
               "doc_count": 4 
            },
            {
               "key": "blue",
               "doc_count": 2
            },
            {
               "key": "green",
               "doc_count": 2
            }
         ]
      }
   }
}
```

因为我们使用的search_type为count，所以没有搜索结果被返回。
每个桶中的key对应的是在color字段中找到的不重复的词条。它同时也包含了一个doc_count，用来表示包含了该词条的文档数量。

响应包含了一个桶列表，每个桶都对应着一个不重复的颜色(比如，红色或者绿色)。每个桶也包含了“掉入”该桶中的文档数量。比如，有4辆红色的车。

前面的例子是完全实时(Real-Time)的：如果文档是可搜索的，那么它们就能够被聚合。这意味着你能够将拿到的聚合结果置入到一个图形库中来生成实时的仪表板(Dashboard)。一旦你卖出了一台银色汽车，在图形上关于银色汽车的统计数据就会被动态地更新。

瞧！你的第一个聚合！

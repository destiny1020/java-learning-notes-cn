#看待时间(Looking at Time)

如果在ES中，搜索是最常见的行为，那么创建日期柱状图(Date Histogram)肯定是第二常见的。为什么要使用日期柱状图呢？

想象在你的数据中有一个时间戳。数据是什么不重要-Apache日志事件，股票交易日期，棒球比赛时间-任何拥有时间戳的数据都能通过日期柱状图受益。当你有时间戳时，你经常会想创建基于时间的指标信息：

- 今年的每个月销售了多少辆车？

- 过去的12小时中，这只股票的价格是多少？ 

- 上周每个小时我们的网站的平均延迟是多少？

常规的histogram通常使用条形图来表示，而date histogram倾向于被装换为线图(Line Graph)来表达时间序列(Time Series)。很多公司使用ES就是为了对时间序列数据进行分析。

date_histogram的工作方式和常规的histogram类似。常规的histogram是基于数值字段来创建数值区间的桶，而date_histogram则是基于时间区间来创建桶。因此每个桶是按照某个特定的日历时间定义的(比如，1个月或者是2.5天)。

>**常规Histogram能够和日期一起使用吗？**
>
>从技术上而言，是可以的。常规的histogram桶可以和日期一起使用。但是，它并懂日期相关的信息(Not calendar-aware)。而对于date_histogram，你可以将间隔(Interval)指定为1个月，它知道2月份比12月份要短。date_histogram还能够和时区一同工作，因此你可以根据用户的时区来对图形进行定制，而不是根据服务器。
>
>常规的histogram会将日期理解为数值，这意味着你必须将间隔以毫秒的形式指定。同时聚合也不理解日历间隔，所以它对于日期几乎是没法使用的。

第一个例子中，我们会创建一个简单的线图(Line Chart)来回答这个问题：每个月销售了多少辆车？

```json
GET /cars/transactions/_search?search_type=count
{
   "aggs": {
      "sales": {
         "date_histogram": {
            "field": "sold",
            "interval": "month", 
            "format": "yyyy-MM-dd" 
         }
      }
   }
}
```

在查询中有一个聚合，它为每个月创建了一个桶。它能够告诉我们每个月销售了多少辆车。同时指定了一个额外的格式参数让桶拥有更"美观"的键值。在内部，日期被简单地表示成数值。然而这会让UI设计师生气，因此使用格式参数可以让日期以更常见的格式进行表示。

得到的响应符合预期，但是也有一点意外(看看你能够察觉到)：

```json
{
   ...
   "aggregations": {
      "sales": {
         "buckets": [
            {
               "key_as_string": "2014-01-01",
               "key": 1388534400000,
               "doc_count": 1
            },
            {
               "key_as_string": "2014-02-01",
               "key": 1391212800000,
               "doc_count": 1
            },
            {
               "key_as_string": "2014-05-01",
               "key": 1398902400000,
               "doc_count": 1
            },
            {
               "key_as_string": "2014-07-01",
               "key": 1404172800000,
               "doc_count": 1
            },
            {
               "key_as_string": "2014-08-01",
               "key": 1406851200000,
               "doc_count": 1
            },
            {
               "key_as_string": "2014-10-01",
               "key": 1412121600000,
               "doc_count": 1
            },
            {
               "key_as_string": "2014-11-01",
               "key": 1414800000000,
               "doc_count": 2
            }
         ]
...
}
```

聚合完整地被表达出来了。你能看到其中有用来表示月份的桶，每个桶中的文档数量，以及漂亮的key_as_string。

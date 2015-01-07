#看待时间(Looking at Time)

如果在ES中，搜索是最常见的行为，那么创建日期柱状图(Date Histogram)肯定是第二常见的。为什么要使用日期柱状图呢？

想象在你的数据中有一个时间戳。数据是什么不重要-Apache日志事件，股票交易日期，棒球比赛时间-任何拥有时间戳的数据都能通过日期柱状图受益。当你有时间戳时，你经常会想创建基于时间的指标信息：

- How many cars sold each month this year?
- 今年的每个月销售了多少辆车？

- What was the price of this stock for the last 12 hours?
- 过去的12小时中，这只股票的价格是多少？ 

- What was the average latency of our website every hour in the last week?
- 上周每个小时我们的网站的平均延迟是多少？

While regular histograms are often represented as bar charts, date histograms tend to be converted into line graphs representing time series. Many companies use Elasticsearch solely for analytics over time series data. The date_histogram bucket is their bread and butter.
常规的histogram通常使用条形图来表示，而date histogram倾向于被装换为线图(Line Graph)来表达时间序列(Time Series)。很多公司使用ES就是为了对时间序列数据进行分析。

The date_histogram bucket works similarly to the regular histogram. Rather than building buckets based on a numeric field representing numeric ranges, it builds buckets based on time ranges. Each bucket is therefore defined as a certain calendar size (for example, 1 month or 2.5 days).

>**Can a Regular Histogram Work with Dates?**
>
>Technically, yes. A regular histogram bucket will work with dates. However, it is not calendar-aware. With the date_histogram, you can specify intervals such as 1 month, which knows that February is shorter than December. The date_histogram also has the advantage of being able to work with time zones, which allows you to customize graphs to the time zone of the user, not the server.
>
>The regular histogram will interpret dates as numbers, which means you must specify intervals in terms of milliseconds. And the aggregation doesn’t know about calendar intervals, which makes it largely useless for dates.

Our first example will build a simple line chart to answer this question: how many cars were sold each month?

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

Our query has a single aggregation, which builds a bucket per month. This will give us the number of cars sold in each month. An additional format parameter is provided so the buckets have "pretty" keys. Internally, dates are simply represented as a numeric value. This tends to make UI designers grumpy, however, so a prettier format can be specified using common date formatting.

The response is both expected and a little surprising (see if you can spot the surprise):

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

The aggregation is represented in full. As you can see, we have buckets that represent months, a count of docs in each month, and our pretty key_as_string.
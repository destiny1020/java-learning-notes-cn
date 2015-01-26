## 精确值字段(Exact-value Fields) ##

在结束对于多字段查询的讨论之前的最后一个话题是作为not_analyzed类型的精确值字段。在multi_match查询中将not_analyzed字段混合到analyzed字段中是没有益处的。

原因可以通过validate-query进行简单地验证，假设我们将title字段设置为not_analyzed：

```json
GET /_validate/query?explain
{
    "query": {
        "multi_match": {
            "query":       "peter smith",
            "type":        "cross_fields",
            "fields":      [ "title", "first_name", "last_name" ]
        }
    }
}
```

因为title字段时没有被解析的，它会以将整个查询字符串作为一个词条进行搜索！

> title:peter smith
> (
>     blended("peter", fields: [first_name, last_name])
>     blended("smith", fields: [first_name, last_name])
> )

很显然该词条在title字段的倒排索引中并不存在，因此永远不可能被找到。在multi_match查询中避免使用not_analyzed字段。
##内部排序(Intrinsic Sorts)

排序模式对于桶而言是内部的：它们工作在桶生成的数据上，比如doc_count。它们有相似的语法，但是根据具体使用的桶的类型会有些许不同。

让我们执行一个以doc_count升序排序的terms聚合：

```json
GET /cars/transactions/_search?search_type=count
{
    "aggs" : {
        "colors" : {
            "terms" : {
              "field" : "color",
              "order": {
                "_count" : "asc" 
              }
            }
        }
    }
}
```

我们向聚合中引入了一个排序对象，它能够让我们对几种值进行排序：

**_count**
根据文档数量排序。在terms，histogram，date_historgram聚合中可以使用。

**_term**
按照一个词条其字符串值的字母表顺序排序。仅在terms聚合中使用。

**_key**
按照每个桶的键的数值(概念上和_term相似)排序。仅在histogram和date_histogram中使用。

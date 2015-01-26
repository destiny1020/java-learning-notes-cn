## multi_match查询 ##

`multi_match`查询提供了一个简便的方法用来对多个字段执行相同的查询。

**NOTE**

> 存在几种类型的`multi_match`查询，其中的3种正好和在["了解你的数据"](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/_single_query_string.html#know-your-data)一节中提到的几种类型相同：`best_fields`，`most_fields`以及`cross_fields`。

默认情况下，该查询以`best_fields`类型执行，它会为每个字段生成一个`match`查询，然后将这些查询包含在一个`dis_max`查询中。下面的`dis_max`查询：

```json
{
  "dis_max": {
    "queries":  [
      {
        "match": {
          "title": {
            "query": "Quick brown fox",
            "minimum_should_match": "30%"
          }
        }
      },
      {
        "match": {
          "body": {
            "query": "Quick brown fox",
            "minimum_should_match": "30%"
          }
        }
      },
    ],
    "tie_breaker": 0.3
  }
}
```

可以通过`multi_match`简单地重写如下：

```json
{
    "multi_match": {
        "query":                "Quick brown fox",
        "type":                 "best_fields", 
        "fields":               [ "title", "body" ],
        "tie_breaker":          0.3,
        "minimum_should_match": "30%" 
    }
}
```

注意到以上的`type`属性为`best_fields`。
`minimum_should_match`和`operator`参数会被传入到生成的`match`查询中。

### 在字段名中使用通配符 ###

字段名可以通过通配符指定：任何匹配了通配符的字段都会被包含在搜索中。你可以通过下面的查询来匹配`book_title`，`chapter_title`以及`section_title`字段：

```json
{
    "multi_match": {
        "query":  "Quick brown fox",
        "fields": "*_title"
    }
}
```

### 提升个别字段 ###

个别字段可以通过caret语法(`^`)进行提升：仅需要在字段名后添加`^boost`，其中的`boost`是一个浮点数：

```json
{
    "multi_match": {
        "query":  "Quick brown fox",
        "fields": [ "*_title", "chapter_title^2" ] 
    }
}
```

`chapter_title`字段的`boost`值为`2`，而`book_title`和`section_title`字段的`boost`值为默认的`1`。

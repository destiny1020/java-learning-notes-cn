## match查询是如何使用bool查询的 ##

现在，你也许意识到了使用了[`match`查询的多词查询](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/match-multi-word.html)只是简单地将生成的`term`查询包含在了一个`bool`查询中。通过默认的`or`操作符，每个`term`查询都以一个k语句被添加，所以至少一个`should`语句需要被匹配。以下两个查询是等价的：

```json
{
    "match": { "title": "brown fox"}
}

{
  "bool": {
    "should": [
      { "term": { "title": "brown" }},
      { "term": { "title": "fox"   }}
    ]
  }
}
```

使用`and`操作符时，所有的`term`查询都以`must`语句被添加，因此所有的查询都需要匹配。以下两个查询是等价的：

```json
{
    "match": {
        "title": {
            "query":    "brown fox",
            "operator": "and"
        }
    }
}

{
  "bool": {
    "must": [
      { "term": { "title": "brown" }},
      { "term": { "title": "fox"   }}
    ]
  }
}
```

如果指定了`minimum_should_match`参数，它会直接被传入到`bool`查询中，因此下面两个查询是等价的：

```json
{
    "match": {
        "title": {
            "query":                "quick brown fox",
            "minimum_should_match": "75%"
        }
    }
}

{
  "bool": {
    "should": [
      { "term": { "title": "brown" }},
      { "term": { "title": "fox"   }},
      { "term": { "title": "quick" }}
    ],
    "minimum_should_match": 2 
  }
}
```

因为只有3个查询语句，`minimum_should_match`的值`75%`会被向下舍入到`2`。即至少两个should语句需要匹配。

当然，我们可以通过`match`查询来编写这类查询，但是理解`match`查询的内部工作原理能够让你根据需要来控制该过程。有些行为无法通过一个`match`查询完成，比如对部分查询词条给予更多的权重。在下一节中我们会看到一个例子。


## 跨字段实体搜索(Cross-fields Entity Search) ##

现在让我们看看一个常见的模式：跨字段实体搜索。类似person，product或者address这样的实体，它们的信息会分散到多个字段中。我们或许有一个person实体被索引如下：

```json
{
    "firstname":  "Peter",
    "lastname":   "Smith"
}
```

而address实体则是像下面这样：

```json
{
    "street":   "5 Poland Street",
    "city":     "London",
    "country":  "United Kingdom",
    "postcode": "W1V 3DG"
}
```

这个例子也许很像在[多查询字符串](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/multi-query-strings.html)中描述的，但是有一个显著的区别。在[多查询字符串](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/multi-query-strings.html)中，我们对每个字段都使用了不同的查询字符串。在这个例子中，我们希望使用一个查询字符串来搜索多个字段。

用户也许会搜索名为"Peter Smith"的人，或者名为"Poland Street W1V"的地址。每个查询的单词都出现在不同的字段中，因此使用dis_max/best_fields查询来搜索单个最佳匹配字段显然是不对的。

### 一个简单的方法 ###

实际上，我们想要依次查询每个字段然后将每个匹配字段的分值进行累加，这听起来很像bool查询能够胜任的工作：

```json
{
  "query": {
    "bool": {
      "should": [
        { "match": { "street":    "Poland Street W1V" }},
        { "match": { "city":      "Poland Street W1V" }},
        { "match": { "country":   "Poland Street W1V" }},
        { "match": { "postcode":  "Poland Street W1V" }}
      ]
    }
  }
}
```

对每个字段重复查询字符串很快就会显得冗长。我们可以使用multi_match查询进行替代，然后将type设置为most_fields来让它将所有匹配字段的分值合并：

```json
{
  "query": {
    "multi_match": {
      "query":       "Poland Street W1V",
      "type":        "most_fields",
      "fields":      [ "street", "city", "country", "postcode" ]
    }
  }
}
```

### 使用most_fields存在的问题 ###

使用most_fields方法执行实体查询有一些不那么明显的问题：

- 它被设计用来找到匹配任意单词的多数字段，而不是找到跨越所有字段的最匹配的单词。
- 它不能使用operator或者minimum_should_match参数来减少低相关度结果带来的长尾效应。
- 每个字段的词条频度是不同的，会互相干扰最终得到较差的排序结果。







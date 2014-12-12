## 根据人气来提升(Boosting by Popularity) ##

假设我们有一个博客网站让用户投票选择他们喜欢的文章。我们希望让人气高的文章出现在结果列表的头部，但是主要的排序依据仍然是全文搜索分值。我们可以通过保存每篇文章的投票数量来实现：

```json
PUT /blogposts/post/1
{
  "title":   "About popularity",
  "content": "In this post we will talk about...",
  "votes":   6
}
```

在搜索期间，使用带有field_value_factor函数的function_score查询将投票数和全文相关度分值结合起来：

```json
GET /blogposts/post/_search
{
  "query": {
    "function_score": { 
      "query": { 
        "multi_match": {
          "query":    "popularity",
          "fields": [ "title", "content" ]
        }
      },
      "field_value_factor": { 
        "field": "votes" 
      }
    }
  }
}
```

function_score查询会包含主查询(Main Query)和希望适用的函数。先会执行主查询，然后再为匹配的文档调用相应的函数。每份文档中都必须有一个votes字段用来保证function_score能够起作用。

在前面的例子中，每份文档的最终_score会通过下面的方式改变：

> new_score = old_score * number_of_votes

它得到的结果并不好。全文搜索的_score通常会在0到10之间。而从下图我们可以发现，拥有10票的文章的分值大大超过了这个范围，而没有被投票的文章的分值会被重置为0。

![](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/images/elas_1701.png)

**modifier**

为了让votes值对最终分值的影响更缓和，我们可以使用modifier。换言之，我们需要让头几票的效果更明显，其后的票的影响逐渐减小。0票和1票的区别应该比10票和11票的区别要大的多。

一个用于此场景的典型modifier是log1p，它将公式改成这样：

> new_score = old_score * log(1 + number_of_votes)

log函数将votes字段的效果减缓了，其效果类似下面的曲线：

![](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/images/elas_1702.png)

使用了modifier参数的请求如下：

```json
GET /blogposts/post/_search
{
  "query": {
    "function_score": {
      "query": {
        "multi_match": {
          "query":    "popularity",
          "fields": [ "title", "content" ]
        }
      },
      "field_value_factor": {
        "field":    "votes",
        "modifier": "log1p" 
      }
    }
  }
}
```

可用的modifiers有：none(默认值)，log，log1p，log2p，ln，ln1p，ln2p，square，sqrt以及reciprocal。它们的详细功能和用法可以参考[field_value_factor文档](http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl-function-score-query.html#_field_value_factor)。

**factor**

可以通过将votes字段的值乘以某个数值来增加该字段的影响力，这个数值被称为factor：

```json
GET /blogposts/post/_search
{
  "query": {
    "function_score": {
      "query": {
        "multi_match": {
          "query":    "popularity",
          "fields": [ "title", "content" ]
        }
      },
      "field_value_factor": {
        "field":    "votes",
        "modifier": "log1p",
        "factor":   2 
      }
    }
  }
}
```

添加了factor将公式修改成这样：

> new_score = old_score * log(1 + factor * number_of_votes)

当factor大于1时，会增加其影响力，而小于1的factor则相应减小了其影响力，如下图所示：

![](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/images/elas_1703.png)

**boost_mode**

将全文搜索的相关度分值乘以field_value_factor函数的结果，对最终分值的影响可能太大了。通过boost_mode参数，我们可以控制函数的结果应该如何与_score结合在一起，该参数接受下面的值：

- multiply：_score乘以函数结果(默认情况)
- sum：_score加上函数结果
- min：_score和函数结果的较小值
- max：_score和函数结果的较大值
- replace：将_score替换成函数结果




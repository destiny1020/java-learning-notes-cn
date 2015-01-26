## 最佳字段查询的调优 ##

如果用户搜索的是"quick pets"，那么会发生什么呢？两份文档都包含了单词`quick`，但是只有文档2包含了单词`pets`。两份文档都没能在一个字段中同时包含搜索的两个单词。

一个像下面那样的简单`dis_max`查询会选择出拥有最佳匹配字段的查询子句，而忽略其他的查询子句：

```json
{
    "query": {
        "dis_max": {
            "queries": [
                { "match": { "title": "Quick pets" }},
                { "match": { "body":  "Quick pets" }}
            ]
        }
    }
}
```

```json
{
  "hits": [
     {
        "_id": "1",
        "_score": 0.12713557, 
        "_source": {
           "title": "Quick brown rabbits",
           "body": "Brown rabbits are commonly seen."
        }
     },
     {
        "_id": "2",
        "_score": 0.12713557, 
        "_source": {
           "title": "Keeping pets healthy",
           "body": "My quick brown fox eats rabbits on a regular basis."
        }
     }
   ]
}
```

可以发现，两份文档的分值是一模一样的。

我们期望的是同时匹配了`title`字段和`body`字段的文档能够拥有更高的排名，但是结果并非如此。需要记住：`dis_max`查询只是简单的使用最佳匹配查询子句得到的`_score`。

### tie_breaker ###

但是，将其它匹配的查询子句考虑进来也是可能的。通过指定`tie_breaker`参数：

```json
{
    "query": {
        "dis_max": {
            "queries": [
                { "match": { "title": "Quick pets" }},
                { "match": { "body":  "Quick pets" }}
            ],
            "tie_breaker": 0.3
        }
    }
}
```

它会返回以下结果：

```json
{
  "hits": [
     {
        "_id": "2",
        "_score": 0.14757764, 
        "_source": {
           "title": "Keeping pets healthy",
           "body": "My quick brown fox eats rabbits on a regular basis."
        }
     },
     {
        "_id": "1",
        "_score": 0.124275915, 
        "_source": {
           "title": "Quick brown rabbits",
           "body": "Brown rabbits are commonly seen."
        }
     }
   ]
}
```

现在文档2的分值比文档1稍高一些。

`tie_breaker`参数会让`dis_max`查询的行为更像是`dis_max`和`bool`的一种折中。它会通过下面的方式改变分值计算过程：

1. 取得最佳匹配查询子句的`_score`。
2. 将其它每个匹配的子句的分值乘以`tie_breaker`。
3. 将以上得到的分值进行累加并规范化。

通过`tie_breaker`参数，所有匹配的子句都会起作用，只不过最佳匹配子句的作用更大。

> **NOTE**
> 
> `tie_breaker`的取值范围是`0`到`1`之间的浮点数，取`0`时即为仅使用最佳匹配子句(译注：和不使用`tie_breaker`参数的`dis_max`查询效果相同)，取`1`则会将所有匹配的子句一视同仁。它的确切值需要根据你的数据和查询进行调整，但是一个合理的值会靠近`0`，(比如，`0.1` -` 0.4`)，来确保不会压倒`dis_max`查询具有的最佳匹配性质。





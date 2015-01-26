## 混合起来(Mixing it up) ##

精确短语(Exact-phrase)匹配也许太过于严格了。也许我们希望含有"quick brown fox"的文档也能够匹配"quick fox"查询，即使位置并不是完全相等的。

我们可以在短语匹配使用slop参数来引入一些灵活性：

```json
GET /my_index/my_type/_search
{
    "query": {
        "match_phrase": {
            "title": {
                "query": "quick fox",
                "slop":  1
            }
        }
    }
}
```

slop参数告诉match_phrase查询词条能够相隔多远时仍然将文档视为匹配。相隔多远的意思是，你需要移动一个词条多少次来让查询和文档匹配？

我们以一个简单的例子来阐述这个概念。为了让查询quick fox能够匹配含有quick brown fox的文档，我们需要slop的值为1：

```
            Pos 1         Pos 2         Pos 3
-----------------------------------------------
Doc:        quick         brown         fox
-----------------------------------------------
Query:      quick         fox
Slop 1:     quick                 ↳     fox
```

尽管在使用了slop的短语匹配中，所有的单词都需要出现，但是单词的出现顺序可以不同。如果slop的值足够大，那么单词的顺序可以是任意的。

为了让fox quick查询能够匹配我们的文档，需要slop的值为3：

```
            Pos 1         Pos 2         Pos 3
-----------------------------------------------
Doc:        quick         brown         fox
-----------------------------------------------
Query:      fox           quick
Slop 1:     fox|quick  ↵  
Slop 2:     quick      ↳  fox
Slop 3:     quick                 ↳     fox
```


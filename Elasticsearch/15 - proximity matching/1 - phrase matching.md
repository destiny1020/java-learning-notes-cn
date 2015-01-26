## 短语匹配(Phrase Matching) ##

就像一提到全文搜索会首先想到match查询一样，当你需要寻找邻近的几个单词时，你会使用match_phrase查询：

```json
GET /my_index/my_type/_search
{
    "query": {
        "match_phrase": {
            "title": "quick brown fox"
        }
    }
}
```

和match查询类似，match_phrase查询首先解析查询字符串来产生一个词条列表。然后会搜索所有的词条，但只保留含有了所有搜索词条的文档，并且词条的位置要邻接。一个针对短语quick fox的查询不会匹配我们的任何文档，因为没有文档含有邻接在一起的quick和box词条。

> **TIP**
> 
> match_phrase查询也可以写成类型为phrase的match查询：
> 
> ```json
> "match": {
>     "title": {
>         "query": "quick brown fox",
>         "type":  "phrase"
>     }
> }
> ```

### 词条位置 ###

当一个字符串被解析时，解析器不仅只返回一个词条列表，它同时也返回每个词条的位置，或者顺序信息：

```json
GET /_analyze?analyzer=standard
Quick brown fox
```

会返回以下的结果：

```json
{
   "tokens": [
      {
         "token": "quick",
         "start_offset": 0,
         "end_offset": 5,
         "type": "<ALPHANUM>",
         "position": 1 
      },
      {
         "token": "brown",
         "start_offset": 6,
         "end_offset": 11,
         "type": "<ALPHANUM>",
         "position": 2 
      },
      {
         "token": "fox",
         "start_offset": 12,
         "end_offset": 15,
         "type": "<ALPHANUM>",
         "position": 3 
      }
   ]
}
```

位置信息可以被保存在倒排索引(Inverted Index)中，像match_phrase这样位置感知(Position-aware)的查询能够使用位置信息来匹配那些含有正确单词出现顺序的文档，在这些单词间没有插入别的单词。

### 短语是什么 ###

对于匹配了短语"quick brown fox"的文档，下面的条件必须为true：

- quick，brown和fox必须全部出现在某个字段中。
- brown的位置必须比quick的位置大1。
- fox的位置必须比quick的位置大2。

如果以上的任何条件没有被满足，那么文档就不能被匹配。

> **TIP**
> 
> 在内部，match_phrase查询使用了低级的span查询族(Query Family)来执行位置感知的查询。span查询是词条级别的查询，因此它们没有解析阶段(Analysis Phase)；它们直接搜索精确的词条。
> 
> 幸运的是，大多数用户几乎不需要直接使用span查询，因为match_phrase查询通常已经够好了。但是，对于某些特别的字段，比如专利搜索(Patent Search)，会使用这些低级查询来执行拥有非常特别构造的位置搜索。



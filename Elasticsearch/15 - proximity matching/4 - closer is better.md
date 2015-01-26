## 越近越好(Closer is better) ##

短语查询(Phrase Query)只是简单地将不含有精确查询短语的文档排除在外，而邻近查询(Proximity Query) - 一个slop值大于0的短语查询 - 会将查询词条的邻近度也考虑到最终的相关度_score中。通过设置一个像50或100这样的高slop值，你可以排除那些单词过远的文档，但是也给予了那些单词邻近的文档一个更高的分值。

下面针对quick dog的邻近查询匹配了含有quick和dog的两份文档，但是给与了quick和dog更加邻近的文档一个更高的分值：

```json
POST /my_index/my_type/_search
{
   "query": {
      "match_phrase": {
         "title": {
            "query": "quick dog",
            "slop":  50 
         }
      }
   }
}
```

```json
{
  "hits": [
     {
        "_id":      "3",
        "_score":   0.75, 
        "_source": {
           "title": "The quick brown fox jumps over the quick dog"
        }
     },
     {
        "_id":      "2",
        "_score":   0.28347334, 
        "_source": {
           "title": "The quick brown fox jumps over the lazy dog"
        }
     }
  ]
}
```


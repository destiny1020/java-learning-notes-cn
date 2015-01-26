# 排序和相关度(Sorting and Relevance) #

## 排序 ##

### 根据字段进行排序 ###

默认Elasticsearch会将文档按照其_score进行降序排序。但是在有些情况下，返回的文档并没有一个有意义的_score值，比如当使用match_all查询时返回的所有文档的_score都等于1。此时，我们就可以通过sort参数来显式指定返回文档的顺序了：

```
# Return all docs for user 1 sorted by `date`
GET /test/_search
{
  "query": {
    "filtered": {
      "filter": {
        "term": {
          "user_id": 1
        }
      }
    }
  },
  "sort": {
    "date": {
      "order": "desc"
    }
  }
}
```

此时，相关度的计算是不会被执行的，因为在query中已经指定了排序的规则。没有必要再对相关度进行计算。当然，此时仍然可以通过设置track_scores=true来强制对相关度进行计算。

一种简便的声明排序方式的方式：

```
"sort": "number_of_children"
```

此时排序的默认方向是升序。即按照number_of_children字段的值进行升序排序。

### 多级排序(Multi-level Sorting) ###

比如首先按照日期进行降序排序，再按照_score进行降序排序：

```
GET /_search
{
    "query" : {
        "filtered" : {
            "query":   { "match": { "tweet": "manage text search" }},
            "filter" : { "term" : { "user_id" : 2 }}
        }
    },
    "sort": [
        { "date":   { "order": "desc" }},
        { "_score": { "order": "desc" }}
    ]
}
```

使用查询字符串同样能够制定排序字段：

```
GET /_search?sort=date:desc&sort=_score&q=search
```

### 基于多值字段排序 ###

因为多值字段的值是没有顺序的，所以将它们作为排序字段的时候要如何处理呢？

对于number和date类型的字段，可以通过使用min，max，avg或者sum排序模式(sort mode)来将多个值归约成一个值之后再进行排序：

```
"sort": {
    "dates": {
        "order": "asc",
        "mode":  "min"
    }
}
```

## 字符串排序和多值字段 ##

对于需要被解析的字符串，它们在索引中也是一个多值字段。对于多值字段排序往往得不得你想要的结果。当需要对字符串字段进行排序时，需要将它的index属性设置为not_analyzed。但是，该字段仍然需要能够被全文搜索。所以一种解决方法是，对该字段使用不同的索引属性进行两次索引(原始数据仍然只被保存一次)：

```
"tweet": { 
    "type":     "string",
    "analyzer": "english",
    "fields": {
        "raw": { 
            "type":  "string",
            "index": "not_analyzed"
        }
    }
}
```

tweet字段本身和以前并无二致，仍然是使用english解析器并且index属性被设置为analyzed。增加的部分是fields属性中的raw对象。它代表了一个新的字段：tweet.raw，它的index被设置为not_analyzed。这样，就可以通过tweet.raw进行排序了：

```
GET /_search
{
    "query": {
        "match": {
            "tweet": "elasticsearch"
        }
    },
    "sort": "tweet.raw"
}
```

另外，如果对一个解析了的字符串字段进行排序会消耗很多内存。

## 相关度(Relevance)是什么 ## (完全翻译)

我们提到过在默认情况下结果会按照相关度进行降序排序。但是相关度是什么？如何计算它？

每个文档的相关度通过一个名为_score的正浮点数进行表示。_score越高，就表示该文档的相关度越强。

对于每个文档，查询语句都会为它产生一个_score。这个分数的计算是基于查询语句的种类 - 不同的查询语句旨在处理不同的问题：一个fuzzy查询通过比较原始的查询词条和搜索到的词条在拼写上的相似度来计算_score，一个terms查询会考虑找到词条的百分比，但是当我们提到相关度时，我们通常指的是用来计算全文查询字符串和全文字段内容之间的相似度时所使用的算法。

在Elasticsearch中使用的标准相似度算法是词条频度/倒排文档频度(Term Frequency/Inverse Document Frequency, TF/IDF)，该算法将以下的因素考虑在内：

- 词条频度(Term Frequency)
	词条在字段中出现的越频繁，相关度越高。在一个字段中，同一个词条出现了5次比另一个字段中，同一词条只出现了1次的相关度要高。

- 倒排文档频度(Inverse Document Frequency)
	每个词条在索引中的出现的越频繁，相关度越低。在很多文档中都会出现的常见词条相比于不那么常见的词条而言，其权重较低。

- 字段长度归一化(Field Length Norm)
	字段长度越长，那么其中的单词和查询关键字的相关程度就越低(译注：长文档对于单个词条相关度的稀释作用)。所以当一个词条出现在较短的title字段中时，其权重要比它出现在较长的content字段中时要大。

查询可能会结合通过TF/IDF得到的相关度分值和其他的因素。比如在短语查询(Phrase Query)中，会考虑词条亲近度(Term Proximity)；在模糊查询(Fuzzy Query)中，会考虑词条相似度(Tern Similarity)。

相关度也并不是只和全文搜索有关。它还能够被用到返回yes|no的语句中，有越多的语句匹配，那么_score的值也就越高。

当多个查询语句通过类似bool查询这种复合查询进行组合时，每个查询语句得到的_score会被合并来得到该文档最终的_score。

### 理解score的意义 ###

在调试一个复杂的查询时，很难理解_score到底是如何被算出来的。为了处理这个问题，Elasticsearch可以为每个查询结果提供一个解释，通过设置explain为true来完成：

```
GET /_search?explain 
{
   "query"   : { "match" : { "tweet" : "honeymoon" }}
}
```

运行这段查询，能够得到结果的元数据：

```
{
    "_index" :      "us",
    "_type" :       "tweet",
    "_id" :         "12",
    "_score" :      0.076713204,
    "_source" :     { ... trimmed ... },
	"_shard" :      1,
    "_node" :       "mzIVYCsqSWCG_M_ZffSs9Q",
	"_explanation": { 
    "description": "weight(tweet:honeymoon in 0)
	                  [PerFieldSimilarity], result of:",
	   "value":       0.076713204,
	   "details": [
	      {
	         "description": "fieldWeight in 0, product of:",
	         "value":       0.076713204,
	         "details": [
	            {
	               "description": "tf(freq=1.0), with freq of:",
	               "value":       1,
	               "details": [
	                  {
	                     "description": "termFreq=1.0",
	                     "value":       1
	                  }
	               ]
	            },
	            { 
	               "description": "idf(docFreq=1, maxDocs=1)",
	               "value":       0.30685282
	            },
	            { 
	               "description": "fieldNorm(doc=0)",
	               "value":        0.25,
	            }
	         ]
	      }
	   ]
	}
}
```

启用explain参数后会带来额外开销，因此它只是一个调试工具，不要在生产环境中使用它。

返回的响应中的第一部分是关于此次计算的一个概要。它告诉我们，通过对0号文档(这是一个内部使用的文档ID编号，这里可以忽略)的tweet针对honeymoon这一词条进行全文搜索后，得到的相关度信息。

同时注意到以上的details数组中存在三个对象，分别表示的是前文中提到的TF/IDF算法考虑的三个方面。

- 词条频度(Term Frequency)
	此文档中，词条honeymoon在tweet字段中的出现了多少次？

- 倒排文档频度(Inverse Document Frequency)
	在索引的所有文档中，词条honeymoon在tweet字段中出现了多少次？

- 字段长度归一化(Field Length Norm)
	此文档中，tweet字段的长度如何？长度越长，那么该数值就越小。

复杂的查询返回的解释也会非常复杂，但是本质上它们也只是包含了以上所解释的几种信息。在调试阶段，这些信息对于解释排序顺序是非常宝贵的。

**小技巧**
使用了explain后的JSON输出比较难以阅读，YAML格式会稍好一些，使用format=yaml来指定它。

### 理解一个文档为何会被匹配 ###

使用explain选项能够为每个结果作出解释，使用explain API则可以让你知道为何一个文档能够匹配以及更重要地，为何不能匹配：

```
# Delete the `test` index
DELETE /test

# Insert example doc
PUT /test/tweet/1
{
  "date": "2014-09-22",
  "name": "John Smith",
  "tweet": "Elasticsearch and I have left the honeymoon stage, and I still love her.",
  "user_id": 1
}

# Use the explain API to figure out why
# this document doesn't match
GET /us/tweet/12/_explain
{
   "query" : {
      "filtered" : {
         "filter" : { "term" :  { "user_id" : 2           }},
         "query" :  { "match" : { "tweet" :   "honeymoon" }}
      }
   }
}
```

此时得到的响应中除了包含以上介绍的所有解释信息外，还有一个description字段：

```
"description": "failure to match filter: cache(user_id:[2 TO 2])"
```

它的意思是，user_id过滤器阻止了文档被匹配。(因为没有user_id为2的文档)
	






























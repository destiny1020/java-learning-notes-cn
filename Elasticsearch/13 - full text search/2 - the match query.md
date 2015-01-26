## `match`查询 ##

在你需要对任何字段进行查询时，`match`查询应该是你的首选。它是一个高级全文查询，意味着它知道如何处理全文字段(Full-text, `analyzed`)和精确值字段(Exact-value，`not_analyzed`)。

即便如此，`match`查询的主要使用场景仍然是全文搜索。让我们通过一个简单的例子来看看全文搜索时如何工作的。

**索引一些数据**

首先，我们会创建一个新的索引并通过[`bulk` API](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/bulk.html)索引一些文档：

```json
DELETE /my_index 

PUT /my_index
{ "settings": { "number_of_shards": 1 }} 

POST /my_index/my_type/_bulk
{ "index": { "_id": 1 }}
{ "title": "The quick brown fox" }
{ "index": { "_id": 2 }}
{ "title": "The quick brown fox jumps over the lazy dog" }
{ "index": { "_id": 3 }}
{ "title": "The quick brown fox jumps over the quick dog" }
{ "index": { "_id": 4 }}
{ "title": "Brown fox brown dog" }
```

注意到以上在创建索引时，我们设置了`number_of_shards`为1：在稍后的[相关度坏掉了(Relevance is broken)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/relevance-is-broken.html)一节中，我们会解释为何这里创建了一个只有一个主分片(Primary shard)的索引。

**单词查询(Single word query)**

第一个例子我们会解释在使用`match`查询在一个全文字段中搜索一个单词时，会发生什么：

```json
GET /my_index/my_type/_search
{
    "query": {
        "match": {
            "title": "QUICK!"
        }
    }
}
```

ES会按照如下的方式执行上面的`match`查询：

1. **检查字段类型**

	`title`字段是一个全文字符串字段(`analyzed`)，意味着查询字符串也需要被分析。

2. **解析查询字符串**

	查询字符串`"QUICK!"`会被传入到标准解析器中，得到的结果是单一词条`"quick"`。因为我们得到的只有一个词条，`match`查询会使用一个`term`低级查询来执行查询。

3. **找到匹配的文档**

	`term`查询会在倒排索引中查询`"quick"`，然后获取到含有该词条的文档列表，在这个例子中，文档`1`，`2`，`3`会被返回。

4. **对每份文档打分**

	`term`查询会为每份匹配的文档计算其相关度分值`_score`，该分值通过综合考虑词条频度(Term Frequency)(`"quick"`在匹配的每份文档的`title`字段中出现的频繁程度)，倒排频度(Inverted Document Frequency)(`"quick"`在整个索引中的所有文档的`title`字段中的出现程度)，以及每个字段的长度(较短的字段会被认为相关度更高)来得到。参考[什么是相关度(What is Relevance?)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/relevance-intro.html)

这个过程会给我们下面的结果(有省略)：

```json
"hits": [
 {
    "_id":      "1",
    "_score":   0.5, 
    "_source": {
       "title": "The quick brown fox"
    }
 },
 {
    "_id":      "3",
    "_score":   0.44194174, 
    "_source": {
       "title": "The quick brown fox jumps over the quick dog"
    }
 },
 {
    "_id":      "2",
    "_score":   0.3125, 
    "_source": {
       "title": "The quick brown fox jumps over the lazy dog"
    }
 }
]
```

文档1最相关，因为它的`title`字段短，意味着`quick`在它所表达的内容中占比较大。
文档3比文档2的相关度更高，因为`quick`出现了两次。


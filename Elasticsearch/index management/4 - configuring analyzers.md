## 配置解析器 ##

第三个重要的索引设置就是 analysis 区域。用来配置用到的现有的解析器或者创建你自己的解析器。

解析器，用来将全文字符串转换成倒排索引，以方便进行搜索。

默认对于全文字符字段使用的是standard解析器，它对于多数西方语言而言是一个不错的选择。它包括：

- standard分词器。根据词语的边界进行分词。
- standard token过滤器。用来整理上一步分词器得到的tokens，但是目前是一个空操作(no-op)。
- lowercase token过滤器。将所有tokens转换为小写。
- stop token过滤器。移除所有的stopword，比如a，the，and，is等

默认下stopwords过滤器没有被使用，可以通过配置来启用。可以通过提供一个stopword列表或者指定一个预先定义的stopword语言来完成：

```
PUT /spanish_docs
{
    "settings": {
        "analysis": {
            "analyzer": {
                "es_std": {
                    "type":      "standard",
                    "stopwords": "_spanish_"
                }
            }
        }
    }
}
```

以上定义了一个es_std解析器，它使用西班牙语中定义的stopword列表。

这个解析器也不是全局的，它只作用于spanish_docs这个索引之上。可以这样测试：

```
# Delete the `spanish_docs` index
DELETE /spanish_docs

# Configuring an analyzer to use Spanish stopwords
PUT /spanish_docs
{
    "settings": {
        "analysis": {
            "analyzer": {
                "es_std": {
                    "type":      "standard",
                    "stopwords": "_spanish_"
                }
            }
        }
    }
}

# Test out the new analyzer
GET /spanish_docs/_analyze?analyzer=es_std
{
    El veloz zorro marrón"
}
```

响应如下：

```
{
   "tokens": [
      {
         "token": "veloz",
         "start_offset": 9,
         "end_offset": 14,
         "type": "<ALPHANUM>",
         "position": 2
      },
      {
         "token": "zorro",
         "start_offset": 15,
         "end_offset": 20,
         "type": "<ALPHANUM>",
         "position": 3
      },
      {
         "token": "marrón",
         "start_offset": 21,
         "end_offset": 27,
         "type": "<ALPHANUM>",
         "position": 4
      }
   ]
}
```
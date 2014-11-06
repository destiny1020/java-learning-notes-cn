## 自定义解析器 ##

通过自定义：

- character filters
- tokenizer
- token filters

来完成解析器的定义。

- 字符过滤器
	比如html_strip字符过滤器，会将所有的HTML标签移除，并将&Aacute;转换为Á

- 分词器
	必须有且只有一个分词器。典型的如standard分词器，通过对单词边界进行划分来得到词条，会移除大部分的标点符号。另外，还有keyword分词器，它直接原样输出。whitespace分词器则只通过对空白字符进行划分来得到词条。pattern分词器根据RE来进行分词。

- 词条过滤器
	典型的如lowercase和stopword filters，其他还有stemming token filter。ascii_folding filter用来removes diacritics, converting a term like "très" into "tres"。还有适合在部分匹配和自动完成场景下使用的ngram和edage_ngram token过滤器。

### 创建一个自定义的解析器 ###

```
PUT /my_index
{
    "settings": {
        "analysis": {
            "char_filter": { ... custom character filters ... },
            "tokenizer":   { ...    custom tokenizers     ... },
            "filter":      { ...   custom token filters   ... },
            "analyzer":    { ...    custom analyzers      ... }
        }
    }
}
```

比如，要创建拥有如下功能的解析器：

1. 使用html_strip完成HTML标签的剔除
2. 将&字符替换成" and "，使用一个自定义的mapping字符过滤器

```
"char_filter": {
    "&_to_and": {
        "type":       "mapping",
        "mappings": [ "&=> and "]
    }
}
```

3. 使用标准分词器对文本进行分词。standard analyzer
4. 使用lowercase token过滤器将所有词条转换为小写
5. 移除一个自定义的stopword列表，使用自定义的stop token filter

```
"filter": {
    "my_stopwords": {
        "type":        "stop",
        "stopwords": [ "the", "a" ]
    }
}
```

解析器就可以定义成：

```
"analyzer": {
    "my_analyzer": {
        "type":           "custom",
        "char_filter":  [ "html_strip", "&_to_and" ],
        "tokenizer":      "standard",
        "filter":       [ "lowercase", "my_stopwords" ]
    }
}
```

测试数据：

```
# Delete the `my_index` index
DELETE /my_index

# Create a custom analyzer
PUT /my_index
{
  "settings": {
    "analysis": {
      "char_filter": {
        "&_to_and": {
          "type": "mapping",
          "mappings": [
            "&=> and "
          ]
        }
      },
      "filter": {
        "my_stopwords": {
          "type": "stop",
          "stopwords": [
            "the",
            "a"
          ]
        }
      },
      "analyzer": {
        "my_analyzer": {
          "type": "custom",
          "char_filter": [
            "html_strip",
            "&_to_and"
          ],
          "tokenizer": "standard",
          "filter": [
            "lowercase",
            "my_stopwords"
          ]
        }
      }
    }
  }
}

# Test out the new analyzer
GET /my_index/_analyze?analyzer=my_analyzer&text=The quick %26 brown fox

# Apply "my_analyzer" to the `title` field
PUT /my_index/_mapping/my_type
{
  "properties": {
    "title": {
      "type": "string",
      "analyzer": "my_analyzer"
    }
  }
}

```




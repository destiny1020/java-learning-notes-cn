## 自定义解析器(Custom Analyzers) ##

虽然ES本身已经提供了一些解析器，但是通过组合字符过滤器(Character Filter)，分词器(Tokenizer)以及词条过滤器(Token Filter)来创建你自己的解析器才会显示出其威力。

在[解析和解析器](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/analysis-intro.html)中，我们提到过解析器(Analyzer)就是将3种功能打包得到的，它会按照下面的顺序执行：

- **字符过滤器(Character Filter)**
	字符过滤器用来在分词前将字符串进行"整理"。比如，如果文本是HTML格式，那么它会含有类似`<p>`或者`<div>`这样的HTML标签，但是这些标签我们是不需要索引的。我们可以使用[`html_strip`字符过滤器](http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.4//analysis-htmlstrip-charfilter.html)移除所有的HTML标签，并将所有的像&Aacute;这样的HTML实体(HTML Entity)转换为对应的Unicode字符：Á。

- **分词器(Tokenizers)**
	一个解析器必须有一个分词器。分词器将字符串分解成一个个单独的词条(Term or Token)。在`standard`解析器中使用的[`standard`分词器](http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.4//analysis-standard-tokenizer.html)，通过单词边界对字符串进行划分来得到词条，同时会移除大部分的标点符号。另外还有其他的分词器拥有着不同的行为。

	比如[`keyword`分词器](http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.4//analysis-keyword-tokenizer.html)，它不会进行任何分词，直接原样输出。[`whitespace`分词器](http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.4//analysis-whitespace-tokenizer.html)则只通过对空白字符进行划分来得到词条。而[`pattern`分词器](http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.4//analysis-pattern-tokenizer.html)则根据正则表达式来进行分词。

- **词条过滤器(Token Filter)**
	在分词后，得到的词条流(Token Stream)会按照顺序被传入到指定的词条过滤器中。

	词条过滤器能够修改，增加或者删除词条。我们已经提到了[`lowercase`词条过滤器](http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.4//analysis-lowercase-tokenfilter.html)和[`stop`词条过滤器](http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.4//analysis-stop-tokenfilter.html)，但是ES中还有许多其它可用的词条过滤器。[`stemming`词条过滤器](http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.4//analysis-stemmer-tokenfilter.html)会对单词进行词干提取来得到其词根形态(Root Form)。[`ascii_folding`词条过滤器](http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.4//analysis-asciifolding-tokenfilter.html)则会移除变音符号(Diacritics)，将类似于`très`的词条转换成`tres`。[`ngram`词条过滤器](http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.4//analysis-ngram-tokenfilter.html)和[`edge_ngram`词条过滤器](http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.4//analysis-edgengram-tokenfilter.html)会产生适用于部分匹配(Partial Matching)或者自动完成(Autocomplete)的词条。

在[深入搜索](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/search-in-depth.html)中，我们会通过例子来讨论这些分词器和过滤器的使用场景和使用方法。但是首先，我们需要解释如何来创建一个自定义的解析器。

TODO

### 创建一个自定义的解析器 ###

和上面我们配置`es_std`解析器的方式相同，我们可以在`analysis`下对字符过滤器，分词器和词条过滤器进行配置：

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

1. 使用`html_strip`字符过滤器完成HTML标签的移除。
2. 将&字符替换成" and "，使用一个自定义的`mapping`字符过滤器。

```
"char_filter": {
    "&_to_and": {
        "type":       "mapping",
        "mappings": [ "&=> and "]
    }
}
```

3. 使用`standard`分词器对文本进行分词。
4. 使用`lowercase`词条过滤器将所有词条转换为小写。
5. 使用一个自定义的stopword列表，并通过自定义的stop词条过滤器将它们移除：

```
"filter": {
    "my_stopwords": {
        "type":        "stop",
        "stopwords": [ "the", "a" ]
    }
}
```

我们的解析器将预先定义的分词器和过滤器和自定义的过滤器进行了结合：

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

因此，整个`create-index`请求就像下面这样：

```
PUT /my_index
{
    "settings": {
        "analysis": {
            "char_filter": {
                "&_to_and": {
                    "type":       "mapping",
                    "mappings": [ "&=> and "]
            }},
            "filter": {
                "my_stopwords": {
                    "type":       "stop",
                    "stopwords": [ "the", "a" ]
            }},
            "analyzer": {
                "my_analyzer": {
                    "type":         "custom",
                    "char_filter":  [ "html_strip", "&_to_and" ],
                    "tokenizer":    "standard",
                    "filter":       [ "lowercase", "my_stopwords" ]
            }}
}}}
```

创建索引之后，使用`analyze` API对新的解析器进行测试：

```
GET /my_index/_analyze?analyzer=my_analyzer
The quick & brown fox
```

得到的部分结果如下，表明我们的解析器能够正常工作：

```
{
  "tokens" : [
      { "token" :   "quick",    "position" : 2 },
      { "token" :   "and",      "position" : 3 },
      { "token" :   "brown",    "position" : 4 },
      { "token" :   "fox",      "position" : 5 }
    ]
}
```

我们需要告诉ES这个解析器应该在什么地方使用。我们可以将它应用在`string`字段的映射中：

```
PUT /my_index/_mapping/my_type
{
    "properties": {
        "title": {
            "type":      "string",
            "analyzer":  "my_analyzer"
        }
    }
}
```
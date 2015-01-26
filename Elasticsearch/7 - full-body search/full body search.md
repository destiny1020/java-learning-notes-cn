## 查询和过滤器(Queries and Filters) ##

Although we refer to the Query DSL, in reality there are two DSLs: the Query DSL and the Filter DSL. Query clauses and filter clauses are similar in nature, but have slightly different purposes.

A filter asks a yes|no question of every document and is used for fields that contain exact values:

is the created date in the range 2013 .. 2014?
does the status field contain the term "published"?
is the lat_lon field within 10km of a specified point?

A query is similar to a filter, but also asks the question: How well does this document match?

Typical uses for a query would be to find documents:

that best match the words: full text search
that contain the word run, but may also match runs, running, jog or sprint
containing the words quick, brown and fox --- the closer together they are, the more relevant the document
tagged with lucene, search or java — the more tags, the more relevant the document

查询最后得到每个文档关于该查询的一个分数，被保存到_score中。这个值会在最后被排序。

### 性能差异 ###

过滤器的性能往往更好，它们本身能够被缓存，而且计算过程也相对简单。

而查询不仅仅需要去匹配文档，还需要计算文档的相关度(_score)。同时，查询的结果还不能被缓存。尽管有倒排索引(Inverted Index)的帮助，过滤器的性能仍然要普遍优于查询。

过滤器的目的是减少在查询阶段需要被检查的文档数量。

### 如何选择 ###

对于全文搜索和对最终的相关度分值有影响的任何条件，使用查询。除此之外使用过滤器。

## 最重要的查询和过滤器 ##

使用的测试数据如下：

```
# Delete the `test` index
DELETE /test

# Insert some examples
PUT /test/test/1
{
  "title": "About search",
  "age": 26,
  "date": "2014-09-01",
  "tag": [
    "full_text",
    "search"
  ],
  "public": false
}

PUT /test/test/2
{
  "age": 38,
  "date": "2014-09-02",
  "tag": [
    "full_text",
    "nosql"
  ],
  "public": true
}
```

### term过滤器 ###

用来根据精确值(Exact Value)进行过滤。通常针对index属性为not_analyzed的字段，比如number，boolean，date以及设置为not_analyzed的string字段。

```
# Where `age` is 26
GET /test/test/_search
{
  "query": {
    "filtered": {
      "filter": {
        "term": {
          "age": 26
        }
      }
    }
  }
}

# Where `tag` is "full_text"
GET /test/test/_search
{
  "query": {
    "filtered": {
      "filter": {
        "term": {
          "tag": "full_text"
        }
      }
    }
  }
}
```

对于tag的过滤，如果目标文档中的tag数组中含有过滤器中指定的tag，那么该文档就会被置于结果集合中。

### terms过滤器 ###

和term过滤器类似。但是它能指定多个值：

```
# Where `tag` contains:
# "search", "full_text" or "nosql"
GET /test/test/_search
{
  "query": {
    "filtered": {
      "filter": {
        "terms": {
          "tag": [
            "search",
            "full_text",
            "nosql"
          ]
        }
      }
    }
  }
}
```

同样，只要目标文档中含有任意一个过滤器中指定的tag就算过滤成功。

### range过滤器 ###

可以针对number和date字段进行过滤：

```
# Where `age` >= 20 and < 30
GET /test/test/_search
{
  "query": {
    "filtered": {
      "filter": {
        "range": {
          "age": {
            "gte": 20,
            "lt": 30
          }
        }
      }
    }
  }
}
```

- gt：大于
- gte：大于等于
- lt：小于
- lte：小于等于

### exists和missing过滤器 ###

```
# Where `title` field exists
GET /test/test/_search
{
  "query": {
    "filtered": {
      "filter": {
        "exists": {
          "field": "title"
        }
      }
    }
  }
}

# Where `title` field is missing
GET /test/test/_search
{
  "query": {
    "filtered": {
      "filter": {
        "missing": {
          "field": "title"
        }
      }
    }
  }
}
```

顾名思义，exists过滤器只会得到那些存在目标字段的文档。而missing过滤器则只会得到那些不存在目标字段的文档。它们的作用类似于SQL中的IS NULL和NOT NULL。

### bool过滤器 ###

它用来合并多个过滤器，它能够接受三个参数：

- must：执行AND逻辑，表示必须匹配
- must_not：执行NOT逻辑，表示必须不匹配
- should：执行OR逻辑，表示可以匹配也可以不匹配

```
# Where `folder` is "inbox"
# and `tag` is not spam
# and either `starred` or `unread` is true
GET /test/test/_search
{
  "query": {
    "filtered": {
      "filter": {
        "bool": {
          "must": {
            "term": {
              "folder": "inbox"
            }
          },
          "must_not": {
            "term": {
              "tag": "spam"
            }
          },
          "should": [
            {
              "term": {
                "starred": true
              }
            },
            {
              "term": {
                "unread": true
              }
            }
          ]
        }
      }
    }
  }
}
```

### match_all查询 ###

它对所有文档进行匹配，是默认的查询方式。它会将所有文档的相关度分值设置为1，因为它认为所有的文档时同等重要的。通常会将它和过滤器结合使用来缩小目标文档的数量。

```
# Match all documents
GET /test/test/_search
{
  "query": {
    "match_all": {}
  }
}
```

### match查询 ###

在需要进行全文搜索或者精确搜索时，都可以选择match查询。
它能够根据目标字段的index属性是analyzed或者not_analyzed来决定查询的执行方式：

```
# Where `title` includes "about" or "search"
GET /test/test/_search
{
  "query": {
    "match": {
      "title": "About Search!"
    }
  }
}


# Where `age` is 26
GET /test/test/_search
{
  "query": {
    "match": {
      "age": 26
    }
  }
}

# Where `date` is "2014-09-01"
GET /test/test/_search
{
  "query": {
    "match": {
      "date": "2014-09-01"
    }
  }
}

# Where `public` is true
GET /test/test/_search
{
  "query": {
    "match": {
      "public": true
    }
  }
}

# Where `tag` is "full_text"
GET /test/test/_search
{
  "query": {
    "match": {
      "tag": "full_text"
    }
  }
}
```

实际上，对于精确搜索可以选择term过滤器。因为它的性能更优。

### multi_match查询 ###

在多个字段执行相同的match查询。

```
# Match "full text search" in the `title` or `body`
GET /_search
{
  "query": {
    "multi_match": {
      "query": "full text search",
      "fields": [
        "title",
        "body"
      ]
    }
  }
}
```

### bool查询 ###

和bool过滤器类似，但是略有不同。
过滤器会给出yes或者no的答案，而查询则会给出一个相关度分值：

- must：定义必须匹配的条件
- must_not：定义必须不匹配的条件
- should：定义可选的条件。如果匹配了则会增加_score，不匹配则对_score无影响。是为了让相关度分值更加准确的条件

```
# Match "how to make millions" in the `title`
# and `tag` must not include "spam"
# and either `tag` should include "starred"
# or `date` must be >= "2014-01-01"
GET /test/test/_search
{
  "query": {
    "bool": {
      "must": {
        "match": {
          "title": "how to make millions"
        }
      },
      "must_not": {
        "match": {
          "tag": "spam"
        }
      },
      "should": [
        {
          "match": {
            "tag": "starred"
          }
        },
        {
          "range": {
            "date": {
              "gte": "2014-01-01"
            }
          }
        }
      ]
    }
  }
}
```

如果在bool查询中没有使用must条件，那么至少一个should条件被匹配。如果使用了must条件，那么可以没有should条件被匹配。

## 结合过滤器和查询 ##

### 过滤一个查询 ###

比如我们想将下面的match查询和term过滤器结合起来：

```
// query
{ "match": { "email": "business opportunity" }}

// filter
{ "term": { "folder": "inbox" }}
```

然而因为search API只能接受一个query参数，所以我们必须使用filtered查询来包含该查询和需要结合的过滤器：

```
{
    "filtered": {
        "query":  { "match": { "email": "business opportunity" }},
        "filter": { "term":  { "folder": "inbox" }}
    }
}
```

然后我们就可以将该filtered查询作为query参数的值：

```
GET /_search
{
    "query": {
        "filtered": {
            "query":  { "match": { "email": "business opportunity" }},
            "filter": { "term": { "folder": "inbox" }}
        }
    }
}
```

### 仅使用一个过滤器 ###

在filtered查询中也可以仅仅指定过滤器而不指定查询：

```
GET /_search
{
    "query": {
        "filtered": {
            "filter":   { "term": { "folder": "inbox" }}
        }
    }
}
```

前面介绍过match_all查询时在没有指定查询时使用的默认查询，因此上面的请求实质上和下面的请求是等价的：

```
GET /_search
{
    "query": {
        "filtered": {
            "query":    { "match_all": {}},
            "filter":   { "term": { "folder": "inbox" }}
        }
    }
}
```

### 将查询作为过滤器 ###

有时候在filter上下文环境中需要使用查询。这个时候可以使用query过滤器，该过滤器会包含一个查询。下面的查询在bool过滤器中使用了query过滤器，该查询是一个match查询：

```
GET /_search
{
    "query": {
        "filtered": {
            "filter":   {
                "bool": {
                    "must":     { "term":  { "folder": "inbox" }},
                    "must_not": {
                        "query": { 
                            "match": { "email": "urgent business proposal" }
                        }
                    }
                }
            }
        }
    }
}
```

一般情况下，不需要使用query过滤器。只有当你需要在filter上下文环境中需要全文搜索功能时它才派的上用场。

## 验证查询(Validating Queries) ##

使用validate API完成对查询的验证：

```
GET /gb/tweet/_validate/query
{
  "query": {
    "tweet": {
      "match": "really powerful"
    }
  }
}

# Validate query with explanation
GET /gb/tweet/_validate/query?explain
{
  "query": {
    "tweet": {
      "match": "really powerful"
    }
  }
}
```

还可以附加上explain来让Elasticsearch给出具体的说明，比如运行上面的查询会得到：

```
{
  "valid" :     false,
  "_shards" :   { ... },
  "explanations" : [ {
    "index" :   "gb",
    "valid" :   false,
    "error" :   "org.elasticsearch.index.query.QueryParsingException:
                 [gb] No query registered for [tweet]"
  } ]
}
```

从error字段我们就可以知道错误的原因了，tweet和match的顺序声明反了，tweet这种类型的查询时不存在的。

### 理解查询 ###

使用validate API还能够得到查询的具体信息：

```
{
  "valid" :         true,
  "_shards" :       { ... },
  "explanations" : [ {
    "index" :       "us",
    "valid" :       true,
    "explanation" : "tweet:really tweet:powerful"
  }, {
    "index" :       "gb",
    "valid" :       true,
    "explanation" : "tweet:realli tweet:power"
  } ]
}
```

因为在us和gb索引中对于tweet字段使用的analyzer分别是标准analyzer和English analyzer，所以上面的explanation字段也有了区别。

## 小结 ##

search和query DSL的介绍。下一章介绍排序和相关度的计算过程。








































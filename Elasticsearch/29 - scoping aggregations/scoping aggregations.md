#聚合作用域(Scoping Aggregations)

到现在给出的聚合例子中，你可能已经发现了在搜索请求中我们省略了query子句。整个请求只是一个简单的聚合。

聚合可以和搜索请求一起运行，但是你需要理解一个新概念：作用域(Scope)。默认情况下，聚合和查询使用相同的作用域。换句话说，聚合作于匹配了查询的文档集。

让我们看看之前的一个聚合例子：

```json
GET /cars/transactions/_search?search_type=count
{
    "aggs" : {
        "colors" : {
            "terms" : {
              "field" : "color"
            }
        }
    }
}
```

你可以发现聚合是孤立存在的。实际上，在ES中"不指定查询"和"查询所有文档"是等价的。前述查询在内部会被转换如下：

```json
GET /cars/transactions/_search?search_type=count
{
    "query" : {
        "match_all" : {}
    },
    "aggs" : {
        "colors" : {
            "terms" : {
              "field" : "color"
            }
        }
    }
}
```

聚合总是在查询的作用域下工作的，因此一个孤立的聚合实际上工作在match_all查询的作用域 - 即所有文档。

一旦了解了这一点，我们就可以开始对聚合进行定制了。前面的所有例子都计算了关于所有数据的统计信息：最热卖的车，所有车的平均价格，每个月的最大销售额等等。

有了作用域，我们可以问这种问题”Ford汽车有几种可选的颜色？“，通过向请求中添加一个查询来完成(使用match查询)：

```json
GET /cars/transactions/_search  
{
    "query" : {
        "match" : {
            "make" : "ford"
        }
    },
    "aggs" : {
        "colors" : {
            "terms" : {
              "field" : "color"
            }
        }
    }
}
```

通过省略search_type=count，我们可以得到搜索结果以及聚合结果如下：

```json
{
...
   "hits": {
      "total": 2,
      "max_score": 1.6931472,
      "hits": [
         {
            "_source": {
               "price": 25000,
               "color": "blue",
               "make": "ford",
               "sold": "2014-02-12"
            }
         },
         {
            "_source": {
               "price": 30000,
               "color": "green",
               "make": "ford",
               "sold": "2014-05-18"
            }
         }
      ]
   },
   "aggregations": {
      "colors": {
         "buckets": [
            {
               "key": "blue",
               "doc_count": 1
            },
            {
               "key": "green",
               "doc_count": 1
            }
         ]
      }
   }
}
```

这看起来没什么，但是它是生成更加强大的高级仪表板的关键。你可以通过添加一个搜索栏将任何静态的仪表板转换成一个实时数据浏览工具。这让用户能够搜索词条然后看到所有实时图表(它们由聚合提供支持，使用查询的作用域)。用Hadoop来实现试试看！

##全局桶(Global Bucket)

你经常需要你的聚合和查询拥有相同的作用域。但是有时你也需要搜索数据的一个子集，而在所有数据上进行聚合。

比如，你想要知道Ford车相较所有车的平均价格。我们可以使用一个通常的聚合(作用域和查询相同)来得到Ford车的平均价格。而所有车的平均价格则可以通过全局桶来得到。

全局桶会包含你的所有文档，无论查询作用域是什么; 它完全绕开了作用域。由于它是一个桶，你仍然可以在其中嵌入聚合：

```json
GET /cars/transactions/_search?search_type=count
{
    "query" : {
        "match" : {
            "make" : "ford"
        }
    },
    "aggs" : {
        "single_avg_price": {
            "avg" : { "field" : "price" } 
        },
        "all": {
            "global" : {}, 
            "aggs" : {
                "avg_price": {
                    "avg" : { "field" : "price" } 
                }

            }
        }
    }
}
```

该聚合作用在查询作用域中(比如，所有匹配了ford的文档); 全局桶没有任何参数; 聚合在所有文档上操作，无论制造商是哪一个。

single_avg_price指标基于查询作用域(即所有ford车)完成计算。avg_price是一个嵌套在全局桶中的指标，意味着他会忽略作用与的概念，而针对所有文档完成计算。该聚合得到的平均值代表了所有车的平均价格。

如果你已经读到了本书的这个地方，你会认识到这一真言：在任何可以使用过滤器(Filter)的地方使用它。这一点对于聚合同样适用，在下一章中我们会介绍如果对聚合进行过滤，而不是仅仅对查询作用域作出限制。

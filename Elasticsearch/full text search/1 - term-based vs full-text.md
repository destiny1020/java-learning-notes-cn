## 基于词条(Term-based)和全文(Full-text) ##

尽管所有的查询都会执行某种程度的相关度计算，并不是所有的查询都存在解析阶段。除了诸如`bool`或者`function_score`这类完全不对文本进行操作的特殊查询外，对于文本的查询可以被划分两个种类：

**基于词条的查询(Term-based Queries)**

类似`term`和`fuzzy`的查询是不含有解析阶段的低级查询(Low-level Queries)。它们在单一词条上进行操作。一个针对词条`Foo`的`term`查询会在倒排索引中寻找该词条的精确匹配(Exact term)，然后对每一份含有该词条的文档通过TF/IDF进行相关度`_score`的计算。

尤其需要记住的是`term`查询只会在倒排索引中寻找该词条的精确匹配 - 它不会匹配诸如`foo`或者`FOO`这样的变体。它不在意词条是如何被保存到索引中。如果你索引了`["Foo", "Bar"]`到一个`not_analyzed`字段中，或者将`Foo Bar`索引到一个使用`whitespace`解析器的解析字段(Analyzed Field)中，它们都会在倒排索引中得到两个词条：`"Foo"`以及`"Bar"`。
	
**全文查询(Full-text Queries)**

类似`match`或者`query_string`这样的查询是高级查询(High-level Queries)，它们能够理解一个字段的映射：

- 如果你使用它们去查询一个`date`或者`integer`字段，它们会将查询字符串分别当做日期或者整型数。
- 如果你查询一个精确值(`not_analyzed`)字符串字段，它们会将整个查询字符串当做一个单独的词条。
- 但是如果你查询了一个全文字段(`analyzed`)，它们会首先将查询字符串传入到合适的解析器，用来得到需要查询的词条列表。

一旦查询得到了一个词条列表，它就会使用列表中的每个词条来执行合适的低级查询，然后将得到的结果进行合并，最终产生每份文档的相关度分值。
	
我们会在后续章节中详细讨论这个过程。

-----

在很少的情况下，你才需要直接使用基于词条的查询(Term-based Queries)。通常你需要查询的是全文，而不是独立的词条，而这个工作通过高级的全文查询来完成会更加容易(在内部它们最终还是使用的基于词条的低级查询)。

如果你发现你确实需要在一个`not_analyzed`字段上查询一个精确值，那么考虑一下你是否真的需要使用查询，而不是使用过滤器。

单词条查询通常都代表了一个二元的`yes|no`问题，这类问题通常使用过滤器进行表达更合适，因此它们也能够得益于[过滤器缓存(Filter Caching)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/filter-caching.html)：

```json
GET /_search
{
    "query": {
        "filtered": {
            "filter": {
                "term": { "gender": "female" }
            }
        }
    }
}
```

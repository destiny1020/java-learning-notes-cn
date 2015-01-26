## 以字段为中心的查询(Field-centric Queries) ##

上述提到的三个问题都来源于most_fields是以字段为中心(Field-centric)，而不是以词条为中心(Term-centric)：它会查询最多匹配的字段(Most matching fields)，而我们真正感兴趣的最匹配的词条(Most matching terms)。

> **NOTE**
> 
> best_fields同样是以字段为中心的，因此它也存在相似的问题。

首先我们来看看为什么存在这些问题，以及如何解决它们。

### 问题1：在多个字段中匹配相同的单词 ###

考虑一下most_fields查询是如何执行的：ES会为每个字段生成一个match查询，让后将它们包含在一个bool查询中。

我们可以将查询传入到validate-query API中进行查看：

```json
GET /_validate/query?explain
{
  "query": {
    "multi_match": {
      "query":   "Poland Street W1V",
      "type":    "most_fields",
      "fields":  [ "street", "city", "country", "postcode" ]
    }
  }
}
```

它会产生下面的解释(explaination)：

> (street:poland   street:street   street:w1v)
> (city:poland     city:street     city:w1v)
> (country:poland  country:street  country:w1v)
> (postcode:poland postcode:street postcode:w1v)

你可以发现能够在两个字段中匹配poland的文档会比在一个字段中匹配了poland和street的文档的分值要高。

### 问题2：减少长尾 ###

在[精度控制(Controlling Precision)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/match-multi-word.html#match-precision)一节中，我们讨论了如何使用and操作符和minimum_should_match参数来减少相关度低的文档数量：

```json
{
    "query": {
        "multi_match": {
            "query":       "Poland Street W1V",
            "type":        "most_fields",
            "operator":    "and", 
            "fields":      [ "street", "city", "country", "postcode" ]
        }
    }
}
```

但是，使用best_fields或者most_fields，这些参数会被传递到生成的match查询中。该查询的解释如下(译注：通过validate-query API)：

> (+street:poland   +street:street   +street:w1v)
> (+city:poland     +city:street     +city:w1v)
> (+country:poland  +country:street  +country:w1v)
> (+postcode:poland +postcode:street +postcode:w1v)

换言之，使用and操作符时，所有的单词都需要出现在相同的字段中，这显然是错的！这样做可能不会有任何匹配的文档。

### 问题3：词条频度 ###

在[什么是相关度(What is Relevance)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/relevance-intro.html)一节中，我们解释了默认用来计算每个词条的相关度分值的相似度算法TF/IDF：

**词条频度(Term Frequency)**

	在一份文档中，一个词条在一个字段中出现的越频繁，文档的相关度就越高。

**倒排文档频度(Inverse Document Frequency)**

	一个词条在索引的所有文档的字段中出现的越频繁，词条的相关度就越低。

当通过多字段进行搜索时，TF/IDF会产生一些令人惊讶的结果。

考虑使用first_name和last_name字段搜索"Peter Smith"的例子。Peter是一个常见的名字，Smith是一个常见的姓氏 - 它们的IDF都较低。但是如果在索引中有另外一个名为Smith Williams的人呢？Smith作为名字是非常罕见的，因此它的IDF值会很高！

像下面这样的一个简单查询会将Smith Williams放在Peter Smith前面(译注：含有Smith Williams的文档分值比含有Peter Smith的文档分值高)，尽管Peter Smith明显是更好的匹配：

```json
{
    "query": {
        "multi_match": {
            "query":       "Peter Smith",
            "type":        "most_fields",
            "fields":      [ "*_name" ]
        }
    }
}
```

smith在first_name字段中的高IDF值会压倒peter在first_name字段和smith在last_name字段中的两个低IDF值。

### 解决方案 ###

这个问题仅在我们处理多字段时存在。如果我们将所有这些字段合并到一个字段中，该问题就不复存在了。我们可以向person文档中添加一个full_name字段来实现：

```json
{
    "first_name":  "Peter",
    "last_name":   "Smith",
    "full_name":   "Peter Smith"
}
```

当我们只查询full_name字段时：

- 拥有更多匹配单词的文档会胜过那些重复出现一个单词的文档。
- minimum_should_match和operator参数能够正常工作。
- first_name和last_name的倒排文档频度会被合并，因此smith无论是first_name还是last_name都不再重要。

尽管这种方法能工作，可是我们并不想存储冗余数据。因此，ES为我们提供了两个解决方案 - 一个在索引期间，一个在搜索期间。下一节对它们进行讨论。



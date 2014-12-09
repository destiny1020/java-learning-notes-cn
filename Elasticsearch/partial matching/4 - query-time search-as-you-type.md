## 查询期间的即时搜索(Query-time Search-as-you-type) ##

现在让我们来看看前缀匹配能够如何帮助全文搜索。用户已经习惯于在完成输入之前就看到搜索结果了 - 这被称为即时搜索(Instant Search, 或者Search-as-you-type)。这不仅让用户能够在更短的时间内看到搜索结果，也能够引导他们得到真实存在于我们的索引中的结果。

比如，如果用户输入了johnnie walker bl，我们会在用户输入完成前显示Johnnie Walker Black Label和Johnnie Walker Blue Label相关的结果。

和往常一样，有多种方式可以达到我们的目的！首先我们从最简单的方式开始。你不需要以任何的方式准备你的数据，就能够在任何全文字段(Full-text Field)上实现即时搜索。

在[短语匹配(Phrase Matching)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/phrase-matching.html)中，我们介绍了match_phrase查询，它能够根据单词顺序来匹配所有的指定的单词。对于查询期间的即时搜索，我们可以使用该查询的一个特例，即match_phrase_prefix查询：

```json
{
    "match_phrase_prefix" : {
        "brand" : "johnnie walker bl"
    }
}
```

次查询和match_phrase查询的工作方式基本相同，除了它会将查询字符串中的最后一个单词当做一个前缀。换言之，前面的例子会查找以下内容：

- johnnie
- 紧接着的是walker
- 紧接着的是以bl开头的单词

如果我们将该查询通过validate-query API执行，它会产生如下的解释：

"johnnie walker bl*"

和match_phrase查询一样，它能够接受一个slop参数(参见[这里](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/slop.html))来让单词间的顺序和相对位置不那么严格：

```json
{
    "match_phrase_prefix" : {
        "brand" : {
            "query": "walker johnnie bl", 
            "slop":  10
        }
    }
}
```

但是，查询字符串中的最后一个单词总是会被当做一个前缀。

在之前介绍[prefix查询](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/prefix-query.html)的时候，我们谈到了prefix查询的一些需要注意的地方 - prefix查询时如何消耗资源的。在使用match_phrase_prefix查询的时候，也面临着同样的问题。一个前缀a你能够匹配非常非常多的词条。匹配这么多的词条不仅会消耗很多资源，同时对于用户而言也是没有多少用处的。

我们可以通过将参数max_expansions设置成一个合理的数值来限制前缀扩展(Prefix Expansion)的影响，比如50：

```json
{
    "match_phrase_prefix" : {
        "brand" : {
            "query":          "johnnie walker bl",
            "max_expansions": 50
        }
    }
}
```

max_expansions参数会控制能够匹配该前缀的词条的数量。它会找到首个以bl开头的词条然后开始收集(以字母表顺序)直到所有以bl开头的词条都被遍历了或者得到了比max_expansions更多的词条。

不要忘了在用户每敲入一个字符的时候，该查询就要被执行一次，因此它的速度需要快。如果第一个结果集不符合用户的期望，那么他们就会继续输入直到得到他们需要的结果。



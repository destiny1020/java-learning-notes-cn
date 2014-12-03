## 相关度出问题了(Relevance is Broken) ##

在继续讨论更加复杂的[多字段查询](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/multi-field-search.html)前，让我们先快速地解释一下为何将[测试索引创建成只有一个主分片的索引](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/match-query.html#match-test-data)。

经常有新的用户反映称相关度出问题了，并且提供了一个简短的重现：用户索引了一些文档，运行了一个简单的查询，然后发现相关度低的结果明显地出现在了相关度高的结果的前面。

为了弄明白它发生的原因，让我们假设我们创建了一个拥有两个主分片的索引，并且索引了10份文档，其中的6份含有单词foo。在分片1上可能保存了包含单词foo的3份文档，在分片2上保存了另外3份。换言之，文档被均匀的分布在分片上。

在[什么是相关度](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/relevance-intro.html)一节中，我们描述了在ES中默认使用的相似度算法 - 即词条频度/倒排文档频度(Term Frequency/Inverse Document Frequency，TF/IDF)。词条频度计算词条在当前文档的对应字段中出现的次数。词条出现的次数越多，那么该文档的相关度就越高。倒排文档频度将一个词条在索引的所有文档中出现程度以百分比的方式考虑在内。词条出现的越频繁，那么它的权重就越小。

但是，因为性能上的原因，ES不会计算索引中所有文档的IDF。相反，每个分片会为其中的文档计算一个本地的IDF。

因为我们的文档被均匀地分布了，两个分片上计算得到的IDF应该是相同的。现在想象一下如果含有foo的5份文档被保存在了分片1上，而只有1份含有`foo`的文档被保存在了分片2上。在这种情况下，词条`foo`在分片1上就是一个非常常见的词条(重要性很低)，但是在分片2上，它是非常少见的词条(重要性很高)。因此，这些IDF的差异就会导致错误的结果。

实际情况下，这并不是一个问题。当你向索引中添加的文档越多，本地IDF和全局IDF之间的差异就会逐渐减小。考虑到真实的世界中的数据量，本地IDF很快就会变的正常。问题不是相关度，而是数据量太小了。

对于测试，有两种方法可以规避该问题。第一种方法是创建只有一个主分片的索引，正如我们在介绍[`match`查询](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/match-query.html)时做的那样。如果你只有一个分片，那么本地IDF就是全局IDF。

第二种方法是在搜索请求中添加`?search_type=dfs_query_then_fetch`。`dfs表`示分布频度搜索(Distributed Frequency Search)，它会告诉ES首先从每个分片中获取本地IDF，然后计算整个索引上的全局IDF。

> **TIP**
> 
> 不要在生产环境中使用`dfs_query_then_fetch`。它真的是不必要的。拥有足够的数据就能够确保你的词条频度会被均匀地分布。没有必要再为每个查询添加这个额外的DFS步骤。


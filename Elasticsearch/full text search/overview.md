# 全文搜索(Full Text Search) #

现在我们已经讨论了搜索结构化数据的一些简单用例，是时候开始探索全文搜索了 - 如何在全文字段中搜索来找到最相关的文档。

对于全文搜索而言，最重要的两个方面是：

**相关度(Relevance)**

	查询的结果按照它们对查询本身的相关度进行排序的能力，相关度可以通过TF/IDF(参见[什么是相关度](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/relevance-intro.html))，地理位置的邻近程度(Proximity to a Geo-location)，模糊相似性(Fuzzy Similarity)或者其它算法进行计算。

**解析(Analysis)**
	
	解析用来将一块文本转换成单独的，规范化的词条(Tokens)(参见[解析和解析器(Analysis and Analyzers)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/analysis-intro.html))，用来完成：(a)倒排索引(Inverted Index)的创建；(b)倒排索引的查询。

一旦我们开始讨论相关度或者解析，也就意味着我们踏入了查询(Query)的领域，而不再是过滤器(Filter)。


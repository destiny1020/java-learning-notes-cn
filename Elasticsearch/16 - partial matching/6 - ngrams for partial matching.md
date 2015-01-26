## 部分匹配(Partial Matching)的ngrams ##

我们说过："你只能找到存在于倒排索引中的词条"。尽管prefix，wildcard以及regexp查询证明了上面的说法并不是一定正确，但是执行一个基于单个词条的查询会比遍历词条列表来得到匹配的词条要更快是毫无疑问的。为了部分匹配而提前准备你的数据能够增加搜索性能。

在索引期间准别数据意味着选择正确的分析链(Analysis Chain)，为了部分匹配我们选择的工具叫做n-gram。一个n-gram可以被想象成一个单词上的滑动窗口(Moving Window)。n表示的是长度。如果我们对单词quick得到n-gram，结果取决于选择的长度：

- 长度1(unigram)： [ q, u, i, c, k ]
- 长度2(bigram)： [ qu, ui, ic, ck ]
- 长度3(trigram)： [ qui, uic, ick ]
- 长度4(four-gram)：[ quic, uick ]
- 长度5(five-gram)：[ quick ]

单纯的n-grams对于匹配单词中的某一部分是有用的，在[复合单词的ngrams](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/ngrams-compound-words.html)中我们会用到它。然而，对于即时搜索，我们使用了一种特殊的n-grams，被称为边缘n-grams(Edge n-grams)。边缘n-grams会将起始点放在单词的开头处。单词quick的边缘n-gram如下所示：

- q
- qu
- qui
- quic
- quick

你也许注意到它遵循了用户在搜索"quick"时的输入形式。换言之，对于即时搜索而言它们是非常完美的词条。


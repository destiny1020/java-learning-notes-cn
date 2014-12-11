## 相关度分值计算背后的理论 ##

Lucene(也就是ES)使用了[布尔模型(Boolean Model)](http://en.wikipedia.org/wiki/Standard_Boolean_model)来寻找匹配的文档，以及一个被称为[Prarical Scoring Function](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/practical-scoring-function.html)的公式来计算相关度。该公式借用了词条频度/倒排文档频度以及向量空间模型的概念，同时也增加了一些更现代的特性比如Coordination Factor，字段长度归约，以及词条/查询子句提升。

> **NOTE**
> 
> 不要害怕！这些概念并不像它们的名字那般复杂。尽管在这一节中提到了算法，公式以及数学模型，它们的作用也只不过是方便人类理解。理解算法本身并不比理解它们对结果的影响更重要。

### 布尔模型(Boolean Model) ###

布尔模型简单地应用查询中的AND，OR以及NOT条件来寻找所有匹配的文档。下面的查询：

> full AND text AND search AND (elasticsearch OR lucene)

会只包括含有所有full，text，search词条，以及词条elasticsearch或者lucene之一的文档。

这个过程是简单且迅速的。它用来排除那些不会匹配查询的文档。

### 词条频度/倒排文档频度(TF/IDF) ###

一旦我们拥有了匹配文档的列表，我们就需要对它们进行相关度排序。并不是所有的文档都会包含所有的词条，而一部分词条也比另一部分的词条更加重要。整个文档的相关度分值取决于(部分)出现在文档中的每个查询词条的权重。

一个词条的权重通过三个因素决定，这在[什么是相关度](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/relevance-intro.html)中已经介绍过了。对该公式的介绍是为了兴趣，你不需要记住它们。

**词条频度(Term Frequency)**

词条在当前文档中出现的有多频繁？越频繁的话，那么权重就越高。在一个字段中出现了5次的词条应该比只出现了1次的文档更加相关。词条频度通过下面的公式进行计算：

> tf(t in d) = √frequency

词条t在文档d中的词条频度tf，是该词条在文档中出现次数的平方根。

如果你不在乎一个词条在一个字段中的出现频度，相反你在意的是词条是否出现过，那么你可以在字段映射中禁用词条频度：

```json
PUT /my_index
{
  "mappings": {
    "doc": {
      "properties": {
        "text": {
          "type":          "string",
          "index_options": "docs" 
        }
      }
    }
  }
}
```

将index_options设置为docs会禁用词条频度以及词条位置信息。使用了该设置的字段不会记录词条出现的次数，也不能被用在短语或者邻近度查询中。精确值的not_analyzed字符串字段默认使用该设置。

**倒排文档频度(Inverse Document Frequency)**

词条在所有的文档中出现的频繁吗？出现的越频繁，权重就越低。像and或者the这样的常见词条对相关度的贡献几乎没有，因为它们在绝大多数的文档中都会出现，而像elastic或者hippopotamus这样的罕见词条则能够帮助找到我们最感兴趣的文档。倒排文档频度的计算方法如下：

> idf(t) = 1 + log ( numDocs / (docFreq + 1))

对于词条t的倒排文档频度(idf)，通过将索引中的文档数量除以含有该词条的文档数量，再取其对数得到。

**字段长度归约(Field-length Norm)**

字段的有多长？字段越短，那么其权重就越高。如果一个词条出现在较短的字段，如title字段中，那么该字段的内容相比更长的body字段而言，更有可能是关于该词条的。字段长度归约的计算方法如下：

> norm(d) = 1 / √numTerms

尽管字段长度归约对于全文搜索而言是重要的，也有许多其它字段不需要。无论文档是否含有该字段，对于索引中每份文档的每个字符串字段，归约大概需消耗1个字节的空间。对于精确值的not_analyzed字符串字段，归约功能默认是被禁用的，但是你也可以对analyzed类型的字段通过字段映射禁用归约：

```json
PUT /my_index
{
  "mappings": {
    "doc": {
      "properties": {
        "text": {
          "type": "string",
          "norms": { "enabled": false } 
        }
      }
    }
  }
}
```

在以上的例子中，该字段不会将字段长度归约考虑在内。这就意味着字段的长度不再被相关度计算考虑在内。

对于日志记录这种用例，归约是没有多大用处的。你在意的只是某个特定错误代码或者某个浏览器标识码是否出现在了某个字段中。字段的长度不会对结果造成影响。禁用归约功能也可以省下相当的内存空间。

**结合起来**

以上的三个因素 - 词条频度，倒排文档频度以及字段长度规范 - 都是在索引期间被计算和保存的。它们被用来计算单个词条对于某一份文档的权重。

> **TIP**
> 
> 我们前面讨论的文档，实际上是在讨论文档中的字段。每个字段都有它自己的倒排索引，因此对于TF/IDF而言，字段的值和文档的值是等效的。

当我们将explain设置为true(参考[理解分值(Understanding the Score)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/relevance-intro.html#explain))，然后再运行简单的term查询时，你会发现参与到分值计算过程中的因素就是我们前面讨论的那些：

```json
PUT /my_index/doc/1
{ "text" : "quick brown fox" }

GET /my_index/doc/_search?explain
{
  "query": {
    "term": {
      "text": "fox"
    }
  }
}
```

前面的请求得到的解释(有省略)如下所示：

> weight(text:fox in 0) [PerFieldSimilarity]:  0.15342641 
> result of:
>     fieldWeight in 0                         0.15342641
>     product of:
>         tf(freq=1.0), with freq of 1:        1.0 
>         idf(docFreq=1, maxDocs=1):           0.30685282 
>         fieldNorm(doc=0):                    0.5

第一行：对于Lucene内部文档ID为0的文档，词条fox在其text字段中的最终分值。
第五行 - 第七行：词条频度，倒排文档频度以及字段长度归约得到的结果。

当然，查询通常都会由不止一个词条组成，因此我们需要一种将多个词条的权重联系起来的方法。为此我们可以求助向量空间模型(Vector Space Model)。

### 向量空间模型 ###

向量空间模型提供了一种多词条查询的比较方法。它的输出是一个代表了文档和查询之间匹配程度的分值。为了计算该分值，文档和查询都被表示成向量。

一个向量实际上就是一个包含了数值的一维数组，比如：

> [1,2,5,22,3,8]

在向量空间模型中，向量中的每个数值都是由[TF/IDF](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/scoring-theory.html#tfidf)计算得到的一个词条的权重。

> **TIP**
> 
> 尽管TF/IDF是在向量空间模型中默认被用来计算词条权重的方法，它并不是唯一可用的方法。在ES中，其它诸如Okapi-BM25的计算模型也是可用的。TF/IDF由于其简洁性，高效性，产生的搜索结果的高质量而经受了时间的考验，从而被当做是默认方法。

假设我们查询了"happy hippopotamus"。一个像happy这样的常见单词的权重是较低的，然而像hippopotamus这样的罕见单词则拥有较高的权重。假设happy的权重为2而hippopotamus的权重为5。我们可以使用坐标来表达这个简单的二维向量 - [2, 5] - 一条从坐标(0, 0)到坐标(2, 5)的直线，如下所示：

![](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/images/elas_17in01.png)

现在，假设我们有三份文档：

1. I am *happy* in summer.
2. After Christmas I’m a *hippopotamus*.
3. The *happy hippopotamus* helped Harry.

我们可以为每份文档创建一个类似的向量，它由每个查询词条的权重组成 - 也就是出现在文档中的词条happy和hippopotamus，然后将它绘制在坐标中，如下图：

- 文档1：(happy,____________) — [2,0]
- 文档2：( ___ ,hippopotamus) — [0,5]
- 文档3：(happy,hippopotamus) — [2,5]

![](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/images/elas_17in02.png)

向量的一个很棒的性质是它们能够被比较。通过测量查询向量和文档向量间的角度，我们可以给每份文档计算一个相关度分值。文档1和查询之间的角度较大，因此它的相关度较低。文档2和查询更靠近，所以它的相关度更高，而文档3和查询之间则是一个完美的匹配。

> **TIP**
> 
> 实际上，只有二维向量(使用两个词条的查询)才能够被简单地表示在坐标中。幸运的是，线性代数 - 数学的一个分支，能够处理向量 - 提供了用来比较多维向量间角度的工具，这意味着我们能够使用上述原理对包含很多词条的查询进行处理。
> 
> 你可以获得关于使用余弦相似性来比较两个向量的更多信息：http://en.wikipedia.org/wiki/Cosine_similarity

我们已经讨论了分值计算的理论基础，现在让我们看看Lucene是如何进行分值计算的。





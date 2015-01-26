## Lucene中的Practical Scoring Function ##

对于多词条查询(Multiterm Queries)，Lucene使用的是[布尔模型(Boolean Model)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/scoring-theory.html#boolean-model)，[TF/IDF](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/scoring-theory.html#tfidf)以及[向量空间模型(Vector Space Model)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/scoring-theory.html#vector-space-model)来将它们结合在一起，用来收集匹配的文档和对它们进行分值计算。

像下面这样的多词条查询：

```json
GET /my_index/doc/_search
{
  "query": {
    "match": {
      "text": "quick fox"
    }
  }
}
```

在内部被重写成下面这样：

```json
GET /my_index/doc/_search
{
  "query": {
    "bool": {
      "should": [
        {"term": { "text": "quick" }},
        {"term": { "text": "fox"   }}
      ]
    }
  }
}
```

bool查询实现了布尔模型，在这个例子中，只有包含了词条quick，词条fox或者两者都包含的文档才会被返回。

一旦一份文档匹配了一个查询，Lucene就会为该查询计算它的分值，然后将每个匹配词条的分值结合起来。用来计算分值的公式叫做Practical Scoring Function。它看起来有点吓人，但是不要退却 - 公式中的绝大多数部分你已经知道了。下面我们会介绍它引入的一些新元素。

```
1	score(q,d)  = 
2            queryNorm(q)  
3          · coord(q,d)    
4          · ∑ (           
5                tf(t in d)   
6              · idf(t)²      
7              · t.getBoost() 
8              · norm(t,d)    
9            ) (t in q) 
```

每行的意义如下：

1. score(q,d)是文档d对于查询q的相关度分值。
2. queryNorm(q)是[查询归约因子(Query Normalization Factor)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/practical-scoring-function.html#query-norm)，是新添加的部分。
3. coord(q,d)是[Coordination Factor](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/practical-scoring-function.html#coord)，是新添加的部分。
4. 文档d中每个词条t对于查询q的权重之和。
5. tf(t in d)是文档d中的词条t的[词条频度(Term Frequency)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/scoring-theory.html#tf)。
6. idf(t)是词条t的[倒排索引频度(Inverse Document Frequency)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/scoring-theory.html#idf)
7. t.getBoost()是适用于查询的[提升(Boost)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/query-time-boosting.html)，是新添加的部分。
8. norm(t,d)是[字段长度归约(Field-length Norm)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/scoring-theory.html#field-norm)，可能结合了[索引期间字段提升(Index-time Field-level Boost)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/practical-scoring-function.html#index-boost)，是新添加的部分。

你应该知道score，tf以及idf的意思。queryNorm，coord，t.getBoost以及norm是新添加的。

在本章的稍后我们会讨论[查询期间提升(Query-time Boosting)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/query-time-boosting.html)，首先对查询归约，Coordination以及索引期间字段级别提升进行解释。

### 查询归约因子(Query Normalization Factor) ###

查询归约因子(queryNorm)会试图去对一个查询进行归约，从而让多个查询的结果能够进行比较。

> **TIP**
> 
> 虽然查询归约的目的是让不同查询的结果能够比较，但是它的效果不怎么好。相关度_score的唯一目的是将当前查询的结果以正确的顺序被排序。你不应该尝试去比较不同查询得到的相关度分值。

该因子会在查询开始阶段就被计算。实际的计算取决于查询本身，但是一个典型的实现如下所示：

> queryNorm = 1 / √sumOfSquaredWeights

sumOfSquaredWeights通过对查询中每个词条的IDF进行累加，然后取其平方根得到的。

> **TIP**
> 
> 相同的查询归约因子会被适用在每份文档上，你也没有办法改变它。总而言之，它是可以被忽略的。

### Query Coordination ###

Coordination因子(coord)被用来奖励那些包含了更多查询词条的文档。文档中出现了越多的查询词条，那么该文档就越可能是该查询的一个高质量匹配。

加入我们查询了quick brown fox，每个词条的权重都是1.5。没有Coordination因子时，分值可能会是文档中每个词条的权重之和。比如：

- 含有fox的文档 -> 分值：1.5
- 含有quick fox的文档 -> 分值：3.0
- 含有quick brown fox的文档 -> 分值：4.5

而Coordination因子会将分值乘以文档中匹配了的词条的数量，然后除以查询中的总词条数。使用了Coordination因子后，分值是这样的：

- 含有fox的文档 -> 分值：1.5 * 1 / 3 = 0.5
- 含有quick fox的文档 -> 分值：3.0 * 2 / 3 = 2.0
- 含有quick brown fox的文档 -> 分值：4.5 * 3 / 3 = 4.5

以上的结果中，含有所有三个词条的文档的分值会比仅含有两个词条的文档高出许多。

需要记住对于quick brown fox的查询会被bool查询重写如下：

```json
GET /_search
{
  "query": {
    "bool": {
      "should": [
        { "term": { "text": "quick" }},
        { "term": { "text": "brown" }},
        { "term": { "text": "fox"   }}
      ]
    }
  }
}
```

bool查询会对所有should查询子句默认启用查询Coordination，但是你可以禁用它。为什么你需要禁用它呢？好吧，通常的答案是，并不需要。查询Coordination通常都起了正面作用。当你使用bool查询来将多个像match这样的高级查询(High-level Query)包装在一起时，启用Coordination也是有意义的。匹配的查询子句越多，你的搜索陈请求和返回的文档之间的匹配程度就越高。

但是，在某些高级用例中，禁用Coordination也是有其意义的。比如你正在查询同义词jump，leap和hop。你不需要在意这些同义词出现了多少次，因为它们表达了相同的概念。实际上，只有其中的一个可能会出现。此时，禁用Coordination因子就是一个不错的选择：

```json
GET /_search
{
  "query": {
    "bool": {
      "disable_coord": true,
      "should": [
        { "term": { "text": "jump" }},
        { "term": { "text": "hop"  }},
        { "term": { "text": "leap" }}
      ]
    }
  }
}
```

当你使用了同义词(参考[同义词(Synonyms)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/synonyms.html))，这正是在内部发生的：重写的查询会为同义词禁用Coordination。多数禁用Coordination的用例都会被自动地处理；你根本无需担心它。

### 索引期间字段级别提升(Index-time Field-level Boosting) ###

现在来讨论一下字段提升 - 让该字段比其它字段更重要一些 - 通过在查询期间使用[查询期间提升(Query-time Boosting)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/query-time-boosting.html)。在索引期间对某个字段进行提升也是可能的。实际上，该提升会适用于字段的每个词条上，而不是在字段本身。

为了在尽可能少占用空间的前提下，将提升值存储到索引中，索引期间字段级别提升会和[字段长度归约](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/scoring-theory.html#field-norm)一起以一个字节被保存在索引中。它是之前公式中norm(t,d)返回的值。

> **警告**
> 
> 我们强烈建议不要使用字段级别索引期间提升的原因如下：
> 
> - 将此提升和字段长度归约存储在一个字节中意味着字段长度归约会损失精度。结果是ES不能区分一个含有三个单词的字段和一个含有五个单词的字段。
> - 为了修改索引期间提升，你不得不对所有文档重索引。而查询期间的提升则可以因查询而异。
> - 如果一个使用了索引期间提升的字段是多值字段(Multivalue Field)，那么提升值会为每一个值进行乘法操作，导致该字段的权重飙升。
> 
> [查询期间提升(Query-time Boosting)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/query-time-boosting.html)更简单，简洁和灵活。

解释完了查询归约，Coordination以及索引期间提升，现在可以开始讨论对影响相关度计算最有用的工具：查询期间提升。
















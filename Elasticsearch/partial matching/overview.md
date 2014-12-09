# 部分匹配(Partial Matching) #

敏锐的读者可能已经发现到目前为止，介绍的查询都是在整个词条层面进行操作的。匹配的最小单元必须是一个词条。你只能找到存在于倒排索引(Inverted Index)中的词条。

但是如果你想匹配词条的一部分，而不是整个词条呢？部分匹配(Partial Matching)允许用户指定词条的一部分然后找到含有该部分的任何单词。

匹配词条一部分这一需求在全文搜索引擎领域比你想象的要不那么常见。如果你有SQL的背景，你可能有过使用下面的SQL语句来实现一个简单的全文搜索功能的经历：

```sql
WHERE text LIKE "*quick*"
      AND text LIKE "*brown*"
      AND text LIKE "*fox*"
```

当然，通过ES我们可以借助分析过程(Analysis Process)和倒排索引来避免这种"蛮力"技术。为了同时匹配"fox"和"foxes"，我们可以简单地使用一个词干提取器，然后将词干进行索引。这样就没有必要进行部分匹配了。

即便如此，在某些场合下部分匹配还是有作用的。常见的用例比如：

- 匹配邮政编码，产品序列号，或者其它以某个特定前缀开头的或者能够匹配通配符甚至正则表达式的not_analyzed值。
- 即时搜索(Search-as-you-type) - 在用户完成搜索词条的输入前就展示最有可能的结果。
- 匹配德语或者荷兰语这一类语言，它们韩哟长复合单词，比如Weltgesundheitsorganisation(World Health Organization)。

我们以针对精确值not_analyzed字段的前缀匹配开始，介绍部分匹配的技术。
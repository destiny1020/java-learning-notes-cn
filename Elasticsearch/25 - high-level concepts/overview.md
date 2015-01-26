#高层概念(High-Level Concepts)

和查询DSL一样，聚合(Aggregations)也拥有一种可组合(Composable)的语法：独立的功能单元可以被混合在一起来满足你的需求。这意味着需要学习的基本概念虽然不多，但是它们的组合方式是几近无穷的。

为了掌握聚合，你只需要了解两个主要概念：

**Buckets(桶)：**

满足某个条件的文档集合。

**Metrics(指标)：**

为某个桶中的文档计算得到的统计信息。

就是这样！每个聚合只是简单地由一个或者多个桶，零个或者多个指标组合而成。可以将它粗略地转换为SQL：

```sql
SELECT COUNT(color) 
FROM table
GROUP BY color
```

以上的COUNT(color)就相当于一个指标。GROUP BY color则相当于一个桶。


桶和SQL中的组(Grouping)拥有相似的概念，而指标则与COUNT()，SUM()，MAX()等相似。

让我们仔细看看这些概念。

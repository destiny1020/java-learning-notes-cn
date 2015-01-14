##根据"嵌套"指标进行排序(Sorting based on "Deep" Metrics)

在前面的例子中，指标直接被定义在桶上。为每个桶都计算了其平均价格。基于更深的指标进行排序也是可行的，比如可以基于桶的孙子(Grandchildren)或者重孙(Great-grandchilren)指标来进行排序 - 只不过有一些限制。

你可以通过尖括号(>)定义一个路径，来得到更深的嵌套指标，比如：my_bucket>another_bucket>metric。

需要注意的是路径上的每个嵌套桶都需要是一个单值桶(Single-value Bucket)。一个过滤桶(Filter Bucket)会产生一个桶：所有匹配过滤条件的文档。多值桶(Multivalue Bucket，比如terms)会动态地生成很多桶，这会使得指定一个确定的路径不可行。

目前，只有三个单值桶：filter，global以及reverse_nested。让我们创建一个关于汽车价格的histogram作为例子，只不过根据每个价格区间(即每个桶)中的红色及绿色(不是蓝色)汽车的价格的方差进行排序。

```json
GET /cars/transactions/_search?search_type=count
{
    "aggs" : {
        "colors" : {
            "histogram" : {
              "field" : "price",
              "interval": 20000,
              "order": {
                "red_green_cars>stats.variance" : "asc" 
              }
            },
            "aggs": {
                "red_green_cars": {
                    "filter": { "terms": {"color": ["red", "green"]}}, 
                    "aggs": {
                        "stats": {"extended_stats": {"field" : "price"}} 
                    }
                }
            }
        }
    }
}
```

在这个例子中，你可以看到我们访问了一个嵌套的指标。stats指标是red_green_cars的一个孩子，而它本身则是colors的一个孩子。为了根据该指标进行排序，我们使用red_green_cars>stats.variance来定义路径。因为filter桶是单值桶，所以这种方式是可行的。


## 索引设置 ##

虽然index设置可以在Index Modules Reference Doc中清楚地找到，但是一般不建议那样做。。。

**Tip**

ES中提供了一些很好的默认值，只有当你知道为什么要去修改它的时候再去修改！

两个最重要的设置：

- number_of_shards
	一个索引中含有的primary shard数量，默认值是5。在索引创建后这个值是不能被更改的。

- number_of_replicas
	每一个primary shard含有的replica shard数量，默认值是1。这个设置在任何时候都可以被修改。

```
PUT /my_temp_index
{
    "settings": {
        "number_of_shards" :   1,
        "number_of_replicas" : 0
    }
}

# 修改replicas的数量
PUT /my_temp_index/_settings
{
    "number_of_replicas": 1
}
```


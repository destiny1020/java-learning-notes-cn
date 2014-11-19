## 索引设置 ##

虽然索引的种种行为可以通过[索引模块的参考文档](http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.4//index-modules.html)介绍的那样进行配置，但是……

> **TIP**
> 
> ES中提供了一些很好的默认值，只有当你知道它是干什么的，以及为什么要去修改它的时候再去修改。

两个最重要的设置：

### number_of_shards ###

一个索引中含有的主分片(Primary Shard)的数量，默认值是5。在索引创建后这个值是不能被更改的。

### number_of_replicas ###

每一个主分片关联的副本分片(Replica Shard)的数量，默认值是1。这个设置在任何时候都可以被修改。

比如，我们可以通过下面的请求创建一个小的索引 - 只有一个主分片 - 同时没有副本分片：

```
PUT /my_temp_index
{
    "settings": {
        "number_of_shards" :   1,
        "number_of_replicas" : 0
    }
}

将来，我们可以动态地通过update-index-settings API完成对副本分片数量的修改：

```
PUT /my_temp_index/_settings
{
    "number_of_replicas": 1
}
```


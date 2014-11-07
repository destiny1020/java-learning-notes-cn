虽然你可以添加新的type，添加新的field，但是你不能添加新的analyzer或者对现有fields进行修改。这是因为这些行为会让已经索引的数据变的不正确，导致搜索不能正常的进行。

而解决办法就是reindex：通过新的设置来创建index并且将当前索引中现有的所有文档拷贝到新的索引中。

所以这个时候_source字段的价值就体现出来了。它已经包含了整个文档的内容，不需要通过DB来重建索引。

为了高效的完成对于所有文档的reindex：

1. 使用scan和scroll来批量的而获取文档
2. 使用bulk API将它们批量的放到新的index中

可以根据date或者timestamp字段进行划分，将reindex的任务切分：

```
GET /old_index/_search?search_type=scan&scroll=1m
{
    "query": {
        "range": {
            "date": {
                "gte":  "2014-01-01",
                "lt":   "2014-02-01"
            }
        }
    },
    "size":  1000
}
```
## 删除索引 ##

使用下面的请求完成索引的删除：

```
DELETE /my_index
```

你也可以删除多个索引：

```
DELETE /index_one,index_two
DELETE /index_*
```

你甚至还可以删除所有的索引：

```
DELETE /_all
```
## 创建索引 ##

创建索引根据Dynamic mapping，index的属性采用默认值。

但是，可以进行更精确的控制：比如primary shards的数量，使用何种analyzer。而且这种控制可以在实际存储任何数据之前进行。

```
PUT /my_index
{
    "settings": { ... any settings ... },
    "mappings": {
        "type_one": { ... any mappings ... },
        "type_two": { ... any mappings ... },
        ...
    }
}
```

如果不希望使用dynamic mapping，可以在config/elasticsearch.yml中设置：

```
action.auto_create_index: false
```

可以使用index template来预先定义(Pre-configure)自动生成的索引。对于索引log数据非常方便。




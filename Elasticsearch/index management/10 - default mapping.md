## 默认映射 ##

一般，索引中的所有types都会有相似的fields和设置。所以可以规定一个_default_ mapping信息，来完成DRY原则。_default_映射就好比一个针对新类型的模板。

比如，我们可以对所有types禁用_all字段，唯独blog type，那么可以这样实现：

```
PUT /my_index
{
    "mappings": {
        "_default_": {
            "_all": { "enabled":  false }
        },
        "blog": {
            "_all": { "enabled":  true  }
        }
    }
}
```

_default_同时也是一个声明dynamic templates的好地方。
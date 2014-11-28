## 默认映射(Default Mapping) ##

一般情况下，索引中的所有类型都会有相似的字段和设置。因此将这些常用设置在`_default`映射中指定会更加方便，这样就不需要在每次创建新类型的时候都重复设置。`_default`映射的角色是新类型的模板。所有在`_default`映射之后创建的类型都会包含所有的默认设置，除非显式地在类型映射中进行覆盖。

比如，我们使用`_default`映射对所有类型禁用`_all`字段，唯独对`blog`类型启用它。可以这样实现：

```json
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

`_default_`映射同时也是一个声明作用于整个索引的[动态模板(Dynamic Templates)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/custom-dynamic-mapping.html#dynamic-templates)的好地方。
## 动态映射 ##

当ES碰到一个以前没见过的字段时，它会利用dynamic mapping来决定该字段的类型，并对他进行映射。

可以通过dynamic设置来控制这一行为：

- true：默认值。动态添加字段
- false：忽略新字段
- strict：如果碰到陌生字段，抛出异常

dynamic设置可以放在root object或者任何字段object上：

```
PUT /my_index
{
    "mappings": {
        "my_type": {
            "dynamic":      "strict", 
            "properties": {
                "title":  { "type": "string"},
                "stash":  {
                    "type":     "object",
                    "dynamic":  true 
                }
            }
        }
    }
}

# Dynamically create a new field under `stash`
PUT /my_index/my_type/1
{
  "title": "This doc adds a new field",
  "stash": {
    "new_field": "Success!"
  }
}

# Check the mapping to verify
GET /my_index/_mapping/my_type

# Throw an error when trying to add a new field
# to the root object
PUT /my_index/my_type/1
{
  "title": "This throws a StrictDynamicMappingException",
  "new_field": "Fail!"
}
```

**NOTE**

将dynamic设置为false并不会改变_source字段的内容。_source字段仍然会保存整个JSON文档。
只不过是陌生的字段不会被添加到mapping中，以至于它不能被搜索到。


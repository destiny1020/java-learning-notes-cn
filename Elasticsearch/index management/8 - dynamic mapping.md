## 动态映射(Dynamic Mapping) ##

当ES在文档中碰到一个以前没见过的字段时，它会利用动态映射来决定该字段的类型，并自动地对该字段添加映射。

有时这正是需要的行为，但有时不是。你或许不知道在以后你的文档中会添加哪些字段，但是你想要它们能够被自动地索引。或许你只是想要忽略它们。或者 - 尤其当你将ES当做主要的数据存储使用时 - 大概你会希望这些未知的字段会抛出异常来提醒你注意这一问题。

幸运的是，你可以通过`dynamic`设置来控制这一行为，它能够接受以下的选项：

- `true`：默认值。动态添加字段
- `false`：忽略新字段
- `strict`：如果碰到陌生字段，抛出异常

`dynamic`设置可以适用在根对象上或者`object`类型的任意字段上。你应该默认地将`dynamic`设置为`strict`，但是为某个特定的内部对象启用它：

```json
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
```

在`my_type`对象上如果碰到了未知字段则会抛出一个异常。
在`stash`对象上会动态添加新字段。

通过以上的映射，你可以向`stash`添加新的可搜索的字段：

```json
PUT /my_index/my_type/1
{
  "title": "This doc adds a new field",
  "stash": {
    "new_field": "Success!"
  }
}
```

但是，如果在顶层对象上试图添加新字段则会失败：

```json
PUT /my_index/my_type/1
{
    "title":     "This throws a StrictDynamicMappingException",
    "new_field": "Fail!"
}
```

> **NOTE**
> 
> 将`dynamic`设置为`false`并不会改变`_source`字段的内容 - `_source`字段仍然会保存你索引的整个JSON文档。只不过是陌生的字段将不会被添加到映射中，以至于它不能被搜索到。


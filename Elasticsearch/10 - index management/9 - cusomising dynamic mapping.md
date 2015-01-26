## 自定义动态映射 ##

如果你知道你需要动态的添加的新字段，那么你也许会启用动态映射。然而有时动态映射的规则又有些不够灵活。幸运的是，你可以调整某些设置来让动态映射的规则更加适合你的数据。

### `date_detection` ###

当ES碰到一个新的字符串字段时，它会检查该字串是否含有一个可被识别的日期，比如`2014-01-01`。如果存在，那么它会被识别为一个`date`类型的字段。否则会将它作为`string`进行添加。

有时这种行为会导致一些问题。如果你想要索引一份这样的文档：

```json
{ "note": "2014-01-01" }
```

假设这是`note`字段第一次被发现，那么根据规则它会被作为`date`字段添加。但是如果下一份文档是这样的：

```json
{ "note": "Logged out" }
```

这时该字段显然不是日期，但是已经太迟了。该字段的类型已经是日期类型的字段了，因此这会导致一个异常被抛出。

可以通过在根对象上将`date_detection`设置为`false`来关闭日期检测：

```json
PUT /my_index
{
    "mappings": {
        "my_type": {
            "date_detection": false
        }
    }
}
```

有了以上的映射，一个字符串总是会被当做`string`类型。如果你需要一个`date`字段，你需要手动地添加它。

> **NOTE**
> 
> ES中识别日期的方法可以通过[`dynamic_date_formats`设置](http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.4//mapping-root-object-type.html#_dynamic_date_formats)改变。

### `dynamic_templates` ###

通过`dynamic_templates`，你可以拥有对新字段的动态映射规则拥有完全的控制。你设置可以根据字段名称或者类型来使用一个不同的映射规则。

每个模板都有一个名字，可以用来描述这个模板做了什么。同时它有一个`mapping`用来指定具体的映射信息，和至少一个参数(比如`match`)用来规定对于什么字段需要使用该模板。

模板的匹配是有顺序的 - 第一个匹配的模板会被使用。比如我们可以为`string`字段指定两个模板：

- `es`：以`_es`结尾的字段应该使用`spanish`解析器
- `en`：其它所有字段使用`english`解析器

我们需要将`es`模板放在第一个，因为它相比能够匹配所有字符串字段的`en`模板更加具体：

```json
PUT /my_index
{
    "mappings": {
        "my_type": {
            "dynamic_templates": [
                { "es": {
                      "match":              "*_es", 
                      "match_mapping_type": "string",
                      "mapping": {
                          "type":           "string",
                          "analyzer":       "spanish"
                      }
                }},
                { "en": {
                      "match":              "*", 
                      "match_mapping_type": "string",
                      "mapping": {
                          "type":           "string",
                          "analyzer":       "english"
                      }
                }}
            ]
}}}
```

`match_mapping_type`允许你只对特定类型的字段使用模板，正如标准动态映射规则那样，比如`string`，`long`等。

`match`参数只会匹配字段名，`path_match`参数用于匹配对象中字段的完整路径，比如`address.*.name`可以匹配如下字段：

```json
{
    "address":
        "city":
            "name": "New York"
        }
    }
}
```

`unmatch`和`path_unmatch`模式能够用来排除某些字段，没有被排除的字段则会被匹配。

更多的配置选项可以在[根对象的参考文档](http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.4//mapping-root-object-type.html)中找到。







## 自定义动态映射 ##

### date_detection ###

当ES碰到一个新的字符串字段时，他会检查该字串是否含有一个可被识别的日期，比如2014-01-01。如果存在，那么它会被识别为一个date类型的字段。但是它存在问题：

如果你添加的第一个文档和第二个文档分别是这样的：

```
# 1st
{ "note": "2014-01-01" }

# 2nd
{ "note": "Logged out" }
```

第一个文档被索引的时候，note字段会被识别为date字段，但是第二个文档的note字段显然不再是date类型，此时就会抛出异常。可以禁用日期识别：

```
PUT /my_index
{
    "mappings": {
        "my_type": {
            "date_detection": false
        }
    }
}
```

**NOTE**

ES中识别日期的方法可以通过[dynamic_date_formats设置](http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.4//mapping-root-object-type.html#_dynamic_date_formats)改变。

### dynamic_templates ###

每个模板都有一个名字，可以用来描述这个模板做了什么。同时它有一个mapping用来指定具体的映射信息，和match参数用来规定对于什么字段需要使用该模板。

模板的匹配是有顺序的 - 第一个匹配的模板会被使用。比如我们可以为string字段指定两个模板：

- es：以_es结尾的字段应该使用spanish解析器
- en：其它所有字段使用english解析器

我们需要将es模板放在第一个，因为它更加具体：

```
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

match参数用于匹配字段名，path_match参数用于匹配字段的完整路径，比如address.*.name可以匹配如下字段：

```
{
    "address":
        "city":
            "name": "New York"
        }
    }
}
```

更完整的文档在[这里](http://www.elasticsearch.org/guide/en/elasticsearch/reference/1.4//mapping-root-object-type.html)







## 自定义_all字段 ##

在[元数据：_all字段](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/root-object.html#all-field)中，我们解释了特殊的_all字段会将其它所有字段中的值作为一个大字符串进行索引。尽管将所有字段的值作为一个字段进行索引并不是非常灵活。如果有一个自定义的_all字段用来索引人名，另外一个自定义的_all字段用来索引地址就更好了。

ES通过字段映射中的copy_to参数向我们提供了这一功能：

```json
PUT /my_index
{
    "mappings": {
        "person": {
            "properties": {
                "first_name": {
                    "type":     "string",
                    "copy_to":  "full_name" 
                },
                "last_name": {
                    "type":     "string",
                    "copy_to":  "full_name" 
                },
                "full_name": {
                    "type":     "string"
                }
            }
        }
    }
}
```

现在first_name和last_name字段中的值会被拷贝到full_name字段中。

有了这个映射，我们可以通过first_name字段查询名字，last_name字段查询姓氏，或者full_name字段查询姓氏和名字。

> **NOTE**
> 
> first_name和last_name字段的映射和full_name字段的索引方式的无关。full_name字段会从其它两个字段中拷贝字符串的值，然后仅根据full_name字段自身的映射进行索引。
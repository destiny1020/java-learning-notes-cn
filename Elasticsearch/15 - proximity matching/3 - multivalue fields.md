## 多值字段(Multivalue Fields) ##

在多值字段上使用短语匹配会产生古怪的行为：

```json
PUT /my_index/groups/1
{
    "names": [ "John Abraham", "Lincoln Smith"]
}
```

运行一个针对Abraham Lincoln的短语查询：

```json
GET /my_index/groups/_search
{
    "query": {
        "match_phrase": {
            "names": "Abraham Lincoln"
        }
    }
}
```

令人诧异的是，以上的这份文档匹配了查询。即使Abraham以及Lincoln分属于name数组的两个人名中。发生这个现象的原因在于数组在ES中的索引方式。

当John Abraham被解析时，它产生如下信息：

- 位置1：john
- 位置2：abraham

然后当Lincoln Smith被解析时，它产生了：

- 位置3：lincoln
- 位置4：smith

换言之，ES对以上数组分析产生的词条列表和解析单一字符串John Abraham Lincoln Smith时产生的结果是一样的。在我们的查询中，我们查询邻接的abraham和lincoln，而这两个词条在索引中确实存在并且邻接，因此查询匹配了。

幸运的是，有一个简单的方法来避免这种情况，通过position_offset_gap参数，它在字段映射中进行配置：

```json
DELETE /my_index/groups/ 

PUT /my_index/_mapping/groups 
{
    "properties": {
        "names": {
            "type":                "string",
            "position_offset_gap": 100
        }
    }
}
```

position_offset_gap设置告诉ES需要为数组中的每个新元素设置一个偏差值。因此，当我们再索引以上的人名数组时，会产生如下的结果：

- 位置1：john
- 位置2：abraham
- 位置3：lincoln
- 位置4：smith

现在我们的短语匹配就无法匹配该文档了，因为abraham和lincoln之间的距离为100。你必须要添加一个值为100的slop的值才能匹配。

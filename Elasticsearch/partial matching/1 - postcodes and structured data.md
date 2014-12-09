## 邮政编码和结构化数据 ##

我们以英国的邮政编码来说明如何在结构化数据上使用部分匹配。英国邮政编码是一个定义清晰的结构。比如，W1V 3DG这个邮政编码可以被分解成以下几个部分：

- W1V：这个部分表明了邮政地域和地区(Postal Area and District)：
	- W 表明了地域(Area)，使用一个或者两个字母。
	- 1V 表明了地区(District)，使用一个或者两个数字，可能跟随一个字母。
- 3DG：该部分表明了街道或者建筑：
	- 3 表明了区域(Sector)，使用一个数字。
	- DG 表明了单元，使用两个字母。

假设我们将邮政编码索引为精确值的not_analyzed字段，因此我们可以创建如下索引：

```json
PUT /my_index
{
    "mappings": {
        "address": {
            "properties": {
                "postcode": {
                    "type":  "string",
                    "index": "not_analyzed"
                }
            }
        }
    }
}
```

然后索引一些邮政编码：

```json
PUT /my_index/address/1
{ "postcode": "W1V 3DG" }

PUT /my_index/address/2
{ "postcode": "W2F 8HW" }

PUT /my_index/address/3
{ "postcode": "W1F 7HW" }

PUT /my_index/address/4
{ "postcode": "WC1N 1LZ" }

PUT /my_index/address/5
{ "postcode": "SW5 0BE" }
```

现在我们的数据就准备就绪了。
##映射

In order to be able to treat date fields as dates, numeric fields as numbers, and string fields as full-text or exact-value strings, Elasticsearch needs to know what type of data each field contains. This information is contained in the mapping.

As explained in [Data In, Data Out](https://www.elastic.co/guide/en/elasticsearch/guide/current/data-in-data-out.html), each document in an index has a type. Every type has its own mapping, or schema definition. A mapping defines the fields within a type, the datatype for each field, and how the field should be handled by Elasticsearch. A mapping is also used to configure metadata associated with the type.

We discuss mappings in detail in [Types and Mappings](https://www.elastic.co/guide/en/elasticsearch/guide/current/mapping.html). In this section, we’re going to look at just enough to get you started.

###核心基础字段类型

Elasticsearch supports the following simple field types:

- String: string
- Whole number: byte, short, integer, long
- Floating-point: float, double
- Boolean: boolean
- Date: date

When you index a document that contains a new field—one previously not seen—Elasticsearch will use [dynamic mapping](https://www.elastic.co/guide/en/elasticsearch/guide/current/dynamic-mapping.html) to try to guess the field type from the basic datatypes available in JSON, using the following rules:

|  JSON类型 | 字段类型  |
|---|---|
| boolean: true或者false  | boolean |
| 整型数: 123 | long |
| 浮点数: 123.45 | double |
| 字符串, 有效日期: 2014-09-15 | date |
| 字符串: foo bar | string |

>**NOTE**
>This means that if you index a number in quotes ("123"), it will be mapped as type string, not type long. However, if the field is already mapped as type long, then Elasticsearch will try to convert the string into a long, and throw an exception if it can’t.

###查看映射

We can view the mapping that Elasticsearch has for one or more types in one or more indices by using the /_mapping endpoint. At the [start of this chapter](https://www.elastic.co/guide/en/elasticsearch/guide/current/mapping-analysis.html), we already retrieved the mapping for type tweet in index gb:

```json
GET /gb/_mapping/tweet
```

This shows us the mapping for the fields (called properties) that Elasticsearch generated dynamically from the documents that we indexed:

```json
{
   "gb": {
      "mappings": {
         "tweet": {
            "properties": {
               "date": {
                  "type": "date",
                  "format": "dateOptionalTime"
               },
               "name": {
                  "type": "string"
               },
               "tweet": {
                  "type": "string"
               },
               "user_id": {
                  "type": "long"
               }
            }
         }
      }
   }
}
```

>**TIP**
>Incorrect mappings, such as having an **age** field mapped as type **string** instead of **integer**, can produce confusing results to your queries.
>
>Instead of assuming that your mapping is correct, check it!

###自定义字段映射

While the basic field datatypes are sufficient for many cases, you will often need to customize the mapping for individual fields, especially string fields. Custom mappings allow you to do the following:

- Distinguish between full-text string fields and exact value string fields
- Use language-specific analyzers
- Optimize a field for partial matching
- Specify custom date formats
- And much more

The most important attribute of a field is the **type**. For fields other than **string** fields, you will seldom need to map anything other than **type**:

```json
{
    "number_of_clicks": {
        "type": "integer"
    }
}
```

Fields of type **string** are, by default, considered to contain full text. That is, their value will be passed through an analyzer before being indexed, and a full-text query on the field will pass the query string through an analyzer before searching.

The two most important mapping attributes for **string** fields are **index** and **analyzer**.

####index
The **index** attribute controls how the string will be indexed. It can contain one of three values:

**analyzed**
First analyze the string and then index it. In other words, index this field as full text.

**not_analyzed**
Index this field, so it is searchable, but index the value exactly as specified. Do not analyze it.

**no**
Don’t index this field at all. This field will not be searchable.

The default value of **index** for a string field is **analyzed**. If we want to map the field as an exact value, we need to set it to **not_analyzed**:

```json
{
    "tag": {
        "type":     "string",
        "index":    "not_analyzed"
    }
}
```

>**NOTE**
>The other simple types (such as **long**, **double**, **date** etc) also accept the **index** parameter, but the only relevant values are **no** and **not_analyzed**, as their values are never analyzed.

####analyzer

For **analyzed** string fields, use the **analyzer** attribute to specify which analyzer to apply both at search time and at index time. By default, Elasticsearch uses the **standard** analyzer, but you can change this by specifying one of the built-in analyzers, such as **whitespace**, **simple**, or **english**:

```json
{
    "tweet": {
        "type":     "string",
        "analyzer": "english"
    }
}
```

In [Custom Analyzers](https://www.elastic.co/guide/en/elasticsearch/guide/current/custom-analyzers.html), we show you how to define and use custom analyzers as well.

###更新映射

You can specify the mapping for a type when you first create an index. Alternatively, you can add the mapping for a new type (or update the mapping for an existing type) later, using the **/_mapping** endpoint.

>**NOTE**
>
>Although you can add to an existing mapping, you can’t change it. If a field already exists in the mapping, the data from that field probably has already been indexed. If you were to change the field mapping, the already indexed data would be wrong and would not be properly searchable.

We can update a mapping to add a new field, but we can’t change an existing field from **analyzed** to **not_analyzed**.

To demonstrate both ways of specifying mappings, let’s first delete the **gb** index:

```json
DELETE /gb
```

Then create a new index, specifying that the **tweet** field should use the **english** analyzer:

```json
PUT /gb 
{
  "mappings": {
    "tweet" : {
      "properties" : {
        "tweet" : {
          "type" :    "string",
          "analyzer": "english"
        },
        "date" : {
          "type" :   "date"
        },
        "name" : {
          "type" :   "string"
        },
        "user_id" : {
          "type" :   "long"
        }
      }
    }
  }
}
```

Later on, we decide to add a new **not_analyzed** text field called **tag** to the **tweet** mapping, using the **_mapping** endpoint:

```json
PUT /gb/_mapping/tweet
{
  "properties" : {
    "tag" : {
      "type" :    "string",
      "index":    "not_analyzed"
    }
  }
}
```

Note that we didn’t need to list all of the existing fields again, as we can’t change them anyway. Our new field has been merged into the existing mapping.

###测试映射

You can use the **analyze** API to test the mapping for string fields by name. Compare the output of these two requests:

```json
GET /gb/_analyze?field=tweet
Black-cats 

GET /gb/_analyze?field=tag
Black-cats 
```

The **tweet** field produces the two terms **black** and **cat**, while the **tag** field produces the single term **Black-cats**. In other words, our mapping is working correctly.
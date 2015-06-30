##复合基础字段类型(Complex Core Field Types)

Besides the simple scalar datatypes that we have mentioned, JSON also has null values, arrays, and objects, all of which are supported by Elasticsearch.

###多值字段(Multivalue Fields)

It is quite possible that we want our **tag** field to contain more than one tag. Instead of a single string, we could index an array of tags:

```json
{ "tag": [ "search", "nosql" ]}
```

There is no special mapping required for arrays. Any field can contain zero, one, or more values, in the same way as a full-text field is analyzed to produce multiple terms.

By implication, this means that all the values of an array must be of the same datatype. You can’t mix dates with strings. If you create a new field by indexing an array, Elasticsearch will use the datatype of the first value in the array to determine the **typse** of the new field.


> **NOTE**
> 
> When you get a document back from Elasticsearch, any arrays will be in the same order as when you indexed the document. The **_source** field that you get back contains exactly the same JSON document that you indexed.
> 
> However, arrays are indexed—made searchable—as multivalue fields, which are unordered. At search time, you can’t refer to “the first element” or “the last element.” Rather, think of an array as a bag of values.

###空字段(Empty Fields)

Arrays can, of course, be empty. This is the equivalent of having zero values. In fact, there is no way of storing a **null** value in Lucene, so a field with a **null** value is also considered to be an empty field.

These four fields would all be considered to be empty, and would not be indexed:


```
"null_value":               null,
"empty_array":              [],
"array_with_null_value":    [ null ]
```

###多层对象(Multilevel Objects)

The last native JSON datatype that we need to discuss is the object — known in other languages as a hash, hashmap, dictionary or associative array.

Inner objects are often used to embed one entity or object inside another. For instance, instead of having fields called user_name and user_id inside our tweet document, we could write it as follows:

```json
{
    "tweet":            "Elasticsearch is very flexible",
    "user": {
        "id":           "@johnsmith",
        "gender":       "male",
        "age":          26,
        "name": {
            "full":     "John Smith",
            "first":    "John",
            "last":     "Smith"
        }
    }
}
```

###内部对象的映射(Mapping for Inner Objects)

Elasticsearch will detect new object fields dynamically and map them as type **object**, with each inner field listed under **properties**:

```json
{
  "gb": {
    "tweet": { 
      "properties": {
        "tweet":            { "type": "string" },
        "user": { 
          "type":             "object",
          "properties": {
            "id":           { "type": "string" },
            "gender":       { "type": "string" },
            "age":          { "type": "long"   },
            "name":   { 
              "type":         "object",
              "properties": {
                "full":     { "type": "string" },
                "first":    { "type": "string" },
                "last":     { "type": "string" }
              }
            }
          }
        }
      }
    }
  }
}
```

The mapping for the **user** and **name** fields has a similar structure to the mapping for the **tweet** type itself. In fact, the **type** mapping is just a special type of **object** mapping, which we refer to as the root object. It is just the same as any other object, except that it has some special top-level fields for document metadata, such as **_source**, and the **_all** field.

###内部对象是如何被索引的(How Inner Objects are Indexed)

Lucene doesn’t understand inner objects. A Lucene document consists of a flat list of key-value pairs. In order for Elasticsearch to index inner objects usefully, it converts our document into something like this:

```json
{
    "tweet":            [elasticsearch, flexible, very],
    "user.id":          [@johnsmith],
    "user.gender":      [male],
    "user.age":         [26],
    "user.name.full":   [john, smith],
    "user.name.first":  [john],
    "user.name.last":   [smith]
}
```

Inner fields can be referred to by name (for example, first). To distinguish between two fields that have the same name, we can use the full path (for example, user.name.first) or even the type name plus the path (tweet.user.name.first).

> **NOTE**
> 
> In the preceding simple flattened document, there is no field called user and no field called user.name. Lucene indexes only scalar or simple values, not complex data structures.

###内部对象数组(Arrays of Inner Objects)

Finally, consider how an array containing inner objects would be indexed. Let’s say we have a followers array that looks like this:


```json
{
    "followers": [
        { "age": 35, "name": "Mary White"},
        { "age": 26, "name": "Alex Jones"},
        { "age": 19, "name": "Lisa Smith"}
    ]
}
```

This document will be flattened as we described previously, but the result will look like this:

```json
{
    "followers.age":    [19, 26, 35],
    "followers.name":   [alex, jones, lisa, smith, mary, white]
}
```

The correlation between **{age: 35}** and **{name: Mary White}** has been lost as each multivalue field is just a bag of values, not an ordered array. This is sufficient for us to ask, "Is there a follower who is 26 years old?"

But we can’t get an accurate answer to this: "Is there a follower who is 26 years old and who is called Alex Jones?"

Correlated inner objects, which are able to answer queries like these, are called nested objects, and we cover them later, in [Nested Objects](https://www.elastic.co/guide/en/elasticsearch/guide/current/nested-objects.html).


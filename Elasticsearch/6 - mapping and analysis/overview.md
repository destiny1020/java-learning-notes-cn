#映射和解析(Mapping and Analysis)

While playing around with the data in our index, we notice something odd. Something seems to be broken: we have 12 tweets in our indices, and only one of them contains the date **2014-09-15**, but have a look at the total hits for the following queries:

```
GET /_search?q=2014              # 12 results
GET /_search?q=2014-09-15        # 12 results !
GET /_search?q=date:2014-09-15   # 1  result
GET /_search?q=date:2014         # 0  results !
```

Why does querying the [_all field](https://www.elastic.co/guide/en/elasticsearch/guide/current/search-lite.html#all-field-intro) for the full date return all tweets, and querying the date field for just the year return no results? Why do our results differ when searching within the _all field or the date field?

Presumably, it is because the way our data has been indexed in the _all field is different from how it has been indexed in the date field. So let’s take a look at how Elasticsearch has interpreted our document structure, by requesting the mapping (or schema definition) for the tweet type in the gb index:

```
GET /gb/_mapping/tweet
```

This gives us the following:

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

Elasticsearch has dynamically generated a mapping for us, based on what it could guess about our field types. The response shows us that the date field has been recognized as a field of type date. The _all field isn’t mentioned because it is a default field, but we know that the _all field is of type string.

So fields of type date and fields of type string are indexed differently, and can thus be searched differently. That’s not entirely surprising. You might expect that each of the core data types—strings, numbers, Booleans, and dates—might be indexed slightly differently. And this is true: there are slight differences.

But by far the biggest difference is between fields that represent exact values (which can include string fields) and fields that represent full text. This distinction is really important—it’s the thing that separates a search engine from all other databases.



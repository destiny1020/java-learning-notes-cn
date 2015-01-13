#对多值桶排序(Sorting Multivalue Buckets)

Multivalue buckets—the terms, histogram, and date_histogram—dynamically produce many buckets. How does Elasticsearch decide the order that these buckets are presented to the user?
多值桶 - terms, histogram以及date_histogram - 会动态地生成很多桶。那么ES是如何决定以何种顺序将它们展现给用户的呢？

By default, buckets are ordered by doc_count in descending order. This is a good default because often we want to find the documents that maximize some criteria: price, population, frequency. But sometimes you’ll want to modify this sort order, and there are a few ways to do it, depending on the bucket.
# 邻近匹配(Proximity Matching) #

使用了TF/IDF的标准全文搜索将文档，或者至少文档中的每个字段，视作"一大袋的单词"(Big bag of Words)。match查询能够告诉我们这个袋子中是否包含了我们的搜索词条，但是这只是一个方面。它不能告诉我们关于单词间关系的任何信息。

考虑以下这些句子的区别：

- Sue ate the alligator.
- The alligator ate Sue.
- Sue never goes anywhere without her alligator-skin purse.

一个使用了sue alligator的match查询会匹配以上所有文档，但是它无法告诉我们这两个词是否表达了部分原文的部分意义，或者是表达了完整的意义。

理解单词间的联系是一个复杂的问题，我们也无法仅仅依靠另一类查询就解决这个问题，但是我们至少可以通过单词间的距离来判断单词间可能的关系。

真实的文档也许比上面几个例子要长的多：Sue和alligator也许相隔了几个段落。也许我们仍然希望包含这样的文档，但是我们会给那些Sue和alligator出现的较近的文档更高的相关度分值。

这就是短语匹配(Phrase Matching)，或者邻近度匹配(Proximity Matching)。

**TIP**

本章中，我们仍然会使用[match查询](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/match-query.html#match-test-data)中使用的示例文档。


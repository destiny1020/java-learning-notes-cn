# 邻近匹配(Proximity Matching) #

使用了TF/IDF的标准全文搜索将文档，或者至少文档中的每个字段，视作"一大袋的单词"(Big bag of Words)。match查询能够告诉我们这个袋子中是否包含了我们的搜索词条，但是这只是一个方面。它不能告诉我们关于单词间关系的任何信息。

考虑以下这些句子的区别：

- Sue ate the alligator.
- The alligator ate Sue.
- Sue never goes anywhere without her alligator-skin purse.

一个使用了sue alligator的match查询会匹配以上所有文档，但是它无法告诉我们
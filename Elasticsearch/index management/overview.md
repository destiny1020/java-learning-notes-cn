# 索引管理 #

本文翻译自Elasticsearch官方指南的[索引管理(Index Management)](http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/index-management.html)一章

我们已经了解了ES是如何在不需要任何复杂的计划和安装就能让我们很容易地开始开发一个新的应用的。但是，用不了多久你就会想要仔细调整索引和搜索过程来更好的适配你的用例。

几乎所有的定制都和索引(Index)以及其中的类型(Type)相关。本章我们就来讨论用于管理索引和类型映射的API，以及最重要的设置。

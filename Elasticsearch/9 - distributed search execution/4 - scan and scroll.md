## scan和scroll ##

`scan`搜索类型以及`scroll` API会被一同使用来从ES中高效地获取大量的文档，而不会有深度分页(Deep Pagination)中存在的问题。

### scroll ###

一个滚动(Scroll)搜索能够让我们指定一个初始搜索(Initial Search)，然后继续从ES中获取批量的结果，直到所有的结果都被获取。这有点像传统数据库中的游标(Cursor)。

一个滚动搜索会生成一个实时快照(Snapshot) - 它不会发现在初始搜索后，索引发生的任何变化。它通过将老的数据文件保存起来来完成这一点，因此它能够保存一个在它开始时索引的视图(View)。

### scan ###

在深度分页中最耗费资源的部分是对全局结果进行排序，但是如果我们禁用了排序功能的话，就能够快速地返回所有文档了。我们可以使用`scan`搜索类型来完成。它告诉ES不要执行排序，只是让每个还有结果可以返回的分片返回下一批结果。

------

为了使用scan和scroll，我们将搜索类型设置为`scan`，同时传入一个`scroll`参数来告诉ES，scroll会开放多长时间：

```
GET /old_index/_search?search_type=scan&scroll=1m 
{
    "query": { "match_all": {}},
    "size":  1000
}
```

以上请求会让scroll开放一分钟。

此请求的响应不会含有任何的结果，但是它会含有一个`_scroll_id`，它是一个通过Base64编码的字符串。现在可以通过将`_scroll_id`发送到`_search/scroll`来获取第一批结果：

```
GET /_search/scroll?scroll=1m 
c2Nhbjs1OzExODpRNV9aY1VyUVM4U0NMd2pjWlJ3YWlBOzExOTpRNV9aY1VyUVM4U0 
NMd2pjWlJ3YWlBOzExNjpRNV9aY1VyUVM4U0NMd2pjWlJ3YWlBOzExNzpRNV9aY1Vy
UVM4U0NMd2pjWlJ3YWlBOzEyMDpRNV9aY1VyUVM4U0NMd2pjWlJ3YWlBOzE7dG90YW
xfaGl0czoxOw==
```

该请求会让scroll继续开放1分钟。`_scroll_id`能够通过请求的正文部分，URL或者查询参数传入。

注意我们又一次指定了`?scroll=1m`。scroll的过期时间在每次执行scroll请求后都会被刷新，因此它只需要给我们足够的时间来处理当前这一批结果，而不是匹配的所有文档。

这个scroll请求的响应包含了第一批结果。尽管我们指定了`size`为1000，我们实际上能够获取更多的文档。`size`会被每个分片使用，因此每批最多能够获取到`size * number_of_primary_shards`份文档。

> **NOTE**
> 
> scroll请求还会返回一个新的`_scroll_id`。每次我们执行下一个scroll请求时，都需要传入上一个scroll请求返回的`_scroll_id`。

当没有结果被返回是，我们就处理完了所有匹配的文档。

> **TIP**
> 
> 一些ES官方提供的客户端提供了scan和scroll的工具用来封装这个功能。





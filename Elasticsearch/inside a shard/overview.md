low-level worker unit，真正干活的。。

本章会回答以下问题：

- 为什么搜索是近乎实时的？
- 为什么文档的CRUD操作是实时的？
- ES是如何保证你所做的修改是可持久的，即发生power failure的时候也不会丢失？
- 为什么删除文档不会立即释放空间？
- refresh，flush和optimize API的作用以及你合适应该使用它们？


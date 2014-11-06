## 删除索引 ##

To delete an index, use the following request:

DELETE /my_index
You can delete multiple indices with:

DELETE /index_one,index_two
DELETE /index_*
You can even delete ALL indices with:

DELETE /_all
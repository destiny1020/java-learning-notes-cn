## 反规范化和并发(Denormalization and Concurrency)

Of course, data denormalization has downsides too. The first disadvantage is that the index will be bigger because the _source document for every blog post is bigger, and there are more indexed fields. This usually isn’t a huge problem. The data written to disk is highly compressed, and disk space is cheap. Elasticsearch can happily cope with the extra data.
当然，数据反规范化也有弊端。首先，它会让索引变大，因为每篇博文的_source都变大了，与此同时需要索引的字段也变多了。通常这并不是一个大问题。写入到磁盘的数据会被高度压缩，而且磁盘存储空间也不贵。ES能够很从容地处理这些多出来的数据。

The more important issue is that, if the user were to change his name, all of his blog posts would need to be updated too. Fortunately, users don’t often change names. Even if they did, it is unlikely that a user would have written more than a few thousand blog posts, so updating blog posts with the [scroll](https://www.elastic.co/guide/en/elasticsearch/guide/current/scan-scroll.html) and [bulk](https://www.elastic.co/guide/en/elasticsearch/guide/current/bulk.html) APIs would take less than a second.
更重要的弊端在于，如果用户修改了他的名字，他名下的所有博文都需要被更新。即使用户真的这么做了，一位用户也不太可能写了上千篇博文，因此通过[scroll](https://www.elastic.co/guide/en/elasticsearch/guide/current/scan-scroll.html)和[bulk](https://www.elastic.co/guide/en/elasticsearch/guide/current/bulk.html) APIs，更新也不会超过1秒。

However, let’s consider a more complex scenario in which changes are common, far reaching, and, most important, concurrent.
然而，让我们来考虑一个变化更常见，影响更深远和重要的复杂情景 - 并发。

In this example, we are going to emulate a filesystem with directory trees in Elasticsearch, much like a filesystem on Linux: the root of the directory is /, and each directory can contain files and subdirectories.
在这个例子中，我们通过ES来模拟一个拥有目录树的文件系统，就像Linux上的文件系统那样：根目录是/，每个目录都能包含文件和子目录。

We want to be able to search for files that live in a particular directory, the equivalent of this:
我们希望能够搜索某个目录下的文件，就像下面这样：

`grep "some text" /clinton/projects/elasticsearch/*`

This requires us to index the path of the directory where the file lives:
它需要我们对文件的路径进行索引：

```json
PUT /fs/file/1
{
  "name":     "README.txt", 
  "path":     "/clinton/projects/elasticsearch", 
  "contents": "Starting a new Elasticsearch project is easy..."
}
```

> **NOTE**
> 
> Really, we should also index directory documents so we can list all files and subdirectories within a directory, but for brevity’s sake, we will ignore that requirement. 说实在的，我们同时也应该一个目录下所有的文件和子目录进行索引，但是为了简洁起见，这里忽略了这一需求。

We also want to be able to search for files that live anywhere in the directory tree below a particular directory, the equivalent of this:
我们还需要能够搜索某个目录下任意深度的文件，就像下面这样：

`grep -r "some text" /clinton`

To support this, we need to index the path hierarchy:
为了支持这一需求，需要对路径层次进行索引：

- /clinton
- /clinton/projects
- /clinton/projects/elasticsearch

This hierarchy can be generated automatically from the path field using the [path_hierarchy tokenizer](http://bit.ly/1AjGltZ):
该层次结构可以通过对path字段使用[path_hierarchy tokenizer](http://bit.ly/1AjGltZ)来自动生成：

```json
PUT /fs
{
  "settings": {
    "analysis": {
      "analyzer": {
        "paths": {   (1)
          "tokenizer": "path_hierarchy"
        }
      }
    }
  }
}
```
(1) The custom paths analyzer uses the path_hierarchy tokenizer with its default settings. See [path_hierarchy tokenizer](http://bit.ly/1AjGltZ). 以上的自定义的分析器使用了path_hierarchy分词器，使用其默认设置。

The mapping for the file type would look like this:
文件类型的映射则像下面这样：

```json
PUT /fs/_mapping/file
{
  "properties": {
    "name": {   (1)
      "type":  "string",
      "index": "not_analyzed"
    },
    "path": {   (2)
      "type":  "string",
      "index": "not_analyzed",
      "fields": {
        "tree": {   (3)
          "type":     "string",
          "analyzer": "paths"
        }
      }
    }
  }
}
```

(1) The name field will contain the exact name. name字段会包含完整的名字。

(2)(3) The path field will contain the exact directory name, while the path.tree field will contain the path hierarchy. path字段会包含完整的目录名，而path.tree字段会包含路径层次结构。

Once the index is set up and the files have been indexed, we can perform a search for files containing elasticsearch in just the /clinton/projects/elasticsearch directory like this:
一旦建立了该索引并完成了文件的索引，我们就能够执行如下查询，它搜索/client/projects/elasticsearch目录下包含有elasticsearch的文件：

```json
GET /fs/file/_search
{
  "query": {
    "filtered": {
      "query": {
        "match": {
          "contents": "elasticsearch"
        }
      },
      "filter": {
        "term": {   (1)
          "path": "/clinton/projects/elasticsearch"
        }
      }
    }
  }
}
```

(1) Find files in this directory only. 只在该目录下搜索。

Every file that lives in any subdirectory under /clinton will include the term /clinton in the path.tree field. So we can search for all files in any subdirectory of /clinton as follows:
任何存储于/clinton目录下的文件都会在path.tree字段中包含/clinton。因此我们可以通过下面的搜索来得到/clinton目录下的所有文件：


```json
GET /fs/file/_search
{
  "query": {
    "filtered": {
      "query": {
        "match": {
          "contents": "elasticsearch"
        }
      },
      "filter": {
        "term": {   (1)
          "path.tree": "/clinton"
        }
      }
    }
  }
}
```

(1) Find files in this directory or in any of its subdirectories. 寻找该目录或其下任意子目录中的文件。

### 重命名文件和目录(Renaming Files and Directories)

So far, so good. Renaming a file is easy—a simple update or index request is all that is required. You can even use [optimistic concurrency control](https://www.elastic.co/guide/en/elasticsearch/guide/current/optimistic-concurrency-control.html) to ensure that your change doesn’t conflict with the changes from another user:
到目前为止还不错。文件重命挺简单 - 只需要一个简单的更新或者索引请求就行了。你甚至可以使用[乐观并发控制(Optimistic Concurrency Control)](https://www.elastic.co/guide/en/elasticsearch/guide/current/optimistic-concurrency-control.html)来确保你的更改不会和另一个用户的更改发生冲突。

```json
PUT /fs/file/1?version=2    (1)
{
  "name":     "README.asciidoc",
  "path":     "/clinton/projects/elasticsearch",
  "contents": "Starting a new Elasticsearch project is easy..."
}
```

(1) The version number ensures that the change is applied only if the document in the index has this same version number. version值能够确保只有当索引中的文档拥有相同的version值时，更改才会生效。

We can even rename a directory, but this means updating all of the files that exist anywhere in the path hierarchy beneath that directory. This may be quick or slow, depending on how many files need to be updated. All we would need to do is to use [scan-and-scroll](https://www.elastic.co/guide/en/elasticsearch/guide/current/scan-scroll.html) to retrieve all the files, and the [bulk API](https://www.elastic.co/guide/en/elasticsearch/guide/current/bulk.html) to update them. The process isn’t atomic, but all files will quickly move to their new home.
我们还可以对目录重命名，但是这意味着对该目录下的所有文件执行更新。这个操作或快或慢，取决于有多少文件需要被更新。我们需要做的是使用[scan-and-scroll](https://www.elastic.co/guide/en/elasticsearch/guide/current/scan-scroll.html)来获取所有的文件，然后使用[bulk API](https://www.elastic.co/guide/en/elasticsearch/guide/current/bulk.html)完成更新。这个过程并不是原子性的，但是所有的文件都能够被迅速地更新。
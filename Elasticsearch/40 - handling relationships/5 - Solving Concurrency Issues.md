# 解决并发问题(Solving Concurrency Issues)

The problem comes when we want to allow more than one person to rename files or directories at the same time. Imagine that you rename the /clinton directory, which contains hundreds of thousands of files. Meanwhile, another user renames the single file /clinton/projects/elasticsearch/README.txt. That user’s change, although it started after yours, will probably finish more quickly.
当我们允许多个用户同时对文件和目录进行重命名时，问题就来了。假设你对/clinton目录进行重命名，它包含了成百上千个文件。同时，另外一个用户重命名了/clinton/projects/elasticsearch/README.txt这个文件。该用户的更改虽然在你的操作之后，但是它完成的也许更快。

One of two things will happen:
下面两种情况中的一种会发生：

- You have decided to use version numbers, in which case your mass rename will fail with a version conflict when it hits the renamed README.asciidoc file. 你决定使用version值，这意味着你对文件README.txt的重命名操作会因为version冲突而失败。
- You didn’t use versioning, and your changes will overwrite the changes from the other user. 你不使用versioning，那么你的更改会直接覆盖掉另一用户的更改。

The problem is that Elasticsearch does not support [ACID transactions](http://en.wikipedia.org/wiki/ACID_transactions). Changes to individual documents are ACIDic, but not changes involving multiple documents.
问题的根源在ES并不支持[ACID事务](http://en.wikipedia.org/wiki/ACID_transactions)。对单个文档的更新虽然是符合ACID原则的，但是对多个文档的更新则不然。

If your main data store is a relational database, and Elasticsearch is simply being used as a search engine or as a way to improve performance, make your changes in the database first and replicate those changes to Elasticsearch after they have succeeded. This way, you benefit from the ACID transactions available in the database, and all changes to Elasticsearch happen in the right order. Concurrency is dealt with in the relational database.
如果你使用的主要数据源是关系行数据库，而ES只是简单地被用来当作搜索引擎或者提升性能的方法，那么首先更新数据库，然后待这些更新操作成功后再将这些变更复制到ES中。这样的话，你就能受益于数据库对于ACID事务的支持了，它能保证ES中的所有更新顺序都是正确的。并发的问题在关系型数据库中被处理了。

If you are not using a relational store, these concurrency issues need to be dealt with at the Elasticsearch level. The following are three practical solutions using Elasticsearch, all of which involve some form of locking:
如果你没有使用关系型数据源，那么这些并发问题就需要在ES中完成。下面有三种可行的方案，这些方案都涉及到了某种形式的锁：

- 全局锁(Global Locking)
- 文档锁(Document Locking)
- 树锁(Tree Locking)

> **TIP**
> 
> The solutions described in this section could also be implemented by applying the same principles while using an external system instead of Elasticsearch. 以上提到的方案都可以通过应用了相同原则的外部系统实现。

### 全局锁(Global Locking)

We can avoid concurrency issues completely by allowing only one process to make changes at any time. Most changes will involve only a few files and will complete very quickly. A rename of a top-level directory may block all other changes for longer, but these are likely to be much less frequent.
我们可以通过在任何时候只允许一个进程执行更新操作来完全避免并发性的问题。大多数的修改只设计很少的几个文件，完成的也相当快。对于顶层目录的重命名也许会阻塞其它变更操作更久一点，但是这种情况发生的频率也会更低一些。

Because document-level changes in Elasticsearch are ACIDic, we can use the existence or absence of a document as a global lock. To request a lock, we try to create the global-lock document:
因为在ES中文档级别的变更是满足ACID的，我们可以将一份文档是否存在作为一个全局锁。为了获取一个锁，我们尝试去创建一个全局锁文档：

```json
PUT /fs/lock/global/_create
{}
```

If this create request fails with a conflict exception, another process has already been granted the global lock and we will have to try again later. If it succeeds, we are now the proud owners of the global lock and we can continue with our changes. Once we are finished, we must release the lock by deleting the global lock document:
如果上述的创建请求由于发生了冲突而失败了，就表明另一个进程已经被授权得到了全局锁，我们只能稍后重试。如果上述请求成功了，我们就拥有了全局锁从而可以进行后续的变更操作。一旦这些操作完成了，必须通过删除全局锁文档来释放它：

```json
DELETE /fs/lock/global
```

Depending on how frequent changes are, and how long they take, a global lock could restrict the performance of a system significantly. We can increase parallelism by making our locking more fine-grained.
取决于变更的频繁程度和它们需要消耗的时间，全局锁对系统的性能或许会有相当程度的限制。可以通过实现更加细粒度的锁来增加并行性。

### 文档锁(Document Locking)

Instead of locking the whole filesystem, we could lock individual documents by using the same technique as previously described. A process could use a [scan-and-scroll](https://www.elastic.co/guide/en/elasticsearch/guide/current/scan-scroll.html) request to retrieve the IDs of all documents that would be affected by the change, and would need to create a lock file for each of them:
相比于对整个文件系统上锁，我们可以通过上面提到的技术来对单个文档完成锁定。一个进程可以使用[scan-and-scroll](https://www.elastic.co/guide/en/elasticsearch/guide/current/scan-scroll.html)请求来获取到变更会影响到的所有文档的IDs，然后对每份文档创建一个锁文件：

```json
PUT /fs/lock/_bulk
{ "create": { "_id": 1}}   (1)
{ "process_id": 123    }   (2)
{ "create": { "_id": 2}}
{ "process_id": 123    }
...
```

(1) The ID of the lock document would be the same as the ID of the file that should be locked. 锁文档的ID需要和被锁定的文档的ID一致。

(2) The process_id is a unique ID that represents the process that wants to perform the changes. process_id是将要执行变更操作进程的唯一ID。

If some files are already locked, parts of the bulk request will fail and we will have to try again. 
如果某些文档已经被锁了，那么部分bulk请求会失败，只好重试。

Of course, if we try to lock all of the files again, the create statements that we used previously will fail for any file that is already locked by us! Instead of a simple create statement, we need an update request with an upsert parameter and this script:
当然，如果我们试图再次去锁定所有的文档，对于那些已经被我们锁定的文档，前面使用的创建语句会失败！相比一个简单的创建语句，我们需要使用带有upsert参数的update请求：

```json
if ( ctx._source.process_id != process_id ) {   (1)
  assert false;   (2)
}
ctx.op = 'noop';  (3)
```

(1) process_id is a parameter that we pass into the script. process_id是我们传入到脚本中的参数。

(2) assert false will throw an exception, causing the update to fail. assert false会抛出一个异常，它导致更新失败。

(3) Changing the op from update to noop prevents the update request from making any changes, but still returns success. 将op从update修改为noop能够防止update请求真的执行变更操作，而仍然返回成功。

The full update request looks like this:
完整的update请求如下所示：

```json
POST /fs/lock/1/_update
{
  "upsert": { "process_id": 123 },
  "script": "if ( ctx._source.process_id != process_id )
  { assert false }; ctx.op = 'noop';"
  "params": {
    "process_id": 123
  }
}
```

If the document doesn’t already exist, the upsert document will be inserted—much the same as the create request we used previously. However, if the document does exist, the script will look at the process_id stored in the document. If it is the same as ours, it aborts the update (noop) and returns success. If it is different, the assert false throws an exception and we know that the lock has failed.
如果文档并不存在，upsert会执行insert操作 - 就和之前使用的create请求一样。但是，如果文档存在，脚本会查看文档中保存的process_id。如果它和我们的相同，就会放弃update(noop)并返回成功。如果它和我们的不同，那么assert false就会抛出一个异常告诉我们锁定失败了。

Once all locks have been successfully created, the rename operation can begin. Afterward, we must release all of the locks, which we can do with a delete-by-query request:
一旦所有的锁都别成功创建了，重命名操作就开始了。在这之后，我们必须释放所有的锁，通过delete-by-query请求来完成：

```json
POST /fs/_refresh   (1)

DELETE /fs/lock/_query
{
  "query": {
    "term": {
      "process_id": 123
    }
  }
}
```

(1) The refresh call ensures that all lock documents are visible to the delete-by-query request. refresh调用保证了所有的锁文档对delete-by-query请求是可见的。

Document-level locking enables fine-grained access control, but creating lock files for millions of documents can be expensive. In certain scenarios, such as this example with directory trees, it is possible to achieve fine-grained locking with much less work.
文档级别的锁拥有更加细粒度的访问控制，但是为百万计的文档创建锁是非常昂贵的。在某些场合下，比如前面例子中出现的目录树，可以通过更少的工作来达到细粒度的锁定。

### 树锁(ree Locking)

Rather than locking every involved document, as in the previous option, we could lock just part of the directory tree. We will need exclusive access to the file or directory that we want to rename, which can be achieved with an exclusive lock document:
相比像前面那样对每份文档上锁，也可以支队目录树的部分上锁。我们需要对重命名的文档或者目录拥有独占性访问，可以通过独占性锁文档实现：

```json
{ "lock_type": "exclusive" }
```

And we need shared locks on any parent directories, with a shared lock document:
同时我们也需要对上层目录使用分享锁：

```json
{
  "lock_type":  "shared",
  "lock_count": 1   (1)
}
```

(1) The lock_count records the number of processes that hold a shared lock. lock_count记录了拥有该分享锁的进程数量。

A process that wants to rename /clinton/projects/elasticsearch/README.txt needs an exclusive lock on that file, and a shared lock on /clinton, /clinton/projects, and /clinton/projects/elasticsearch.
对/clinton/projects/elasticsearch/README.txt重命名的进程需要该文件的独占锁，以及针对目录/clinton，/clinton/projects和/clinton/projects/elasticsearch的分享锁。

A simple create request will suffice for the exclusive lock, but the shared lock needs a scripted update to implement some extra logic:
对于独占锁，可以通过简单的创建请求来实现，但是分享锁需要带有脚本的update请求来实现：

```json
if (ctx._source.lock_type == 'exclusive') {
  assert false;   (1)
}
ctx._source.lock_count++   (2)
```

(1) If the lock_type is exclusive, the assert statement will throw an exception, causing the update request to fail. 如果lock_type是独占性的，那么assert语句会抛出一个异常，导致update请求失败。

(2) Otherwise, we increment the lock_count. 否则，增加lock_count。

This script handles the case where the lock document already exists, but we will also need an upsert document to handle the case where it doesn’t exist yet. The full update request is as follows:
该脚本能够处理当锁文档已经存在的情况，但是我们仍然需要使用upsert来处理它不存在时的情况。完整的update请求如下所示：

```json
POST /fs/lock/%2Fclinton/_update   (1)
{
  "upsert": {   (2)
    "lock_type":  "shared",
    "lock_count": 1
  },
  "script": "if (ctx._source.lock_type == 'exclusive')
  { assert false }; ctx._source.lock_count++"
}
```

(1) The ID of the document is /clinton, which is URL-encoded to %2fclinton. 文档的ID是/clinton，再进行URL编码后变成了%2fclinton。

(2) The upsert document will be inserted if the document does not already exist. 当文档不存在时，upsert代表的文档会被插入。

Once we succeed in gaining a shared lock on all of the parent directories, we try to create an exclusive lock on the file itself:
一旦我们成功获取了文件所在目录的所有上级目录的分享锁后，就可以尝试去创建一个针对该文件的独占锁了：

```json
PUT /fs/lock/%2Fclinton%2fprojects%2felasticsearch%2fREADME.txt/_create
{ "lock_type": "exclusive" }
```

Now, if somebody else wants to rename the /clinton directory, they would have to gain an exclusive lock on that path:
现在，如果另外的某个人想要对/clinton目录重命名，他们就需要获取该目录的独占锁：

```json
PUT /fs/lock/%2Fclinton/_create
{ "lock_type": "exclusive" }
```

This request would fail because a lock document with the same ID already exists. The other user would have to wait until our operation is done and we have released our locks. The exclusive lock can just be deleted:
该请求会失败，因为一个拥有相同ID的锁文档已经存在了。该用户只好等待我们的操作完成并释放锁。独占锁可以被删除：

```json
DELETE /fs/lock/%2Fclinton%2fprojects%2felasticsearch%2fREADME.txt
```

The shared locks need another script that decrements the lock_count and, if the count drops to zero, deletes the lock document:
而分享锁需要另一个脚本来减少lock_count的计数，如果该计数减少到了0,就删除它：

```json
if (--ctx._source.lock_count == 0) {
  ctx.op = 'delete'   (1)
}
```

(1) Once the lock_count reaches 0, the ctx.op is changed from update to delete. 一旦lock_count为0，ctx.op就从update变成delete。

This update request would need to be run for each parent directory in reverse order, from longest to shortest:
对每个上级目录都需要以相反的顺序执行update请求，从最长的目录到最短的目录：

```json
POST /fs/lock/%2Fclinton%2fprojects%2felasticsearch/_update
{
  "script": "if (--ctx._source.lock_count == 0) { ctx.op = 'delete' } "
}
```

Tree locking gives us fine-grained concurrency control with the minimum of effort. Of course, it is not applicable to every situation—the data model must have some sort of access path like the directory tree for it to work.
树锁通过最少的代价实现了细粒度的并发控制。当然，它并不能适用于每个场景 - 数据模型必须要有类似目录树这种结构才行。

> **NOTE**
> 
> None of the three options—global, document, or tree locking—deals with the thorniest problem associated with locking: what happens if the process holding the lock dies? 以上的三种方案 - 全局锁，文档锁和树锁 - 都没有处理关于锁的最棘手的问题：如果持有锁的进程挂了怎么办？
> 
> The unexpected death of a process leaves us with two problems: 一个进程的意外挂掉给我们留下了两个问题：
> 
> - How do we know that we can release the locks held by the dead process? 我们如何知道我们可以释放被挂掉的进程持有的锁？
> - How do we clean up the change that the dead process did not manage to complete? 我们如何清理挂掉的进程没有完成的工作？
>
> These topics are beyond the scope of this book, but you will need to give them some thought if you decide to use locking. 这些话题都超出了本书的范畴，但是如果你决定使用锁，就需要考虑一下它们。

While denormalization is a good choice for many projects, the need for locking schemes can make for complicated implementations. Instead, Elasticsearch provides two models that help us deal with related entities: nested objects and parent-child relationships.
尽管反规范化对于很多项目而言都是一个好的选择，由于需要锁的支持，会导致较为复杂的实现。相比之下，ES提供了另外两种模型来处理关联的实体：嵌套对象(Nested Objects)和父子关系(Parent-Child Relationship)。
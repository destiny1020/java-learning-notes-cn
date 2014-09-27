# Java SE API Tips #

---

# 缓冲I/O(Buffered I/O) #

`InputStream.read()`以及`OutputStream.write()`操作的对象是单个字节。根据它们访问的资源的不同，使用这些方法可能会相当慢。

比如在使用`FileInputStream.read()`时，速度会慢的令人发指。因为每次调用都会访问操作系统的内核去拿到1个字节的数据。在现代的操作系统中，内核往往会使用缓冲I/O实现，因此这个操作还不至于每次调用时会触发一次磁盘读取操作。但是缓冲区毕竟是在内核中的，所以每次调用该方法还是意味着会发生一次昂贵的系统调用来获取到内核I/O缓冲区中的1个字节。

对于写数据也是一样的。每次调用`FileOutputStream.write()`方法都会将1个自己的数据存储到内核的缓冲区中。最终当文件被关闭或者调用flush方法的时候，内核才会将缓冲区的内容写入到磁盘。

**对于基于文件的二进制数据I/O(File-based Binary Data I/O)，务必使用BufferedInputStream或者BufferedOutputStream对底层的文件字节流进行一次封装。**

**对于基于文件的字符数据I/O(File-based Character Data I/O)，务必使用BufferedReader或者BufferedWriter对底层的文件字符流进行一次封装。**

实际上，以上的最佳实践不仅仅只限于文件I/O，对于其它各种类型的I/O几乎都适用。比如通过Socket得到的字节流(通过`getInputStream()`和`getOutputStream()`获取)，在使用它们之前，也务必使用缓冲过滤流(Buffering Filter Stream)对它们进行封装。

然而，还是有特例的。当使用ByteArrayInputStream和ByteArrayOutputStream类型时，不要对它们使用缓冲过滤流。这两种类型会在内存中设置一片区域作为缓冲区，所以在为它们设置缓冲过滤流时，相当于会让数据被拷贝两次，以ByteArrayInputStream为例：

1. 从内核缓冲区到缓冲过滤流的缓冲区
2. 从缓冲过滤流的缓冲区到ByteArrayInputStream

当有其它过滤流(Filtering Stream)参与进来时，是否使用缓冲过滤流就需要具体问题具体分析了。比如在一个序列化的例子中：

```java
private void writeObject(ObjectOutputStream out) throws IOException {
	if (prices == null) {
		makePrices();
	}
	out.defaultWriteObject();
}

protected void makePrices() throws IOException {
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	ObjectOutputStream oos = new ObjectOutputStream(baos);
	oos.writeObject(prices);
	oos.close();
}
```

尽管ObjectOutputStream一次只会发送一个字节到下一个Stream，但是当下一个Stream就是最终的ByteArrayOutputStream时，使用BufferedOutputStream就没有意义了。这只会增加数据的拷贝次数，从而导致性能的下降。

但是当在ByteArrayOutputStream和ObjectOutputStream之间还存在其它的过滤流，也许过滤缓冲流就能派上用场了。比如当需要使用一个压缩过滤流将字节数组进行压缩时：

```java
private void writeObject(ObjectOutputStream out) throws IOException {
	if (prices == null) {
		makeZippedPrices();
	}
	out.defaultWriteObject();
}

protected void makeZippedPrices() throws IOException {
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	GZIPOutputStream zip = new GZIPOutputStream(baos);
	BufferedOutputStream bos = new BufferedOutputStream(zip);
	ObjectOutputStream oos = new ObjectOutputStream(bos);
	oos.writeObject(prices);
	oos.close();
	zip.close();
}
```

以上在GZIPOutputStream和ObjectOutputStream之间添加了一个BufferedOutputStream，这样做能够提高性能的原因是：当GZIPOutputStream的操作对象是一块数据时的性能会高于操作对象是一个字节时。

当使用Encoder/Decoder流来转换字节数据和字符数据时，使用缓冲过滤流对它们进行封装，也能够获得更好的性能。

下表是一组在进行带有压缩的序列化/反序列化时，是否使用缓冲过滤流对最终时间的影响：

| 操作 | 序列化时间 | 反序列化时间 |
| --- | --- | --- |
| 无缓冲的压缩/解压缩 | 60.3s | 79.3s |
| 有缓冲的压缩/解压缩 | 26.8s | 12.7s |

可见，当向GZIPOutputStream和ObjectOutputStream之间添加一个BufferedOutputStream后，性能的提升是多么地明显。

## 总结 ##

1. `InputStream.read()`以及`OutputStream.write()`的性能比较低，因为它们只是操作了一个字节。
2. 在对文件流，Socket流，压缩流和字符编码流进行操作时，确保使用了缓冲过滤流来封装它们。

---

# 字符串性能 #

字符串在任何的Java应用中都会被大量使用。它们的性能有以下几个方面的考虑：

- 字符串驻留(String Interning)
	TODO

- 字符串编码(String Encoding)
	TODO

- 网络编码(Network Encoding)
	TODO

- 字符串拼接(String Concatenation)
	```java
	// 编译器优化前
	String answer = integerPart + "." + mantissa;

	// 编译器优化后
	String answer = new StringBuilder(integerPart).append(".").append(mantissa).toString();
	```
	因为编译器会对字符串的拼接操作进行优化，所以在**同一条语句**中使用字符串拼接操作对性能并没有负面影响。正因为编译器在幕后的优化，在任何场景下都使用StringBuilder替代“+”操作符来进行字符串拼接是没有必要的。

	但是，当字符串的拼接是由**多条语句(或者循环)**完成的，就有问题了：
	```java
	// 编译器优化前
	String answer = integerPart;
	answer += ".";
	answer += mantissa;

	// 编译器优化后
	String answer = new StringBuilder(integerPart).toString();
	answer = new StringBuilder(answer).append(".").toString();
	answer = new StringBuilder(answer).append(mantissa).toString();
	```
	被编译器优化的代码中，中间的String对象以及StringBuilder对象实际上都是不需要的。此时，可以考虑将以上的拼接语句合并成一条拼接语句。或者直接显式地使用StringBuilder对象。

## 总结 ##

1. 当字符串拼接使用一条语句完成时，性能和使用StringBuilder时相当。
2. 对于使用多条语句完成字符串拼接时，考虑合并这些语句或者显式使用StringBuilder。

---

# 日志(Logging) #

TODO

---

# Java集合API #

Java 7提供了至少58个功能和实现各异的集合类型，在不同的场景下选择合适的集合类型十分重要。因为，程序的性能和集合类型的选择有莫大的关联。

关于选择哪个集合类型，第一个需要考虑的就是程序使用的算法和操作方式。实际上这就是从数据结构的出发点来看问题，和使用的语言无关。

比如，LinkedList不适合用在搜索操作较多的场合；如果需要以O(1)的开销从集合中得到某个元素，那么使用HashMap；如果集合中的元素需要保持有序，那么使用TreeMap而不要试图自己来对集合排序；如果希望能够通过索引访问元素那么可以考虑ArrayList；如果经常性的需要在有序集合的中间插入元素，那么就不要选择ArrayList，等等。

除了这些通用于所有语言的考虑之外，在Java中选择合适的集合时当然也有其他考虑。

## 同步还是非同步 ##

几乎所有的Java集合都是非同步的。除了Hashtable，Vector和其相关同步集合外。

> ### 同步集合的历史 ###
> 
> 在Java 1.2之前，Hashtable和Vector是仅有的集合类型。那时还没有Java集合框架(Java Collection Framework)这一概念。当时的Java是一门新兴的语言，大多数开发者都不能很好地理解线程机制(Threading)，所以Java的设计者们希望能尽量把语言设计的简单一些来让开发者们避开一些由于使用线程而导致的问题。因此，这些集合类就被设计成同步的，从而保证了线程安全。
> 
> 然而，在早期的Java中，同步带来的性能弊端是很严重的，即使是在没有竞争的同步(Uncontended Synchronization)中。所以在Java的下一个版本中，使用了一种截然不同的设计思路：所有的集合类型默认都是非同步的。

当在一个不存在竞争的环境中调用同步方法的性能如何呢？下表是在一个无竞争的环境中对分别对调用5亿次基于CAS的方法，同步方法和非同步方法的执行性能：

| 模式 | 总时间 | 单次操作时间 | 基线百分比 |
| --- | --- | --- | --- |
| CAS操作 | 6.6s | 13ns | 169.2% |
| 同步方法 | 11.8s | 23s | 302.6% |
| 非同步方法 | 3.9s | 7.8s | 100% |

就单词操作时间而言，差异并不太大。当时当一个应用需要运行相当长的时间且相应方法被执行的非常频繁时，还是能够看出它们的性能差异。无论使用相对高级的CAS操作还是传统的同步方法，在非竞争环境下的性能都比费同步方法落后很多。因此，需要仔细查看和考虑程序中被声明为同步的方法及同步代码块是否真的有必要。

所以在一个非竞争环境中，ArrayList的性能要比Vector的性能好大概2倍(100% vs 302.6%)。同时，HashMap的性能也比ConcurrentHashMap好大概0.7倍(100% vs 169.2%)。

## 集合容量(Collection Size) ##

对于Java中的集合类型，表示集合中元素的方法有以下几种情况：

- 使用数组进行集合元素的保存，比如ArrayList，HashMap等
- 使用自定义类型进行元素的保存，比如LinkedList中使用了Node类型表示单个元素
- 结合了数组和自定义类型的方式，比如HashMap它使用数组来作为元素的保存方式，但是数组元素的类型是HashMap$Entry

如何知道一个集合类型是否使用的是数组作为其元素的保存方式呢？可以查看该类型的构造函数，如果其中会接受一个初始空间的整型变量作为参数，那么它在内部使用的就是数组。

对于使用数组的集合，需要在对它们进行初始化时，给出一个精确的容量。这样能够带来更好的性能。比如ArrayList类型内部的数组默认容量是10，那么当需要存放第11个元素的时候，ArrayList会做以下几件事：

1. 计算扩容数组的新空间值
2. 创建该数组
3. 将当前数组的所有元素拷贝到新数组中

以上的第二步和第三步对性能会有较大的影响。

其它类型比如HashMap在对其内部数组进行扩容时，使用的算法会更加复杂，但是本质上也遵循上述的三个步骤。

数组的扩容容量是通过每次增加当前容量的一半计算得到的。比如对于一个ArrayList对象，初始的空容量是10，那么再需要扩容时，下一次会分配一个能容纳15个元素的数组，再下次是22个，然后33个，以此类推。使用这种方式扩容，在平均情况下数组的空间利用率大概是83.3%。所以当这个数组的容量本身已经非常大时，每次扩容都会带来大量的内存浪费，从而增加了GC的压力。这还不算拷贝数组这个操作带来的性能损耗。

> ### 非集合类型中的扩容 ###
> 
> 除了集合类型，还有不少类型也会在内部使用数组来存储和表示实际的数据。典型的比如：ByteArrayOutputStream，StringBuilder和StringBuffer。对于这些类型，也可以发现它们的构造函数也能够接受一个size作为参数来指定初始的容量。所以，在使用它们时尽可能地估计一个靠谱的初始容量能够带来更好的性能。

## 集合和内存效率(Collections and Memory Efficiency) ##

在使用基于数组的集合时，还有另外一个更极端的情况，即集合元素非常少。这种情况下，数组的空间利用率就更低了，导致的就是无谓的内存空间浪费和GC压力。对于这种情况，有两个解决方案：

- 在创建集合时指定容量(通过构造函数)
- 考虑使用单独的对象进行表示

当开发人员问问到哪种排序方法能够最快地对一个数组进行排序时，不少人会回答“快速排序”。但是好的开发人员则会首先问这个数组的容量是多大。如果一个数组的容量足够小，那么使用插入排序才是最快的排序方法。实际上，在快速排序中当被划分的子数组的容量小于某个阈值时，会转而使用插入排序。JDK中的Arrays.sort()方法中，就假设了当数组的容量小于47时，插入排序会有更好的性能。

> ### JDK 7u40 中集合的延迟初始化 ###
> 
> 由于在很多应用中，集合空间都没有被充分利用。在JDK 7u40中引入了一种针对ArrayList和HashMap实现的优化：当创建它们的实例时如果未指定容量信息，那么不再创建内部数组。只有当第一次对集合进行操作的时候，才会创建内部数组。这是延迟初始化的一个典型应用。在进行了大量测试后，证明在大多数应用中，使用该优化能够带来更好的性能。

## 总结 ##

1. 根据需要使用最合适的集合类型，注意应用场景是否真的需要同步集合。
2. 对于基于数组的集合，在创建它们时指定容量至关重要，这样能够带来更好的性能。



















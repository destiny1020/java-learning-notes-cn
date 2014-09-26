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












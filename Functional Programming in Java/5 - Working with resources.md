# 资源处理 #

Java本身自带了垃圾回收(Garbage Collection)功能，但是只有垃圾回收的目标是内部资源(Internal Resource)，典型的比如堆上分配的内存区域等。对于外部资源(External Resource)，如数据库连接，文件句柄，套接字等资源，还是需要在程序中进行显式回收的。

使用Lambda表达式可以实现一种叫做Execute Around的模式，用来处理外部资源的回收。关于Execute Around模式，可以参考这个[链接](http://stackoverflow.com/questions/341971/what-is-the-execute-around-idiom)。

## 回收资源 ##

下面是一个利用FileWriter完成消息写入的例子：

```java
public class FileWriterExample {
	private final FileWriter writer;
	public FileWriterExample(final String fileName) throws IOException {
		writer = new FileWriter(fileName);
	}
	public void writeStuff(final String message) throws IOException {
		writer.write(message);
	}
	public void finalize() throws IOException {
		writer.close();
	}
	//...
}

public static void main(final String[] args) throws IOException {
	final FileWriterExample writerExample = new FileWriterExample("peekaboo.txt");
	writerExample.writeStuff("peek-a-boo");
}
```

但是运行以上的main方法后会发现，文件peekaboo.txt虽然被创建了，但是它是空的。出现这种情况的原因在于文件并没有被关闭，也就是说finalize方法没有被调用。这个方法是由JVM负责调用的，这里没有调用是因为JVM认为此刻还有足够的内存，不需要执行finalize操作用来回收。毕竟垃圾回收操作也是需要消耗时间的，而且还是一种“Stop-the-world”(停下所有正在运行的应用程序代码)的方式。关于垃圾回收的基础知识，可以参考这篇[文章](http://blog.csdn.net/dm_vincent/article/details/39452011)。

实际上，在《Effective Java》这本书中，明确的指出了不要依赖于finalize方法来执行资源的回收。以上的代码违背这一准则。

### 关闭资源 ###

更好的方式是直接调用资源的close方法用来回收外部资源：

```java
public void close() throws IOException {
	writer.close();
}

final FileWriterExample writerExample = new FileWriterExample("peekaboo.txt");
writerExample.writeStuff("peek-a-boo");
writerExample.close();
```

调用以上的代码后，文件中确实有内容了，但是这种做法还是有问题。如果在调用writeStuff方法的时候就发生了异常，那么close方法就没有机会被执行了。

### 确保资源的关闭 ###

可以将close方法的调用放到finally语句中：

```java
final FileWriterExample writerExample = new FileWriterExample("peekaboo.txt");
try {
	writerExample.writeStuff("peek-a-boo");
} finally {
	writerExample.close();
}
```

这种写法也是目前十分主流的写法，很多代码都是这样处理外部资源的。但是不觉得这段代码噪声过多了，不够简洁吗？针对这种问题，Java 7中引入了自动资源管理(ARM，Automatic Resource Management)这一特性。它使用了一种特殊形式的try语句，编译器会自动地将包含close方法调用的finally语句块插入到try的最后。下面是一个例子：

```java
try(final FileWriterARM writerARM = new FileWriterARM("peekaboo.txt")) {
	writerARM.writeStuff("peek-a-boo");
	System.out.println("done with the resource...");
}
```

当try语句块执行完毕之后，writeARM这一资源就会被关闭。然而并不是所有的资源都能够利用ARM进行自动回收的，需要该资源类实现AutoCloseable接口，其中值包含了一个方法：close()。在Java 8中，Stream接口实现了AutoCloseable接口，也就意味着基于I/O的Stream也能够利用ARM来实现资源的自动回收。

为了使用ARM，重新实现的FileWriterARM如下：

```java
public class FileWriterARM implements AutoCloseable {
	private final FileWriter writer;
	public FileWriterARM(final String fileName) throws IOException {
		writer = new FileWriter(fileName);
	}
	public void writeStuff(final String message) throws IOException {
		writer.write(message);
	}
	public void close() throws IOException {
		System.out.println("close called automatically...");
		writer.close();
	}
	//...
}
```

ARM确实简化了代码，但是仍然需要开发人员去显示的调用它。如果没有调用，程序除了不会关闭资源外，也不会出现什么其他错误。因此，可以对它进行进一步的优化。

### 使用Lambda表达式来回收资源 ###

之前介绍的ARM有两个主要的缺点：

1. 资源需要实现AutoCloseable接口
2. 需要显式地使用它

下面我们看看如何使用Lambda表达式结合Execute Around模式来进行优化：

```java
public class FileWriterEAM {
	private final FileWriter writer;
	private FileWriterEAM(final String fileName) throws IOException {
		writer = new FileWriter(fileName);
	}
	private void close() throws IOException {
		System.out.println("close called automatically...");
		writer.close();
	}
	public void writeStuff(final String message) throws IOException {
		writer.write(message);
	}
	//...
}
```

可以发现，这个资源类的构造函数被声明成私有的了，也就意味着外部代码不能直接创建这种资源。close方法也被声明为私有的，只有writeStuff是公有的方法。

我们需要一个工厂方法来得到该资源类的实例，这一点可以通过静态方法结合Lambda表达式来办到：

```java
public static void use(final String fileName,
	final UseInstance<FileWriterEAM, IOException> block) throws IOException {
	final FileWriterEAM writerEAM = new FileWriterEAM(fileName);
	try {
		block.accept(writerEAM);
	} finally {
		writerEAM.close();
	}
}

@FunctionalInterface
public interface UseInstance<T, X extends Throwable> {
	void accept(T instance) throws X;
}
```

这个静态工厂方法和传统意义上的静态工厂方法不太一样。它并没有返回被创建的实例，而是立即在方法中使用了被创建的实例。use方法接受的第二个参数是UseInstance类型的函数接口，它和JDK中的Consumer非常类似，只不过它能够抛出一个异常。关于这一点，在之前的文章中进行了介绍。

另外还可以将ARM融合到上面的代码中：

```java
public static void use(final String fileName,
	final UseInstance<FileWriterEAM, IOException> block) throws IOException {
	try(final FileWriterEAM writerEAM = new FileWriterEAM(fileName)) {
		block.accept(writerEAM);
	}
}
```

只不过此时需要FileWriterEAM实现AutoCloseable接口，并将之前的close方法访问级别从私有变成公有。

使用它也非常简单：

```java
// case 1
FileWriterEAM.use("eam.txt", writerEAM -> writerEAM.writeStuff("sweet"));

// case 2
FileWriterEAM.use("eam2.txt", writerEAM -> {
	writerEAM.writeStuff("how");
	writerEAM.writeStuff("sweet");
});
```

这种模式克服了之前提到的首要缺点，即需要显式调用try with resource语句进行资源回收。并且它对资源对象的生命周期也进行了很好的控制，因此它也实现了Loan模式，只有在需要使用一个资源的时候才会创建它，并且在利用完毕之后立即将它标记为回收。

## 锁管理 ##

在并发程序中，锁是一类相当重要的资源，下面我们看看Lambda表达式如何处理锁资源。

历史悠久的synchronized代码块实际上就是一个典型的Execute Around模式的实现。synchronized关键词的出现能保证同一时刻至多只有一个线程能够运行这段代码。

但是synchronized关键字也有其缺点：

1. synchronized代码块难以进行超时处理
2. synchronized代码块难以进行单元测试

因此为了解决这些问题，Lock接口应运而生。Lock接口能够处理超时的情况，并且因为其本身是一个接口，也容易被Mocking而完成单元测试。但是天下没有免费的午餐，使用Lock时需要显式地进行加锁和解锁操作。

但是在Java 8中，可以使用Lambda表达式结合前面提到的Execute Around模式来轻松解决这一类问题，下面是一段使用了Lock的代码：

```java
public class Locking {
	Lock lock = new ReentrantLock(); //or mock
	protected void setLock(final Lock mock) {
		lock = mock;
	}
	public void doOp1() {
		lock.lock();
		try {
			//...critical code...
		} finally {
			lock.unlock();
		}
	}
	//...
}
```

上述的doOp1方法噪声太多，过多的加锁解锁和try finally语句块让代码的意图不够清晰。为了使用Lambda表达式，我们可以首先设计一段代码：

```java
public class Locker {
	public static void runLocked(Lock lock, Runnable block) {
		lock.lock();
		try {
			block.run();
		} finally {
			lock.unlock();
		}
	}
}
```

上述代码将加锁解锁操作和固定的try finally语句块给抽象成一个方法，然后将真正需要在锁环境中运行的代码通过一个Runnable参数传入。这样一来，其它需要锁环境的操作就可以这样实现了：

```java
public void doOp2() {
	runLocked(lock, () -> {/*...critical code ... */});
}
public void doOp3() {
	runLocked(lock, () -> {/*...critical code ... */});
}
public void doOp4() {
	runLocked(lock, () -> {/*...critical code ... */});
}
```
































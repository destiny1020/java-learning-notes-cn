# 使用JIT(Just-In-Time)编译器 #
---

## JIT编译器概览 ##

1. JIT编译器是JVM的核心。它对于程序性能的影响最大。
2. CPU只能执行汇编代码或者二进制代码，所有程序都需要被翻译成它们，然后才能被CPU执行。
3. C++以及Fortran这类编译型语言都会通过一个静态的编译器将程序编译成CPU相关的二进制代码。
4. PHP以及Perl这列语言则是解释型语言，只需要安装正确的解释器，它们就能运行在任何CPU之上。当程序被执行的时候，程序代码会被逐行解释并执行。
5. 编译型语言的优缺点：
	- 速度快：因为在编译的时候它们能够获取到更多的有关程序结构的信息，从而有机会对它们进行优化。
	- 适用性差：它们编译得到的二进制代码往往是CPU相关的，在需要适配多种CPU时，可能需要编译多次。
6. 解释型语言的优缺点：
	- 适应性强：只需要安装正确的解释器，程序在任何CPU上都能够被运行
	- 速度慢：因为程序需要被逐行翻译，导致速度变慢。同时因为缺乏编译这一过程，执行代码不能通过编译器进行优化。
7. Java的做法是找到编译型语言和解释性语言的一个中间点：
	- Java代码会被编译：被编译成Java字节码，而不是针对某种CPU的二进制代码。
	- Java代码会被解释：Java字节码需要被java程序解释执行，此时，Java字节码被翻译成CPU相关的二进制代码。
	- JIT编译器的作用：在程序运行期间，将Java字节码编译成平台相关的二进制代码。正因为此编译行为发生在程序运行期间，所以该编译器被称为Just-In-Time编译器。


### HotSpot 编译 ###

1. HotSpot VM名字也体现了JIT编译器的工作方式。在VM开始运行一段代码时，并不会立即对它们进行编译。在程序中，总有那么一些“热点”区域，该区域的代码会被反复的执行。而JIT编译器只会编译这些“热点”区域的代码。这么做的原因在于：
	- 编译那些只会被运行一次的代码性价比太低，直接解释执行Java字节码反而更快。
	- JVM在执行这些代码的时候，能获取到这些代码的信息，一段代码被执行的次数越多，JVM也对它们愈加熟悉，因此能够在对它们进行编译的时候做出一些优化。
		- 一个例子是：当在解释执行`b = obj.equals(otherObj)`的时候，需要查询该equals方法定义在哪个类型上，因为equals方法可能存在于继承树上的任意一个类。如果这段代码被会执行很多次，那么查询操作会耗费很多时间。而在JVM运行这段代码的时候，也许会发现equals方法定义在String类型上，那么当JIT编译器编译这段代码的时候，就会直接调用String类型上的equals方法(当然，在JIT编译得到的代码中，也会考虑到当obj的引用发生变化的时候，需要再次进行查询)。此时，这段代码会在两个方面被优化：
			- 由解释执行转换为编译执行
			- 跳过了方法查询阶段(直接调用String的equals方法)

### 总结 ###

1. Java综合了编译型语言和解释性语言的优势。
2. Java会将类文件编译成为Java字节码，然后Java字节码会被JIT编译器选择性地编译成为CPU能够直接运行的二进制代码。
3. 将Java字节码编译成二进制代码后，性能会被大幅度提升。

## 调优基础：客户端版或服务器版 ##

1. 一般只需要选择是使用客户端版或者服务器版的JIT编译器即可。
2. 客户端版的JIT编译器使用：`-client`指定，服务器版的使用：`-server`。
3. 选择哪种类型一般和硬件的配置相关，当然随着硬件的发展，也没有一个确定的标准哪种硬件适合哪种配置。
4. 两种JIT编译器的区别：
	- Client版对于代码的编译早于Server版，也意味着代码的执行速度在程序执行早期Client版更快。
	- Server版对代码的编译会稍晚一些，这是为了获取到程序本身的更多信息，以便编译得到优化程度更高的代码。因为运行在Server上的程序通常都会持续很久。
5. Tiered编译的原理：
	- JVM启动之初使用Client版JIT编译器
	- 当HotSpot形成之后使用Server版JIT编译器再次编译
6. 在Java 8中，默认使用Tiered编译方式。

---
### 启动优化 ###

**一组数据：**

| Application | -client | -server | -XX:+TieredCompilation | 类数量 |
| --- | --- | --- | --- | --- |
| HelloWorld | 0.08s | 0.08s | 0.08s | Few |
| NetBeans | 2.83s | 3.92s | 3.07s | ~10000 |
| HelloWorld | 51.5s | 54.0s | 52.0s | ~20000 |

#### 总结 ####

1. 当程序的启动速度越快越好时，使用Client版的JIT编译器更好。
2. 就启动速度而言，Tiered编译方式的性能和只使用Client的方式十分接近，因为Tiered编译本质上也会在启动是使用Client JIT编译器。

---
### 批处理优化 ###

对于批处理任务，任务量的大小是决定运行时间和使用哪种编译策略的最重要因素：

| Number of Tasks | -client | -server | -XX:+TieredCompilation |
| --- | --- | --- | --- |
| 1 | 0.142s | 0.176s | 0.165s |
| 10 | 0.211s | 0.348s | 0.226s |
| 100 | 0.454s | 0.674s | 0.472s |
| 1000 | 2.556s | 2.158s | 1.910s |
| 10000 | 23.78s | 14.03s | 13.56s |

可以发现几个结论：
- 当任务数量小的时候，使用Client或者Tiered方式的性能类似，而当任务数量大的时候，使用Tiered会获得最好的性能，因为它综合使用了Client和Server两种编译器，在程序运行之初，使用Client JIT编译器得到一部分编译过的代码，在程序“热点”逐渐形成之后，使用Server JIT编译器得到高度优化的编译后代码。
- Tiered编译方式的性能总是好于单独使用Server JIT编译器。
- Tiered编译方式在任务量不大的时候，和单独使用Client JIT编译器的性能相当。

#### 总结 ####
1. 当一段批处理程序需要被执行时，使用不同的策略进行测试，使用速度最快的那一种。
2. 对于批处理程序，考虑使用Tiered编译方式作为默认选项。

---
### 长时间运行应用的优化 ###

对于长时间运行的应用，比如Servlet程序等，一般会使用吞吐量来测试它们的性能。
以下的一组数据表示了一个典型的数据获取程序在使用不同“热身时间”以及不同编译策略时，对吞吐量(OPS)的影响(执行时间为60s)：

| Warm-up Period | -client | -server | -XX:+TieredCompilation |
| --- | --- | --- | --- |
| 0s | 15.87 | 23.72 | 24.23 |
| 60s | 16.00 | 23.73 | 24.26 |
| 300s | 16.85 | 24.42 | 24.43 |

即使当“热身时间”为0秒，因为执行时间为60秒，所以编译器也有机会在次期间做出优化。

从上面的数据可以发现的几个结论：
- 对于典型的数据获取程序，编译器对代码编译和优化发生的十分迅速，当“热身时间”显著增加时，如从60秒增加到300秒，最后得到的OPS差异并不明显。
- -server JIT编译器和Tiered编译的性能显著优于-client JIT编译器。

#### 总结 ####
1. 对于长时间运行的应用，总是使用-server JIT编译器或者Tiered编译策略。

## Java和JIT编译器版本 ##

以上讨论了JIT编译器的Client以及Server版本，但实际上，JIT编译器有三种：

- 32-bit Client (-client)
- 32-bit Server (-server)
- 64-bit Server (-d64)

在32-bit的JVM中，最多可以使用两种JIT编译器。
在64-bit的JVM中，只能使用一种，即-d64。(虽然实际上也含有两种，因为在Tiered编译模式下，Client和Server JIT都会被使用到)

### 关于32位或者64为JVM的选择 ###

- 如果你的OS是32位的，那么必须选择32位的JVM；如果你的OS是64位的，那么你可以选择32位或者64为的JVM。
- 如果你的计算机的内存小于3GB，那么使用32位的JVM性能更优。因为此时JVM中对于内存的引用也是32位的(也就是声明一个指向内存的变量会占用32位的空间)，所以操作这些引用的速度也会更快。
- 使用32位版本的缺点如下：
	- 可用的内存小于4GB，在某些Windows OS中是小于3GB，在某些旧版本的Linux中是小于3.5GB。
	- 对于double和long变量类型的操作速度会慢于64位版本，因为它们不能使用CPU的64位寄存器。
- 在通常情况下，如果你的程序对于内存的容量要求不那么高，且并不含有很多对于long和double类型的操作，那么选择32位往往更快，和64位相比，性能往往会提高5%-20%不等。

### OS和编译器参数之间的关系 ###

| JVM版本 | -client | -server | -d64 |
| --- | --- | --- | --- |
| Linux 32-bit | 32-bit client compiler | 32-bit server compiler | Error |
| Linux 64-bit | 64-bit server compiler | 64-bit server compiler | 64-bit server compiler |
| Mac OS X | 64-bit server compiler | 64-bit server compiler | 64-bit server compiler |
| Solaris 32-bit | 32-bit client compiler | 32-bit server compiler | Error |
| Solaris 64-bit | 32-bit client compiler | 32-bit server compiler | 64-bit server compiler |
| Windows 32-bit | 32-bit client compiler | 32-bit server compiler | Error |
| Windows 64-bit | 64-bit server compiler | 64-bit server compiler | 64-bit server compiler |

### OS和默认编译器的关系 ###

| OS | 默认JIT编译器 |
| --- | --- |
| Windows, 32-bit, any number of CPUs | -client |
| Windows, 64-bit, any number of CPUs | -server |
| Mac OS X, any number of CPUs | -server |
| Linux/Solaris, 32-bit, 1 CPU | -client |
| Linux/Solaris, 32-bit, 2 or more CPUs | -server |
| Linux, 64-bit, any number of CPUs | -server |
| Solaris, 32-bit/64-bit overlay, 1 CPU | -client |
| Solaris, 32-bit/64-bit overlay, 2 or more CPUs | -server (32-bit mode) |

OS和默认JIT的关系是建立在以下两个事实之上：
- 当Java程序运行在Windows 32位的计算机上时，程序的启动速度往往是最重要的，因为它面向的往往是最终用户。
- 当Java程序运行在Unix/Linux系统上时，程序往往是长时间运行的服务端程序，所以Server JIT编译器的优势更明显。

#### 总结 ####

1. 32位和64位的JVM支持的JIT编译器是不一样的。
2. 在不同的OS和架构(32bit/64bit)对于JIT编译器的支持是不一样的。
3. 即使声明了要使用某种JIT编译器，根据运行时平台的不同，实际使用的也不一定是指定的编译器。


## JIT编译器调优进阶 ##

对于绝大部分的场景，只设置使用哪种JIT编译器就足够了：-client, -server或者-XX:+TieredCompilation。
对长时间运行的应用，使用Tiered编译方式更好，即使在短时间运行的引用上使用它，性能也和使用Client编译器类似。

但是在另外一些场合下，还是需要进行另外一些调优。

### 代码缓存调优(Tuning the Code Cache) ###








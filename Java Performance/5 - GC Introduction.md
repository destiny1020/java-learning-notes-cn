# 垃圾回收(Garbage Collection)简介 #
---

## 概览 ##

在目前的JVM中，主要有4中垃圾回收器(Garbage Collector)：
- 串行回收器(Serial Collector)，主要用于单核计算机
- 吞吐量(并行)回收器(Throughput/Parallel Collector)
- 并发回收器(Concurrent/CMS Collector)
- G1回收器

它们的性能特点各不相同，具体会在下一章进行介绍。但是它们拥有一些共同的原理和概念，这一章就是为了讲解这些关于垃圾回收的基础知识。

---

简而言之，GC的任务就是找到那些不再被使用的的对象，然后释放它们占用的内存。
通常JVM会通过寻找不再被引用的那些对象，认为它们能够被回收。所以，也表示对象的所有引用都会通过一个计数器进行保存。但是仅仅通过这种引用计数的方式是不够的。比如循环引用的情况，所有的对象都会被引用到，但是并没有实际被使用。

所以，JVM必须要周期性地去搜索堆内存来发现那些没有被使用的对象，然后对它们进行回收，释放它们占用的内存。然而仅仅释放也是不够的，这样可能会带来大量的内存碎片(Memory Fragmentation)，所以JVM还需要对内存中的其它对象重新安排位置(Compacting)，来消除那些碎片。

**TODO:** image, f5-1

虽然GC的实现各异并且有很多细节，但是GC的性能主要取决于以上提到的几个操作：

- 搜索未被使用的对象
- 释放它们占用的内存
- 压缩堆(Compacting the Heap)


如果在GC运行的时候，应用程序的线程并不运行，那么事情就会简单许多。但是这是不太可能的，Java应用通常都会运用很多线程，那么当GC参与到这个过程中时，所有的线程大概能够被分为两个组：

- 应用程序线程
- GC线程

当GC线程在工作的时候，因为它们会对内存中的对象进行调整，而这些对象会被应用程序线程使用，所以必须保证GC线程工作的时候，应用程序线程要停下它们当前的工作。这种停止也会被称做Stop-the-world停止，它会对应用程序的性能造成严重的影响，所以在对GC进行调优的时候，减少这些停止的时间就是关键。

### 世代垃圾回收器(Generational Garbage Collectors) ###

虽然GC的实现各异，但是它们都会将堆内存分割成几个世代。它们分别是：

- 年老代(Old/Tenured Generation)
- 新生代(Young Generation)
	- Eden Space
	- Survivor Space

将内存进行分割的原因在于，在程序运行时的某一小段时间内，也许会有很多的对象被创建，使用，然后丢弃，比如：

```java
sum = new BigDecimal(0);
for (StockPrice sp : prices.values()) {
	BigDecimal diff = sp.getClosingPrice().subtract(averagePrice);
	diff = diff.multiply(diff);
	sum = sum.add(diff);
}
```

因为BigDecimal在Java中是不可变类型(Immutable Class)，在上面的每轮循环中都会创建若干个新的BigDecimal对象来完成计算。这些对象的生命周期很短，在一轮循环结束之后，马上就会变成GC的候选回收对象。

这种行为在Java应用中十分普遍，所有新创建的对象会被首先分配到新生代内存区域中。当新生代内存区域被占满之后，GC会停止所有应用程序的线程，然后尝试对新生代区域进行回收：
- 未被使用的对象的内存被释放
- 仍在使用的对象会被移动到另外的地方

这个过程被称为**Minor GC**。

使用这种设计的两个好处：
- 新生代仅仅是整个堆内存区域的一部分，所以只处理该区域更快，对应用程序的影响也更小(因为GC工作时，会造成应用程序的暂停)，但是你可能也注意到了，这会造成应用程序频繁被停止，因为GC的更频繁了，关于这一点，在后文中会进行讨论。
- 因为新创建的对象被分配在新生代区域的Eden Space中，它占了新生代的大部分区域。当GC对该区域进行回收时，对象要么被回收，要么被移动到一个Survivor Space，要么被移动到Old Generation，这也就保证了当GC执行完毕之后，Eden Space会被清空，也就省去了Compacting的环节。

**所有的GC算法在回收新生代内存区域的时候，都会停止所有应用程序线程(Stop-the-world Pause)。**

随着对象被移动到Old Generation，那么最终该区域也会被填满。此时JVM就需要该区域进行回收，不同的GC算法的实现大不相同。最简单的算法会停止所有应用程序线程，完成回收的三个步骤(寻找不用对象，释放内存，压缩内存)。这个过程被称为**Full GC**。它通常会造成应用程序的更长时间的停止。

另一方面，在寻找不用对象的过程中，也可以实现不停止应用程序线程：CMS和G1回收器就能够办到。这也是它们被称为并发回收器的原因。同时，它们在对Old Generation进行压缩的时候使用的算法也不一样。但是，使用并发回收器会消耗更多的CPU资源，因为它们的计算过程更加复杂。在某些场景下，CMS和G1也会造成应用程序的长时间停止，在使用它们时需要调优来避免这一点。

#### GC选择的一些建议 ####

- 服务器程序：并发回收器或者Throughput回收器
- 批处理程序：当CPU资源不是瓶颈时，使用并发回收器，否则可能会造成性能下降

#### 总结 ####

1. 所有的GC算法都会将堆内存分为Young Generation和Old Generation。
2. 所有的GC算法在回收Young Generation时，都会有一个Stop-the-world暂停，但是回收Young Generation的过程十分迅速。

---
### GC算法 ###

#### 串行回收器(Serial Garbage Collector) ####

它是四种回收器中最简单的一种，当应用运行在Client级别的计算机(使用32位JVM的Windows或者单处理器)上时，它是默认使用的回收器。

它使用一个线程来完成Heap的处理，无论Minor GC还是Full GC，都会造成应用程序的停止。在Full GC中，会对整个Old Generation进行压缩。

可以使用：`-XX:+UseSerialGC` 来使用它(注意在很多情况下，它就是默认的选择)。

#### 吞吐量回收器 ####

它是Server级别的计算机(多核Unix和使用64位JVM)的默认选择。

它会使用多个线程对回收Young Generation，因此Minor GC的速度相比串行回收器更快。对于Old Generation，它也能够利用多个线程进行回收操作，这是JDK 7u4和之后版本的默认行为。当然，在JDK 7u4之前，也可以通过：`-XX:+UseParallelOldGC` 来启用这一特性。因为它使用了多个线程，所以也被称为并行回收器。

和串行回收器一样，在Minor GC和Full GC时，它也会暂停所有的应用程序线程，在Full GC时完成整个Old Generation的压缩。

要想启用它，使用：`-XX:+UseParallelGC`

#### CMS回收器 ####

CMS回收器旨在解决串行和吞吐量回收器在Full GC造成的长时间暂停。虽然CMS回收器在回收Young Generation时，也会造成所有应用程序线程的暂停，但是它使用不同的算法对Young Generation进行回收。

CMS会使用一个或者多个后台线程来周期性地扫描Old Generation用来回收不被使用的对象，因此它不需要停止应用程序线程。但是因为后台线程的存在，CMS会增加CPU的负荷，而且后台线程不会对堆进行压缩。

当可用的CPU资源不足或者堆内存中碎片太多时，CMS会求助于串行回收器，即停止所有的应用程序线程，执行垃圾回收，完成堆内存的压缩。最后，CMS又会启动后台线程，恢复它本来的算法。

如果启用CMS回收器，使用：`-XX:+UseConcMarkSweepGC`， `-XX:+UseParNewGC`

#### G1回收器 ####














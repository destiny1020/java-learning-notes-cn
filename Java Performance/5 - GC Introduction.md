# 垃圾回收(Garbage Collection)概览 #
---

在目前的JVM中，主要有4中垃圾回收器(Garbage Collector)：
- 串行回收器(Serial Collector)，主要用于单核计算机
- 吞吐量(并行)回收器(Throughput/Parallel Collector)
- 并发回收器(Concurrent/CMS Collector)
- G1回收器

它们的性能特点各不相同，具体会在下一章进行介绍。但是它们拥有一些共同的原理和概念，这一章就是为了讲解这些关于垃圾回收的基础知识。




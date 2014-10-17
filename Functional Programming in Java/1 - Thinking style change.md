# 思维方式的转变 #

以从一个城市集合中寻找是否存在Chicago为例：

## 习惯的方式 ##

```java
boolean found = false;
for(String city : cities) {
	if(city.equals("Chicago")) {
		found = true;
		break;
	}
}
System.out.println("Found chicago?:" + found);
```

以上代码就是绝大多数开发人员在面对这个问题时的第一反应。它通过命令式风格(Imperative Style)的代码来完成需要的逻辑，但是看起来会比较复杂，因为代码量较多。

稍有经验的开发人员则会利用现有的API来实现，使代码更简洁同时也更具有可读性，因为它将代码风格由命令式转变为声明式(Declarative Style)。

```java
System.out.println("Found chicago?:" + cities.contains("Chicago"));
```

简单的一行代码，就能够将程序的意图显示出来。

### 另一个例子 ###

假设对于20元以上的商品，进行9折处理，最后得到这些商品的折后价格。如下解决方案立即会映入脑海：

```java
final List<BigDecimal> prices = Arrays.asList(
	new BigDecimal("10"), new BigDecimal("30"), new BigDecimal("17"),
	new BigDecimal("20"), new BigDecimal("15"), new BigDecimal("18"),
	new BigDecimal("45"), new BigDecimal("12"));

BigDecimal totalOfDiscountedPrices = BigDecimal.ZERO;
for(BigDecimal price : prices) {
	if(price.compareTo(BigDecimal.valueOf(20)) > 0)
		totalOfDiscountedPrices = totalOfDiscountedPrices.add(price.multiply(BigDecimal.valueOf(0.9)));
}
System.out.println("Total of discounted prices: " + totalOfDiscountedPrices);
```

当你经常性的写这种类型的代码时，不知道是否会产生一种无聊或者不安的情绪。因为这段代码已经普通到有点乏味了，虽然它能够正常工作，但是总会感觉到它并不是那么优雅。

更优雅的方式，是使用声明式的代码：

```java
final BigDecimal totalOfDiscountedPrices = prices.stream()
	.filter(price -> price.compareTo(BigDecimal.valueOf(20)) > 0)
	.map(price -> price.multiply(BigDecimal.valueOf(0.9)))
	.reduce(BigDecimal.ZERO, BigDecimal::add);

System.out.println("Total of discounted prices: " + totalOfDiscountedPrices);
```

没有声明任何的临时变量，没有各种if判断，逻辑一气呵成。同时也更具有可读性：
首先将价格集合根据条件进行过滤（filter)，然后对过滤后的集合进行折扣处理(map)，最后将折扣后的价格进行相加(reduce)。

它利用了Java 8的新特性，Lambda表达式以及相关的方法如stream()，reduce()等将代码转变成函数式的风格(Functional Style)。Lambda表达式和其相关内容会在后文中进行详细介绍。

## 使用函数式代码的好处 ##

- 减少了可变量(Immutable Variable)的声明
- 能够更好的利用并行(Parallelism)
- 代码更加简洁和可读

当然，Java 8中对于函数式编程风格的引入，并不是为了要颠覆已经根深蒂固面向对象编程风格。而是让它们和谐共处，取长补短。比如，使用面向对象对实体进行建模，对实体之间的关系进行表述；而使用函数式编码来实现实体中的各种状态改变，业务流程以及数据处理。

## 函数式编程的核心 ##

- 声明式的代码风格(Declarative Style)
	这需要提高代码的抽象层次，比如在前面的例子中，将从集合中搜索一个元素的操作封装到contains方法中。

- 更多的不变性(Promote Immutability)
	能不声明变量就不要声明，需要变量时尽量使用final来修饰。因为变量越多，就意味着程序越难以并行。实现了不变性的方法意味着它不再有副作用，不会因为调用而改变程序的状态。

- 使用表达式来代替语句(Prefer Expression to Statement)
	使用语句也就意味着不变性的破坏和程序状态的改变，比如赋值语句的使用。

- 使用高阶函数(High-Order Function)
	在Java 8以前，重用是建立在对象和类型系统之上。而Java 8中则将重用的概念更进一步，使用函数也能够实现代码的重用。所谓高阶函数，不要被其名字唬住了，实际上很简单：

	- 将函数作为参数传入到另外一个函数中
	- 函数的返回值可以是函数类型
	- 在函数中创建另一个函数

在前文中，已经见过函数作为参数传入到另一个函数的例子了：

```java
prices.stream()
	.filter(price -> price.compareTo(BigDecimal.valueOf(20)) > 0)
	.map(price -> price.multiply(BigDecimal.valueOf(0.9)))
	.reduce(BigDecimal.ZERO, BigDecimal::add);
```

`price -> price.multiply(BigDecimal.valueOf(0.9))`实际上就是一个函数。只不过它的写法使用了Lambda表达式，当代码被执行时，该表达式会被转换为一个函数。

### 函数式接口(Functional Interface) ###

为了在Java中引入函数式编程，Java 8中引入了函数式接口这一概念。

函数式接口就是仅声明了一个方法的接口，比如我们熟悉的Runnable，Callable，Comparable等都可以作为函数式接口。当然，在Java 8中，新添加了一类函数式接口，如Function，Predicate，Consumer，Supplier等。

在函数式接口中，可以声明0个或者多个default方法，这些方法在接口内就已经被实现了。因此，接口的default方法也是Java 8中引入的一个新概念。

函数式接口使用@FunctionalInterface注解进行标注。虽然这个注解的使用不是强制性的，但是使用它的好处是让此接口的目的更加明确，同时编译器也会对代码进行检查，来确保被该注解标注的接口的使用没有语法错误。

如果一个方法接受一个函数式接口作为参数，那么我们可以传入以下类型作为参数：

- 匿名内部类(Anonymous Inner Class)
- Lambda表达式
- 方法或者构造器的引用(Method or Constructor Reference)

第一种方式是Java的以前版本中经常使用的方式，在Java 8中不再被推荐。
第二种方式中，Lambda表达式会被编译器转换成相应函数式接口的一个实例。
第三种方式会在后文中详细介绍。















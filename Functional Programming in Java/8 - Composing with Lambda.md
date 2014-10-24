Java 8中同时存在面向对象编程(OOP)和函数式编程(FP, Functional Programming)这两种编程范式。实际上，这两种范式并不矛盾，只是着重点不同。在OOP中，着重于通过丰富的类型系统对需要解决的问题进行建模；而FP中则着重于通过高阶函数和Lambda表达式来完成计算。所以我们完全可以将这两者融合在一起，对问题提出更加优雅的解决方案。

在这篇文章中，会介绍如何通过函数组合(Function Composition)来将若干个函数单元组合成一个Map-Reduce模式的应用。同时，还会介绍如何将整个计算过程并行化。

## 使用函数组合 ##

在使用函数式编程的时候，函数是组成程序的单元。通过将函数以高阶函数的形式组织，可以有效地提高不变性(Immutability)，从而减少程序的状态变化，最终让并行化更加容易。

下面这张图反映了，纯粹的面向对象设计和混合式设计(面向对象和函数式)的风格。

![](https://github.com/destiny1020/java-learning-notes-cn/blob/master/Functional%20Programming%20in%20Java/images/4.PNG)

在OOP中，对象的状态会随着程序的进行而不断发生变化，但是对象始终只有一个。
而在FP中，对象每次被一个函数处理之后，都会得到一个新的对象，而原来的对象并不会发生变化。

下面是一个小例子，让你对这种混合式的编程范式有一个初步的了解。假设我们有一些股票的代码，需要得到股票价格大于100美元的股票并对它们进行排序：

```java
public class Tickers {
	public static final List<String> symbols = Arrays.asList(
		"AMD", "HPQ", "IBM", "TXN", "VMW", "XRX", "AAPL", "ADBE",
		"AMZN", "CRAY", "CSCO", "DELL", "GOOG", "INTC", "INTU",
		"MSFT", "ORCL", "TIBX", "VRSN", "YHOO");
}
```

对于每只股票代码，可以通过调用下面这段程序借助Yahoo提供的Web Service来得到对应的股价：

```java
public class YahooFinance {
	public static BigDecimal getPrice(final String ticker) {
		try {
			final URL url = new URL("http://ichart.finance.yahoo.com/table.csv?s=" + ticker);
			final BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
			final String data = reader.lines().skip(1).findFirst().get();
			final String[] dataItems = data.split(",");
			return new BigDecimal(dataItems[dataItems.length - 1]);
		} catch(Exception ex) {
			throw new RuntimeException(ex);
		}
	}
}
```

最后，通过一串操作来得到我们需要的答案：

```java
final BigDecimal HUNDRED = new BigDecimal("100");
System.out.println("Stocks priced over $100 are " +
	Tickers.symbols
		.stream()
		.filter(symbol -> YahooFinance.getPrice(symbol).compareTo(HUNDRED) > 0)
		.sorted()
		.collect(joining(", ")));
```

这就是一个混合范式的应用，将主要的计算逻辑通过方法进行封装，然后将这些函数根据其所属的类型进行面向对象建模，比如getPrice方法属于类型YahooFinance。最后使用Stream类型和Lambda表达式完成需要执行的计算逻辑，得到最终结果。

将计算逻辑封装成一个函数调用链的好处在于：

- 更简洁，代码量会少很多，从而代码也更容易被理解
- 提高了对象的不变性(Immutability)，从而更加容易并行化
- 调用链中的每一环都很容易被复用，如filter，sorted等

## 使用Map-Reduce ##

顾名思义，Map-Reduce实际上分为了两个步骤：

1. Map阶段：对集合中的元素进行操作
2. Reduce阶段：将上一步得到的结果进行合并得到最终的结果

正是因为这个模式十分简单，同时它也能够最大限度的利用多核处理器的能力，所以它得到了广泛关注。

比如，当我们需要得到股票价格小于500美元的最高价格的股票时，应该如何做呢？
首先我们还是从最熟悉的命令式代码开始。

### 准备工作 ###

首先，我们需要对这个问题进行一个基础的建模，这个步骤就是面向对象设计的过程。很容易地，可以得到下面的实体类型：

```java
public class StockInfo {
	public final String ticker;
	public final BigDecimal price;
	public StockInfo(final String symbol, final BigDecimal thePrice) {
		ticker = symbol;
		price = thePrice;
	}
	public String toString() {
		return String.format("ticker: %s price: %g", ticker, price);
	}
}
```

同时，也需要一些工具方法来帮助我们解决这个问题：

1. 通过股票代码得到对应的实体信息。我们可以使用前面介绍的YahooFinance中定义的getPrice方法来完成这一任务。
2. 判断股票的价格是否小于某个值，可以通过Predicate函数接口实现，它是一个高阶函数，会将传入的price信息作为阈值来生成一个Lambda表达式并返回。
3. 用来比较取得两个股价实体对象中股价较高的对象的方法。

分别实现如下：

```java
public class StockUtil {
	public static StockInfo getPrice(final String ticker) {
		return new StockInfo(ticker, YahooFinance.getPrice(ticker));
	}
	
	public static Predicate<StockInfo> isPriceLessThan(final int price) {
		return stockInfo -> stockInfo.price.compareTo(BigDecimal.valueOf(price)) < 0;
	}

	public static StockInfo pickHigh(
		final StockInfo stockInfo1, final StockInfo stockInfo2) {
		return stockInfo1.price.compareTo(stockInfo2.price) > 0 ? stockInfo1 : stockInfo2;
	}
}
```

### 命令式风格 ###

有了以上的准备工作，我们就可以着手实现了。首先是命令式风格的代码，这也是最熟悉的方式：

```java
final List<StockInfo> stocks = new ArrayList<>();
for(String symbol : Tickers.symbols) {
	stocks.add(StockUtil.getPrice(symbol));
}

final List<StockInfo> stocksPricedUnder500 = new ArrayList<>();
final Predicate<StockInfo> isPriceLessThan500 = StockUtil.isPriceLessThan(500);
for(StockInfo stock : stocks) {
	if(isPriceLessThan500.test(stock))
		stocksPricedUnder500.add(stock);
}

StockInfo highPriced = new StockInfo("", BigDecimal.ZERO);
for(StockInfo stock : stocksPricedUnder500) {
	highPriced = StockUtil.pickHigh(highPriced, stock);
}

System.out.println("High priced under $500 is " + highPriced);
```

上述代码完成了以下几个工作：

1. 首先是根据股票代码得到股价信息，然后将股价实体放到一个列表对象中。
2. 然后对集合进行一次遍历，得到所有价格低于500美元的股价实体。
3. 对步骤2中的结果进行遍历，得到其中拥有最高股价的实体。

当然，如果觉得循环的次数太多了，我们也可以将它们合并到一个循环中：

```java
StockInfo highPriced = new StockInfo("", BigDecimal.ZERO);
final Predicate<StockInfo> isPriceLessThan500 = StockUtil.isPriceLessThan(500);

for(String symbol : Tickers.symbols) {
	StockInfo stockInfo = StockUtil.getPrice(symbol);
	if(isPriceLessThan500.test(stockInfo))
		highPriced = StockUtil.pickHigh(highPriced, stockInfo);
}

System.out.println("High priced under $500 is " + highPriced);
```

可以发现，只是使用了一个Predicate类型的Lambda表达式就可以将代码的篇幅大大的较少。
只不过，以上的代码仍然是命令式风格，仍然会通过对变量进行修改来实现计算逻辑。更重要的是，以上的代码复用性比较差，当我们需要更改过滤条件的时候，就需要对它进行修改。

更好的办法是将所有会发生变化的代码封装成一个个单独的小模块，然后使用函数式风格的代码将它们联系起来。

### 函数式风格 ###

使用函数式风格后，代码中看不到for循环的踪影了：

```java
public static void findHighPriced(final Stream<String> symbols) {
	final StockInfo highPriced = symbols
		.map(StockUtil::getPrice)
		.filter(StockUtil.isPriceLessThan(500))
		.reduce(StockUtil::pickHigh)
		.get();

	System.out.println("High priced under $500 is " + highPriced);
}
```

map，filter和reduce方法分别替代了三个for循环，而且代码也变的异常简洁。除了简洁之外，更重要的是这段代码随时可以被并行化。

以上的计算逻辑可以使用下图进行表达：

图5

## 并行化 ##

在实施并行化之前，让我们看看上面的几个操作：map，filter和reduce。

显然，map方法的速度是最慢的，因为它依赖于外部的Web Service。但是同时也可以注意到，对于每个股票代码，获取它们对应的股价信息是完全独立的，故而可以考虑将这部分并行化。

当需要让一段代码以并行的方式运行时，需要考虑两个方面：

- 如何完成？
- 如何以合适的方式完成？

对于第一个方面，我们可以使用JDK中提供的各种并发相关的库来完成。
对于第二个方面，就需要我们根据这段代码的特点进行考虑了。对于并发程序，首先需要避免的是竞态条件(Race Condition)，当多个线程试图去更新一个对象或者一个变量时，就有可能发生。所以对于这类更新，我们需要小心翼翼地维护其线程安全性。反过来，如果对象的状态是不可变的(状态变量被修饰为final)，那么滋生竞态条件的土壤也就不复存在了，而这一点正是函数式编程所一再强调和标榜的。

因此，在严格遵守函数式编程的最佳实践后，并行化只不过是临门一脚的功夫而已：

```java
// 串行执行的调用方式
findHighPriced(Tickers.symbols.stream());

// 并行执行的调用方式
findHighPriced(Tickers.symbols.parallelStream());
```

只不过是把stream方法替换成了parallelStream方法，就给代码插上了并行的翅膀。不需要考虑如何完成，也不需要考虑如竞态条件那样的各种风险。

关于这两个方法的定义，可以在Collection接口中找到，这也意味着不仅仅对于List类型可以很方便的实现并行，对其它实现了Collection接口的类型也非常方便：

```java
default Stream<E> stream() {
    return StreamSupport.stream(spliterator(), false);
}

default Stream<E> parallelStream() {
    return StreamSupport.stream(spliterator(), true);
}
```

这里不打算对深层的实现原理进行剖析，但是当使用parallelStream时，意味着像map，filter这样的方法都会以并行的方式被运行，而工作线程则是来自于底层的一个线程池。这些细节都已经被封装的相当好，作为开发人员只需要保证你的代码确实遵守了游戏规则。

串行方式和并行方式的性能比较如下：

| 串行 | 并行 |
| --- | --- |
| 24.325s | 5.621s |

通过简单地改变一个方法，就将性能提高了接近5倍！这也许就是函数式编程的魅力之一吧。

那么，在从stream和parallelStream方法中进行选择时，需要考虑以下几个问题：

1. 是否需要并行？
2. 任务之间是否是独立的？是否会引起任何竞态条件？
3. 结果是否取决于任务的调用顺序？

对于问题2，如果任务之间是独立的，并且代码中不涉及到对同一个对象的某个状态或者某个变量的更新操作，那么就表明代码是可以被并行化的。

对于问题3，由于在并行环境中任务的执行顺序是不确定的，因此对于依赖于顺序的任务而言，并行化也许不能给出正确的结果。

对于问题1，在回答这个问题之前，你需要弄清楚你要解决的问题是什么，数据量有多大，计算的特点是什么？并不是所有的问题都适合使用并发程序来求解，比如当数据量不大时，顺序执行往往比并行执行更快。毕竟，准备线程池和其它相关资源也是需要时间的。

但是，当任务涉及到I/O操作并且任务之间不互相依赖时，那么并行化就是一个不错的选择。通常而言，将这类程序并行化之后，执行速度会提升好几个等级，正如上面的例子那样。
















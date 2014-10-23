# 递归优化 #

很多算法都依赖于递归，典型的比如分治法(Divide-and-Conquer)。但是普通的递归算法在处理规模较大的问题时，常常会出现StackOverflowError。处理这个问题，我们可以使用一种叫做尾调用(Tail-Call Optimization)的技术来对递归进行优化。同时，还可以通过暂存子问题的结果来避免对子问题的重复求解，这个优化方法叫做备忘录(Memoization)。

## 使用尾调用优化 ##

当递归算法应用于大规模的问题时，容易出现StackOverflowError，这是因为需要求解的子问题过多，递归嵌套层次过深。这时，可以采用尾调用优化来避免这一问题。该技术之所以被称为尾调用，是因为在一个递归方法中，最后一个语句才是递归调用。这一点和常规的递归方法不同，常规的递归通常发生在方法的中部，在递归结束返回了结果后，往往还会对该结果进行某种处理。

Java在编译器级别并不支持尾递归技术。但是我们可以借助Lambda表达式来实现它。下面我们会通过在阶乘算法中应用这一技术来实现递归的优化。以下代码是没有优化过的阶乘递归算法：

```java
public class Factorial {
	public static int factorialRec(final int number) {
		if(number == 1)
			return number;
		else
			return number * factorialRec(number - 1);
	}
}
```

以上的递归算法在处理小规模的输入时，还能够正常求解，但是输入大规模的输入后就很有可能抛出StackOverflowError：

```java
try {
	System.out.println(factorialRec(20000));
} catch(StackOverflowError ex) {
	System.out.println(ex);
}

// java.lang.StackOverflowError
```

出现这个问题的原因不在于递归本身，而在于在等待递归调用结束的同时，还需要保存了一个number变量。因为递归方法的最后一个操作是乘法操作，当求解一个子问题时(`factorialRec(number - 1)`)，需要保存当前的number值。所以随着问题规模的增加，子问题的数量也随之增多，每个子问题对应着调用栈的一层，当调用栈的规模大于JVM设置的阈值时，就发生了StackOverflowError。

### 转换成尾递归 ###

转换成尾递归的关键，就是要保证对自身的递归调用是最后一个操作。不能像上面的递归方法那样：最后一个操作是乘法操作。而为了避免这一点，我们可以先进行乘法操作，将结果作为一个参数传入到递归方法中。但是仅仅这样仍然是不够的，因为每次发生递归调用时还是会在调用栈中创建一个栈帧(Stack Frame)。随着递归调用深度的增加，栈帧的数量也随之增加，最终导致StackOverflowError。可以通过将递归调用延迟化来避免栈帧的创建，以下代码是一个原型实现：

```java
public static TailCall<Integer> factorialTailRec(
	final int factorial, final int number) {
	if (number == 1)
		return TailCalls.done(factorial);
	else
		return TailCalls.call(() -> factorialTailRec(factorial * number, number - 1));
}
```

需要接受的参数factorial是初始值，而number是需要计算阶乘的值。
我们可以发现，递归调用体现在了call方法接受的Lambda表达式中。以上代码中的TailCall接口和TailCalls工具类目前还没有实现。

### 创建TailCall函数接口 ###

TailCall的目标是为了替代传统递归中的栈帧，通过Lambda表达式来表示多个连续的递归调用。所以我们需要通过当前的递归操作得到下一个递归操作，这一点有些类似UnaryOperator函数接口的apply方法。同时，我们还需要方法来完成这几个任务：

1. 判断递归是否结束了
2. 得到最后的结果
3. 触发递归

因此，我们可以这样设计TailCall函数接口：

```java
@FunctionalInterface
public interface TailCall<T> {
	TailCall<T> apply();
	default boolean isComplete() { return false; }
	default T result() { throw new Error("not implemented"); }
	default T invoke() {
		return Stream.iterate(this, TailCall::apply)
			.filter(TailCall::isComplete)
			.findFirst()
			.get()
			.result();
	}
}
```

isComplete，result和invoke方法分别完成了上述提到的3个任务。只不过具体的isComplete和result还需要根据递归操作的性质进行覆盖，比如对于递归的中间步骤，isComplete方法可以返回false，然而对于递归的最后一个步骤则需要返回true。对于result方法，递归的中间步骤可以抛出异常，而递归的最终步骤则需要给出结果。

invoke方法则是最重要的一个方法，它会将所有的递归操作通过apply方法串联起来，通过没有栈帧的尾调用得到最后的结果。串联的方式利用了Stream类型提供的iterate方法，它本质上是一个无穷列表，这也从某种程度上符合了递归调用的特点，因为递归调用发生的数量虽然是有限的，但是这个数量也可以是未知的。而给这个无穷列表画上终止符的操作就是filter和findFirst方法。因为在所有的递归调用中，只有最后一个递归调用会在isComplete中返回true，当它被调用时，也就意味着整个递归调用链的结束。最后，通过findFirst来返回这个值。

如果不熟悉Stream的iterate方法，可以参考上一篇文章，在其中对该方法的使用进行了介绍。

### 创建TailCalls工具类 ###

在原型设计中，会调用TailCalls工具类的call和done方法：

- call方法用来得到当前递归的下一个递归
- done方法用来结束一系列的递归操作，得到最终的结果

```java
public class TailCalls {
	public static <T> TailCall<T> call(final TailCall<T> nextCall) {
		return nextCall;
	}
	public static <T> TailCall<T> done(final T value) {
		return new TailCall<T>() {
			@Override public boolean isComplete() { return true; }
			@Override public T result() { return value; }
			@Override public TailCall<T> apply() {
				throw new Error("end of recursion");
			}
		};
	}
}
```

在done方法中，我们返回了一个特殊的TailCall实例，用来代表最终的结果。注意到它的apply方法被实现成被调用抛出异常，因为对于最终的递归结果，是没有后续的递归操作的。

以上的TailCall和TailCalls虽然是为了解决阶乘这一简单的递归算法而设计的，但是它们无疑在任何需要尾递归的算法中都能够派上用场。

### 使用尾递归函数 ###

使用它们来解决阶乘问题的代码很简单：

```java
System.out.println(factorialTailRec(1, 5).invoke());      // 120
System.out.println(factorialTailRec(1, 20000).invoke());  // 0
```

第一个参数代表的是初始值，第二个参数代表的是需要计算阶乘的值。

但是在计算20000的阶乘时得到了错误的结果，这是因为整型数据无法容纳这么大的结果，发生了溢出。对于这种情况，可以使用BigInteger来代替Integer类型。

实际上factorialTailRec的第一个参数是没有必要的，在一般情况下初始值都应该是1。所以我们可以做出相应地简化：

```java
public static int factorial(final int number) {
	return factorialTailRec(1, number).invoke();
}

// 调用方式
System.out.println(factorial(5));
System.out.println(factorial(20000));
```

### 使用BigInteger代替Integer ###

主要就是需要定义decrement和multiple方法来帮助完成大整型数据的阶乘操作：

```java
public class BigFactorial {
	public static BigInteger decrement(final BigInteger number) {
		return number.subtract(BigInteger.ONE);
	}

	public static BigInteger multiply(
		final BigInteger first, final BigInteger second) {
		return first.multiply(second);
	}

	final static BigInteger ONE = BigInteger.ONE;
	final static BigInteger FIVE = new BigInteger("5");
	final static BigInteger TWENTYK = new BigInteger("20000");
	//...

	private static TailCall<BigInteger> factorialTailRec(
		final BigInteger factorial, final BigInteger number) {
		if(number.equals(BigInteger.ONE))
			return done(factorial);
		else
			return call(() ->
				factorialTailRec(multiply(factorial, number), decrement(number)));
	}

	public static BigInteger factorial(final BigInteger number) {
		return factorialTailRec(BigInteger.ONE, number).invoke();
	}
}
```

## 使用备忘录模式(Memoization Pattern)提高性能 ##

这个模式说白了，就是将需要进行大量计算的结果缓存起来，然后在下次需要的时候直接取得就好了。因此，底层只需要使用一个Map就够了。

但是需要注意的是，只有一组参数对应得到的是同一个值时，该模式才有用武之地。

在很多算法中，典型的比如分治法，动态规划(Dynamic Programming)等算法中，这个模式运用的十分广泛。
以动态规划来说，动态规划在求最优解的过程中，会将原有任务分解成若干个子任务，而这些子任务势必还会将自身分解成更小的任务。因此，从整体而言会有相当多的重复的小任务需要被求解。显然，当输入的参数相同时，一个任务只需要被求解一次就好了，求解之后将结果保存起来。待下次需要求解这个任务时，会首先查询这个任务是否已经被解决了，如果答案是肯定的，那么只需要直接返回结果就行了。

就是这么一个简单的优化措施，往往能够将代码的时间复杂度从指数级的变成线性级。

以一个经典的[杆切割问题(Rod Cutting Problem)](http://www.geeksforgeeks.org/dynamic-programming-set-13-cutting-a-rod/)(或者这里也有更加正式的定义：[维基百科](http://en.wikipedia.org/wiki/Cutting_stock_problem))为例，来讨论一下如何结合Lambda表达式来实现备忘录模式。

首先，简单交代一下这个问题的背景。

一个公司会批发一些杆(Rod)，然后对它们进行零售。但是随着杆的长度不同，能够卖出的价格也是不同的。所以该公司为了将利润最大化，需要结合长度价格信息来决定应该将杆切割成什么长度，才能实现利润最大化。

比如，下面的代码：

```java
final List<Integer> priceValues = Arrays.asList(2, 1, 1, 2, 2, 2, 1, 8, 9, 15);
```

表达的意思是：长度为1的杆能够卖2元，长度为2的杆能够卖1元，以此类推，长度为10的杆能够卖15元。

当需要被切割的杆长度为5时，存在的切割方法多达16种(2^(5 - 1))。如下所示：

图3

针对这个问题，在不考虑使用备忘录模式的情况下，可以使用动态规划算法实现如下：

```java
public int maxProfit(final int length) {
	int profit = (length <= prices.size()) ? prices.get(length - 1) : 0;
	for(int i = 1; i < length; i++) {
		int priceWhenCut = maxProfit(i) + maxProfit(length - i);
		if(profit < priceWhenCut) profit = priceWhenCut;
	}
	return profit;
}
```

而从上面的程序可以发现，有很多重复的子问题。对这些重复的子问题进行不断纠结，损失了很多不必要的性能。分别取杆长为5和22时，得到的运行时间分别为：0.001秒和34.612秒。可见当杆的长度增加时，性能的下降时非常非常显著的。

因为备忘录模式的原理十分简单，因此实现起来也很简单，只需要在以上maxProfit方法的头部加上Map的读取操作并判断结果就可以了。但是这样做的话，代码的复用性会不太好。每个需要使用备忘录模式的地方，都需要单独写判断逻辑，那么有没有一种通用的办法呢？答案是肯定的，通过借助Lambda表达式的力量可以轻易办到，以下代码我们假设有一个静态方法callMemoized用来通过传入一个策略和输入值，来求出最优解：

```java
public int maxProfit(final int rodLenth) {
	return callMemoized(
		(final Function<Integer, Integer> func, final Integer length) -> {
		int profit = (length <= prices.size()) ? prices.get(length - 1) : 0;
		for(int i = 1; i < length; i++) {
			int priceWhenCut = func.apply(i) + func.apply(length - i);
			if(profit < priceWhenCut) profit = priceWhenCut;
		}
		return profit;
		}, rodLenth);
}
```

让我们仔细分析一下这段代码的意图。首先callMemoized方法接受的参数类型是这样的：

```java
public static <T, R> R callMemoized(final BiFunction<Function<T,R>, T, R> function, final T input)
```

BiFunction类型的参数function实际上封装了一个策略，其中有三个部分：

1. Function<T, R>：通过传入参数T，来得到解答R。这一点从代码`int priceWhenCut = func.apply(i) + func.apply(length - i)`很明显的就能够看出来。可以把它想象成一个备忘录的入口。
2. T：代表求解问题时需要的参数T。
3. R：代表问题的答案R。

以上的T和R都是指的类型。

下面我们看看callMemoized方法的实现：

```java
public class Memoizer {
	public static <T, R> R callMemoized(final BiFunction<Function<T,R>, T, R> function, final T input) {
		Function<T, R> memoized = new Function<T, R>() {
			private final Map<T, R> store = new HashMap<>();
			public R apply(final T input) {
				return store.computeIfAbsent(input, key -> function.apply(this, key));
			}
		};

		return memoized.apply(input);
	}
}
```

在该方法中，首先声明了一个匿名Function函数接口的实现。其中定义了备忘录模式的核心---Map结构。
然后在它的apply方法中，会借助Java 8中为Map接口新添加的一个computeIfAbsent方法来完成下面的逻辑：

1. 通过传入的key检查(在以上代码中是input)对应的值是否存在于备忘录的底层Map中
2. 如果存在，跳转到步骤4
3. 如果不存在，根据computeIfAbsent的第二个参数(是一个Lambda表达式)来计算得到key对应的value
4. 返回得到的value

具体到该方法的源码：

```java
default V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
    Objects.requireNonNull(mappingFunction);
    V v;
    if ((v = get(key)) == null) {
        V newValue;
        if ((newValue = mappingFunction.apply(key)) != null) {
            put(key, newValue);
            return newValue;
        }
    }

    return v;
}
```

也可以很清晰地看出以上的几个步骤是如何体现在代码中的。

关键的地方就在于第三步，如果不存在对应的value，那么需要调用传入的Lambda表达式进行求解。以上代码传入的是`key -> function.apply(this, key)`，这里的this使用的十分巧妙，它实际上指向的就是这个用于容纳Map结构的匿名Function实例。它作为第一个参数传入到算法策略中，同时需要求解的key被当做第二个参数传入到算法策略。这里所谓的算法策略，实际上就是在调用callMemoized方法时，传入的形式为`BiFunction<Function<T,R>, T, R>`的参数。

因此，所有的子问题仅仅会被求解一次。在得到子问题的答案之后，答案会被放到Map数据结构中，以便将来的使用。这就是借助Lambda表示实现备忘录模式的方法。

以上的代码可能会显得有些怪异，这很正常。在你反复阅读它们后，并且经过自己的思考能够重写它们时，也就是你对Lambda表达式拥有更深理解之时。

使用备忘录模式后，杆长仍然取5和22时，得到的运行时间分别为：0.050秒和0.092秒。可见当杆的长度增加时，性能并没有如之前那样下降的很厉害。这完全是得益于备忘录模式，此时所有的任务都只会被运行一次。








































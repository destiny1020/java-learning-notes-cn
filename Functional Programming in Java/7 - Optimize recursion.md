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





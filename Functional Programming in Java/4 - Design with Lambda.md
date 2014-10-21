# 使用Lambda表达式进行设计 #

在前面的几篇文章中，我们已经见识到了Lambda表达式是如何让代码变的更加紧凑和简洁的。

这一篇文章主要会介绍Lambda表达式如何改变程序的设计，如何让程序变的更加轻量级和简洁。如何让接口的使用变得更加流畅和直观。

## 使用Lambda表达式来实现策略模式 ##

假设现在有一个Asset类型是这样的：

```java
public class Asset {
	public enum AssetType { BOND, STOCK };
	private final AssetType type;
	private final int value;
	public Asset(final AssetType assetType, final int assetValue) {
		type = assetType;
		value = assetValue;
	}
	public AssetType getType() { return type; }
	public int getValue() { return value; }
}
```

每一个资产都有一个种类，使用枚举类型表示。同时资产也有其价值，使用整型类型表示。

如果我们想得到所有资产的价值，那么使用Lambda可以轻松实现如下：

```java
public static int totalAssetValues(final List<Asset> assets) {
	return assets.stream()
		.mapToInt(Asset::getValue)
		.sum();
}
```

虽然上述代码能够很好的完成计算资产总值这一任务，但是仔细分析会发现这段代码将以下三个任务给放在了一起：

1. 如何遍历
2. 计算什么
3. 如何计算

如何现在来了一个新需求，要求计算Bond类型的资产总值，那么很直观的我们会考虑将上段代码复制一份然后有针对性地进行修改：

```java
public static int totalBondValues(final List<Asset> assets) {
	return assets.stream()
		.mapToInt(asset ->
			asset.getType() == AssetType.BOND ? asset.getValue() : 0)
		.sum();
}
```

而唯一不同的地方，就是传入到mapToInt方法中的Lambda表达式。当然，也可以在mapToInt方法之前添加一个filter，过滤掉不需要的Stock类型的资产，这样的话mapToInt中的Lambda表达式就可以不必修改了。

```java
public static int totalBondValues(final List<Asset> assets) {
	return assets.stream()
		.filter(asset -> asset.getType == AssetType.BOND)
		.mapToInt(Asset::getValue)
		.sum();
}
```

这样虽然实现了新需求，但是这种做法明显地违反了DRY原则。我们需要重新设计它们来增强可重用性。
在计算Bond资产的代码中，Lambda表达式起到了两个作用：

1. 如何遍历
2. 如何计算

当使用面向对象设计时，我们会考虑使用策略模式(Strategy Pattern)来将上面两个职责进行分离。但是这里我们使用Lambda表达式进行实现：

```java
public static int totalAssetValues(final List<Asset> assets, final Predicate<Asset> assetSelector) {
	return assets.stream().filter(assetSelector).mapToInt(Asset::getValue).sum();
}
```

重构后的方法接受了第二个参数，它是一个Predicate类型的函数式接口，很显然它的作用就是来指定如何遍历。这实际上就是策略模式在使用Lambda表达式时的一个简单实现，因为Predicate表达的是一个行为，而这个行为本身就是一种策略。这种方法更加轻量级，因为它没有额外创建其他的接口或者类型，只是重用了Java 8中提供的Predicate这一函数式接口而已。

比如，当我们需要计算所有资产的总值时，传入的Predicate可以是这样的：

```java
System.out.println("Total of all assets: " + totalAssetValues(assets, asset -> true));
```

因此，在使用了Predicate自后，就将“如何遍历”这个任务也分离出来了。因此，任务之间不再纠缠在一起，实现了单一职责的原则，自然而然就提高了重用性。

## 使用Lambda表达式实现组合(Composition) ##

在面向对象设计中，一般认为使用组合的方式会比使用继承的方式更好，因为它减少了不必要的类层次。其实，使用Lambda表达式也能够实现组合。

比如，在下面的CalculateNAV类中，有一个用来计算股票价值的方法：

```java
public class CalculateNAV {
	public BigDecimal computeStockWorth(final String ticker, final int shares) {
		return priceFinder.apply(ticker).multiply(BigDecimal.valueOf(shares));
	}
	//... other methods that use the priceFinder ...
}
```

因为传入的ticker是一个字符串，代表的是股票的代码。而在计算中我们显然需要的是股票的价格，所以priceFinder的类型很容易被确定为Function<String, BigDecimal>。因此我们可以这样声明CalculateNAV的构造函数：

```java
private Function<String, BigDecimal> priceFinder;
public CalculateNAV(final Function<String, BigDecimal> aPriceFinder) {
	priceFinder = aPriceFinder;
}
```

实际上，上面的代码使用了一种设计模式叫做“依赖倒转原则(Dependency Inversion Principle)”，使用依赖注入的方式将类型和具体的实现进行关联，而不是直接将实现写死到代码中，从而提高了代码的重用性。

为了测试CalculateNAV，可以使用JUnit：

```java
public class CalculateNAVTest {
	@Test
	public void computeStockWorth() {
		final CalculateNAV calculateNAV = new CalculateNAV(ticker -> new BigDecimal("6.01"));
		BigDecimal expected = new BigDecimal("6010.00");
		assertEquals(0, calculateNAV.computeStockWorth("GOOG", 1000).compareTo(expected));
	}
	//...
}
```

当然，也可以使用真实的Web Service来得到某只股票的价格：

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

这里想说明的是在Java 8中，BufferedReader也有一个新方法叫做lines，目的是得到包含所有行数据的一个Stream对象，很明显这也是为了让该类和函数式编程能够更好的融合。

另外想说明的是是Lambda表达式和异常之间的关系。很明显，当使用getPrice方法时，我们可以直接传入方法引用：`YahooFinance::getPrice`。但是如果在调用此方法期间发生了异常该如何处理呢？上述代码在发生了异常时，将异常包装成RuntimeException并重新抛出。这样做是因为只有当函数式接口中的方法本身声明了会抛出异常时(即声明了throws XXX)，才能够抛出受检异常(Checked Exception)。而显然在Function这一个函数式接口的apply方法中并未声明可以抛出的受检异常，因此getPrice本身是不能抛出受检异常的，我们可以做的就是将异常封装成运行时异常(非受检异常)，然后再抛出。

## 使用Lambda表达式实现装饰模式(Decorator Pattern) ##

装饰模式本身并不复杂，但是在面向对象设计中实现起来并不轻松，因为使用它需要设计和实现较多的类型，这无疑增加了开发人员的负担。比如JDK中的各种InputStream和OutputStream，在其上有各种各样的类型用来装饰它，所以最后I/O相关的类型被设计的有些过于复杂了，学习成本较高，要想正确而高效地使用它们并不容易。

使用Lambda表达式来实现装饰模式，就相对地容易多了。在图像领域，滤镜(Filter)实际上就是一种装饰器(Decorator)，我们会为一幅图像增加各种各样的滤镜，这些滤镜的数量是不确定的，顺序也是不确定的。

比如以下代码为摄像机对色彩的处理进行建模：

```java
@SuppressWarnings("unchecked")
public class Camera {
	private Function<Color, Color> filter;
	public Color capture(final Color inputColor) {
		final Color processedColor = filter.apply(inputColor);
		//... more processing of color...
		return processedColor;
	}
	//... other functions that use the filter ...
}
```

目前只定义了一个filter。我们可以利用Function的compose和andThen来进行多个Function(也就是filter)的串联操作：

```java
default <V> Function<V, R> compose(Function<? super V, ? extends T> before) {
    Objects.requireNonNull(before);
    return (V v) -> apply(before.apply(v));    
}

default <V> Function<T, V> andThen(Function<? super R, ? extends V> after) {
    Objects.requireNonNull(after);
    return (T t) -> after.apply(apply(t));
}
```

可以发现，compose和andThen方法的区别仅仅在于串联的顺序。使用compose时，传入的Function会被首先调用；使用andThen时，当前的Function会被首先调用。

因此，在Camera类型中，我们可以定义一个方法用来串联不定数量的Filters：

```java
public void setFilters(final Function<Color, Color>... filters) {
	filter = Arrays.asList(filters).stream()
		.reduce((current, next) -> current.andThen(next))
		.orElse(color -> color);
}
```

前面介绍过，由于reduce方法返回的对象是Optional类型的，因此当结果不存在时，需要进行特别处理。以上的orElse方法在结果不存在时会被调用来得到一个替代方案。那么当setFilters方法没有接受任何参数时，orElse就会被调用，`color -> color`的意义就是直接返回该color，不作任何操作。

实际上，Function接口中也定义了一个静态方法identity用来处理需要直接返回自身的场景：

```java
static <T> Function<T, T> identity() {
    return t -> t;
}
```

因此可以将上面的setFilters方法的实现改进成下面这样：

```java
public void setFilters(final Function<Color, Color>... filters) {
	filter = Arrays.asList(filters).stream()
		.reduce((current, next) -> current.andThen(next))
		.orElseGet(Function::identity);
}
```

orElse被替换成了orElseGet，两者的定义如下：

```java
public T orElse(T other) {
    return value != null ? value : other;
}

public T orElseGet(Supplier<? extends T> other) {
    return value != null ? value : other.get();
}
```

前者当value为空时会直接返回传入的参数other，而后者则是通过调用调用Supplier中的get方法来得到要返回的对象。这里又出现了一个新的函数式接口Supplier：

```java
@FunctionalInterface
public interface Supplier<T> {
    T get();
}
```

它不需要任何参数，直接返回需要的对象。这一点和类的无参构造函数(有些情况下也被称为工厂)有些类似。

说到了Supplier，就不能不提和它相对的Consumer函数接口，它们正好是一种互补的关系。Consumer中的accept方法会接受一个参数，但是没有返回值。

现在，我们就可以使用Camera的滤镜功能了：

```java
final Camera camera = new Camera();
final Consumer<String> printCaptured = (filterInfo) ->
	System.out.println(String.format("with %s: %s", filterInfo,
		camera.capture(new Color(200, 100, 200))));

camera.setFilters(Color::brighter, Color::darker);
printCaptured.accept("brighter & darker filter");
```

在不知不觉中，我们在setFilters方法中实现了一个轻量级的装饰模式(Decorator Pattern)，不需要定义任何多余的类型，只需要借助Lambda表达式即可。

## 了解接口的default方法 ##

在Java 8中，接口中也能够拥有非抽象的方法了，这是一个非常重大的设计。那么从Java编译器的角度来看，default方法的解析有以下几个规则：

1. 子类型会自动拥有父类型中的default方法。
2. 对于接口中实现的default方法，该接口的子类型能够对该方法进行覆盖。
3. 类中的具体实现以及抽象的声明，都会覆盖所有实现接口类型中出现的default方法。
4. 当两个或者两个以上的default方法实现中出现了冲突时，实现类需要解决这个冲突。

举个例子来说明以上的规则：

```java
public interface Fly {
	default void takeOff() { System.out.println("Fly::takeOff"); }
	default void land() { System.out.println("Fly::land"); }
	default void turn() { System.out.println("Fly::turn"); }
	default void cruise() { System.out.println("Fly::cruise"); }
}
public interface FastFly extends Fly {
	default void takeOff() { System.out.println("FastFly::takeOff"); }
}
public interface Sail {
	default void cruise() { System.out.println("Sail::cruise"); }
	default void turn() { System.out.println("Sail::turn"); }
}
public class Vehicle {
	public void turn() { System.out.println("Vehicle::turn"); }
}
```

对于规则1，FastFly和Fly接口能够说明。FastFly虽然覆盖了Fly中的takeOff方法，但是它同时也继承了Fly中的其它三个方法。

对于规则2，如果有任何接口继承了FastFly或者任何类型实现了FastFly，那么该子类型中的takeOff方法是来自于FastFly，而非Fly接口。

```java
public class SeaPlane extends Vehicle implements FastFly, Sail {
	private int altitude;
	//...
	public void cruise() {
		System.out.print("SeaPlane::cruise currently cruise like: ");
		if(altitude > 0)
			FastFly.super.cruise();
		else
			Sail.super.cruise();
	}
}
```

以上的SeaPlane继承了Vehicle类型，同时实现了FastFly和Sail接口。由于这两个接口中都定义了cruise方法，根据规则4，会发生冲突。因此需要SeaPlane来解决这个冲突，而解决的办法就是重新定义这个方法。但是，父类型中的方法还是能够被调用的。正如上述代码会首先判断高度，然后调用相应父类的方法。

实际上，turn方法也存在于FastFly和Sail类型中。该方法之所以不存在冲突得益于Vehicle方法覆盖了接口中的default方法。因此根据规则3，turn方法就不存在冲突了。

也许有人会觉得在Java 8中，接口类型更像是一个抽象类，因为它不仅仅能够拥有抽象方法，现在它还能够有具体实现。但是这种说法还是过于片面了，毕竟接口还是不能够拥有状态的，而抽象类则可以持有状态。同时，从继承的角度来看，一个类可以实现多个接口，而一个类至多只能继承一个类。

## 使用Lambda表达式创建更流畅的API ##

比如，现在有一个用于发送邮件的类型和它的使用方式：

```java
public class Mailer {
	public void from(final String address) { /*... */ }
	public void to(final String address) { /*... */ }
	public void subject(final String line) { /*... */ }
	public void body(final String message) { /*... */ }
	public void send() { System.out.println("sending..."); }
	//...
}

Mailer mailer = new Mailer();
mailer.from("build@agiledeveloper.com");
mailer.to("venkats@agiledeveloper.com");
mailer.subject("build notification");
mailer.body("...your code sucks...");
mailer.send();
```

这种代码我们几乎每天都要碰到。但是其中的坏味道你发现了吗？不觉得mailer实例出现的太频繁了吗？对于这种情况，我们可以使用方法链模式(Method Chaining Pattern)来改进：

```java
public class MailBuilder {
	public MailBuilder from(final String address) { /*... */; return this; }
	public MailBuilder to(final String address) { /*... */; return this; }
	public MailBuilder subject(final String line) { /*... */; return this; }
	public MailBuilder body(final String message) { /*... */; return this; }
	public void send() { System.out.println("sending..."); }
	//...
}

new MailBuilder()
	.from("build@agiledeveloper.com")
	.to("venkats@agiledeveloper.com")
	.subject("build notification")
	.body("...it sucks less...")
	.send();
```

这样就流畅多了，代码也更加简练。
但是上述代码还是存在问题：

1. new关键字的使用增加了噪声
2. 新创建的对象的引用可以被保存，意味着该对象的生命周期不可预测

这次我们让Lambda也参与到改进的过程中：

```java
public class FluentMailer {
	private FluentMailer() {}
	public FluentMailer from(final String address) { /*... */; return this; }
	public FluentMailer to(final String address) { /*... */; return this; }
	public FluentMailer subject(final String line) { /*... */; return this; }
	public FluentMailer body(final String message) { /*... */; return this; }
	public static void send(final Consumer<FluentMailer> block) {
		final FluentMailer mailer = new FluentMailer();
		block.accept(mailer);
		System.out.println("sending...");
	}
	//...
}

FluentMailer.send(mailer ->
	mailer.from("build@agiledeveloper.com")
		.to("venkats@agiledeveloper.com")
		.subject("build notification")
		.body("...much better..."));
```

最重要的改进就是让类型的构造函数声明成私有的，这就防止了在类外部被显式的实例化，控制了实例的生命周期。然后send方法被重构成了一个static方法，它接受一个Consumer类型的Lambda表达式来完成对新创建的实例的操作。

此时，新创建的实例的生命周期非常明确：即当send方法调用结束之后，该实例的生命周期也就随之结束了。这种模式也被直观地称为“Loan Pattern”。即首先得到它，操作它，最后归还它。

## 处理异常 ##

前文中提到过，如果函数接口的方法本身没有定义可以被抛出的受检异常，那么在使用该接口时是无法处理可能存在的受检异常的，比如典型的IOException这类，在进行文件操作的时候必须要处理：

```java
public class HandleException {
	public static void main(String[] args) throws IOException {
		List<String> paths = Arrays.asList("/usr", "/tmp");
		paths.stream()
			.map(path -> new File(path).getCanonicalPath())
			.forEach(System.out::println);
		// 以上代码不能通过编译，因为没有处理可能存在的IOException
	}
}

// ... unreported exception IOException; must be caught or declared to be thrown
// .map(path -> new File(path).getCanonicalPath())
//                                             ^
```

这是因为在map方法需要的Function函数接口中，并没有声明任何可以被抛出的受检异常。这里我们有两个选择：

1. 在Lambda表达式内处理受检异常
2. 捕获该受检异常并重新以非受检异常(如RuntimeException)的形式抛出

使用第一个选择时，代码是这样的：

```java
paths.stream()
	.map(path -> {
		try {
			return new File(path).getCanonicalPath();
		} catch(IOException ex) {
			return ex.getMessage();
		}
	})
	.forEach(System.out::println);
```

使用第二个选择时，代码是这样的：

```java
paths.stream()
	.map(path -> {
		try {
			return new File(path).getCanonicalPath();
		} catch(IOException ex) {
			throw new RuntimeException(ex);
		}
	})
	.forEach(System.out::println);
```

在单线程环境中，使用捕获受检异常并重新抛出非受检异常的方法是可行的。但是在多线程环境这样用，就存在一些风险。

多线程环境中，Lambda表达式中发生的错误会被自动传递到主线程中。这会带来两个问题：

1. 这不会停止其他正在并行执行的Lambda表达式。
2. 如果有多个线程抛出了异常，在主线程中却只能捕获到一个线程中的异常。如果这些异常信息都很重要的话，那么更好的方法是在Lambda表达式中就进行异常处理并将异常信息作为结果的一部分返回到主线程中。

实际上，还有一种方法。就是根据异常处理的需要定义我们自己的函数接口，比如：

```java
@FunctionalInterface
public interface UseInstance<T, X extends Throwable> {
	void accept(T instance) throws X;
}
```

这样的话，任何使用UseInstance类型的Lambda表达式就能够抛出各种异常了。







































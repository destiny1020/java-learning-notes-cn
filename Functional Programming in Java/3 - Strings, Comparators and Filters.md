## 遍历一个字符串 ##

在Java 8中，CharSequence接口新添加了一个方法叫做chars()，方法的签名是这个样子的：

```java
public default IntStream chars()
```

default关键字修饰的接口方法是Java 8中新添加的特性，目的是向接口中添加已经实现的方法。chars()方法返回了一个IntStream类型，所以chars()实际上是为所有实现了CharSequence接口的类型(String类，StringBuffer类，StringBuilder类等)开启了通往函数式编程和Lambda表达式的大门。

既然得到了IntStream类型的实例，那么下一步就可以使用内部遍历器forEach进行遍历了：

```java
final String str = "w00t";

str.chars().forEach(ch -> System.out.println(ch));
```

对于如println之类的静态方法，也可以使用方法引用来简化Lambda表达式的书写：

```java
str.chars().forEach(System.out::println);
```

Java编译器会自动地将目标字符串的每一个字符作为参数传入到System.out.println方法中。然而正是因为返回的Stream是IntStream类型，所以打印出来的是一个个的数字而不是我们期待的字符。可以使用下面的类型转换来确保打印的是字符：

```java
public class IterateString {
	public static void printChar(int aChar) {
		System.out.println((char)(aChar));
	}
}

str.chars().forEach(IterateString::printChar);
```

为了解决这种常见的问题，IntStream提供了一个mapToObj方法用来执行Int类型到其它任意类型的转换，该方法的签名和其相关的函数接口IntFunction如下：

```java
<U> Stream<U> mapToObj(IntFunction<? extends U> mapper);

@FunctionalInterface
public interface IntFunction<R> {
    R apply(int value);
}
```

根据mapToObj的签名，整型类型到字符类型的转换和输出就可以这样实现了：

```java
str.chars()
	.mapToObj(ch -> Character.valueOf((char)ch))
	.forEach(System.out::println);
```

因为chars()返回的是Stream类型，因此它也开启了诸多的可能性，比如使用filter方法：

```java
str.chars()
	.filter(ch -> Character.isDigit(ch))
	.forEach(ch -> IterateString.printChar(ch));

// 使用方法引用
str.chars().filter(Character::isDigit).forEach(IterateString::printChar);
```

### 参数路由(Parameter Routing) ###

我们已经看到了方法引用的两种类型：

- 实例方法的引用
- 静态方法的引用

编译器在处理这两种引用类型时，会有一点点不同：
在处理实例方法引用诸如String::toUpperCase时，集合中的元素会被当做调用目标，如果以element表示集合中的元素的话，最终的调用是这样的：element.toUpperCase()。

在处理静态方法引用诸如Character.isDigit时，集合中的元素会被当做方法的参数传入：Character.isDigit(element)。

以上的机制就是参数路由。

当一个方法引用能够同时匹配一个实例方法和一个静态方法时，编译器会因为不能确定使用哪个而报出异常。

## Comparator接口的使用 ##

Comparator接口是在JDK库中被广泛使用的一个接口。为了更好地融合函数式编程，此接口在Java 8中得到了加强。

### 使用Comparator排序 ###

以下是一个简单的JavaBean和其实例，用它来说明Comparator的使用方式的变革：

```java
public class Person {
	private final String name;
	private final int age;
	public Person(final String theName, final int theAge) {
		name = theName;
		age = theAge;
	}
	public String getName() { return name; }
	public int getAge() { return age; }
	public int ageDifference(final Person other) {
		return age - other.age;
	}
	public String toString() {
		return String.format("%s - %d", name, age);
	}
}

final List<Person> people = Arrays.asList(
	new Person("John", 20),
	new Person("Sara", 21),
	new Person("Jane", 21),
	new Person("Greg", 35));
```

1. 对年龄进行Ascending排序：

因为待排序的集合类型是List，所以自然而然地想到了使用Collections.sort()。但是这个方法的缺点在于它没有返回值，它直接对传入的List进行了修改。因此在某些场合下，并不是最好的选择。

我们可以尝试使用Stream类型的sorted方法结合Collectors.toList来完成排序：

```java
List<Person> ascendingAge =
	people.stream()
		.sorted((person1, person2) -> person1.ageDifference(person2))
		.collect(toList());
```

Stream类型的sorted方法有两个版本：

```java
Stream<T> sorted();
Stream<T> sorted(Comparator<? super T> comparator);
```

不接受参数的sorted方法会按照目标对象的自然排序(Natural Order)方式进行排序，即需要目标对象类型实现Comparable接口，如果没有实现会抛出ClassCaseException。接受参数的sorted方法接受一个Comparator接口作为参数。由于此接口是一个函数式接口(Functional Interface)，这里可以传入Lambda表达式或者方法引用。

最后，使用collect方法结合toList这种具体的Collector完成了对于Stream的归约操作(Reduction)，得到了最终排序了的集合，原来的集合没有被改变。

sorted方法在行为上和之前介绍的reduce方法非常相似，都会按照集合的顺序进行两两操作。只不过sorted方法和reduce方法的返回值类型不同，sorted方法返回的仍然是一个Stream对象，而reduce方法返回的则是一个具体的归约对象，通常它不再代表一个集合。

对于Lambda表达式：(person1, person2) -> person1.ageDifference(person2)，我们能不能将它转变成方法引用呢？答案是肯定的，转变之后的方法引用是这个样子的：Person::ageDifference。

但是这里有一个问题，在前面介绍的参数路由中，编译器处理的参数都只有一个。要么将它当做调用对象，如实例方法引用那样；要么将它当做参数传入，如静态方法引用那样。而Comparator接口中的方法需要两个参数(person1，person2)，那么编译器会如何处理呢？

实际上，编译器会将第一个参数当做调用对象，将第二个参数当做方法的参数传入。所以方法引用最终的实现和person1.ageDifference(person2)并无二致。所以这里又引出了参数路由(Parameter Routing)的第三个规则：第一个参数会被当做调用对象，剩下的会按照其顺序被当做参数传入。

### 重用Comparator ###

如果需要按照descending的方式进行排序：

```java
printPeople("Sorted in descending order of age: ",
	people.stream()
		.sorted((person1, person2) -> person2.ageDifference(person1))
		.collect(toList()));
```

根据参数路由的规则，以上就不能使用方法引用了。因为person1和person2的顺序需要被调换。所以上述代码使用了Lambda表达式。

但是不觉得这里违反了DRY原则吗。两个Lambda表达式基本上一样，除了person1和person2的声明顺序不一样。所以为了处理这种情况，Comparator接口有了新的reversed方法，它也是一个default方法：

```java
default Comparator<T> reversed() {
	return Collections.reverseOrder(this);
}
```

因此，为了提高重用性，Lambda表达式可以这样写：

```java
Comparator<Person> compareAscending = (person1, person2) -> person1.ageDifference(person2);
Comparator<Person> compareDescending = compareAscending.reversed();
```

### Comparator和max/min方法 ###

前面介绍了IntStream类型上的max和min方法，它们能够很方便的实现找到最大值和最小值的归约操作。同样地，在Stream类型上提供了更具有普遍性的max和min方法，只不过此时它们需要接受一个Comparator作为参数。

比如，下面代码的作用分别是找出年龄最小的和年龄最大的人：

```java
people.stream()
	.min(Person::ageDifference)
	.ifPresent(youngest -> System.out.println("Youngest: " + youngest));

people.stream()
	.max(Person::ageDifference)
	.ifPresent(eldest -> System.out.println("Eldest: " + eldest));
```

min和max方法的返回值是Optional对象。

### 多重比较 ###

当需要通过名字来进行排序时，仍然可以使用sorted方法：

```java
people.stream()
	.sorted((person1, person2) -> person1.getName().compareTo(person2.getName()));
```

上述Lambda表达式的右边虽然不复杂，但是有点啰嗦并且不利于重用，可以通过Comparator接口的一个静态方法进行简化：

```java
final Function<Person, String> byName = person -> person.getName();
people.stream().sorted(Comparator.comparing(byName));
```

接口静态方法是Java 8中为接口添加的另一特性。比如comparing方法的实现如下：

```java
public static <T, U extends Comparable<? super U>> Comparator<T> comparing(
    Function<? super T, ? extends U> keyExtractor)
{
    Objects.requireNonNull(keyExtractor);
    return (Comparator<T> & Serializable)
           (c1, c2) -> keyExtractor.apply(c1).compareTo(keyExtractor.apply(c2));
}
```

它实际上是一个高阶方法，通过传入一个函数式接口来返回另外一个函数式接口。

除此之外，还能够利用Comparator接口的default方法thenComparing来实现多重比较：

```java
final Function<Person, Integer> byAge = person -> person.getAge();
final Function<Person, String> byTheirName = person -> person.getName();
printPeople("Sorted in ascending order of age and name: ", people.stream()
	.sorted(Comparator.comparing(byAge).thenComparing(byTheirName))
	.collect(toList()));
```

## 使用collect方法和Collectors类 ##

前面在介绍reduce方法时，提到过collect方法和其相关联的Collectors类。下面我们就来看看它们在实际应用中是如何使用的。

比如，现在我们需要将年龄大于20的人从集合中找出来：

```java
List<Person> olderThan20 = new ArrayList<>();
people.stream()
	.filter(person -> person.getAge() > 20)
	.forEach(person -> olderThan20.add(person));
System.out.println("People older than 20: " + olderThan20);
```

上述代码很直观，但是存在几个问题：

1. 向新的集合添加元素的操作是命令式的风格，不太合适
2. 因为声明了可变的List实例，代码难以并行化

collect方法的使用可以很好地处理上面这两个问题。顾名思义，collect方法的目的就是从Stream对象中拿到并且收集元素到一个目标容器对象中。从这个角度来看，collect方法的使用需要以下几种信息：

1. 如何创建这个目标容器，比如可以使用ArrayList::new
2. 如何将元素添加到这个目标容器，比如ArrayList::add
3. 考虑到可能的并行处理，如何合并两个目标容器，比如ArrayList::addAll

使用collect方法的代码如下：

```java
List<Person> olderThan20 = people.stream()
	.filter(person -> person.getAge() > 20)
	.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
System.out.println("People older than 20: " + olderThan20);
```

collect方法的定义和用法：
```java
<R> R collect(Supplier<R> supplier,
                  BiConsumer<R, ? super T> accumulator,
                  BiConsumer<R, R> combiner);
```

- 第一个参数表示的是如何创建目标容器。它需要是一个Supplier类型的函数式接口，该类型的定义也很简单：

	```java
	@FunctionalInterface
	public interface Supplier<T> {
    	T get();
	}
	```
实际上根据定义的特点，类型的构造函数也是可以被传入的，即上面的ArrayList::new。

- 第二个参数表示的是如何收集元素到容器中。它需要接受一个BiConsumer类型的函数式接口：
	
	```java
	@FunctionalInterface
	public interface BiConsumer<T, U> {
    	void accept(T t, U u);
	}
	```

- 第三个参数表示的是如何合并多个目标容器。这是考虑到在多线程环境中，每个线程都会拥有一个容器，当所有的元素都被处理完毕之后，需要将每个线程中的容器合并起来。

下面我们来分析一下上述代码的优点：

1. 代码意图更明显，更清晰简洁。
2. 更容易并行化，因为没有显式地对任何对象进行修改的操作

因为将集合中的元素进行筛选并添加到另外一个集合中是一个非常非常普遍的操作，所以Collectors工具类中提供了一个toList方法来直接实现将元素添加到ArrayList中的操作：

```java
List<Person> olderThan20 = people.stream()
	.filter(person -> person.getAge() > 20)
	.collect(Collectors.toList());
System.out.println("People older than 20: " + olderThan20);
```

除了toList方法，Collectors工具类中还提供了需多方法比如toSet，toMap，之前介绍过的joining等。

下面介绍Collectors.groupingBy的使用：

```java
Map<Integer, List<Person>> peopleByAge = people.stream()
	.collect(Collectors.groupingBy(Person::getAge));
System.out.println("People grouped by age: " + peopleByAge);
```

上述代码很简短，可是实现的功能却并不简单。当使用传统的命令式风格进行编码时，代码量估计是上述代码量的5倍左右，还不包括为了让程序能够并行运行，需要添加的那部分代码。

groupingBy方法接受一个函数式接口作为分类器(Classifier)，用来实现分类的逻辑。正如以上的getAge方法，该方法的每一个返回值都会被作为一个分类，也就是得到的结果Map中的一个Key。

如果每个分类是由一个Key和该Key对应的分类结果组成的，那么对于分类结果实际上还可以进一步使用进行各种操作来得到该结果的一种变型，看上去有点难以理解，举一个例子就简单了：

```java
Map<Integer, List<String>> nameOfPeopleByAge = people.stream()
	.collect(Collectors.groupingBy(Person::getAge, mapping(Person::getName, Collectors.toList())));
System.out.println("People grouped by age: " + nameOfPeopleByAge);
```

groupingBy方法接受的两个参数：
- Function<? super T, ? extends K> classifier
- Collector<? super T, A, D> downstream

第一个作为分类器，第二个作为对分类结果进行进一步操作的collector。

再举一个更复杂一点的例子，我们需要根据名字的首字母进行分类，分类结果是名字以该首字母起头的年龄最大的人。

```java
Comparator<Person> byAge = Comparator.comparing(Person::getAge);
Map<Character, Optional<Person>> oldestPersonInEachAlphabet = people.stream()
	.collect(groupingBy(person -> person.getName().charAt(0), reducing(BinaryOperator.maxBy(byAge))));
System.out.println("Oldest person in each alphabet: " + oldestPersonInEachAlphabet);
```

以上的groupingBy方法的第二个参数执行了归约(Reduction)操作，而不是之前的映射(Mapping)操作。并且利用了BinaryOperator中定义的静态方法maxBy。在归约过程中，每次都会取参与的两个元素中较大的那个。最后就得到了整个集合中最大的那个元素。

-----

## 列举目录中的所有文件 ##

首先给出代码：

```java
Files.list(Paths.get(".")).forEach(System.out::println);
```

Files.list方法得到的是一个Stream类型的对象，它代表了目标路径下所有的文件。如果只想获取目标路径下的所有目录文件：

```java
Files.list(Paths.get("."))
	.filter(Files::isDirectory)
	.forEach(System.out::println);
```

在以前的Java版本中，如果需要实现一个自定义的过滤器，那么通常会选择使用FilenameFilter结合匿名类的方式：

```java
final String[] files =
	new File("target_dir").list(new java.io.FilenameFilter() {
	public boolean accept(final File dir, final String name) {
		return name.endsWith(".java");
	}
});
System.out.println(files);
```

我们说过，当遇见了匿名内部类的时候，如果被实现的接口是一个函数式接口，那么可以考虑将该匿名内部类以Lambda表达式的形式重新实现，再结合Java 8中新添加的DirectoryStream，可以将上述代码重新实现为：

```java
Files.newDirectoryStream(
	Paths.get("target_dir"), 
	path -> path.toString().endsWith(".java"))
		.forEach(System.out::println);
```

当目标目录下含有大量的文件或者子目录时，使用DirectoryStream往往会具有更好的性能。因为它实际上是一个Iterator用来遍历目标目录，而直接使用listFiles方法时，得到的是一个代表了所有文件和目录的数组，意味着内存的开销会更大。

### 使用flatMap列举所有直接子目录 ###

所谓的直接子目录(Immediate Subdirectory)，指的就是目标目录下一级的所有目录。对于这样一个任务，最直观的实现方式恐怕是这样的：

```java
public static void listTheHardWay() {
	List<File> files = new ArrayList<>();
	File[] filesInCurerentDir = new File(".").listFiles();
	for(File file : filesInCurerentDir) {
		File[] filesInSubDir = file.listFiles();
		if(filesInSubDir != null) {
			files.addAll(Arrays.asList(filesInSubDir));
		} else {
			files.add(file);
		}
	}
	System.out.println("Count: " + files.size());
}
```

很显然，此段代码噪声太多，没有清晰地反映出代码的整体目标。下面就用flatMap方法来简化它：

```java
public static void betterWay() {
	List<File> files = Stream.of(new File(".").listFiles())
		.flatMap(file -> file.listFiles() == null ?
			Stream.of(file) : Stream.of(file.listFiles()))
		.collect(toList());
	System.out.println("Count: " + files.size());
}

// flatMap
<R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper);
```

从flatMap方法的签名来看，它接受了一个Function接口作为参数，将一种类型转换为另一种类型的Stream类型。而从flatMap方法的命令来看，它的执行过程主要包含两个步骤：

1. 首先是会对当前Stream的每个元素执行一次map操作，根据传入的mapper对象将一个元素转换为对应的Stream对象
2. 将第一步中得到的若干个Stream对象汇集成一个Stream对象

从上面的代码来看，签名中的T类型就是File类型，而R类型同样也是File类型。当一个File对象不含有任何的子目录或者子文件时，那么通过Stream.of(file)来仅仅包含它自身，否则使用Stream.of(file.listFiles())来包含其下的所有子目录和子文件。

### 监视文件变化 ###

WatchService是Java 7中新添加的一个特性，用来监视一某个路径下的文件或者目录是否发生了变化。

```java
final Path path = Paths.get(".");
final WatchService watchService = path.getFileSystem().newWatchService();

path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

System.out.println("Report any file changed within next 1 minutes...");
```

注册了需要监视的目录后，需要使用WatchKey来得到一段时间内的，该目录的变化情况：

```java
final WatchKey watchKey = watchService.poll(1, TimeUnit.MINUTES);
if(watchKey != null) {
	watchKey.pollEvents().stream().forEach(event ->
	System.out.println(event.context()));
}
```

这里使用了Java 8中的内部遍历器forEach来完成对于事件的遍历。这也算是一个Java 7和Java 8特性的联合使用吧。

































































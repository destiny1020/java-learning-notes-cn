# 高级映射 #

前面介绍的可搜索的域基本上都是字符串类型，实际上可搜索的类型是非常丰富的。

本文会介绍以下几个方面的内容：

- Lucene对实体进行索引的过程
- 借助Solr组件对这个过程的改进
- 修改域的重要程度，从而让基于相关度的排序更加有意义
- 动态决定是否对一个实体类型进行索引

## 桥接器(Bridges) ##

实体类型中可以使用的类型是无穷无尽的，但是对于Lucene索引而言，任何类型归根到底都会以字符串来表示。所以，在对实体的域进行索引时，这些域最终需要被转换为字符串类型的对象。

在Hibernate Search的术语中，这个过程叫做桥接，而实现这个过程的对象就被称为桥接器。在Hibernate Search中，已经存在很多自带的桥接器用来处理大多数常见类型到字符串类型的转换。

### 一对一(One-to-One)自定义转换 ###

这是最常见的情况，表示的是实体中的一个Java属性和一个Lucene索引域的关联。下面介绍一些常见类型的映射和转换。

#### 日期域的映射 ####

日期类型的值首先会被转换成GMT时间，然后将它以“yyyyMMddHHmmssSSS”的字符串保存到索引中。

当一个日期类型被@Field标注是，这个过程是自动发生的。但是你也可以显式的使用@DateBridge，这样能够对存储到索引中的字符串进行更加精细的控制，比如我们只关注日期中的yyyyMMdd部分：

```java
@Column
@Field
@DateBridge(resolution=Resolution.DAY)
private Date releaseDate;
```

#### 处理null值 ####

默认情况下，只要值是null，无论它的类型是什么都不会被保存到索引中。但是，通过@Field的indexNullAs属性也能够对这一行为进行修改：

```java
@Column
@Field(indexNullAs=Field.DEFAULT_NULL_TOKEN)
private String description;
```

indexNullAs的默认值是Field.DO_NOT_INDEX_NULL。当使用Field.DEFAULT_NULL_TOKEN时，Hibernate Search会将null值以一个可配置的全局值保存到索引中。

这个可配置的全局值可以在hibernate.cfg.xml或者persistence.xml中：

```
hibernate.search.default_null_token=xxx
```

如果没有使用了Field.DO_NOT_INDEX_NULL但没有配置这个全局值，那么Hibernate Search会使用字符串“_null_”作为默认值。但是需要注意的是，这个是一个全局的值，意味着任何类型的null值都会以该值保存到索引中。如果需要为不同的类型设置不同的值作为null的替代，那么需要使用自定义的桥接器。

#### 自定义的字符串转换 ####

**StringBridge**

为了自定义地将一个Java属性映射到一个索引域，可以通过实现Hibernate Search提供的StringBridge接口来完成由Java属性到索引域单向的转换。

比如，对于App有一个属性叫做currentDiscountPercentage用来表示当前的折扣信息。为了计算方便，这个属性的类型时Float，但是在搜索的时候，我们希望这类信息能够通过更加有意义的方式搜索到，比如25%的折扣可以通过25搜索到，而不是0.25。此时，就需要为该属性实现一个桥接器了：

```java
import org.hibernate.search.bridge.StringBridge;
/** Converts values from 0-1 into percentages (e.g. 0.25 -> 25) */
public class PercentageBridge implements StringBridge {
	public String objectToString(Object object) {
		try {
			float fieldValue = ((Float) object).floatValue();
			if(fieldValue< 0f || fieldValue> 1f) return "0";
			int percentageValue = (int) (fieldValue * 100);
			return Integer.toString(percentageValue);
		} catch(Exception e) {
			// default to zero for null values or other problems
			return "0";
		}
	}
}
```

以上的`objectToString`方法会将输入转换成最终存储到索引中的字符串。同时注意到，当代码产生任何异常时，都会返回一个0。这也是处理null值的一种方式，毕竟当传入的object为null时，这里是会抛出异常并被捕捉到的。

为了让以上的桥接器起作用，还需要在相应实体的域上添加一个注解：

```java
@Column
@Field
@FieldBridge(impl=PercentageBridge.class)
private float currentDiscountPercentage;
```

这样一来，该桥接器就会在对该域建立索引时被调用了。

**TwoWayStringBridge**

除了StringBridge外，还可以通过实现TwoWayStringBridge借口来完成自定义的映射。正如它名字所表示的那样，它提供了双向映射的功能，来完成Java域到Index域的双向转化。

正是因为它是双向转换的，所以在实现该接口时还需要实现一个`stringToObject`的方法来完成从索引域到实体域的转化：

```java
public Object stringToObject(String stringValue) {
	return Float.parseFloat(stringValue) / 100;
}
```

仅仅当实体域需要被当做Lucene索引中的ID域时，才需要实现它。也就意味着在该实体中，当需要转换被@Id或者@DocumentId标注的域，可以考虑使用它。

**ParameterizedBridge**

这是桥接器灵活性的体现。在实现桥接器时，甚至可以将配置参数传入其中。此时的桥接器需要额外实现`ParameterizedBridge`接口(StringBridge或者TwoWayStringBridge还是需要实现的)。

比如我们需要根据具体的需求来指定折扣信息的精度，比如当discount的值是0.2533时，使用之前的桥接器得到的结果只会得到一个取整后的结果25，而显然我们能够显示的更加精确比如25.33。因此，可以通过定义一个参数来表达小数点后应该保留的位数信息：

```java
public class PercentageBridge implements StringBridge, ParameterizedBridge {
	public static final String DECIMAL_PLACES_PROPERTY = "decimal_places";
	private int decimalPlaces = 2; // default
	public String objectToString(Object object) {
		String format = "%." + decimalPlaces + "g%n";
		try {
			float fieldValue = ((Float) object).floatValue();
			if(fieldValue< 0f || fieldValue> 1f) return "0";
			return String.format(format, (fieldValue * 100f));
		} catch(Exception e) {
			return String.format(format, "0");
		}
	}

	public void setParameterValues(Map<String, String> parameters) {
		try {
			this.decimalPlaces = Integer.parseInt(parameters.get(DECIMAL_PLACES_PROPERTY) );
		} catch(Exception e) {}
	}
}
```

此时，桥接器期待接受一个名为`decimal_places`的参数。该参数会被保存到变量`decimalPlaces`中。如果没有参数被传入，那么也会使用默认的2作为`decimalPlaces`的值。

现在的问题就是，如何传入这个参数。答案是，通过@FieldBridge注解的params属性传入：

```java
@Column
@Field
@FieldBridge(
	impl=PercentageBridge.class,
	params=@Parameter(
		name=PercentageBridge.DECIMAL_PLACES_PROPERTY, value="4")
	)
private float currentDiscountPercentage;
```

另外需要注意的是，`StringBridge`和`TwoWayStringBridge`的实现都不是线程安全的。所以要避免在其中保存任何状态，如果确实需要保存某种状态的话，可以考虑让桥接器实现`ParameterizedBridge`来进行参数的传入。

### 使用FieldBridge完成更复杂的映射 ###

以上的映射都只是一对一的映射，即一个实体域对应一个索引域。其实，映射还可以是一对多或者多对一的关系，即一个实体域对应多个索引域或者多个实体域对应一个索引域。

#### 映射一个实体域到多个索引域 ####

比如当我们对文件名这一域建立索引时，会希望能够通过文件名或者文件扩展名进行搜索。那么就需要建立两个索引域，这时可以考虑使用FieldBridge接口，这个接口需要实现一个set方法，在该方法中进行索引域的设置：

```java
import org.apache.lucene.document.Document;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;

public class FileBridge implements FieldBridge {
	public void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
		String file = ((String) value).toLowerCase();
		String type = file.substring(file.indexOf(".") + 1 ).toLowerCase();
		luceneOptions.addFieldToDocument(name+".file", file, document);
		luceneOptions.addFieldToDocument(name+".file_type", type, document);
	}
}
```

set方法的参数解释如下：

- String name：表示该实体域的名字
- Object value：表示当前被映射的实体域的值
- Document document：表示的是Lucene用来表达该实体的索引结构
- LuceneOptions luceneOptions：为了和Lucene交互，如向索引中添加更多的域

set方法中的参数中的LuceneOptions对象就是为了让代码能够和Lucene交互。而Document对象。我们可以使用`luceneOptions.addFieldToDocument(name+".file", file, document);`来完成索引域的添加。

最后，通过使用@FieldBridge来使用它：

```java
@Column
@Field
@FieldBridge(impl=FileBridge.class)
private String file;
```

#### 映射多个实体域到一个索引域 ####

为了实现将多个实体域映射到一个索引域，需要在实体类层次上进行一些操作。所以此时桥接器不再是单纯的域桥接器(Field Bridge)，而是一个类桥接器(Class Bridge)。

比如对于Device实体类型，我们不想为它的manufacturer和name分别建立索引，而希望将它们作为一个整体，建立一个索引：

```java
public class DeviceClassBridge implements FieldBridge {
	public void set(String name, Object value, Document document, LuceneOptionsluceneOptions) {
		Device device = (Device) value;
		String fullName = device.getManufacturer() + " " + device.getName();
		luceneOptions.addFieldToDocument(name + ".name", fullName, document);
	}
}
```

此时就可以在该实体类型上使用它了：

```java
@Entity
@Indexed
@ClassBridge(impl=DeviceClassBridge.class)
public class Device {

	@Id
	@GeneratedValue
	private Long id;

	@Column
	private String manufacturer;

	@Column
	private String name;
	// constructors, getters and setters...
}
```

#### TwoWayFieldBridge ####

之前我们接触到了为了实现双向转换的TwoWayStringBridge。而同样的，FieldBridge接口也有其“双向”的版本：TwoWayFieldBridge。和TwoWayStringBridge类似，在被@Id或者@DocumentId标注的域上使用@FieldBridge时必须使用TwoWayFieldBridge。

在单向的FieldBridge中，我们只需使用set方法来指明从实体域到索引域是如何映射的。而在双向的TwoWayFieldBridge中，我们还需要实现一个get方法来得到索引中的字符串表示并进行必要的转换：

```java
public Object get(String name, Object value, Document document) {
	// return the full file name field... the file type field
	// is not needed when going back in the reverse direction
	return document.get(name + ".file");
}

public String objectToString(Object object) {
	// "file" is already a String, otherwise it would need conversion
	return object;
}
```

## 解析(Analysis) ##

当一个实体域被Lucene索引时，往往还会经历一个语法分析(Parsing)和转换(Conversion)的步骤，这些步骤被称为解析。在前文中，我们提到过Hibernate Search会默认对字符串类型的实体域进行分词，而这个分词过程就需要用到解析器(Analyzer)。在需要对实体域进行排序的场合，需要禁用这个默认的分词行为。

在解析过程中，还可以借助Apache Solr提供的组件来完成更多的操作。为了弄清楚Solr组件是如何参与到这个过程中并完成更多的操作，需要首先明白Lucene在进行解析时经理的三个步骤：

- 字符过滤(Character Filtering)
- 分词(Tokenization)
- 词条过滤(Token Filtering)

在第一个阶段，会使用零个或者多个字符过滤器(Character Filter)来帮助完成这个过程。它们会在字符这个水平上对数据源进行操作，比如将特定的字符进行替换，删除等等。

在第二个阶段，分词器会根据其定义的规则对数据源进行分词，得到一系列的token。这样做能够让基于关键字的搜索更具效率。

在第三个阶段，会使用零个或者多个词条过滤器来将不需要的token从数据中移除。

经历了以上三个阶段后，数据才会真正地被保存到索引中。
下面对这三个阶段进行详细的介绍。

### 字符过滤(Character Filtering) ###

当需要创建自定义的解析器时，字符过滤的定义是可选的。目前有三个可选的字符过滤器：

- MappingCharFilterFactory
	这个过滤器会将特定的字符或者字符序列根据定义进行替换，比如将1替换成one，将2替换成two等。被替换的字符和替换字符通过java.util.Properties资源文件进行声明，这个资源文件需要置于classpath上。比如1=one就表示将1替换成one。
	
- PatternReplaceCharFilter
	会基于正则表达式进行操作。正则表达式通过参数pattern传入，而替换的字符通过replacement参数传入。

- HTMLStripCharFilterFactory
	这个过滤器在处理HTML文本时非常有用，它会移除HTML的标签，同时也会将转义字符替换成其原始的形式，比如将\&\gt;替换成为>









































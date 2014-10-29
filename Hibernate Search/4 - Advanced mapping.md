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

### 分词(Tokenization) ###

和第一阶段及第三阶段使用的过滤器不一样，分词阶段使用的分词器必须有且只有一个。

常用的分词器举例如下：

- WhitespaceTokenizerFactory
	通过简单地对空白字符进行分割来得到结果，比如“Hello World”的分词结果就是Hello和World。

- LetterTokenizerFactory
	这个分词器在WhitespaceTokenizerFactory上更进一步，对于非字母类型的字符也会进行分割，所以“Please don't go”这句话会被分割成：Please, don, t, go。

- StandardTokenizerFactory
	这是默认使用的分词器。它会通过空白字符进行分割，同时会忽略掉多余的字符。多余的字符可以是各种标点符号等。比如“it's 25.5 degrees outside!!!”的分词结果是：it's, 25.5, degrees, and outside。对分词没有特殊要求时，使用默认分词即能够达到较好的效果。

### 词条过滤(Token Filtering) ###

词条过滤可以说是整个解析功能中最丰富多彩的一个阶段了，Solr提供了很多的可用组件。下面列举一些：

- StopFilterFactory
	这个过滤器会将停用词(Stop Word)全部丢弃，同时也会将一些太常见的词语过滤拆掉，因为一般而言这些词语是不会被当做关键字进行查询的，比如a，the，if，for，and，or等词都在此列。更详细的停用词和常见词汇过滤表，可以搜索此Filter的文档。

- PhoneticFilterFactory
	当你使用搜索引擎时，也许已经发现了它能够很智能地自动对一些输入错误进行更正。更正的方法之一就是查询发音相似的词语。它会将发音相似的词语也保存到索引中，因此当输入了错误的单词时，或许仍然能够返回期望的结果。

- SnowballPorterFilterFactory
	这个过滤器名中的Snowball和Porter指代的是词干提取(Stemming)算法，所谓的词干提取，就是将词干从词汇中抽取出来，比如developer和development这两个词汇在进行词干提取后，得到的结果都是develop。因此当你输入develop作为搜索关键字时，包含developer和development的内容也会被返回。这个过滤器需要接受一个language作为参数，比如English。

### 定义和选择解析器(Analyzer) ###

定义解析器实际上就是根据解析的三个步骤，定义或者选择每个步骤需要使用的组件的过程。解析器只是一个统称，用来包含它所使用的各种字符过滤器，分词器和词条过滤器。解析器可以通过静态或者动态的方式进行定义：

#### 静态定义解析器 ####

无论使用哪种方式定义解析器，都会使用@AnalyzerDef。比如，我们这里会为App实体类中的description字段定义一个解析器，用来将HTML标签全部移除，同时使用各种词条过滤器来减少被索引内容的噪声：

```java
@AnalyzerDef(
	name="appAnalyzer",
	charFilters={
		@CharFilterDef(factory=HTMLStripCharFilterFactory.class)
	},
	tokenizer=@TokenizerDef(factory=StandardTokenizerFactory.class),
	filters={
		@TokenFilterDef(factory=StandardFilterFactory.class),
		@TokenFilterDef(factory=StopFilterFactory.class),
		@TokenFilterDef(factory=PhoneticFilterFactory.class,
			params = {
				@Parameter(name="encoder", value="DoubleMetaphone")
			}),
		@TokenFilterDef(factory=SnowballPorterFilterFactory.class,
			params = {
				@Parameter(name="language", value="English")
			})
		}
)
```

可以清晰的发现，在定义中，charFilters用来定义字符过滤器；tokenizer有且只有一个用来定义分词器；filters用来定义词条过滤器。另外，charFilters和filters的执行顺序是通过定义它们的顺序决定的。这一点需要特别注意，以防出现意料之外的结果。

实际上，一个类型可以定义多个解析器。此时对每个域就可以使用不同的解析器来实现具体需求了。

```java
@AnalyzerDefs({
	@AnalyzerDef(name="stripHTMLAnalyzer", ...),
	@AnalyzerDef(name="applyRegexAnalyzer", ...)
})
```

定义了解析器后，使用@Analyzer注解来使用以上定义的解析器：

```java
@Column(length = 1000)
@Field
@Analyzer(definition="appAnalyzer")
private String description;
```

@Analyzer注解不仅可以用在单独的域，还可以直接用在类上。此时类中所有被索引的域(即被@Field标注的域)都会使用该解析器。

#### 动态定义解析器 ####

在支持多语言的应用中，语言的切换势必造成需要索引的数据的改变。而根据每种语言的特点，往往需要定义不同的解析器。所以这个定义的过程应该是一个动态的过程。为了实现这一过程，可以使用@AnalyzerDiscriminator注解，它和@Analyzer一样，可以使用在域或者类上。在以下代码中，我们将该注解使用在类之上：

```java
@AnalyzerDefs({
@AnalyzerDef(name="englishAnalyzer", ...),
@AnalyzerDef(name="frenchAnalyzer", ...)
})
@AnalyzerDiscriminator(impl=CustomerReviewDiscriminator.class)
public class CustomerReview {
	// ...

	@Field
	private String language;

	// ...
}
```

具体而言，CustomerReviewDiscriminator需要实现Discriminator接口来完成解析器的动态选择工作，它通过评论的语言来分别使用英语解析器或者法语解析器：

```java
public class CustomerReviewDiscriminator implements Discriminator {
	public String getAnalyzerDefinitionName(Object value, Object entity, String field) {
		if( entity == null || !(entity instanceof CustomerReview) ) {
			return null;
		}
		CustomerReview review = (CustomerReview) entity;
		if(review.getLanguage() == null) {
			return null;
		} else if(review.getLanguage().equals("en")) {
			return "englishAnalyzer";
		} else if(review.getLanguage().equals("fr")) {
			return "frenchAnalyzer";
		} else {
			return null;
		}
	}
}
```

当@AnalyzerDiscriminator直接使用在某个域上时，传入到getAnalyzerDefinitionName方法的第一个参数就是当前域，第二个参数entity为null。而像上述代码那样当@AnalyzerDiscriminator用在类型上时，传入的第一个参数是null，第二个参数是当前的实例对象。

当getAnalyzerDefinitionName方法返回的是null时，会告诉Hibernate Search使用默认的解析器。

## 提升搜索结果的相关度 ##

搜索结果的默认排序是根据结果和搜索关键字之间的相关度进行的。这就意味着如果一个结果中有两个域对搜索关键字匹配成功，而另外一个结果只有一个域匹配成功，那么会认为第一个结果的相关度更高。

Hibernate Search允许我们通过提升某些实体或者某些域的重要程度来对相关度的计算造成影响，从而最终得到更加有意义的搜索结果。这个提升的过程可以是静态的或者动态的，即我们可以根据运行时的环境来动态地调节某些实体和域的重要程度。

### 索引时的静态提升 ###

可以通过使用@Boost注解来设定实体或者域的重要性权重，@Boost的默认值是1.0F。当设置的值大于1.0F时，表示重要性被提升了，当设置的值小于1.0F时，表示重要性被降低了。

比如当进行如下设置时：

```java
@Boost(2.0f)
public class App implements Serializable {
	// ...

	@Boost(1.5f)
	private String name;

	@Boost(1.2f)
	private String description;

	// ...
}
```

App的重要性相比那些没有被提升的实体如Device，提升了一倍。除此之外，App实体中的name及description域也被提升了。这些提升会进行合并和叠加，意味着name和description的重要性会发生如下变化：

name：1.0F -> 1.0F * 2.0F * 1.5F = 3.0F
description：1.0F -> 1.0F * 2.0F * 1.2F = 2.4F

### 索引时的动态提升 ###

对于App的评价，我们希望5星评价会有更高的权重。这就是动态提升的一个典型用例，我们需要通过检查评价对象的星级来决定该评价的权重。此时需要使用@DynamicBoost注解结合实现BoostStrategy接口来完成：

```java
@DynamicBoost(impl=FiveStarBoostStrategy.class)
public class CustomerReview
```

```java
public class FiveStarBoostStrategy implements BoostStrategy {
	public float defineBoost(Object value) {
		if(value == null || !(value instanceof CustomerReview)) {
			return 1;
		}
		CustomerReview customerReview = (CustomerReview) value;
		if(customerReview.getStars() == 5) {
			return 1.5f;
		} else {
			return 1;
		}
	}
}
```

当@DynamicBoost被应用在类型上时，传入到defineBoost方法中的参数是该实体的当前实例。
当@DynamicBoost被应用在实体域上时，传入到defineBoost方法中的参数是该域的值。

这种模式在Hibernate Search的诸多接口中都有体现，传入的参数值会根据注解是应用到类或者域而发生不同，这一点需要注意。

以上代码的功能很简单，只有当评价是5星评价时才会将该评论的重要性提升一些。

## 条件索引(Conditional Indexing) ##

对于某些实例，我们也许并不想将它们设置为可搜索的。比如典型的当实例的active属性被设置为false时，往往意味着该实例不应该被搜索到。这个时候就需要应用条件索引了，即只有当实体符合某种要求时，它才会被索引。

在@Indexed注解中有一个名为interceptor的属性，它能够帮助我们完成条件索引。为该属性进行配置后，正常的索引过程会被拦截，从而让索引行为更具可控性。

比如，我们可以在App实体中添加一个active属性来控制App的实例是否需要被索引：

```java
@Column
private boolean active;

public App(String name, String image, String description) {
	this.name = name;
	this.image = image;
	this.description = description;
	this.active = true;
}

public boolean isActive() {
	return active;
}

public void setActive(boolean active) {
	this.active = active;
}
```

在正常情况下，active被设置成true，意味着它是可以被搜索到的。而当它变成false时，即意味着对应的App实例信息应该从索引中移除，通过@Indexed注解的interceptor属性：

```java
@Entity
@Indexed(interceptor = IndexWhenActiveInterceptor.class)
public class App {
	// ...
}
```

该拦截器IndexWhenActiveInterceptor需要实现EntityIndexingInterceptor接口：

```java
public class IndexWhenActiveInterceptor implements EntityIndexingInterceptor<App> {
	/** Only index newly-created App's when they are active */
	public IndexingOverride onAdd(App entity) {
		if(entity.isActive()) {
			return IndexingOverride.APPLY_DEFAULT;
		}
		return IndexingOverride.SKIP;
	}

	public IndexingOverride onDelete(App entity) {
		return IndexingOverride.APPLY_DEFAULT;
	}

	/** Index active App's, and remove inactive ones */
	public IndexingOverride onUpdate(App entity) {
		if(entity.isActive()) {
			return IndexingOverride.UPDATE;
		} else {
			return IndexingOverride.REMOVE;
		}
	}	

	public IndexingOverride onCollectionUpdate(App entity) {
		return onUpdate(entity);
	}
}
```

在EntityIndexingInterceptor接口中定义了四个会被Hibernate Search调用的方法，调用的时机则是根据实体对象的生命周期决定：

- onAdd：
	当实体对象被创建时发生调用。

- onDelete：
	当实体对象从数据库中移除时发生调用。

- onUpdate：
	当已经存在的实体对象被更新时发生调用。

- onCollectionUpdate：
	当实体对象是通过批量的方式完成更新时发生调用。一般而言，直接调用onUpdate方法就可以了。

以上的每个方法都需要返回IndexingOverride枚举类型，它拥有四个枚举值：

- IndexingOverride.SKIP：
	用来告诉Hibernate Search不要为该实例更新Lucene索引。

- IndexingOverride.REMOVE：
	用来告诉Hibernate Search删除该实例的Lucene索引，如果该实例本身就没有被索引，那么不会执行任何操作。

- IndexingOverride.UPDATE：
	用来告诉Hibernate Search为该实例更新Lucene索引，如果该实例没有被索引过，就为它添加索引。

- IndexingOverride.APPLY_DEFAULT：
	代表了默认行为。在onAdd中使用它意味着会为该实例添加索引；在onDelete中使用意味着会将该实例的索引信息移除；在onUpdate或者onCollectionUpdate中使用则是用来更新实例的索引。

了解了EntityIndexingInterceptor接口和IndexingOverride的用法，上述代码的作用就一目了然了。





















































































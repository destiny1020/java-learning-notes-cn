# 实体类型映射 #

在上一篇文章中，简要介绍了如何使用Hibernate Search来对一个实体进行全文搜索。

然而，在真实的应用中，实体与实体之间的关系也许更为复杂。为了对复杂实体进行搜索，就需要让底层的Lucene查询也能够理解这些关系。

下图反映了Database，Hibernate，Hibernate Search和Lucene之间的关系：

图1

## 域映射选项(Field Mapping Options) ##

我们已经知道@Field注解用来让某个域可以被全文搜索到。
实际上，在添加该注解后，Hibernate Search会将一些有意义的默认设置添加到相应的Lucene索引中。

这些被添加的默认设置通常都可以通过@Field的属性进行设置，下面对这些属性进行简要介绍：

- analyze：告诉Lucene是否直接保存该域的数据或者将该域进行某种处理(Analysis, Parsing, Processing, etc)后再进行保存。可以设置成Analyze.NO或者Analyze.YES。在后面的“执行查询”一文中会对它们进行更详细的说明。
- index：告诉Lucene当前域是否需要被索引，默认值是Index.YES。也许这个属性会显得有些奇怪，既然已经被@Field标注了，那为什么要设置是否需要被索引呢？其实，在一些高级查询中这种情况是存在的，在后面的“高级查询”一文中会进行说明。
- indexNullAs：声明如何处理域值为null的情况。默认情况下，null值会被忽略并且也不会被添加到索引中。但是，我们也可以将null值索引成某些特殊的值。在“高级映射”中会进行介绍。
- name：用来给该域在Lucene索引中赋予一个名字，默认使用的就是该域的名字进行设置。
- norms：用来决定是否存储用在索引时提升(Index-time Boosting)的信息。默认设置为Norms.YES。在“高级映射”中会进行说明。
- store：通常情况下，域会以一种优化后的形式被保存到索引中。这样做虽然可以提升搜索性能，但是在取得搜索结果时，该域的原始数据可能也会被丢失。默认设置是Store.NO，也就意味着原始数据也许会被丢失。当设置成Store.YES或者Store.COMPRESS后，在获取搜索结果时可以直接从Lucene索引中取得，而不需要再访问一次数据库。在“高级查询”中会进行说明。

## 一域多映射 ##

比如，当一个域需要同时被设置成“能搜索”和“能排序”时，就需要设置多个映射：

```java
@Column
@Fields({
	@Field,
	@Field(name="sorting_name", analyze=Analyze.NO)
})
private String name;
```

目前，只需要了解一个域可以被设置多个@Field就够了，而且每个@Field的name属性必须不同。因此上面代码中的一个@Field的name被设置成了sorting_name，另一个@Field使用的是默认的name。

## 映射数值类型的域 ##

前面使用@Field的例子都是针对字符串类型的，显然数值类型在某些情况下也需要被索引。但是在默认情况下，数值类型也会当做字符类型被索引，因此在对数值类型的域进行排序等操作时的效率十分低下。

为了提高这种情况下的性能，Hibernate Search提供了一个注解@NumericField用来处理Integer，Long，Float和Double等数值类型：

```java
@Column
@Field
@NumericField
private float price;
```

当一个域存在多个@Field时，如果需要对某个@Field使用@NumericField，那么还需要设置@NumericField的forField属性为对应的name来完成关联。

## 实体间的关系 ##

一旦一个实体类型被标注为@Indexed，那么Hibernate Search仅仅会为该实体创建一个Lucene索引。因此，我们可以为每个需要被搜索的实体创建单独的Lucene索引，然而这种做法是十分低效的。

当我们使用Hibernate ORM对实体进行建模时，实体间的关系通常已经被诸如@ManyToMany，@ManyToOne等注解表示了。因此，我们可以利用这一点来建立更加高效的Lucene索引。

### 关联的实体 ###

为了表示某个App能够运行的平台，我们可以建立Device实体类型并让它和App实体建立联系：

```java
@Entity
public class Device {
	@Id
	@GeneratedValue
	private Long id;

	@Column
	@Field
	private String manufacturer;

	@Column
	@Field
	private String name;

	@ManyToMany(mappedBy="supportedDevices",
		fetch=FetchType.EAGER,
		cascade = { CascadeType.ALL })
	@ContainedIn
	private Set<App> supportedApps;

	public Device() {
	}

	public Device(String manufacturer, String name, Set<App>supportedApps) {
		this.manufacturer = manufacturer;
		this.name = name;
		this.supportedApps = supportedApps;
	}

	// Getters and setters for all fields...
}
```

将supportedApps这一域的@ManyToMany中的cascade属性设置为CascadeType.ALL的作用是为了让Device或者App中任一实体发生改变时，索引都会被自动更新。除了保证cascade的属性外，还需要将以上多对多的关系设置成双向的：

```java
// 在App实体类中

@ManyToMany(fetch=FetchType.EAGER, cascade = { CascadeType.ALL })
@IndexedEmbedded(depth=1)
private Set<Device>supportedDevices;
```
另外，在Device实体类的supportedApps域上我们还使用了另外一个注解@ContainedIn。这个注解的作用是告诉Lucene，在App实体类型的索引中需要同时包含对应Device的数据。

同时，在App实体类中我们也使用了新的注解叫做@IndexedEmbedded。这个注解和@ContainedIn正好是一对，需要同时使用。它的目的是为了防止循环依赖，在Device中我们声明了在App的索引中需要包含Device信息，而在这里我们限制了包含的信息层次，即depth=1的意义。它表示App中包含了Device信息，但是该Device信息不会再包含其对应的App信息，从而防止了循环引用。

#### 查询关联的实体 ####

一旦实体之间建立了联系，那么在查询中的声明就十分简单了：

```java
QueryBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder()
	.forEntity(App.class ).get();
org.apache.lucene.search.Query luceneQuery = queryBuilder
	.keyword()
	.onFields("name", "description", "supportedDevices.name")
	.matching(searchString)
	.createQuery();
org.hibernate.Query hibernateQuery = fullTextSession.createFullTextQuery(luceneQuery, App.class);
```

在onFields方法中，我们不仅声明了App实体上的name和description域，还声明了关联的Device实体的name域。此时，如果我们使用某种设备的名称作为关键字进行App实体的搜索，就能够获取到支持该种设备的所有App实例了。

### 嵌入对象(Embedded Objects) ###

关联的实体本身是独立的，它们拥有独立的数据库表结构和Lucene索引。这意味着当我们删除某一个App实体的时候，与其关联的Device实体并不会被同时删除。

而与之相反，嵌入对象并不是独立的，它们没有独立的数据库表结构和Lucene索引。顾客对于某个App实体的评价信息就可以被建模为嵌入对象。当某个App实体被删除之后，相关联的评价信息也应该同时被删除。可以建模如下：

```java
@Embeddable
public class CustomerReview {

	@Field
	private String username;
	private int stars;

	@Field
	private String comments;

	publicCustomerReview() {
	}

	public CustomerReview(String username, int stars, String comments) {
		this.username = username;
		this.stars = stars;
		this.comments = comments;
	}
	// Getter and setter methods...
}
```

这类实体使用@Embeddable进行标注，而不是使用@Entity。它表示CustomerReview对象的生命周期是由包含它的实体类型所决定的。

而@Field注解的使用方式和之前并无二致。但是，对于@Embeddable类型的实体，Hibernate Search并不会为它创建单独的Lucene索引。因此@Field此时只会向包含它的实体的索引中添加必要的信息。

在App实体类型中，是这样于CustomerReview建立关联的：

```java
@ElementCollection(fetch=FetchType.EAGER)
@Fetch(FetchMode.SELECT)
@IndexedEmbedded(depth=1)
private Set<CustomerReview> customerReviews;
```

和之前使用的@ManyToMany这一类表示实体间关系的注解不同，以上代码中使用的是@ElementCollection来表示实体和嵌入对象的关系。如果关联的嵌入对象只有一个，那么@ElementCollection也是不需要的。

紧接着的@Fetch注解是Hibernate特有的一个注解，它用来保证多个CustomerReview实例是通过多个SELECT语句而不是一个OUTER JOIN进行获取的。这避免了Hibernate在读取嵌入对象时可能存在的重复记录，具体细节可以参考[这里](http://stackoverflow.com/questions/1093153/hibernate-collectionofelements-eager-fetch-duplicates-elements)。然而在嵌入对象的数量十分巨大时，使用这种Eager的方式并不好，考虑到这里的应用场景，每个App实体的评论对象不会太多，所以使用Eager读取方式是合理的。

在查询中对嵌入对象进行查询的方式和对关联对象的查询是一致的：

```java
QueryBuilder queryBuilder = fullTextSession.getSearchFactory().buildQueryBuilder()
	.forEntity(App.class ).get();
org.apache.lucene.search.Query luceneQuery = queryBuilder
	.keyword()
	.onFields("name", "description", "supportedDevices.name", "customerReviews.comments")
	.matching(searchString)
	.createQuery();
org.hibernate.Query hibernateQuery = fullTextSession.createFullTextQuery(
	luceneQuery, App.class);
```

在onFields方法中使用了`customerReviews.comments`。
现在我们的应用不仅能够搜索App实体，还能够搜索其关联的Device实体和CustomerReview嵌入对象了。

## 部分索引(Partial Indexing) ##

重申一遍：对于关联实体，它们会拥有自己的索引。对于嵌入对象，它们的信息只会存在于包含它的实体的索引中。

但是，需要注意的是嵌入对象可能被嵌入到不止一个实体中。比如嵌入对象Address就可以用在和地址相关的任何实体类型中。

通常而言，@Field注解用来告诉Hibernate Search那些域是需要被索引和搜索的。但是我们能不能根据使用的场合不同，而进一步进行区分呢？我们举一个例子来说明这个应用场景：

在App中，含有其对应的CustomerReview嵌入对象，而这个对象中有两个字段username和comments被@Field注解标注了。显然，在对App进行全文搜索时，评论信息来自于哪个用户是不需要被搜索到的。所以我们想在App的索引信息中只包含CustomerReview的comments域，而不包含username域。

我们可以通过@IndexedEmbedded注解的includePath属性来进行声明：

```java
@ElementCollection(fetch=FetchType.EAGER)
@Fetch(FetchMode.SELECT)
@IndexedEmbedded(depth=1, includePaths = { "comments" })
private Set<CustomerReview> customerReviews;
```

此时CustomerReview的username信息就不会被添加到App的索引中。这样做满足我们的需求，同时也节省了空间，提高了性能。而在具体的查询中，我们也需要注意不能够再使用customerReviews.username。

## 映射API ##

前面我们介绍了如何使用Hibernate Search提供的注解来完成从Hibernate到Lucene的映射。当然，完全不使用那些注解，仅仅使用映射API也是能够完成映射的。

使用映射API在映射信息不固定时是非常有效的，比如运行时的映射信息会根据运行环境的不同而发生改变等等。同时，使用映射API也是在你无法对实体类型进行修改时，建立映射的唯一方法。

映射API的核心是SearchMapping类，它保存了Hibernate Search的配置信息，而这些配置信息通常是来源于散步在各个实体类中的注解。该类提供的方法都非常直观，比如entity方法对应的就是@Entity注解，indexed方法对应的就是@Indexed注解。

如果你需要查阅更多关于映射API的信息和用法，可以参考[这里](http://hibernate.org/search/documentation/)。

以下的映射代码能够完成以上使用注解完成映射工作：

```java
public class SearchMappingFactory {
	@Factory
	public SearchMapping getSearchMapping() {
		SearchMapping searchMapping = new SearchMapping();
		searchMapping
			.entity(App.class)
				.indexed()
				.interceptor(IndexWhenActiveInterceptor.class)
				.property("id", ElementType.METHOD).documentId()
				.property("name", ElementType.METHOD).field()
				.property("description", ElementType.METHOD).field()
				.property("supportedDevices",
					ElementType.METHOD).indexEmbedded().depth(1)
				.property("customerReviews",
					ElementType.METHOD).indexEmbedded().depth(1)

			.entity(Device.class)
				.property("manufacturer", ElementType.METHOD).field()
				.property("name", ElementType.METHOD).field()
				.property("supportedApps",
					ElementType.METHOD).containedIn()

			.entity(CustomerReview.class)
				.property("stars", ElementType.METHOD).field()
				.property("comments", ElementType.METHOD).field();

		return searchMapping;
	}
}
```

使用的类名和方法名都不重要，重要的是方法需要被@org.hibernate.search.annotations.Factory注解标注。然而，在hibernate.cfg.xml配置文件中添加一个属性来使用该方法：

```
...
<property name="hibernate.search.model_mapping">
	a.b.c.SearchMappingFactory
</property>
...
```

此时，当Hibernate ORM打开一个session时，Hibernate Search就会委托Luecene为相应实体建立索引和其它相关信息了。












































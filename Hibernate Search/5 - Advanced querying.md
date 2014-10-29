# 高级查询 #

在介绍了更多的高级映射功能之后，是时候回顾一下之前介绍过的查询功能了，看看如何借助这些高级的映射功能来使用一些高级的查询功能。本文会通过以下几个方面进行介绍：

- 如何在不和数据库进行任何交互的前提下，借助Lucene的力量来动态的筛选结果
- 如何通过使用基于投影(Projection)的查询来获取需要的属性，从而避免与数据库的交互
- 如何使用分面搜索(Faceted Search)对搜索结果进行划分
- 如何使用查询时提升(Boosting)
- 如何给查询设置时间限制

## 过滤(Filtering) ##

虽然是全文搜索，但是我们有时候需要将搜索的结果限定到某个范围内。比如，当我们只需要搜索特定设备上的支持的App，有以下几个思路：

- 将限定范围作为搜索关键字传入到查询对象中。但是稍微想想就会发现问题：这样做只会增大搜索的范围而导致更多的结果被返回，因为搜索关键字变多了。

- 使用布尔查询，向其中添加must子查询。这样做是可行的，只不过这样做会让DSL难以维护，失去其简洁的特点。同时，如果需要过滤逻辑相对比较复杂的话，使用DSL会让代码变的臃肿。

- 由于Hibernate Search中的FullTextQuery是继承自Hibernate ORM Query(或者相应的JPA Query)对象。所以我们可以考虑使用类似ResultTransformer这种对象进行过滤。但是这样做的问题是会让代码和数据库之间的交互变的更多，导致性能的下滑。

实际上，针对这一类问题Hibernate Search提供了一套更优雅和高效的解决方案：过滤器(Filter)。

过滤器会将过滤的逻辑封装到其中，然后在运行时通过动态地使用这些过滤器来完成需要的过滤操作。过滤行为是针对Lucene索引的，被过滤的内容绝对不会出现在最终的搜索结果中。因此从某种意义上而言，它也减小了最终需要从数据库中获取的数据量。

### 创建一个过滤器工厂 ###

过滤器对应着Lucene中的org.apache.lucene.search.Filter类型。因此，对于简单的过滤器直接创建Filter类型的一个子类就够了。但是，如果想在运行时根据条件动态地生成Filter实例，就需要使用过滤器工厂：

```java
public class DeviceFilterFactory {
	private String deviceName;

	@Factory
	public Filter getFilter() {
		PhraseQuery query = new PhraseQuery();
		StringTokenizertokenzier = new StringTokenizer(deviceName);
		while(tokenzier.hasMoreTokens()) {
			Term term = new Term("supportedDevices.name", tokenzier.nextToken());
			query.add(term);
		}
		Filter filter = new QueryWrapperFilter(query);
		return new CachingWrapperFilter(filter);
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName.toLowerCase();
	}
}
```

上述代码中最关键的就是@Factory注解的使用。它表明了getFilter方法能够返回一个过滤器实例。在getFilter的实现中，必须要使用一些Lucene的原生API，它虽然没有Hibernate Search DSL方便，但是也并不难理解。

最终返回的过滤器的类型时CachingWrapperFilter，使用它是为了将过滤器进行缓存来避免创建不必要的重复Filter，它封装了QueryWrapperFilter实例，而后者则建立在一个Query对象上。这个Query对象表示的就是进行筛选操作所必要的查询。这里我们想精确的匹配设备名称，因此使用的查询类型时短语查询(PhraseQuery)。

让我们回顾一下数据在被Lucene索引时所经历的过程：

- 解析器会进行字符过滤，分词和词条过滤，然后将每个词条都抽象为Lucene中的一个数据单元(即Term类型，上面的代码中有用到)。
- 在将数据写入索引前，默认的解析器会将字符串数据转换为小写的形式。

但是在使用Hibernate Search时，这些Lucene细节都不需要开发人员费心。可是，当像上述代码那样使用底层Lucene API时，就需要注意这些细节了。因此，在setDeviceName方法中，我们会将传入的deviceName转换为小写的。然后在创建Query类型时，会将分词得到的每个词条都先转换为Term类型，再添加到Query中。

#### 添加过滤器键(Filter Key) ####

正因为在创建过滤器时，我们使用了CachingWrapperFilter完成了一次封装用来缓存该过滤器。所以当需要从缓存中取回某个过滤器时，我们还需要使用一个Key，这个Key就是所谓的过滤器键(Filter Key)。这里我们使用需要过滤的设备名称作为键，配合@Key实现如下：

```java
@Key
Public FilterKey getKey() {
	DeviceFilterKey key = new DeviceFilterKey();
	key.setDeviceName(this.deviceName);
	return key;
}
```

该方法也实现在DeviceFilterFactory类型中。getKey方法返回的DeviceFilterKey类型，是FilterKey的一个子类型。它作为Key，自然而然就需要覆盖equals和hashCode方法：

```java
public class DeviceFilterKey extends FilterKey {
	private String deviceName;

	@Override
	public boolean equals(Object otherKey) {
		if(this.deviceName == null || !(otherKey instanceof DeviceFilterKey)) {
			return false;
		}
		DeviceFilterKey otherDeviceFilterKey = (DeviceFilterKey) otherKey;
		return otherDeviceFilterKey.deviceName != null && this.deviceName.equals(otherDeviceFilterKey.deviceName);
	}

	@Override
	public int hashCode() {
		if(this.deviceName == null) {
			return 0;
		}
		return this.deviceName.hashCode();
	}

	// GETTER AND SETTER FOR deviceName...
}
```

实际上，通过使用Apache Commons库中的相关API可以很方便的对这两个方法完成覆盖。

### 建立过滤器定义 ###

完成了@Factory和@Key需要的创建过滤器和获取过滤器的方法，下面需要做就是使用它。
通过@FullTextFilterDefs和@FullTextFilterDef完成定义：

```java
@FullTextFilterDefs({
	@FullTextFilterDef(
		name="deviceName", impl=DeviceFilterFactory.class
	)
})
public class App
```

@FullTextFilterDef的name属性中定义的值可以在Hibernate Search的查询中引用到，下面会进行介绍。@FullTextFilterDefs的使用也说明了每个类型能够定义多个Filters。

### 在查询中使用过滤器 ###

现在是万事俱备只欠东风。最后是在代码中对定义的过滤器进行调用：

```java
if(selectedDevice != null && !selectedDevice.equals("all")) {
	hibernateQuery.enableFullTextFilter("deviceName").setParameter("deviceName", selectedDevice);
}
```
通过判断从前台传入的selectedDevice的值来决定是否启用过滤器。前台通常可以使用一个下拉菜单来让用户选择从而缩小搜索范围。

enableFullTextFilter方法中接受的参数就是在Filter定义中的name属性值。然后调用setParameter方法，它转而会调用DeviceFilterFactory中的setDeviceName方法完成所需参数的注入。

## 投影(Projection) ##

在目前使用的查询中，最终都会通过和数据库进行交互来得到我们所需要的记录。尽管当数据量较大时，我们可以采用分页(Pagination)的技术来限制每次获取的数据量，不过无论获取的数据量多小，都难免需要和数据库进行交互。然而，我们是否能通过直接读取Lucene索引来获得感兴趣的数据呢？

答案是肯定的，Hibernate Search提供了一种叫做投影的技术来消除或者减少查询对于数据库的依赖。
基于投影的查询只会返回实体对象在Lucene索引中已经存在的数据，而不是返回读取数据库后得到的完整实体对象。

通常而言，全文搜索返回的结果通常会以一种摘要的形式呈现给用户。这也意味着，详细的信息往往在这一阶段是不需要的。只有当用户点击了诸如“更多信息”这种按钮或者链接后，具体的信息才需要被读取并展现。在显示摘要信息时，需要的信息在大多数情况下已经存在于Lucene建立的索引中了，而这正是投影操作的用武之地。

### 让查询建立在投影上 ###

我们可以通过setProjection方法来让一个查询转换成基于投影的查询：

```java
hibernateQuery.setProjection("id", "name", "description", "image");
```

传入到setProjection方法中的域名需要是存在于Lucene索引中的域，即它们需要被@Field标注。除此之外，还需要对@Field的store属性进行配置，在后面会进行介绍。

### 将投影结果转换为对象 ###

在执行了基于投影的查询后，返回的对象类型并不是我们需要的App，而是Object[]。因此索引位0到4的值分别就是id，name，description和image。

Object[]也被称为元组(Tuple)类型，可是Java语言并不在其语言层次支持元组这种类型，所以我们往往需要将它转换成相应的实体类型。我们可以借助Hibernate ORM提供的ResultTransformer来完成这个转换，具体而言是AliasToBeanResultTransformer类型：

```java
hibernateQuery.setResultTransformer(new AliasToBeanResultTransformer(App.class));
```

该类型会将投影操作的对象域和对应实体类之间的域进行比较，实现从元组到实体类型的转换。比如我们在投影操作中指定了description域，那么当AliasToBeanResultTransformer发现了App类中也存在同名的域时，就会将元组中的description赋值到正在创建的App实例的description中。

因为投影操作只是针对App实体类型的一部分域，所以最终经过转换得到的App实例也不是一个完整的实例，它只包含了部分的域。但是，这些域已经能够支持概要视图的显示了。

### 使Lucene域能够被投影 ###

默认情况下，Lucene在建立索引时会认为投影操作不会被使用。因此，索引的建立也会被相应地优化。
所以在需要使用投影操作时，还需要进行一些修改。

首先，域数据需要被保存到索引中，从而让投影操作能够直接从索引中获取到数据。为了让索引同时也保存域数据，需要修改@Field的store属性：

```java
@Field(store=Store.COMPRESS)
private String description;
```

Store枚举类型中有三个选项：

- Store.NO
	这是默认值。它会对域进行解析从而为它建立索引，但是它不会将域值也保存到索引中。因此，使用该选项时是不能支持投影操作的。

- Store.YES
	在建立索引的同时，它会将域值也保存到索引中，用来支持投影操作。但是这样显然会增加索引的空间占用。

- Store.COMPRESS
	前面两个选项的折中方案。它仍然会在索引中保存域值，但是它会通过使用压缩算法来减小索引的空间占用。因此，在使用该选项时，意味着建立索引的过程会消耗更多的计算资源。另外，在处理被@NumericField注解标注的域时，是不能够使用它的。

同时，在使用Store.YES或者Store.COMPRESS时，该域必须要使用一个双向域桥接器(Bi-directional Field Bridge)。但是不要紧张，Hibernate Search已经为JDK中的基本类型提供了一套默认的双向域桥接器。只不过，当需要在你自定义的类型上使用投影操作时，就需要你为它提供一个双向域桥接器了。它必须基于TwoWayStringBridge或者TwoWayFieldBridge。

最后，投影只能应用于实体类型中的的基础属性(Basic Property)，比如字符串类型等。它不能够获取到关联对象和嵌入对象中的相关属性。

在使用投影操作时，如果确实有必要获取到关联对象或者嵌入对象时，可以考虑首先获得到实体对象的主键信息，然后通过该主键信息去读取其关联对象表中的相应记录，因为在关联对象表中往往会有一个外键用来表示关联信息。

## 分面搜索(Faceted Search) ##

### 离散分面(Discrete Facets) ###

### 范围分面(Range Facets) ###

## 查询时提升(Query-time Boosting) ##

## 查询的时间限制 ##
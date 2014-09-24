# 数据库性能最佳实践 #

当应用需要连接数据库时，那么应用的性能就可能收到数据库性能的影响。比如当数据库的I/O能力存在限制，或者因缺失了索引而导致执行的SQL语句需要对整张表进行遍历。对于这些问题，仅仅对应用代码进行优化可能是不够，还需要了解数据库的知识和特点。

### 示例数据库 ###

该数据库表示了128只股票在1年内(261个工作日)的股价信息。

其中有两张表：STOCKPRICE和STOCKOPTIONPRICE。
STOCKPRICE中使用股票代码作为主键，另外还有日期字段。它有33408条记录(128 \* 261)。
STOCKOPTIONPRICE中存放了每只股票在每天的5个Options，主键是股票代码，另外还有日期字段和表示Option号码的一个整型字段。它有167040条记录(128 \* 261 \* 5)。

## JDBC ##

TODO

## JPA ##

对JPA的性能影响最大的是它使用的JDBC Driver。除此之外，还有一些其他因素也会影响JPA的性能。

JPA是通过对实体类型的字节码进行增强来提高JPA的性能的，这一点在Java EE环境中对用户是透明的。但是在Java SE环境中需要确保这些字节码操作的正确性。否则，会出现各种各样的问题影响JPA的性能，比如：

- 需要懒加载(Lazy Load)的字段被立即加载(Eager Load)了
- 保存到数据库中的字段出现了不必要的冗余
- 应当保存到JPA缓存中的数据没有保存，导致本不必要的重取(Refetch)操作

JPA对于字节码的增强一般作为编译阶段的一部分。在实体类型被编译成为字节码后，它们会被后置处理程序(它们是实现相关的，也就是EclipseLink和Hibernate使用的后置处理程序是不同的)进行处理来增强这些字节码，得到经过优化了的字节码文件。

在有的JPA实现中，还提供了当类被加载到JVM中时，动态增强字节码的方法。需要为JVM指定一个agent，通过启动参数的形式提供。比如当希望使用EclipseLink的这一功能时，可以传入：`-javaagent:path_to/eclipselink.jar`

### 事务处理(Transaction Handling) ###

JPA可以使用在Java SE和Java EE应用中。区别在于事务的处理方式。

在Java EE中，JPA事务只是应用服务器的Java事务API(JTA)实现的一部分。它提供了两种方式用来处理事务的边界：

- 容器管理事务(Container-Managed Transaction，CMT)
- 用户管理事务(User-Managed Transaction, UMT)

顾名思义，CMT会将事务的边界处理委托给容器，而UMT则需要用户在应用中指定边界的处理方式。在合理使用的情况下，CMT和UMT并没有显著的区别。但是，在使用不当的情况下，性能就会出现差异了，尤其是在使用UMT时，事务的范围可能会定义的过大或者过小，这样会对性能造成较大的影响。可以这样理解：CMT提供了一种通用的和折中的事务边界处理方式，使用它通常会更安全，而UMT则提供了一种更加灵活的处理方式，但是灵活是建立在用户必须十分了解它的基础上的。

```java
@Stateless
public class Calculator {
	@PersistenceContext(unitName="Calc")
	EntityManager em;
	@TransactionAttribute(REQUIRED)
	public void calculate() {
		Parameters p = em.find(...);
		// ...perform expensive calculation...
		em.persist(...answer...);
	}
}
```

上述代码使用了CMT(使用了@TransactionAttribute注解)，事务的作用域是整个方法。当隔离等级是可重复读(Repeatable Read)时，意味着在进行计算(以上的Expensive Calculation注释行)时，需要的数据会一直被锁定，从而对性能造成了影响。

在使用UMT时，会更灵活一点：

```java
@Stateless
public class Calculator {
	@PersistenceContext(unitName="Calc")
	EntityManager em;
	public void calculate() {
		UserTransaction ut = ... lookup UT in application server...;
		ut.begin();
		Parameters p = em.find(...);
		ut.commit();

		// ...perform expensive calculation...

		ut.begin();
		em.persist(...answer...);
		ut.commit();
	}
}
```

上述代码的calculate方法没有使用@TransactionAttribute注解。而是在方法中声明了两段Transaction，将昂贵的计算过程放在了事务外。当然，也可以使用CMT结合3个方法来完成上面的逻辑，但是显然UMT更加方便和灵活。

在Java SE环境中，EntityManager被用来提供事务对象，但是事务的边界仍然需要在程序中进行设划分(Demarcating)。比如在下面的例子中：

```java
public void run() {
	for (int i = startStock; i < numStocks; i++) {
		EntityManager em = emf.createEntityManager();
		EntityTransaction txn = em.getTransaction();
		txn.begin();
		while (!curDate.after(endDate)) {
			StockPrice sp = createRandomStock(curDate);
			if (sp != null) {
				em.persist(sp);
				for (int j = 0; j < 5; j++) {
					StockOptionPriceImpl sop = createRandomOption(sp.getSymbol, sp.getDate());
					em.persist(sop);
				}
			}
			curDate.setTime(curDate.getTime() + msPerDay);
		}
		txn.commit();
		em.close();
	}
}
```
上述代码中，整个while循环被包含在了事务中。和在JDBC中使用事务时一样，在事务的范围和事务的提交频度上总会做出一些权衡，在下一节中会给出一些数据作为参考。

#### XA事务 ####
TODO

#### 总结 ####

1. 在了解UMT的前提下，使用UMT进行事务的显式管理会有更好的性能。
2. 希望使用CMT进行事务管理时，可以通过将方法划分为多个方法从而将事务的范围变小。


### JPA写优化 ###

在JDBC中，有两个关键的性能优化方法：

- 重用PreparedStatement对象
- 使用批量更新操作

JPA也能够完成这两种优化，但是这些优化不是通过直接调用JPA的API来完成的，在不同的JPA实现中启用它们的方式也不尽相同。对于Java SE应用，想启用这些优化通常需要在persistence.xml文件中设置一些特定的属性。

比如，在JPA的参考实现(Reference Implementation)EclipseLink中，重用PreparedStatement需要向persistence.xml中添加一个属性：

```xml
<property name="eclipselink.jdbc.cache-statements" value="true" />
```

当然，如果JDBC Driver能够提供一个Statement Pool，那么启用该特性比启用JPA的以上特性更好。毕竟JPA也是建立在JDBC Driver之上的。

如果需要使用批量更新这一优化，可以向persistence.xml中添加属性：

```xml
<property name="eclipselink.jdbc.batch-writing" value="JDBC" />
<property name="eclipselink.jdbc.batch-writing.size" value="10000" />
```

批量更新的Size不仅可以通过上面的`eclipselink.jdbc.batch-writing.size`进行设置，还可以通过调用EntityManager上的flush方法来让当前所有的Statements立即被执行。

下表显示了在使用不同的优化选项时，执行时间的不同：

| 优化选项 | 时间 |
| --- | --- |
| 无批量更新, 无Statement缓存 | 240s |
| 无批量更新, 有Statement缓存 | 200s |
| 有批量更新, 无Statement缓存 | 23.37s |
| 有批量更新, 有Statement缓存 | 21.08s |

#### 总结 ####

1. JPA应用和JDBC应用类似，限制对数据库写操作的次数能够提高性能。
2. Statement缓存能够在JPA或者JDBC层实现，如果JDBC Driver提供了这个功能，优先在JDBC层实现。
3. JPA更新操作有两种方式实现，一是通过声明式(即向persistence.xml添加属性)，二是通过调用flush方法。

### JPA读优化 ###

因为JPA缓存的参与，使得JPA的读操作比想象中的要复杂一点。同时也因为JPA会将缓存的因素考虑进来，JPA生成的SQL也并不是最优的。

JPA的读操作会在三个场合下发生：

- 调用EntityManager的find方法
- 执行JPA查询语句
- 需要使用某个实体对象关联的其它实体对象

对于前两种情况，能够控制的是读取实体对象对应表的部分列还是整行，是否读取该实体对象关联的其它对象。

**尽量少地读取数据**

可以将某个域设置为懒加载来避免在读该对象时就将此域同时读取。当读取一个实体对象时，被声明为懒加载的域将会从被生成的SQL语句中排除。此后只要在调用该域的getter方法时，才会促使JPA进行一次读取操作。对于基本类型，很少使用这个懒加载，因为它们的数据量较小。但是对于BLOB或者CLOB类型的对象，就有必要了：

```java
@Lob
@Column(name = "IMAGEDATA")
@Basic(fetch = FetchType.LAZY)
private byte[] imageData
```

以上的IMAGEDATA字段因为太大且不会经常被使用，所以被设置成懒加载。这样做的好处是：

- 让SQL执行的更快
- 节省了内存，减小了GC的压力

另外需要注意的是，懒加载的注解(fetch = FetchType.LAZY)对于JPA的实现只是一个提示(Hint)。真正在执行读取操作的时候，JPA也许会忽略它。

与懒加载相反，还可以指定某些字段为立即加载(Eager Load)字段。比如当一个实体被读取时，该实体的相关实体也会被读取，像下面这样：

```java
@OneToMany(mappedBy="stock", fetch=FetchType.EAGER)
private Collection<StockOptionPriceImpl> optionsPrices;
```

对于@OneToOne和@ManyToOne类型的域，它们默认的加载方式就是立即加载。所以在需要改变这一行为时，使用`fetch = FetchType.LAZY`。同样的，立即加载对于JPA也是一个提示(Hint)。

当JPA读取对象的时候，如果该对象含有需要被立即加载的关联对象。在很多JPA的实现中，并不会使用JOIN语句在一条SQL中完成所有对象的读取。它们会执行一条SQL命令首先获取到主要对象，然后生成一条或者多条语句来完成其它关联对象的读取。当使用find方法时，无法改变这一默认行为。而在使用JPQL时，是能够使用JOIN语句的。

使用JPQL时，并不能指定需要选择一个对象的哪些域，比如下面的查询：

```java
Query q = em.createQuery("SELECT s FROM StockPriceImpl s");
```

生成的SQL是这样的：

```sql
SELECT <enumerated list of non-LAZY fields> FROM StockPriceTable
```

这也意味着当你不需要某些域时，只能将它们声明为懒加载的域。

使用JPQL的JOIN语句能够通过一条SQL来得到一个对象和它的关联对象：

```java
Query q = em.createQuery("SELECT s FROM StockOptionImpl s " + "JOIN FETCH s.optionsPrices");
```

以上的JPQL会生成如下的SQL：

```sql
SELECT t1.<fields>, t0.<fields> FROM StockOptionPrice t0, StockPrice t1 WHERE ((t0.SYMBOL = t1.SYMBOL) AND (t0.PRICEDATE = t1.PRICEDATE))
```

JOIN FETCH和域是懒加载还是立即加载没有直接的关系。当JOIN FETCH了懒加载的域，那么这些域也会读取，然后在程序需要使用这些懒加载的域时，不会再去从数据库中读取。

当使用JOIN FETCH得到的所有数据都会被程序所使用时，它就能帮助提高程序的性能。因为它减少了SQL的执行次数和数据库的访问次数，这通常是一个使用了数据库的应用的瓶颈所在。

但是JOIN FETCH和JPA缓存的关系会有些微妙，在后面介绍JPA缓存时会讲述。

---

**JOIN FETCH的其它实现方式**

除了直接在JPQL中使用JOIN FETCH，还可以通过设置提示来实现。这种方式在很多JPA实现中被支持，比如：

```java
Query q = em.createQuery("SELECT s FROM StockOptionImpl s");
q.setQueryHint("eclipselink.join-fetch", "s.optionsPrices");
```

在有些JPA实现中，还提供了一个@JoinFetch注解来提供JOIN FETCH的功能。

---

**获取组(Fetch Group)**

当一个实体对象有多个懒加载的域，那么在它们同时被需要时，JPA通常会为每个别需要的域生成并执行一条SQL语句。显而易见的是，在这种场景下，生成并执行一条SQL语句会更好。

然而，JPA标准中并没有定义这种行为。但是大多数JPA实现都定义了一个获取组来完成这一行为。即将多个懒加载域定义成一个获取组，每次加载它们中的任意一个时，整个组都会被加载。所以，当需要这种行为时，可以参考具体JPA实现的文档。

---

**批量处理和查询(Batching and Queries)**

JPA也能像JDBC处理ResultSet那样处理查询的结果：

- 一次性返回所有结果集中的所有记录
- 每次获取结果集中的一条记录
- 一次获取结果集中的N条记录(和JDBC的Fetch Size类似)

同样，这个Fetch Size也是和具体的JPA实现相关的，比如在EclipseLink和Hibernate中按如下的方式进行设置：

```java
// EclipseLink
q.setHint("eclipselink.JDBC_FETCH_SIZE", "100000");

// Hibernate
@BatchSize
// Query here...
```

同时，可以对Query设置分页相关的设置：

```java
Query q = em.createNamedQuery("selectAll");
query.setFirstResult(101);
query.setMaxResults(100);
List<? implements StockPrice> = q.getResultList();
```

这样就能够仅仅获取第101条到第200条这个区间的数据了。

同时，以上使用了命名查询(Named Query，createNamedQuery())而不是临时查询(Ad-hoc Query，createQuery())，在很多JPA实现中命名查询的速度要更快，因为一个命名查询会对应Statement Cache Pool中的一个PreparedStatement，剩下需要做的就只是给该对象绑定参数。虽然对于临时查询，也可以使用同样的实现方式，只不过此时的JPQL只有在运行时才能够知晓，所以实现起来比较困难，在很多JPA实现中会为临时查询新建一个Statement对象。

#### 总结 ####

1. JPA有一些优化选项能够限制(增加)单次数据库访问的读取数据量。
2. 对于BLOB和CLOB类型的字段，将它们的加载方式设置为懒加载。
3. JPA实体的关联实体可以被设置为懒加载或者立即加载，选择取决于应用的具体需求。
4. 当需要立即加载实体的关联实体时，可以结合命名查询和JOIN语句。注意它对于JPA缓存的影响。 
5. 使用命名查询比临时查询更快。


## JPA缓存(JPA Caching) ##

JPA有两种类型的缓存：

- EntityManager自身就是一种缓存。事务中从数据库获取的和写入到数据库的数据会被缓存(什么样的数据会被缓存，在后面有介绍)。在一个程序中也许会有很多个不同的EntityManager实例，每一个实例运行着不同的事务，拥有着它们自己的缓存。

- 当EntityManager提交一个事务后，它缓存的所有数据就会被合并到一个全局的缓存中。所有的EntityManager都能够访问这个全局的缓存。


全局缓存被称为二级缓存(Level 2 Cache)，而EntityManager拥有的本地缓存被称为一级缓存(Level 1 Cache)。所有的JPA实现都拥有一级缓存，并且对它没有什么可以调优的。而二级缓存就不同了：大多数JPA实现都提供了二级缓存，但是有些并没有把启用它作为默认选项，比如Hibernate。一旦启用了二级缓存，它的设置会对性能产生较大的影响。

只有当使用**实体的主键**进行访问时，JPA的缓存才会工作。这意味着，下面的两种获取方式会将获取的结果放入到JPA的缓存中：

- 调用find()方法，因为它需要接受实体类的主键作为参数
- 调用实体类型的getter方法来得到关联的实体类型，本质上，获取关联的实体对象也是通过关联对象的主键得到，因为在数据库的表结构中，存放的是该关联对象的外键信息。

那么当EntityManager需要通过主键或者关联关系获取一个实体对象时，它首先会去二级缓存中寻找。如果找到了，那么它就不需要对数据库进行访问了。

通过查询(JPQL)方式得到的实体对象是不会被放到二级缓存中的。


























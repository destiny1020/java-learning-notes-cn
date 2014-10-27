# 初识Hibernate Search #

要让你的应用具备Hibernate Search赋予的全文搜索的能力，需要做以下三件事：

1. 给项目添加必要的依赖和配置信息
2. 给你的实体类添加必要的信息，从而让Lucene知道如何对它们进行索引(Indexing)
3. 在需要的地方使用符合Hibernate Search规范的查询来完成业务逻辑

对于需要添加的依赖信息，会在以后进行介绍。我们首先来看看代码该如何写。
我们会使用一个类似于经典的“Java Pet Store”那样的Web应用来展示Hibernate Search的使用方式，这个应用的主要功能是展示一个软件应用的目录。

## 创建实体类 ##

既然目的是为了展示软件应用，那么首当其冲的实体类就是App类了，现在我们将它定义成下面这样，拥有4个字段：

- id：主键信息
- name：App的名字
- description：App的介绍
- image：App图片的链接

```java
@Entity
public class App {
	@Id
	@GeneratedValue
	private Long id;

	@Column
	private String name;

	@Column(length=1000)
	private String description;

	@Column
	private String image;

	public App() {}
	public App(String name, String image, String description) {
		this.name = name;
		this.image = image;
		this.description = description;
	}
	// 省略了众多的getter和setter
}
```

## 为使用Hibernate Search而修改实体类 ##

有了实体类型后，我们需要告诉Hibernate Search如何来利用Lucene对该实体进行管理。

在最基本的场景中，我们只需要向该实体类型添加两个注解：

首先是添加@Indexed注解：

```java
import org.hibernate.search.annotations.Indexed;

@Entity
@Indexed
public class App implements Serializable
// ...
```

这个注解告诉Lucene去为App实体类型创建索引。注意，并不是每个实体类型都需要这个注解，只有确定将会作为搜索目标的实体类才需要使用它。

其次，需要向具体的字段添加@Field注解：

```java
import org.hibernate.search.annotations.Field;

// ...

@Id
@GeneratedValue
private Long id;

@Column
@Field
private String name;

@Column(length=1000)
@Field
private String description;

@Column
private String image;

// ...
```

这里我们向name和description字段添加了@Field注解，表示这两个字段将会作为搜索的目标字段。同时注意到image字段并没有被@Field标注，这是因为我们不需要将图片的名字也作为可搜索的字段。

## 建立查询 ##

向实体类添加了必要的注解后，我们就可以对它们建立查询了。主要会使用到FullTextSession和QueryBuilder类型：

```java
import org.hibernate.Session;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;

// ...

Session session = StartupDataLoader.openSession();
FullTextSession fullTextSession = Search.getFullTextSession(session);
fullTextSession.beginTransaction();

// ...
```

首先建立一个Session并开始一个事务。紧接着，就需要通过传入的关键字(Keyword)来建立查询了：

```java
import org.hibernate.search.query.dsl.QueryBuilder;

// ...

String searchString = request.getParameter("searchString");
QueryBuilder queryBuilder = fullTextSession.getSearchFactory()
	.buildQueryBuilder().forEntity( App.class ).get();
org.apache.lucene.search.Query luceneQuery = queryBuilder
	.keyword()
	.onFields("name", "description")
	.matching(searchString)
	.createQuery();
```

非常直观的代码，forEntity用来指定对哪个实体进行查询，onFields用来指定对哪些字段进行查询。
将上面的代码翻译成更加容易理解的语言是这样的：

**为App实体的name和description字段创建一个匹配searchString参数的基于关键字的查询。**

这因为这种API的设计十分流畅，它也被称为Hibernate Search DSL(Domain-Specific Language)。
另外，注意到以上的queryBuilder对象创建的查询类型是org.apache.lucene.search.Query。这就是Hibernate Search和Lucene建立联系的一种方式。在Lucene得到搜索结果后，类似地也会将结果转换成一个org.hibernate.Query对象：

```java
org.hibernate.Query hibernateQuery = fullTextSession.createFullTextQuery(luceneQuery, App.class);
List<App> apps = hibernateQuery.list();
request.setAttribute("apps", apps);
```

因此，Hibernate Search封装了大量的Lucene使用细节，让只了解Hibernate的开发人员也能够轻松的为应用加上全文搜索功能。

## 建立工程并导入Hibernate Search ##

这里我们考虑使用maven时需要添加的依赖，最关键的就是：

```
<dependency>
	<groupId>org.hibernate</groupId>
	<artifactId>hibernate-search</artifactId>
	<version>4.2.0.Final</version>
</dependency>
```

对于测试环境，往往还可以利用内存数据库h2：

```
<dependency>
	<groupId>com.h2database</groupId>
	<artifactId>h2</artifactId>
	<version>1.3.168</version>
</dependency>
<dependency>
	<groupId>commons-dbcp</groupId>
	<artifactId>commons-dbcp</artifactId>
	<version>1.4</version>
</dependency>
```

当然，具体的版本号会根据你的需求而有所不同。









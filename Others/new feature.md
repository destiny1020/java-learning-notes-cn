# 12.2 更新 #

## 1. 信息传入后台 ##

首先页面上有一个入口，点击之后将如下信息传送给后台：

n：演员名字
l：crawler link
f：过滤flag
p：页码，从演员入口进入的页码为1，即第一页

关于过滤flag的几个选项：
0 - 原样显示
1 - 只显示没有的，即待筛选的
2 - 只显示已经存在的

## 2. 后台处理 ##

首先根据crawler link拼接完整的url，然后使用已有的API读取该url的所有bricks。
后台返回一个wrapper对象，其中含有的信息如下：

- crawler link
- 演员名字
- bricks
	每个brick应该含有的信息包括：

	- original link
	- title
	- movie number
	- cover ps link
	- movieclip list

- 页码
- 原始页面的bricks记录数
- 过滤后的bricks记录数
- 过滤模式flag，即上述flag是0,1还是2。可以考虑使用一个enum对它进行描述

处理的详细流程：

1. 使用现有API得到的是一个ElementsWrapper
2. 处理其中的每个Element，得到相应的brick对象
3. 设置页码，原始页面的bricks数，过滤模式
4. 根据过滤模式，进行过滤。所谓过滤实际上是赋值给一个show字段true或者false
	1. 如果是0，则过滤本身就是一个noop
	2. 如果是1，那么根据movie number对数据库进行查询，没有的话赋值true
	3. 如果是2，那么根据movie number对数据库进行查询，没有的话赋值false
5. 读取movieclip表，如果存在记录，将记录存放到movieclip list中

情况2和3过滤部分的逻辑可以封装成lambda。

当请求页码不存在时，返回一个bricks为空的wrapper

## 3. 前台显示 ##

参考相关页面的style进行显示。还是使用的deckgrid。

当bricks为空时，显示一个没有记录的message，提示用户当前页面没有记录了
此时底部只显示上一页的链接

左侧空间显示：
1. 演员avatar以及name
2. total clip数量以及total volume 
3. Folder链接

右侧空间显示：
顶部显示当前使用的过滤模式
显示bricks。对于show属性为false的brick，添加一个overlay用来遮罩。
底部显示当前页码，以及上一页和下一页的链接。

对于每个brick：

1. 点击cover ps或者title之后都会以_blank的方式打开原始页面
2. 播放按钮，可能有多个。每个按钮的显示：播放icon + actual name

点击底部的页码链接的行为：

参考第一步：信息传入后台

-----
# 12.3 更新 #

## 演员表中的新增加字段： ##

1. explorer页面的上次浏览时间，没有浏览过则为null。记为last_explore_time
2. explorer是否已全部探索完，默认值为false。记为explore_done

这两个字段属于一个新的类型中，演员Explorer类型。该类型和演员之间的关系是OneToOne。

需要修改演员的JsonSerializer实现以及ES的重索引。

相关信息也需要反映在Table中：
浏览时间直接放在Explorer按钮的后面，使用moment
全部探索完的explorer且上次探索时间距离现在小于1个月的，按钮的颜色为 **绿色**
全部探索完的explorer且上次探索时间距离现在大于等于1个月的，按钮的颜色为 **黄色**
未探索过的为 **红色**

关于探索完成/未完成的标记，放在explorer view的头部，放一个button。用来更新explore_done的状态：
完成时显示绿色的button，text为Finished，Toggle to Unfinished
未完成时显示红色的button，text为Unfinished，Toggle to Finished

## explorer view强化 ##

explorer view左侧信息栏添加更多的信息。比如Table中的可用信息都可以copy到这里一份。

explorer view的body部分，为每个brick添加直接播放的按钮。有些brick可能对应了多个播放路径：
单个路径使用单一按钮，多个路径使用btn-group实现，颜色为primary系，text是影片名字。

ps cover点击放大的功能：
考虑使用fancybox？参考类似网站。
[angular-bootstrap-lightbox](https://github.com/compact/angular-bootstrap-lightbox)

## Dashboard相关功能： ##

1. 总的影片数量和总体volume

调研 D3和Angular的结合：
[angular-nvd3](https://github.com/krispo/angular-nvd3)

以及：
[angularjs-nvd3-directives](https://github.com/cmaurer/angularjs-nvd3-directives)

-----
# 12.4 更新 #

添加loading bar：[angular-loading-bar](https://github.com/chieffancypants/angular-loading-bar)

一些可能需要的额外filters：[angular filter](https://github.com/a8m/angular-filter)

drag-drop支持：[angular-dragdrop](https://github.com/codef0rmer/angular-dragdrop)

播放计数器：
每次点击播放之后，计数器加1。需要新字段：play_count

影片列表页面，和演员页面一样，需要支持List模式和Grid模式。
List模式中需要显示Cover PS，在Grid中也需要。点击之后使用lightbox显示LS。

影片的rating以及演员的rating，使用angular bootstrap的rating控件。
对于演员的rating字段，放在其EXPLORER_xxx表中，对于影片，直接放在对应表中即可。

增加一点随机性，问题是如何随机呢？
随机性相关功能应该放在一个单独的random view中，因为将来会有更多的相关功能。

random view中包含的几个tabs：
- 影片random
- 演员random
- To be added...

Movie crawler到底要如何进行？
主要问题是要避免crawl一些无关的信息，目前这类信息很多。

Highlight view的建立：
1. 创建记录要方便！
2. 影片拥有的highlight记录需要在movie view中直接能够访问到。

自定义标签的定义：
标签的颜色如何方便的定义？标签如何快速的被建立？
http://decipherinc.github.io/angular-tags/

预先定义的tags需要定义一个group属性，这个属性可以被映射成一个class name，然后就可以对其样式进行控制。如果要新添加tag，还是需要在一个专门的dialog中进行，因为快速添加这种模式无法设置group属性。

-----
# 12.8 更新 #

## **添加explore by sequence的功能：** ##

这个功能的入口在为Sequence，位于一级菜单中。
首先需要向Sequence表中添加一些数据！

**页面的设计：**
左边是一个固定的用于设置过滤条件的区域，具体的过滤条件有：
1. Sequence名字。
2. 状态，四种可用状态。

右边是列表/网格区域。
有一个用于列表和网格状态的radio切换。

对于列表，可用的列：
- name
- pl cover
- 状态列，可以直接进行状态切换
- 可用操作列 

同时还需要有screenshot的支持？

**一个状态字段，状态包括以下几种：**
1. 就绪。可用操作：绿色的播放按钮。
2. 需要。可用操作：蓝色的打开链接的按钮；更改为不需要；更改为不定。
3. 不需要。可用操作：更改为需要，更改为不定。
4. 不定。可用操作：更改为需要，更改为不需要。

**后三种状态可以相互切换，切换的内在逻辑：**

- 不定 -> 需要：表中建立记录，字段：name和needed等于true
- 不定 -> 不需要：表中建立记录，字段：name和needed等于false
- 不需要 -> 需要：得到表中对应记录，修改字段needed为true
- 不需要 -> 不定：删除表中对应记录
- 需要 -> 不需要：得到表中对应记录，修改字段needed为false
- 需要 -> 不定：删除表中对应记录

关于不定状态和就绪状态的识别，因为这两种状态下，表中均无记录。
**首先，所有的记录都是不定状态的，然后逐条操作如下：**
1. 查找影片表，若查到记录，则将状态修改为就绪。继续check下一条记录
2. 查找EXPLORE_SEQUENCE_MOVIE表，如查到记录，根据needed字段设置状态为需要/不需要

**具体的UI元素：**
已经就绪的话，直接显示一个label用来显示"就绪"。然后是播放按钮，颜色绿。
其余3种状态，使用一个select控件让它们互相切换。
在需要的状态下，显示一个primary颜色的按钮，用于打开链接。

**对于这个新增表，需要更新的场合：**

dispatch之后，遍历所有的影片，针对每条记录，执行下述逻辑：

1. 查找EXPLORE_SEQUENCE_MOVIE，如果存在记录，删除之。

TO ADD MORE...

## **添加本地File Server的功能** ##

确保目前存在的所有sequence都在本地server中存在。

首先确认所有的sequence都有对应的folder。

folder的层次：
```
sequence/
	pl covers in the top level
	ps/
	screenshots/
```














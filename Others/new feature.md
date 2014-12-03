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








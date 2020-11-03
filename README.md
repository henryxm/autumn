# Autumn

#### 项目介绍
- 采用Spring、Spring Boot、Redis、MyBatis、Shiro、Druid框架开发,搭载mysql数据。
- 如果你厌烦了MyBatis中需要手动创建表的事情，这个项目非常适合你，自动为你生成表。
- 从此你不在需要导入sql文件了，项目初始化变得异常简单，结构清晰，易于开发，真正拿来可用。
- 全注解MyBatis开发，没有任何mapper文件，一切sql 映射都用代码实现，全程无xml配置，对xml编写mapper有恐惧症的人的福音。

- 提供双向生成功能：
  1. 实体类自动生成数据库表，全过程不需要任何SQL语句，所有表自动生成
  2. 通过表生成基础结构代码，生成代码中已包含CRUD功能，表级别的API接口全部都有
  3. 基本实例数据全自动通过代码初始化，无需干预
  4. 仅仅只需要修改数据库连接地址，用户名，密码，即可启动运行。

#### 具有如下特点
- 灵活的权限控制，可控制到页面或按钮，满足绝大部分的权限需求
- 完善的部门管理及数据权限，通过注解实现数据权限的控制
- 完善的XSS防范及脚本过滤，彻底杜绝XSS攻击
- 支持分布式部署，session存储在redis中
- 友好的代码结构及注释，便于阅读及二次开发
- 引入quartz定时任务，可动态完成任务的添加、修改、删除、暂停、恢复及日志查看等功能
- 页面交互使用Vue2.x，极大的提高了开发效率
- 引入swagger文档支持，方便编写API接口文档


#### 数据权限设计思想
- 管理员管理、角色管理、部门管理，可操作本部门及子部门数据
- 菜单管理、定时任务、参数管理、字典管理、系统日志，没有数据权限
- 业务功能，按照用户数据权限，查询、操作数据【没有本部门数据权限，也能查询本人数据】


#### 项目结构
```
Autumn
├─autumn-lib     公共模块
│ 
├─autumn-modules      系统核心模块
│    │ 
│    ├─modules  模块
│    │    ├─gen 代码生成
│    │    ├─job 定时任务
│    │    ├─lan 多语言
│    │    ├─oss 文件存储
│    │    ├─spm 超级位置模型
│    │    ├─sys 系统管理(核心)
│    │    ├─user 普通用户
│    │    └─wall 防火墙
│    │ 
│    └─resources 
│        ├─statics 静态文件
│        ├─template 代码生成模板文件
│        └─templates 生成的模块代码
│
├─autumn-web    系统启动入口
│        ├─statics  静态资源
│        ├─templates 系统页面
│        │    ├─modules      模块页面
│        │    ├─index.html   AdminLTE主题风格（默认主题）
│        │    └─index1.html  Layui主题风格
│        └─application.yml   全局配置文件
│
├─autumn-demo    生成的代码
```

#### 技术选型
- 核心框架：Spring Boot 2.0
- 安全框架：Apache Shiro 1.4
- 视图框架：Spring MVC 5.0
- 持久层框架：MyBatis 3.3
- 定时器：Quartz 2.3
- 数据库连接池：Druid 1.1
- 日志管理：SLF4J 1.7、Log4j
- 页面交互：Vue2.x

#### 超级位置模型
- 增加对超级位置模型的支持，方便统计网站点击埋点的统计

#### 防火墙
- 增加IP防火墙
- 增加URL防火墙
- 增加主机访问统计
- 增加IP,URL启动禁用功能

#### 定时任务
- 增加对定时任务代码级支持

#### 软件需求
- JDK1.8
- MySQL5.5+
- Maven3.0+

#### 本地部署
- 通过git下载源码
- 创建数据库autumn，用户名：autumn， 密码：autumn，数据库编码为UTF-8
- 修改application-dev.yml文件，更新MySQL账号和密码
- 在autumn-web目录下，执行mvn clean install

- Eclipse、IDEA运行AdminApplication.java，则可启动项目【autumn-web】
- admin访问路径：http://localhost/autumn
- swagger文档路径：http://localhost/autumn/swagger/index.html
- 账号密码：admin/admin

#### 分布式部署
- 分布式部署，需要安装redis，并配置config.properties里的redis信息
- 需要配置【autumn.redis.open=true】，表示开启redis缓存
- 需要配置【autumn.shiro.redis=true】，表示把shiro session存到redis里

#### 如何交流、反馈、参与贡献？
- github仓库：https://github.com/henryxm/autumn

# Redis缓存管理模块

## 功能特性

- ✅ Redis服务器状态监控
- ✅ 数据库信息查看
- ✅ 键列表浏览和搜索
- ✅ 键值查看
- ✅ 单个键删除
- ✅ 数据库清空
- ✅ 所有数据库清空
- ✅ 分页显示
- ✅ 实时搜索

## 技术栈

- **后端**: Spring Boot + Spring Data Redis
- **前端**: Vue.js + Bootstrap + jQuery
- **模板引擎**: Freemarker

## 安装和配置

### 1. 依赖配置

确保项目中已包含以下依赖：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.15.6</version>
</dependency>
```

### 2. Redis配置

在 `application.yml` 中配置Redis连接：

```yaml
spring:
  redis:
    database: 0
    host: localhost
    port: 6379
    password: autumn
    timeout: 6000ms
    jedis:
      pool:
        max-active: 1000
        max-wait: -1ms
        max-idle: 10
        min-idle: 5

autumn:
  redis:
    open: true  # 开启Redis缓存
```

### 3. 启用Redis

将 `autumn.redis.open` 设置为 `true` 以启用Redis功能。

## 使用方法

### 1. 访问页面

启动应用后，访问：`http://localhost/redis`

### 2. 功能说明

#### Redis状态监控
- 显示Redis服务器版本、运行时间、连接数、内存使用情况
- 实时监控连接状态

#### 数据库管理
- 显示当前数据库的键数量和大小
- 支持清空当前数据库
- 支持清空所有数据库

#### 键管理
- 浏览所有键，支持分页显示
- 搜索键（支持通配符模式）
- 查看键的详细信息（类型、大小、过期时间）
- 查看键值内容
- 删除单个键

## 文件结构

```
autumn-modules/
├── src/main/java/cn/org/autumn/modules/sys/
│   ├── controller/
│   │   ├── RedisController.java          # Redis REST API控制器
│   │   └── SysPageController.java        # 页面路由控制器
│   └── service/
│       └── RedisService.java             # Redis业务逻辑服务
├── src/main/resources/
│   ├── statics/
│   │   └── redis.html                    # Redis管理页面（静态资源）
│   └── templates/
│       └── redis.html                    # Redis管理页面（模板）
└── src/test/java/cn/org/autumn/modules/
    └── RedisServiceTest.java             # 测试类
```

## API接口

### 1. 获取Redis状态
```
GET /sys/redis/status
```

### 2. 获取数据库列表
```
GET /sys/redis/databases
```

### 3. 获取键列表
```
GET /sys/redis/keys/{database}?pattern={pattern}&page={page}&size={size}
```

### 4. 获取键值
```
GET /sys/redis/value/{key}
```

### 5. 删除键
```
DELETE /sys/redis/key/{key}
```

### 6. 清空数据库
```
DELETE /sys/redis/database/{database}
```

### 7. 清空所有数据库
```
DELETE /sys/redis/all
```

## 问题解决

### UnsupportedOperationException 异常

**问题描述**: 在Redis数据库切换时出现 `UnsupportedOperationException` 异常。

**原因分析**: 
- 项目使用了Jedis连接池配置
- 连接池中的连接是共享的，不支持 `select` 命令
- `select` 命令会改变连接的数据库上下文，影响其他线程

**解决方案**:
1. 移除了数据库切换功能
2. 只操作当前配置的数据库（database: 0）
3. 在服务器信息中添加了说明提示

**修改内容**:
- `RedisService.java`: 移除了 `selectDatabase()` 方法和相关逻辑
- `redis.html`: 简化了界面，移除了数据库选择功能
- 所有操作都在当前数据库上进行

**配置建议**:
如果需要使用多个Redis数据库，建议：
1. 为每个数据库配置独立的Redis连接
2. 或者使用不同的Redis实例
3. 或者通过键名前缀来区分不同的业务数据

## 测试

运行测试类验证功能：

```bash
mvn test -Dtest=RedisServiceTest
```

## 注意事项

1. **安全性**: 生产环境中请确保Redis管理页面有适当的访问控制
2. **性能**: 大量键的情况下，搜索和分页操作可能较慢
3. **连接池**: 当前实现基于连接池，不支持数据库切换
4. **权限**: 确保应用有足够的Redis操作权限

## 扩展功能

可以考虑添加的功能：
- 键值编辑功能
- 批量操作功能
- 键过期时间设置
- 数据导入导出
- 性能监控图表
- 操作日志记录

## 版本历史

- v1.0.0: 基础功能实现
- v1.1.0: 修复UnsupportedOperationException问题
- v1.2.0: 优化界面和用户体验 
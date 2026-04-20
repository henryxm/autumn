# Redisson 与 Spring Data Redis 版本对齐（StackOverflowError / `pExpire`）

## 1. 文档定位

当工程同时使用 **Spring Data Redis** 与 **Redisson**（常见于 `redisson-spring-boot-starter`）时，若 **`redisson-spring-data-XX` 桥接模块**与当前 classpath 上的 **`spring-data-redis` 主版本**不一致，可能在运行期出现 **`java.lang.StackOverflowError`**，栈顶反复出现：

`org.springframework.data.redis.connection.DefaultedRedisConnection.pExpire(...)`

本文说明**原理**、**Maven 必选修复**、以及 **`cn.org.autumn.utils.RedisExpireUtil`（autumn-lib）编码侧纵深防御**。其它业务仓库遇到同类问题时，应按**同一顺序**处理。

---

## 2. 典型现象与触发路径

- **日志**：同一方法帧在 `DefaultedRedisConnection.pExpire`（或邻近 `expire`）无限重复。
- **触发操作**：任意经 Spring Data Redis 模板触发的 TTL 语义，例如：
  - `RedisTemplate.expire(...)`
  - `ValueOperations.set(key, value, timeout, unit)`（内部往往落到 **`pExpire`**）
  - 其它最终调用 **`RedisConnection`** 上 **`pExpire`/`expire` 默认实现**的路径

---

## 3. 原理（为何会栈溢出）

Spring Data Redis 为兼容旧 API，在 **`DefaultedRedisConnection`** 上对过期等操作提供了 **default 方法**，典型实现为委托给 **`keyCommands()`** 返回的命令接口。

Redisson 提供的 **`RedisConnection`** 实现若与当前 **Spring Data Redis 大版本**不匹配，`keyCommands()` / 兼容层之间可能出现 **互相委托**：逻辑上再次进入带 `DefaultedRedisConnection` 的连接视图，最终在 **`pExpire`** 上形成 **无限递归**，表现为 **`StackOverflowError`**。

因此这是 **集成模块版本（`redisson-spring-data-XX`）与 `spring-data-redis` 不一致** 导致的结构性问题，而不是业务某一行的算法错误。

---

## 4. 必选修复：Maven / BOM 对齐（治本）

**原则**：`redisson-spring-data-XX` 中的 **XX 必须对应 Spring Data Redis 的主次版本线**，与当前 Spring Boot 管理的 **`spring-data-redis`** 一致。

### 4.1 如何确认当前 SDR 版本

在业务工程执行：

```bash
mvn dependency:tree -pl <入口模块> | grep spring-data-redis
```

记下 **`org.springframework.data:spring-data-redis`** 的版本（例如 **3.5.8**）。

### 4.2 如何确认 Redisson 桥接模块

```bash
mvn dependency:tree -pl <入口模块> | grep redisson
```

检查是否出现 **`redisson-spring-data-34`**、**`redisson-spring-data-35`** 等。  
若 Boot **3.5.x** 已引入 **SDR 3.5.x**，而树上仍是 **`redisson-spring-data-34`**（面向 **SDR 3.4**），即 **错配**。

### 4.3 推荐改法（业务根 POM）

在 **`dependencyManagement`** 中**覆盖**传递依赖（版本号随 Redisson 发行说明与 Boot 线选择，示例仅说明结构）：

```xml
<properties>
    <redisson.version>3.52.0</redisson.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson-spring-boot-starter</artifactId>
            <version>${redisson.version}</version>
        </dependency>
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson</artifactId>
            <version>${redisson.version}</version>
        </dependency>
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson-spring-data-35</artifactId>
            <version>${redisson.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

具体 **`<redisson.version>`** 与 **`redisson-spring-data-XX`  artifact 名**请以：

- 当前 **Spring Boot** 版本对应的 **Spring Data Redis** 版本
- [Redisson 官方「Integration with Spring」](https://github.com/redisson/redisson/wiki/14.-Integration-with-Spring#spring-data-redis-session) 中的 **模块对照表**

为准（例如 **SDR 3.5.x → `redisson-spring-data-35`**）。

对齐后，`dependency:tree` 中 **不应再出现** 与当前 SDR 主线冲突的旧 **`redisson-spring-data-34`**（在 SDR 3.5 场景下）。

---

## 5. 纵深防御：`RedisExpireUtil`（autumn-lib）

即使依赖已对齐，所有 **TTL / 过期** 语义仍应统一走 **`cn.org.autumn.utils.RedisExpireUtil`**（**Lua** 或 **`INCR` + 本类续期**），避免在问题环境下经过 Java 侧 **`expire` / `pExpire`** 默认委托链。

完整 **常见写法 ↔ API 映射**、**滑动窗口计数**、**NX 占位**、**PEXPIREAT** 等见 **`docs/REDIS_TTL_GUIDE.md` §3**。

**推荐流程**（与 **`docs/REDIS_TTL_GUIDE.md`** 一致）：先 **Maven 对齐**，再按需将 TTL 语义收敛到 **`RedisExpireUtil`**；可用 **`scripts/constraints-scan --redis-expire-only`**（全文体检含 **H 组**）辅助检索。

---

## 6. 其它项目处理同一问题的推荐顺序

1. **`mvn dependency:tree`**：确认 **`spring-data-redis`** 与 **`redisson-spring-data-XX`**。
2. **根 POM `dependencyManagement`**：按 Redisson 对照表对齐 **`redisson-spring-boot-starter`** 与同版本的 **`redisson-spring-data-XX`**（必要时显式声明 **`redisson`**）。
3. **复核 `dependency:tree`**：无冲突的旧桥接模块。
4. **代码**：若使用 Autumn，可按 **`docs/REDIS_TTL_GUIDE.md`** 用 **`RedisExpireUtil`** 表达 TTL；非 Autumn 工程亦可复制 Lua 思路或引入 **`autumn-lib`**。

---

## 7. 相关文档

- **`docs/REDIS_TTL_GUIDE.md`**：**Redis TTL**、何时用 **`RedisExpireUtil`**、API 对照、推荐顺序与可选扫描脚本
- **`docs/REDIS_STANDALONE.md`**：Redis 可选装配与框架 `RedisConfig`
- **`docs/REDIS_RESILIENCE.md`**：熔断与锁（与本文正交互补：本文解决 **集成版本**，该文档解决 **运行时故障降级**）

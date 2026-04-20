# Redis TTL、`Redisson` 集成与 `RedisExpireUtil` 使用说明（Autumn）

本页面向**日常业务开发**：说明 **`RedisExpireUtil`** 要解决什么问题、**在什么情况下值得你改用**它，以及如何把 **Maven 依赖对齐**（治本）和 **Lua 封装调用**（常见场景下的稳妥写法）结合起来。

**文档定位**：本文 **`docs/REDIS_TTL_GUIDE.md`** 为正式文件名。旧链接 **`docs/REDIS_EXPIRE_MANDATORY.md`** 仅保留一页重定向，避免历史引用失效。

---

## 1. 你可能遇到什么问题？

### 1.1 典型异常（严重）

当工程里同时存在 **Spring Data Redis** 与 **Redisson**（例如 `redisson-spring-boot-starter`），且 **`redisson-spring-data-XX` 桥接模块版本**与当前 **`spring-data-redis` 主版本线**不一致时，有可能在运行期出现：

- **`java.lang.StackOverflowError`**
- 栈顶反复出现 **`DefaultedRedisConnection`**、**`pExpire`** 一类帧

这不是业务「算错 TTL」导致的，而是 **集成层委托链**在特定版本组合下形成递归。原理说明见 **`docs/REDIS_REDISSON_SPRING_DATA.md`**。

### 1.2 不用 `RedisExpireUtil` 时，你还可能遇到什么？

即使**没有**栈溢出，在以下情况仍可能有负担：

| 情况 | 可能的影响 |
|------|------------|
| 依赖长期未对齐 | 线上某次升级 Spring Boot / Redisson 后又踩坑，排查成本高 |
| 各处手写 `expire` / `set(k,v,ttl)` | 风格分散；有人改用边界写法后引入新的 Redis 调用路径 |
| 限流「INCR + 首次 EXPIRE」 | 若 `expire` 走问题路径，仍可能触发同一类故障 |

因此：**先把 Redisson 与 SDR 对齐**是最重要的；**`RedisExpireUtil` 是帮你把「带 TTL 的语义」收敛成少数几条 Lua 路径**，降低将来升级与排查的心智负担。

---

## 2. 什么时候建议用 `RedisExpireUtil`？什么时候可以不必？

### 2.1 建议优先使用的场景（收益明显）

- 工程里 **确实用了 Redisson**（或传递依赖里带有 **`redisson-spring-boot-starter`**）。
- 你需要：**设置过期**、**SET 并带 TTL**、**NX+TTL**、**绝对时间过期**、**滑动窗口 INCR 首次续期** 等任何「带时效」的写入。
- 你们正在 **排查 / 预防** 与 **`pExpire` / `expire`** 相关的栈溢出或线上 Redis 异常。

### 2.2 通常不必强行替换的情况

- **仅纯读**（`GET`、无 TTL 的 `INCR` 且不续期）——与过期无关。
- 你已确认 **Redisson 桥接模块与 `spring-data-redis` 版本严格匹配**，且团队约定继续用 Spring 默认 API，并能在升级后回归——这是团队自主决策；Autumn 仍**推荐**在「写 TTL」路径统一走 `RedisExpireUtil`，以便跨项目一致、减少重复 Lua。

**结论**：不是「不用就会出错」，而是 **在 Redisson 栈上，这是 Autumn 提供的、与文档配套的稳妥默认写法**。

---

## 3. 推荐处理顺序（从治本到写法）

### 阶段 A — Maven：对齐 `redisson-spring-data-XX`（强烈推荐）

1. 在业务入口模块执行：
   ```bash
   mvn dependency:tree -pl <模块名> | grep -E 'spring-data-redis|redisson'
   ```
2. 对照 Redisson 官方说明，确认 **`redisson-spring-data-XX`** 与 **`spring-data-redis`** 主版本线一致（例：Boot 3.5 常见为 SDR 3.5.x → **`redisson-spring-data-35`**）。
3. 在根 **`dependencyManagement`** 管理 **`redisson-spring-boot-starter`** / **`redisson`** / 对应 **`redisson-spring-data-XX`**。

详见 **`docs/REDIS_REDISSON_SPRING_DATA.md`**。

### 阶段 B — 代码：用 `RedisExpireUtil` 表达 TTL 语义（推荐）

下列 Spring 写法在 **Redisson 与 SDR 错配**时更容易踩到集成层问题；若你希望与 Autumn 文档及兄弟项目保持一致，可按下表替换：

| 常见写法 | `RedisExpireUtil` 对应方法 |
|----------|----------------------------|
| `redisTemplate.expire(key, duration, unit)` | **`expire(...)`** |
| 需要毫秒级对已有键续期 | **`pExpire(...)`** |
| `opsForValue().set(k, v, duration, unit)` | **`setWithExpire`** / **`setWithExpirePx`** |
| `SET NX` 且带 TTL | **`setIfAbsentWithExpire`** / **`setIfAbsentWithExpirePx`** |
| 指定 Unix 时刻失效 | **`expireAtSeconds`** / **`pExpireAtMillis`** |
| 限流：`INCR` 且首次设窗口 | **`incrementAndExpireIfFirst`** / **`incrementAndPExpireIfFirst`** |

完整 API 列表见 **§4**。实现上多为 **Lua**（在 Redis 服务端一次完成语义），或在 **`INCR` 后调用本类的 `expire`/`pExpire`**。

### 阶段 C — 静态检索（可选但实用）

在业务仓库执行（路径按本机 autumn 克隆位置调整）：

```bash
bash /path/to/autumn/scripts/constraints-scan --redis-expire-only .
```

用于**快速找出**仍使用 `redisTemplate.expire`、`opsForValue().set(..., TimeUnit)` 等写法的文件，便于评审或重构时对照上表替换。

### 阶段 D — 回归（发布前）

- 编译 / 测试通过；若变更了 Redisson 版本，务必再看一眼 **`dependency:tree`**。

---

## 4. `RedisExpireUtil` API 速查

| 方法 | Redis 语义 |
|------|------------|
| **`expire`** | `EXPIRE`，秒 |
| **`pExpire`** | `PEXPIRE`，毫秒 |
| **`expireAtSeconds`** | `EXPIREAT`，Unix 秒 |
| **`pExpireAtMillis`** | `PEXPIREAT`，Unix 毫秒 |
| **`setWithExpire`** | `SET … EX` |
| **`setWithExpirePx`** | `SET … PX` |
| **`setIfAbsentWithExpire`** | `SET … NX EX` |
| **`setIfAbsentWithExpirePx`** | `SET … NX PX` |
| **`incrementAndExpireIfFirst`** | `INCR` + 首次 `EXPIRE` |
| **`incrementAndPExpireIfFirst`** | `INCR` + 首次 `PEXPIRE` |

源码入口：`cn.org.autumn.utils.RedisExpireUtil`（**`autumn-lib`**）。

---

## 5. 给 AI / 自动化助手的一句话口径

当用户提到 **Redis 过期、TTL、expire、Redisson、栈溢出与 pExpire** 等时：优先引导阅读 **`docs/REDIS_REDISSON_SPRING_DATA.md`**（原理与 Maven）与本文（何时用 `RedisExpireUtil`、推荐替换表）；**避免**只改一行业务代码却忽略依赖树。

---

## 6. 相关文档

- **`docs/REDIS_REDISSON_SPRING_DATA.md`**：错配原理与 Maven 示例  
- **`docs/REDIS_STANDALONE.md`**：`autumn.redis.open` 与 Bean 装配  
- **`docs/REDIS_RESILIENCE.md`**：熔断与锁（与 TTL 正交）  
- **`docs/AI_STANDARDS.md` §15**：应用层对本文的引用关系

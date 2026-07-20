# Redis 批量扫描规范（禁止 KEYS · 推荐 SCAN）

## 1. 文档定位

本文面向 **Autumn 框架及所有依赖 `cn.org.autumn` 的业务工程**，说明 Redis **批量枚举键** 的性能风险、**禁止写法** 与 **推荐写法**。

与下列文档分工：

| 文档 | 主题 |
|------|------|
| **`docs/REDIS_STANDALONE.md`** | `autumn.redis.open`、注入模式 A/B、运维接口入口 |
| **`docs/REDIS_TTL_GUIDE.md`** | TTL、`RedisExpireUtil`、过期语义 |
| **`docs/REDIS_REDISSON_SPRING_DATA.md`** | Redisson ↔ Spring Data Redis 版本对齐 |
| **`docs/REDIS_RESILIENCE.md`** | 熔断、`RedisResilience`、分布式锁降级 |
| **本文** | **`KEYS` / 全量扫描 / 大键空间运维** |

---

## 2. 典型现象（为何必须规范）

打开 **`redis.html`** 或调用 **`/sys/redis/*`** 时，若实现使用 **`RedisTemplate.keys("*")`** 或等价 **`KEYS`**：

1. Redis **单线程**被阻塞扫描（键越多越久，百万级可达分钟级）。
2. 应用侧拉回**全部键名**并在 JVM 内排序、分页，**内存与 CPU** 飙升。
3. 若再对每个键 **`GET` 反序列化**（如 Shiro Session）估大小，应用 CPU 长期占满。
4. 同实例上的 **会话、缓存、分布式锁** 等全部 **`QueryTimeoutException`**；重启发起扫描的应用后 Redis 才恢复。

**Autumn 3.0.0+** 已将框架内 **`RedisService`、`CacheService.clear`、`SysCacheController`、`ShiroSessionService`** 等改为 **`SCAN` + 分批 + 上限**。业务工程**不得**再写 `keys()` 类逻辑。

---

## 3. 禁止写法（生产与运维代码）

| 禁止 | 原因 |
|------|------|
| **`redisTemplate.keys(pattern)`** / **`stringRedisTemplate.keys(...)`** | 底层 **`KEYS`**，O(N) 阻塞 |
| **`KEYS *`** 或默认 `pattern="*"` 且无上限 | 大库必拖垮 Redis |
| 全库 **`keys("*")` → 循环 `GET`** 估值/统计 | Redis + JVM 双高 CPU |
| 用 **`keys` 探测 Redis 是否启用** | 无键时误判；有键时每次全扫 |
| **`getConnection()` 后不关闭** | 连接泄漏，加剧池耗尽 |
| 管理页同步 HTTP 里 **无超时、无截断** 的全库浏览 | 线程长期占用 |

```java
// ❌ 禁止
Set<String> keys = redisTemplate.keys("cache:" + name + ":*");
redisTemplate.delete(keys);

// ❌ 禁止：用 KEYS 判断 Redis 是否可用
return !redisTemplate.keys(sessionPrefix + "*").isEmpty();

// ❌ 禁止：列表页对 STRING 用 GET 估大小（大对象反序列化）
Object v = redisTemplate.opsForValue().get(key);
return v.toString().getBytes().length;
```

---

## 4. 推荐写法

### 4.1 精确键读写（业务主路径）

**已知完整 key** 时只用 **`GET` / `SET` / `DELETE` / `HGET`**，键名走 **`RedisKeys`** 或 **`namespace:` 前缀**，不要「先扫再筛」。

```java
String key = RedisKeys.getSysConfigKey(sysConfigService.getNameSpace(), paramKey);
stringRedisTemplate.opsForValue().get(key);
```

写入带 TTL 时优先 **`RedisExpireUtil`**（见 **`docs/REDIS_TTL_GUIDE.md`**），避免 Redisson 集成层 `pExpire` 递归风险。

```java
RedisExpireUtil.setWithExpire(redisTemplate, key, value, 600, TimeUnit.SECONDS);
```

### 4.2 按前缀批量删除 — `SCAN` + 分批 `DELETE`

```java
private void deleteByScan(RedisTemplate<String, ?> template, String pattern) {
    if (template == null) {
        return;
    }
    List<String> batch = new ArrayList<>(100);
    ScanOptions options = ScanOptions.scanOptions().match(pattern).count(200).build();
    try (Cursor<String> cursor = template.scan(options)) {
        while (cursor.hasNext()) {
            batch.add(cursor.next());
            if (batch.size() >= 100) {
                template.delete(batch);
                batch.clear();
            }
        }
    }
    if (!batch.isEmpty()) {
        template.delete(batch);
    }
}
```

**纪律**：`pattern` 尽量**具体**（如 `cache:sysconfig:*`），避免 `*`；管理类接口应设 **扫描上限** 并在响应中标记 **`truncated`**。

### 4.3 分页列表 — `SCAN` + 上限 + 仅当前页取元数据

- **总数**：全库用 **`DBSIZE`**（O(1)），不要用 `keys` 计数。
- **内存占用**：用 **`INFO memory`** 的 `used_memory`，不要全键 `GET` 求和。
- **STRING 长度**：用 **`STRLEN`** / `connection.stringCommands().strLen(...)`，不要 `GET` 大对象。
- **列表**：`SCAN` 收集或 skip 到当前页；键数超过安全阈值时 **截断** 并提示用户缩小 `pattern`。

框架参考实现：**`cn.org.autumn.modules.sys.service.RedisService`**（`/sys/redis/*`）。

### 4.4 连接与韧性

```java
try (RedisConnection conn = factory.getConnection()) {
    conn.ping();
}

// 非关键路径可包熔断
redisResilience.execute(
    () -> stringRedisTemplate.opsForValue().get(key),
    () -> null);
```

见 **`docs/REDIS_RESILIENCE.md`**。

### 4.5 缓存清理 — 走 `CacheService`，勿自建 `keys`

- 按缓存名清理：**`cacheService.clear(name)`**（内部已 SCAN）。
- 按 key 失效：**`cacheService.remove(name, key)`** 或实体 Service 的 **`removeCache*`**。
- 不要复制 **`CacheService`** 的 Redis 键规则或手写 `cache:xxx:*` 扫描。

---

## 5. 键空间治理（避免再次膨胀）

1. **几乎所有业务键设置 TTL**（`RedisExpireUtil` / `CacheConfig.redis`）。
2. **统一命名空间**：`RedisKeys.getXxxKey(namespace, ...)`，集群多租户用 **`CLUSTER_NAMESPACE`**。
3. **定期看前缀分布**（运维侧，非应用内 `KEYS`）：

   ```bash
   redis-cli --scan --pattern 'your-ns:*' | head -5000 | cut -d: -f1-3 | sort | uniq -c | sort -rn
   ```

4. **运维页**（`redis.html`）默认 `*` 仅适合看 **DBSIZE**；排查请用 **`xsignco:system:session:*`** 等精确前缀。
5. 键量 **> 百万** 时考虑 **会话/缓存/业务分库或分实例**，避免单库混合承载。

---

## 6. 自检与 CI

### 6.1 快速检索（业务仓根目录）

```bash
rg '\.keys\s*\(' --glob '*.java' .
rg 'redisTemplate\.keys|stringRedisTemplate\.keys' --glob '*.java' .
```

### 6.2 框架约束扫描

```bash
bash scripts/constraints-scan --redis-keys-only .    # J 组：禁止 .keys(
bash scripts/constraints-scan --redis-expire-only .  # H 组：TTL / RedisExpireUtil
bash scripts/constraints-scan --redis-only .         # H + J
bash scripts/constraints-scan .                      # 全文含 H + J
AUTUMN_SCAN_FAIL_ON_HIT=1 bash scripts/constraints-scan .
```

合并前建议业务工程至少跑 **`--redis-keys-only`**（若并列 clone autumn，路径指向业务根并调用 **`../autumn/scripts/constraints-scan`**）。

---

## 7. 与 Spring / Redisson 的关系

- **本次 CPU 打满 / 全站 Redis 超时**：几乎都是 **`KEYS` + 大键空间** 的实现问题，**不是** Spring Boot 先天 bug。
- **`StackOverflowError` + `pExpire`**：Redisson 桥接模块与 **`spring-data-redis`** 版本错配，见 **`docs/REDIS_REDISSON_SPRING_DATA.md`**。
- 调大 **`spring.data.redis.timeout`** 或 **`nettyThreads`** **不能替代** 禁止 `KEYS`；只能缓解单次命令等待，无法消除阻塞扫描。

---

## 8. 相关实现入口（Autumn 仓库）

| 类 | 说明 |
|----|------|
| `RedisService` | `/sys/redis/*`，SCAN 分页、`DBSIZE`、`STRLEN` |
| `CacheService` | 两级缓存；`clear` / `clear(name)` 用 SCAN 删 Redis |
| `RedisUtils` | 单键读写；TTL 走 `RedisExpireUtil` |
| `RedisKeys` | 键前缀与命名空间 |
| `ShiroSessionService` | 会话列举 SCAN（有上限） |

---

## 9. 业务工程 Checklist

- [ ] 代码中无 **`redisTemplate.keys` / `stringRedisTemplate.keys`**
- [ ] 批量删改前缀键使用 **`SCAN` + 分批**
- [ ] 列表/统计不用 **`KEYS *`**；总数用 **`DBSIZE`** / **`INFO`**
- [ ] 新 Redis 键有 **TTL** 或明确持久化理由
- [ ] 键名带 **namespace / 业务前缀**
- [ ] 合并前 **`constraints-scan --redis-keys-only`** 无新增命中
- [ ] Redisson 依赖树已对齐 **`redisson-spring-data-XX`**（见 REDIS_REDISSON 文档）

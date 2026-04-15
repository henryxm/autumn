# Redis 韧性模块（熔断 · 单机 · 宕机 · 网络）

## 1. 目标与边界

框架提供 **`cn.org.autumn.redis.resilience.RedisResilience`**（Spring Bean）与 **`DistributedLockService`** 的深度集成，在 **未改业务代码路径** 的前提下：

- **单机 / 未启用 Redis**：仍由 `autumn.redis.open` + `AutumnRedisStackEnvironmentPostProcessor` 控制是否装配 Redis 栈（见 `docs/REDIS_STANDALONE.md`）。
- **Redis 宕机、网络不通、超时**：对分布式锁与部分 Redis 调用做 **计数熔断（CLOSED → OPEN → HALF_OPEN → CLOSED）**，OPEN 期间 **默认不再打 Redisson tryLock**，避免雪崩时线程堆积在故障依赖上。
- **业务异常**：不参与熔断（仅 `RedisResilience.isInfrastructureFailure` 判定的异常会计次）。

熔断 **不是** 全路径 AOP 包装：自定义 `RedisTemplate` 调用建议在热点路径使用 **`RedisResilience#execute`** 自行包裹。

## 2. 配置项（`autumn.redis.resilience`）

| 属性 | 默认 | 说明 |
|------|------|------|
| `enabled` | `true` | 总开关；`false` 关闭熔断门控（仍可能因未装配 Redis 而无客户端）。 |
| `circuit.failure-threshold` | `5` | 连续基础设施失败进入 OPEN。 |
| `circuit.half-open-success-threshold` | `1` | HALF_OPEN 下成功多少次恢复 CLOSED（一次完整分布式锁 acquire+release 记一次成功）。 |
| `circuit.open-wait-ms` | `30000` | OPEN 持续时长，之后进入 HALF_OPEN 允许试探。 |
| `lock.skip-redisson-when-circuit-open` | `true` | OPEN 时跳过 `tryLock`，直接本地执行（`TagRunnable` / `LockOnce` 通过 `DistributedLockHelper` 同步该策略）。 |

示例（`application.yml`）：

```yaml
autumn:
  redis:
    resilience:
      enabled: true
      circuit:
        failure-threshold: 5
        half-open-success-threshold: 1
        open-wait-ms: 30000
      lock:
        skip-redisson-when-circuit-open: true
```

## 3. 分布式锁（开发者零改动主路径）

继续使用 **`DistributedLockService`**（或继承链上的 `withLock*`）。已自动处理：

1. 配置关闭 / 无 `RedissonClient` → **本地执行**。  
2. **熔断 OPEN** 且 `skip-redisson-when-circuit-open=true` → **本地执行**（打 warn 日志）。  
3. **`tryLock` 抛基础设施异常** → `recordFailure` + **本地执行**。  
4. **成功持锁并释放** → `recordSuccess`（用于 HALF_OPEN 恢复）。  
5. **锁竞争失败**（`tryLock` 返回 false）→ **不计入熔断**（属于业务语义，由 `degradeOnAcquireFailure` 等控制）。

后台 **`DISTRIBUTED_LOCK_CONFIG`** 新增 **`ignoreCircuitBreaker`**（默认 `false`）：为 `true` 时 **仍尝试** `tryLock`（强一致且接受雪崩风险时使用）。

`TagRunnable` / `TagCallable` / `LockOnce` 与 **`DistributedLockHelper.isRedissonLockPermitted()`** 对齐，熔断 OPEN 时与无客户端一致走 **单机回退执行**。

## 4. 自定义 Redis 调用（推荐写法）

注入 **`RedisResilience`**：

```java
@Autowired(required = false)
private RedisResilience redisResilience;

public Optional<String> readPromotion(String id) {
    if (redisResilience == null) {
        return Optional.empty();
    }
    try {
        return Optional.ofNullable(redisResilience.execute(
            () -> stringRedisTemplate.opsForValue().get("promo:" + id),
            () -> null));
    } catch (Exception e) {
        return Optional.empty();
    }
}
```

- **`execute(Callable, Supplier)`**：熔断不允许或基础设施异常时走 `fallback`。  
- **`executeUnchecked`**：将受检异常包装为运行时异常，适合非关键路径。  
- **`probeConnectionQuick()`**：可选健康探测（会 `PING`），成功则 `recordSuccess`。

复制即用示例见 **`docs/examples/redis-resilience/ExampleService.java`**（注释块，粘贴到业务模块后取消注释）。

## 5. 与缓存失效发布

`CacheService` 发布跨实例失效消息时，若存在 `RedisResilience`，会经 **`execute`** 包裹，避免 Redis 短时故障拖垮业务线程（失败则静默跳过发布）。

## 6. 运维与排错

- 日志关键字：`redis circuit OPEN`、`distributed lock skipped (redis circuit)`、`redis unreachable`。  
- 熔断长期 OPEN：检查 Redis 网络、连接数、DNS；适当调大 `open-wait-ms` 或 `failure-threshold`。  
- 需要 **绝对串行** 且不能接受本地降级：设置 `ignoreCircuitBreaker=true` 并接受雪崩风险，或保证 Redis SLA。

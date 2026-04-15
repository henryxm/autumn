# Redis 可选与单机运行说明

> 熔断、宕机门控与自定义调用的统一封装见 **`docs/REDIS_RESILIENCE.md`**（`RedisResilience` + `DistributedLockService`）。

## 1. 总开关：`autumn.redis.open`

- **`false`（默认推荐）**：通过 `AutumnRedisStackEnvironmentPostProcessor` 排除 `RedisAutoConfiguration`、`RedisRepositoriesAutoConfiguration`、`RedissonAutoConfiguration`，不创建 `RedisConnectionFactory` / `RedissonClient`，应用可在**不写 `spring.redis.*`** 的情况下启动。
- **`true`**：加载 Spring Data Redis 与 Redisson 自动配置，需正确配置 `spring.redis.*`（或安装向导写入的等价项），且进程需能连上 Redis。

安装向导占位启动（`autumn.install.mode=true`）时，会**强制**视为未启用 Redis 并排除上述自动配置，避免安装阶段依赖真实 Redis。

## 2. 与 `spring.redis` 的关系

仅注释 `application.yml` 里的 `spring.redis` 而仍将 `autumn.redis.open` 设为 `true` 时，仍会尝试启用 Redis 栈并可能启动失败。单机运行请保持：

```yaml
autumn:
  redis:
    open: false
  shiro:
    redis: false   # 无 Redis 时不要开启 Shiro 会话存 Redis
```

## 3. 安装向导

向导「数据库连接」步骤可选勾选 **启用 Redis**，并填写主机、端口、密码。写入的 `config/datasource.yml`（或 `autumn.install.config-path`）中会包含 `autumn.redis.open`、`autumn.shiro.redis` 以及（若勾选）`spring.redis` 段。

## 4. 分布式锁与 `@TagValue(lock=true)`

- `DistributedLockService`：无 `RedissonClient`、配置关闭、或 `tryLock` 因连接异常失败时，**在捕获异常后回退为本地执行** `Callable`。
- `TagRunnable` / `TagCallable`：`RedissonClient` 不可用时**不再跳过任务**，改为与无锁路径一致**本地执行**（多节点时仅未连 Redis 的节点会执行，存在重复执行风险，属单机降级语义）。
- `LockOnce`：同样在无客户端时调用 `executeDirectly()`；仍可在 `onRedisUnavailable()` 中插入日志或指标。

## 5. 管理接口

`/sys/redis` 依赖的 `RedisService` 在无 Redis Bean 时返回空数据或 `connected: false`。`/sys/cache` 在 Redis 未启用时仅展示本地 EhCache 侧信息。

## 6. 依赖方 / 多模块工程：无 Redis 时的 `RedisTemplate` 注入

当 **`autumn.redis.open=false`**、安装向导占位启动、或未配置 `spring.redis.*` 导致 **未创建 `RedisConnectionFactory`** 时，框架 **`RedisConfig` 不会注册 `RedisTemplate`**（`@ConditionalOnBean(RedisConnectionFactory.class)`）。任何 **独立 Starter、兄弟模块、业务 `@Component`** 若写默认 **`@Autowired RedisTemplate`**（`required` 默认为 `true`），会在 **启动期** 直接失败，例如：

`Field redisTemplate in … required a bean of type 'RedisTemplate' that could not be found`。

**推荐修复（通用，适用于所有依赖 autumn 的工程）：**

1. **字段注入**：`@Autowired(required = false) RedisTemplate<…> redisTemplate`（或 `ObjectProvider<RedisTemplate<…>>` / `Optional<RedisTemplate<…>>`）。
2. **方法体**：凡调用 `redisTemplate.opsForValue()` 等 API 前 **`if (redisTemplate == null)`** 走降级（仅内存、直接调远程、或跳过缓存）。
3. **构造器注入**：对可选依赖使用 `ObjectProvider` 或 `@Autowired(required = false)`，避免构造器阶段强制解析缺失 Bean。
4. **需要强依赖 Redis 的模块**：在文档与配置中明确要求 **`autumn.redis.open=true`** 且提供 **`spring.redis.*`**，而不是假定框架总会提供 `RedisTemplate`。

框架侧 **不会** 在未启用 Redis 时注册“空实现”的 `RedisTemplate`，以免运行期静默失败；可选 Redis 语义由业务在 **注入 + 判空** 两层显式表达。

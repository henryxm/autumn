# Redis 可选与单机运行说明

> 熔断、宕机门控与自定义调用的统一封装见 **`docs/REDIS_RESILIENCE.md`**（`RedisResilience` + `DistributedLockService`）。

## 1. 总开关：`autumn.redis.open`

- **`false`（默认）**：`AutumnRedisStackEnvironmentPostProcessor`（环境准备**较晚**执行，可读到 `application.yml` / Profile / 外部 `config/datasource.yml`）合并 `spring.autoconfigure.exclude`，排除 `RedisAutoConfiguration`、`RedisRepositoriesAutoConfiguration`、`RedissonAutoConfiguration`，应用可在**不写 `spring.redis.*`** 的情况下启动。
- **`true`**：不再排除上述自动配置，加载 Spring Data Redis 与 Redisson；需配置 **`spring.redis.*`**（或安装向导写入的等价项），且进程能连上 Redis。
- **排除项写法**：YAML 若将 `spring.autoconfigure.exclude` 写成**列表**（展开为 `spring.autoconfigure.exclude[0]` 等键），标量 `getProperty("spring.autoconfigure.exclude")` 读不到；`AutumnRedisStackEnvironmentPostProcessor` 使用 **`Binder` + 索引键回退** 解析，并在 `autumn.redis.open=true` 时剥离 Redis/Redisson 相关类名。

## 2. 框架 `RedisConfig`（`autumn-lib`）

- **类**：`cn.org.autumn.config.RedisConfig`，标注 **`@Configuration`**，随应用 **`cn.org.autumn`** 包（及子包）**组件扫描**加载；**不**通过 `META-INF/spring.factories` 的 `EnableAutoConfiguration` 注册。
- **工厂注入**：`RedisConnectionFactory` 使用 **`@Autowired(required = false)`** 字段注入。
- **Bean 方法**：`@Bean` 提供 **`@Primary` 的 `RedisTemplate<String, Object>`**、**JSON 序列化版 `RedisTemplate<String, Object>`**，以及 **`HashOperations` / `ValueOperations` / `ListOperations` / `SetOperations` / `ZSetOperations`**；模板方法内 **`setConnectionFactory(factory)`** 使用上述字段。
- **刻意不采用**：类或方法上的 **`@ConditionalOnBean(RedisConnectionFactory.class)`**，以及类上的 **`@AutoConfigureAfter(RedisAutoConfiguration.class)`**（避免 Bean 定义阶段条件与工厂注册时机不一致导致模板 Bean 被跳过）。
- **与 `autumn.redis.open` 的关系**：开关为 **`true`** 且已配置 **`spring.redis.*`** 时，由 Spring Boot 创建 **`RedisConnectionFactory`**，刷新阶段 **`redisTemplate()` 等会执行**且字段通常已注入；开关为 **`false`** 或自动配置被排除时，工厂可能为 **`null`**，当前实现**不在框架侧对 `factory` 做空值分支**，业务侧仍须按 **§3** 选择强依赖或全可选注入策略。

## 3. 业务侧：`RedisTemplate` 注入模式

### 3.1 模式 A：存在默认强依赖（`@Autowired` 未写 `required = false`）

适用：**任意一处**对 `RedisTemplate`（及同类按类型注入）使用 **`@Autowired` 默认 `required=true`**（或显式 `required=true`）。

须满足：

1. **`autumn.redis.open=true`**，并提供 **`spring.redis.*`**（或向导写入的等价项）。
2. 使用安装向导（**`autumn.install.wizard=true`**）时，在向导中**启用 Redis** 并写入可达连接，使运行期具备 **`RedisConnectionFactory`** 与框架 **`RedisTemplate`**；否则易出现 **`Field redisTemplate … required a bean … could not be found`** 等启动失败。

> 只要工程中仍有一处强依赖，**整工程按模式 A** 约束。

### 3.2 模式 B：全部为可选注入

适用：所有 `RedisTemplate` 注入均为 **`@Autowired(required = false)`** 或 **`ObjectProvider` / `Optional`**，且**调用前判空**、无 Redis 时有降级路径。

可 **`autumn.redis.open=false`** 启动；向导下可按需是否启用 Redis。若仍有一处回到模式 A，则**整工程按模式 A**。

## 4. 与 `spring.redis` 的关系

仅注释 `application.yml` 里的 `spring.redis` 而仍将 `autumn.redis.open` 设为 `true` 时，仍会尝试启用 Redis 栈并可能启动失败。单机且无意使用 Redis 时请保持：

```yaml
autumn:
  redis:
    open: false
  shiro:
    redis: false   # 无 Redis 时不要开启 Shiro 会话存 Redis
```

## 5. 安装向导

向导「数据库连接」步骤可选勾选 **启用 Redis**，并填写主机、端口、密码。写入的 `config/datasource.yml`（或 `autumn.install.config-path`）中会包含 `autumn.redis.open`、`autumn.shiro.redis` 以及（若勾选）`spring.redis` 段。

**模式 A** 的工程：向导中**必须**启用 Redis 并完成可达连接配置。

## 6. 分布式锁与 `@TagValue(lock=true)`

- `DistributedLockService`：无 `RedissonClient`、配置关闭、或 `tryLock` 因连接异常失败时，**在捕获异常后回退为本地执行** `Callable`。
- `TagRunnable` / `TagCallable`：`RedissonClient` 不可用时**不再跳过任务**，改为与无锁路径一致**本地执行**（多节点时仅未连 Redis 的节点会执行，存在重复执行风险，属单机降级语义）。
- `LockOnce`：同样在无客户端时调用 `executeDirectly()`；仍可在 `onRedisUnavailable()` 中插入日志或指标。

## 7. 管理接口

`/sys/redis` 依赖的 `RedisService` 在无 Redis Bean 时返回空数据或 `connected: false`。`/sys/cache` 在 Redis 未启用时仅展示本地 EhCache 侧信息。

## 8. 模式 B 推荐写法

1. **注入**：`@Autowired(required = false) RedisTemplate<…> redisTemplate`（或 `ObjectProvider<RedisTemplate<…>>`）。
2. **调用前**：`if (redisTemplate == null)` 则走内存/直连/跳过缓存等降级。
3. **构造器注入**：对可选依赖使用 `ObjectProvider` 或 `@Autowired(required = false)`，避免缺失 Bean 时构造失败。

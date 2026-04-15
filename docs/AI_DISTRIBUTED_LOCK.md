# Autumn 分布式锁能力说明

> 适用：`autumn-lib` / `autumn-modules`，用于跨节点互斥执行、并发削峰、重复任务抑制。

## 1. 能力入口与选型

- 已继承 `ModuleService`（继承链含 `DistributedService`）：
  - 直接使用 `withLock(...)` / `withLockRetry(...)` / `withLockOrFallback(...)`
- 未继承框架基础能力（例如独立组件、监听器、过滤器）：
  - 直接注入 `DistributedLockService`

## 2. 三种加锁模式（场景化）

- `withLock`（严格模式）
  - 获取不到锁直接失败（抛异常）
  - 适合：库存扣减、互斥写入、幂等关键写路径
- `withLockOrFallback`（降级模式）
  - 锁竞争失败时执行 fallback（例如返回缓存快照、跳过重复任务、记录稍后补偿）
  - 适合：非强一致、可容忍“本次不执行”的场景
- `withLockRetry`（抗雪崩模式）
  - 锁竞争失败后按配置做短重试 + 随机退避（jitter）
  - 适合：高并发同键竞争、瞬时突发流量

## 3. 后台配置（`DistributedLockConfig`）

- 配置对象：`cn.org.autumn.model.DistributedLockConfig`
- 配置键：`DISTRIBUTED_LOCK_CONFIG`
- 获取方式：`sysConfigService.getObject("DISTRIBUTED_LOCK_CONFIG", DistributedLockConfig.class)`
- 默认值：
  - `enabled=true`：全局分布式能力开关
  - `enableRedisson=true`：Redisson 锁开关
  - `waitMs=100`
  - `leaseMs=30000`
  - `keyPrefix="autumn:lock:"`
  - `degradeOnAcquireFailure=false`（默认严格失败，不自动降级执行业务）
  - `retryTimes=0`（默认不重试）
  - `retryBackoffMinMs=30`
  - `retryBackoffMaxMs=120`
  - `ignoreCircuitBreaker=false`：为 true 时 **Redis 熔断 OPEN 仍尝试** Redisson（强一致且接受雪崩风险时使用；见 **`docs/REDIS_RESILIENCE.md`**）

## 4. 降级策略说明

- 自动降级（基础可用性）
  - `enabled=false` 或 `enableRedisson=false`：直接本地执行
  - 无 `RedissonClient`：直接本地执行
  - `RedissonClient` 存在但 `tryLock` 因 Redis 连接类异常失败：捕获后**本地执行**（与 `autumn.redis.open=false` 时排除 Redisson 自动配置互补）
- `@TagValue(lock=true)` / `TagRunnable`：无可用 `RedissonClient` 时**单机回退执行**（不再仅跳过任务）；多节点时仅未连 Redis 的节点会执行，可能重复，属降级语义（详见 `docs/REDIS_STANDALONE.md`）
- 竞争降级（业务可控）
  - `degradeOnAcquireFailure=true`：锁竞争失败时可执行本地逻辑
  - 或使用 `withLockOrFallback` 显式定义 fallback 处理

## 5. 雪崩处理方式

- 默认不做热点自旋（避免 CPU 空转）
- `withLockRetry` 使用“有限重试 + 随机退避”：
  - 降低同一时刻对同一锁键的争抢同步性
  - 防止热点键在高并发下产生重试风暴
- 推荐搭配：
  - 业务幂等校验
  - 失败事件记录/告警
  - 必要时异步补偿任务

## 6. 使用限制与注意事项

- `lockKey` 必须具备业务唯一性（建议含实体 ID、业务维度）
- 不要在锁内执行超长阻塞 IO；租约应覆盖业务执行时间
- fallback 必须是“可重复、可观测、可追踪”的降级逻辑，不要静默吞错
- 强一致场景不要开启 `degradeOnAcquireFailure`

## 7. 场景选型决策表（先看再写代码）

| 场景 | 推荐 API | 失败策略 | 是否建议重试 |
|---|---|---|---|
| 强一致写入（库存、余额、状态机迁移） | `withLock` | 直接失败并抛错 | 否（通常靠上游重试） |
| 非强一致写入（统计、通知、非关键同步） | `withLockOrFallback` | fallback 降级执行 | 可选 |
| 热点键高并发冲突 | `withLockRetry` | 有限重试 + 退避后失败 | 是（小次数） |
| 周期任务防重入 | `withLock` 或 `withLockOrFallback` | 重复触发直接跳过 | 否 |
| 批量任务分片并发 | `withLock`（分片 key） | 单分片失败不影响其他分片 | 可选 |
| 非继承链组件（Listener/Filter） | `DistributedLockService` 直接注入 | 同上按场景选 | 同上 |

## 8. 可直接复制的模板代码

### 8.1 强一致写路径模板（推荐默认）

```java
@Service
public class OrderService extends ModuleService<OrderDao, OrderEntity> {

    public void updateOrderStatus(Long orderId, String toStatus) throws Exception {
        final String lockKey = "order:status:" + orderId;
        withLock(lockKey, () -> {
            OrderEntity entity = baseMapper.selectById(orderId);
            if (entity == null) {
                throw new IllegalStateException("order not found: " + orderId);
            }
            if (toStatus.equals(entity.getStatus())) {
                return;
            }
            entity.setStatus(toStatus);
            baseMapper.updateById(entity);
        });
    }
}
```

### 8.2 降级模板（可容忍本次不执行）

```java
@Service
public class MetricService extends ModuleService<MetricDao, MetricEntity> {

    public boolean aggregateToday(Long appId) {
        final String lockKey = "metric:aggregate:" + appId;
        return withLockOrFallbackUnchecked(lockKey, () -> {
            doAggregate(appId);
            return true;
        }, (key, ex) -> {
            // 降级策略：记录一次“本轮跳过”，由后续周期任务补偿
            log.warn("aggregate degraded key={} err={}", key, ex.toString());
            return false;
        });
    }

    private void doAggregate(Long appId) {
        // 业务聚合逻辑
    }
}
```

### 8.3 抗雪崩模板（热点并发）

```java
@Service
public class CouponService extends ModuleService<CouponDao, CouponEntity> {

    public boolean tryConsume(String userId, Long couponId) {
        final String lockKey = "coupon:consume:" + userId + ":" + couponId;
        return withLockRetryUnchecked(lockKey, () -> {
            CouponEntity coupon = baseMapper.selectById(couponId);
            if (coupon == null || coupon.getRemainCount() <= 0) {
                return false;
            }
            coupon.setRemainCount(coupon.getRemainCount() - 1);
            baseMapper.updateById(coupon);
            return true;
        });
    }
}
```

### 8.4 周期任务防重入模板（LoopJob）

```java
@Component
public class BillingCloseJob implements LoopJob.OneMinute {

    @Autowired
    private DistributedLockService distributedLockService;

    @Override
    public void onOneMinute() {
        final String lockKey = "job:billing:close";
        distributedLockService.withLockOrFallbackUnchecked(lockKey, () -> {
            // 真正的结算逻辑
            runBillingClose();
            return null;
        }, (key, ex) -> {
            // 多实例重复触发时，非持锁实例直接跳过
            log.debug("job skipped key={}", key);
            return null;
        });
    }

    private void runBillingClose() {
        // ...
    }
}
```

### 8.5 批量分片模板（分片锁，避免全局大锁）

```java
@Service
public class SyncService extends ModuleService<SyncDao, SyncEntity> {

    public void syncByTenant(List<Long> tenantIds) {
        for (Long tenantId : tenantIds) {
            final String lockKey = "sync:tenant:" + tenantId;
            withLockOrFallbackUnchecked(lockKey, () -> {
                syncOneTenant(tenantId);
                return null;
            }, (key, ex) -> {
                log.warn("tenant sync skipped tenantId={} key={}", tenantId, key);
                return null;
            });
        }
    }

    private void syncOneTenant(Long tenantId) {
        // ...
    }
}
```

### 8.6 非继承链组件模板（直接注入 `DistributedLockService`）

```java
@Component
public class ThirdPartyCallbackHandler {

    @Autowired
    private DistributedLockService distributedLockService;

    public void onCallback(String bizNo) {
        String lockKey = "callback:biz:" + bizNo;
        distributedLockService.withLockOrFallbackUnchecked(lockKey, () -> {
            // 幂等处理 + 状态推进
            processCallback(bizNo);
            return null;
        }, (key, ex) -> {
            // 降级为仅记录，避免回调风暴导致阻塞
            log.warn("callback contention key={} bizNo={}", key, bizNo);
            return null;
        });
    }

    private void processCallback(String bizNo) {
        // ...
    }
}
```

## 9. 配置建议模板（可直接抄）

### 9.1 强一致业务（默认建议）

- `degradeOnAcquireFailure=false`
- `retryTimes=0`
- 说明：拿不到锁即失败，交给调用方重试或补偿

### 9.2 热点竞争业务（抢券/秒杀类）

- `degradeOnAcquireFailure=false`
- `retryTimes=2~3`
- `retryBackoffMinMs=30`
- `retryBackoffMaxMs=120~200`
- 说明：短重试 + 抖动退避，避免竞争雪崩

### 9.3 可丢失一次执行的业务（统计/异步对账）

- `degradeOnAcquireFailure=true` 或使用 `withLockOrFallback`
- `retryTimes=0~1`
- 说明：降低失败面，后续由周期任务补偿

## 10. 团队统一约束（AI 与人工开发共用）

- 已继承 `ModuleService` 时，不要再注入并绕过 `DistributedService` 能力
- 需要分布式互斥时，优先“业务分片锁 key”，避免全局单 key
- 锁内逻辑保持短小、可幂等，IO 重操作尽量外移
- fallback 必须可观测（日志/指标/告警），禁止静默吞错
- 不允许业务自旋 while 重试锁，统一使用 `withLockRetry`

## 11. 业务域快捷模板索引（直接按业务复制）

| 业务场景 | 推荐模板 | API 组合 | 关键点 |
|---|---|---|---|
| 订单状态推进 | 8.1 强一致写路径 | `withLock` | 状态机必须单线程推进 |
| 库存扣减/回补 | 8.1 + 8.3 | `withLock` / `withLockRetry` | 高并发冲突可短重试 |
| 支付回调幂等处理 | 8.6 非继承链组件 | `withLockOrFallback` | 回调风暴时降级为记录+补偿 |
| 分钟级对账任务 | 8.4 周期任务防重入 | `withLockOrFallback` | 非持锁实例直接跳过 |
| 报表聚合/统计汇总 | 8.2 降级模板 | `withLockOrFallback` | 可容忍某轮跳过，后续补偿 |
| 多租户批量同步 | 8.5 批量分片模板 | `withLockOrFallback` | 分片锁替代全局大锁 |
| 秒杀/抢券热点 | 8.3 抗雪崩模板 | `withLockRetry` | 有限重试 + 随机退避 |
| 数据修复脚本任务 | 8.4 + 8.5 | `withLock` | 全局任务防重入 + 分片执行 |

### 11.1 复制步骤（团队统一流程）

- 第一步：按上表选最近的业务场景模板
- 第二步：替换 `lockKey` 规则（业务域 + 主键/分片）
- 第三步：选择失败策略（严格失败 / fallback / 重试）
- 第四步：补齐观测（日志 + 指标 + 告警）
- 第五步：在联调环境验证“多实例并发”与“降级路径”

### 11.2 模板占位符清单（复制后只改这些）

- `lockKey`：必须改为真实业务 key 规则
- `doXxx/processXxx/runXxx`：替换成业务方法
- `fallback`：替换为真实降级语义（记录、返回缓存、补偿入队等）
- `retry` 配置：按业务热点程度在后台 `DistributedLockConfig` 调整

## 12. AI 生成任务提示词（可直接复制）

```md
请基于 Autumn 框架分布式锁能力实现以下需求，并严格复用现有能力。

需求：
- {一句话业务目标}

场景：
- {订单状态推进/库存扣减/支付回调/报表聚合/批量同步/热点抢券}

约束：
- 若 Service 已继承 ModuleService/BaseService，必须使用 DistributedService.withLock*；
- 若当前类不在继承链中，使用 DistributedLockService；
- lockKey 必须可定位到业务主键或分片；
- 输出必须包含：严格模式、降级模式、抗雪崩模式三种说明（即使只实现一种，也要说明取舍）；
- 提供可直接落地的 Java 模板代码 + 使用限制 + 回归检查点。
```

## 13. 项目内示例源码入口

- 示例目录：`docs/examples/distributed-lock/`
- 推荐阅读顺序：
  - `OrderStateStrictLockExample.java`（强一致写路径）
  - `LoopJobReentryGuardExample.java`（周期任务防重入）
  - `CallbackFallbackLockExample.java`（非继承链组件降级）
- 使用方式：复制示例后替换包名、Dao/Entity、业务方法与 `lockKey` 规则。

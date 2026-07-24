# LoopJob 集群任务编排

> 与 `AI_NODE_PROFILE.md`、`AI_DISTRIBUTED_LOCK.md` 配套。强调**完全兼容**：未手动调整角色/职责时行为与历史一致。

## 1. JobDuty

`@JobMeta(duty = ...)` / DB `sys_schedule_assign.duty`：

| 值 | 语义 | 框架行为 |
|----|------|----------|
| **ALL**（缺省） | 过 `assign`/`server.tag` 后本机执行 | **不加**集群锁（历史等价） |
| SINGLETON | 全集群互斥执行 | 集群互斥；未获锁或**无法互斥时跳过**（fail-closed）；可选 `oncePerPeriod` |
| SEQUENTIAL | 在线成员按 uuid 排序轮转 | Redis 令牌 + 集群互斥；成员来自 Registry `online()`（按心跳淘汰） |
| DISABLED | 不执行 | return |

另：`@JobMeta(roles={...})` 仅当本机 `roles` 已手动非空（`adjusted()`）时过滤；`@JobMeta(lock="...")` 覆盖默认锁键 `autumn:job:{jobId}`。

### 1.0 同步 / 异步与持锁

- `@JobMeta(async=true)` / `delay>0`：把**整段**（周期栅栏 + 抢锁 + 业务）丢到 `TagTaskExecutor`，不堵调度线程。
- SINGLETON **始终在持锁的同一线程内**跑业务（Redisson 锁不可跨线程持有）。
- **不要**理解为「调度线程抢锁后再异步执行」——那会导致无法正确解锁。

### 1.0.1 `oncePerPeriod`（周期栅栏）

仅 `duty=SINGLETON` 且 `oncePerPeriod=true` 时生效（缺省 `false`，兼容旧行为）。

| 步骤 | 行为 |
|------|------|
| 1 | 用 Redis `TIME`（非各机本地时钟）按 LoopJob 分类间隔分桶：`bucket = redisMs / intervalMs`；经 Redisson 时须 `StringCodec`（`TIME`/`SETNX` 明文，勿走默认 Kryo） |
| 2 | `SETNX autumn:job:once:{lockKey}:{bucket}`，TTL ≈ `interval + 10s` |
| 3 | 占桶失败 → 跳过；成功 → 再 `withClusterMutexOrSkip` 持锁执行 |

解决：先到节点跑完释放互斥锁后，同逻辑周期内晚到节点再次获锁再跑。Redis/TIME 不可用时 fail-closed（跳过）。

方法级合并：方法注解缺省 `oncePerPeriod=false` **不会**把类级 `true` 打回；方法写 `true` 可打开。

```java
@JobMeta(name = "轮次兜底", duty = JobDuty.SINGLETON, oncePerPeriod = true, lock = "biz:turn-fallback")
public class TurnFallbackJob implements LoopJob.FiveMinute { ... }
```

### 1.1 类级 / 方法级 duty 合并

实现：`JobDutySupport.mergeDuty`（`LoopJob.resolveAnnotation`）。

| 场景 | 生效 duty |
|------|-----------|
| 仅类级 `@JobMeta(duty=SINGLETON)` | 该类**每个**已注册周期均为 SINGLETON |
| 方法级只写 `@JobMeta(name=...)`（注解缺省 `duty=ALL`） | **保留类级**；不会把 SINGLETON 打回 ALL |
| 方法级显式 `duty=SEQUENTIAL` / `SINGLETON` / `DISABLED`（非 ALL） | **覆盖类级** |
| 方法级须强制 ALL，而类级是 SINGLETON | **注解做不到**；用 DB `sys_schedule_assign.duty=ALL`，或改用「类级 ALL + 方法级非 ALL」模式（见 §1.3） |

`assign` / `roles` / `lock` / `name` 等：方法级有值则覆盖类级（与 duty 的「缺省 ALL 不覆盖」不同）。

### 1.2 DB duty 清空

清除 DB `duty`（空/null）时，内存回落到**注解快照**（`annotationDuty`），不必重启。

### 1.3 同一服务实现多个 LoopJob 接口

一个 Spring Bean 可同时实现多个周期接口，例如：

```java
implements LoopJob.FiveSecond, LoopJob.OneMinute, LoopJob.OneDay
```

#### 注册与身份（一周期一 Job）

- 框架按**周期分类**分别注册：`JobInfo.id = "{Category}|{FQCN}"`  
  例：`FiveSecond|cn.example.ShieldService`、`OneMinute|cn.example.ShieldService`。
- 每个分类独立解析注解：先类级 `@JobMeta`，再方法 `on{Category}()` 上的 `@JobMeta`（方法名约定：`onFiveSecond` / `onOneMinute` / …）。
- 管理面、DB `sys_schedule_assign`、默认锁键均按 **jobId** 区分，**不是**「整个类一个任务」。

#### 默认锁键

未写 `lock` 时：`autumn:job:{jobId}`。  
同一类的不同周期 → **不同锁键** → 可在不同节点、不同时刻各自抢锁，互不阻塞。  
若多周期必须互斥同一业务资源，在相关方法（或类）上写**同一** `lock="your:biz:key"`。

#### 如何标记 JobDuty（推荐写法）

**A. 全部周期同一职责（最常见）** — 只标类级：

```java
@Component
@JobMeta(name = "轮次兜底", duty = JobDuty.SINGLETON, oncePerPeriod = true, lock = "biz:turn-fallback")
public class TurnFallbackJob implements LoopJob.FiveMinute {
    @Override
    public void onFiveMinute() { /* ... */ }
}
```

多接口同理：类级 `duty` / `lock` 落到每一个已实现的周期。

**B. 多数本机 ALL，少数全集群 SINGLETON** — 类级不写 duty（或显式 ALL），方法级标非 ALL：

```java
@Service
@JobMeta(name = "防御护盾", group = "security") // duty 缺省 ALL
public class ShieldService implements LoopJob.FiveSecond, LoopJob.OneMinute, LoopJob.OneDay {

    @Override
    @JobMeta(name = "清空访问记录", skipIfRunning = true) // 仍 ALL：每机执行
    public void onFiveSecond() { /* ... */ }

    @Override
    @JobMeta(name = "刷新URI规则", duty = JobDuty.SINGLETON, lock = "wall:uris-refresh")
    public void onOneMinute() { /* 全集群一台 */ }

    @Override
    @JobMeta(name = "日清理", duty = JobDuty.SEQUENTIAL)
    public void onOneDay() { /* 成员轮转 */ }
}
```

**C. 多数 SINGLETON，个别要 ALL** — 不要指望方法级缺省 ALL 覆盖类级；任选其一：

1. **推荐**：类级不写 SINGLETON，改为各 `onXxx` 方法分别写 `duty=SINGLETON`；要 ALL 的方法不写 duty。  
2. 类级 SINGLETON + 对该 jobId 在 DB 写 `duty=ALL`。  
3. 拆成两个 Bean（ALL 与 SINGLETON 分服务）。

**D. 仅改展示名 / 超时，不改职责** — 方法级只写 `name`/`timeout`/`skipIfRunning` 等即可；类级 `duty` 继续生效。

#### 决策速查

| 意图 | 标法 |
|------|------|
| 该类所有周期都全集群一台 | 类 `@JobMeta(duty=SINGLETON)`；方法可只写 name |
| 仅某个周期全集群一台 | 类缺省 ALL；该 `onXxx` 写 `duty=SINGLETON` |
| 各周期职责不同 | 类缺省 ALL；各方法分别写非 ALL 的 duty |
| 两周期共享一把业务锁 | 两方法（或类）同一 `lock=` |
| 两周期独立互斥 | 不写 `lock`（默认按 jobId 分锁）或写不同 lock |
| 方法要从类级 SINGLETON「降回」ALL | DB `duty=ALL`，或改用模式 B/C.1 |

#### 与 assign / roles 的关系

- `assign`（`server.tag`）先过滤「本机是否该跑」；通过后再套 `JobDuty` 锁。  
- `roles` 仅节点 Profile 已手动非空时生效；与业务「集群角色」无关。  
- 多接口时可为不同 `onXxx` 配不同 `assign`/`roles`。

## 2. 完全兼容铁律

- 未写 `duty` → **ALL**。
- 未手动写非空节点 `roles` → 不过角色闸。
- DB `enabled` 仅显式 `0` 禁用；null 不改变。
- 业务自有 `withLock*` 不剥离；仅任务显式 `SINGLETON`/`SEQUENTIAL` 时框架再包锁。
- JobDuty 路径**不**走通用锁的「无 Redis 本地降级执行」：无法互斥则跳过，避免多机重复跑。

## 3. 节点画像

见 `AI_NODE_PROFILE.md`。`ProfileService.uuid()` 供业务注册节点。

运维手动编辑 `node-profile.json`（如改 `roles`）后：默认缓存 TTL 约 1 分钟内下次读路径会重新加载；亦可 `POST /sys/node/profile/reload` 立即生效。详见 `AI_NODE_PROFILE.md` §4。

## 4. 集群登记（可选）

`autumn.node.registry=true` 后 Registry 心跳上报；`PUT /sys/node/registry/assign` 远程改目标机 `roles`。关闭时不影响 LoopJob 原始流程。

- 每条成员 JSON 含 `beat`（epoch ms）；`online()` 过滤超过约 180s 无心跳的节点并清理。
- 命令 channel 带 namespace：`autumn:cluster:profile-cmd:{ns}`。
- **SEQUENTIAL** 依赖新鲜成员列表；勿在无 registry / 无 Redis 时指望轮转（将 fail-closed 跳过或无法互斥时跳过）。

## 5. 升级注意

- 发布前扫描 `sys_schedule_assign.enabled=0`：升级后门禁会真正跳过这些任务（以前重启后常又跑起来）。
- 非法 duty 字符串会 warn 并回落 ALL。
- Registry 旧格式（无 `beat`）成员视为离线，需各节点重新心跳后出现在 `online()`。

# Autumn 异步任务（`TagRunnable` / `TagTaskExecutor`）

> 适用：`autumn-lib` 中 `cn.org.autumn.thread.*`；业务工程通过 `@Autowired TagTaskExecutor asyncTaskExecutor` 提交任务。  
> 与 **`BaseQueueService` 持久化队列** 不同：本文档针对**线程池一次性异步**与**本机/内存队列 drain** 场景。  
> 跨节点互斥写路径见 **`docs/AI_DISTRIBUTED_LOCK.md`**；Redis 熔断与锁降级见 **`docs/REDIS_RESILIENCE.md`**。

## 1. 组件职责

| 组件 | 职责 |
|------|------|
| `TagTaskExecutor` | 线程池封装；任务去重（`id`）；运行中/历史统计 |
| `TagRunnable` | 异步任务基类：`exe()` 业务入口；`onFinished(FinishStatus)` 生命周期收口 |
| `LockOnce` | 继承 `TagRunnable`；**时间窗内最多执行一次**（框架 Redisson 锁，未持锁可跳过 `exe()`） |
| `@TagValue` | 标在 `exe()` 上：tag、delay、timeout、`lock=true` 等元数据 |
| `FinishStatus` | `onFinished` 回调参数：任务如何结束 |
| `JobPhase` | 本机调度相位：`IDLE` / `DISPATCHING`（单飞闸门与内存队列 drain） |
| `JobPhaseGate` | `JobPhase` 的 CAS / end / 再调度 / 积压补偿工具 |

**不要混用语义：**

- **防重 / 整点任务只跑一次** → `LockOnce` 或 `@TagValue(lock = true)`（可接受「本轮未进 `exe()`」）。
- **内存队列 drain / 须释放本机调度闸门** → 普通 `TagRunnable`（`lock = false`）+ `onFinished` + 业务侧 `withLockOrFallback*`（见 §4）。

## 2. 生命周期与 `FinishStatus`

框架保证：**每个任务实例 `onFinished` 最多调用一次**。

| `FinishStatus` | 何时触发 | 是否调用过 `exe()` |
|----------------|----------|-------------------|
| `COMPLETED` | `invokeExe()` 内 `exe()` 正常返回 | 是 |
| `FAILED` | `invokeExe()` 内 `exe()` 抛错（含中断） | 是 |
| `SKIPPED` | 已进入 `run()`，但未进 `exe()`（系统未就绪、错峰中断、`@TagValue(lock=true)` 未持锁等） | 否 |
| `NOT_DISPATCHED` | `TagTaskExecutor.execute` 未提交线程池（`id` 去重、提交失败） | 否 |

要点：

- 只要进了 `exe()`，返回或异常后**立即**在 `invokeExe()` 的 `finally` 中 `onFinished(COMPLETED|FAILED)`，再向上抛异常。
- 未进 `exe()` 时，在 `run()` 的 `finally` 中 `onFinished(SKIPPED)`。
- **禁止**在业务代码中直接调用 `exe()`；异步入口统一由框架 `invokeExe()` 调用。

```java
asyncTaskExecutor.execute(new TagRunnable() {
    @Override
    protected void onFinished(FinishStatus status) {
        // 释放 DISPATCHING、补偿再次 schedule 等（见 §4）
    }

    @Override
    @TagValue(type = MyService.class, method = "doAsync", tag = "说明", lock = false)
    public void exe() {
        // 仅写业务；不要在这里 release 本机闸门
    }
});
```

`execute(TagRunnable)` 返回 `boolean`：`false` 表示未提交且已 `onFinished(NOT_DISPATCHED)`。

## 3. 选型：`TagRunnable` vs `LockOnce` vs `ModuleService.withLock*`

| 需求 | 推荐 |
|------|------|
| 定时任务整点只跑一次、多节点防重 | `LockOnce` 或 `@TagValue(lock=true, time=…)` |
| 订单/库存强一致单笔写 | 同步或异步内 `withLock`（`docs/AI_DISTRIBUTED_LOCK.md` §8.1） |
| 内存队列消费、要尽量执行、失败可下轮再试 | `TagRunnable` + `onFinished` + `withLockOrFallback` 在 `exe()` 内 |
| 周期扫尾补偿 | `LoopJob` + `onFinished` 或 `recoverIfStalled()`（§4.3） |

## 4. 本机队列 + 状态机（子项目推荐范式）

典型错误：在 `execute()` **之前** `CAS` 把 `running=true`，在 `exe()` 的 `finally` 里 `running=false` —— 当 `LockOnce` / `lock=true` 跳过 `exe()` 时闸门永久卡住。

推荐两阶段：

1. **本机调度相位**（框架枚举 {@code cn.org.autumn.thread.JobPhase}：`IDLE` / `DISPATCHING`）：只表示「是否已向线程池提交 drain 任务」；在 **`onFinished` 中** 置回 `IDLE`，若队列非空再 `schedule`。**勿**在各 Service 内再定义私有 `JobPhase` / `DrainPhase`，请用 {@code JobPhase} + {@code JobPhaseGate}。
2. **跨节点互斥**：在 `exe()` 内用 `withLockOrFallbackUnchecked("biz:queue:drain", …)`，抢不到锁记日志，依赖下一轮 `onFinished` 或 `LoopJob` 补偿。

```java
import cn.org.autumn.thread.JobPhase;
import cn.org.autumn.thread.JobPhaseGate;
import cn.org.autumn.thread.FinishStatus;
import cn.org.autumn.thread.TagRunnable;

private final AtomicReference<JobPhase> phase = JobPhaseGate.create();
private final ConcurrentMap<String, Item> queue = new ConcurrentHashMap<>();

void enqueue(Item item) {
    queue.put(item.getKey(), item);
    scheduleDrain();
}

void scheduleDrain() {
    if (queue.isEmpty()) {
        JobPhaseGate.resetIdle(phase);
        return;
    }
    if (!JobPhaseGate.tryBegin(phase)) {
        return;
    }
    asyncTaskExecutor.execute(new TagRunnable() {
        @Override
        protected void onFinished(FinishStatus status) {
            JobPhaseGate.endAndMaybeReschedule(phase, () -> !queue.isEmpty(), this::scheduleDrain);
        }

        @Override
        @TagValue(type = MyService.class, method = "drainQueue", tag = "队列消费", lock = false)
        public void exe() {
            withLockOrFallbackUnchecked("my:module:queue-drain", () -> {
                drainQueue();
                return null;
            }, (key, ex) -> {
                log.warn("drain 未持锁: key={}", key);
                return null;
            });
        }
    });
}

void recoverIfStalled() {
    JobPhaseGate.recoverIfStalled(phase, () -> !queue.isEmpty(), this::scheduleDrain);
}
```

`LoopJob.OneMinute` 中可调用 `recoverIfStalled()`，防止 `SKIPPED` / `NOT_DISPATCHED` 后队列积压无消费者。

单飞周期任务（非队列）同样使用 `JobPhaseGate.tryBegin` / `JobPhaseGate.end`：

```java
private final AtomicReference<JobPhase> syncPhase = JobPhaseGate.create();

void onOneMinute() {
    if (!JobPhaseGate.tryBegin(syncPhase))
        return;
    asyncTaskExecutor.execute(new TagRunnable() {
        @Override
        protected void onFinished(FinishStatus s) {
            JobPhaseGate.end(syncPhase);
        }
        @Override
        @TagValue(..., lock = false)
        public void exe() { /* 业务 */ }
    });
}
```

## 4.1 LoopJob 周期回调纪律（秒级 / ≤1 分钟）

`LoopJob` 同类周期任务**默认串行**执行：上一个 `onXxx` 阻塞会拖慢同分类后续任务。因此：

| 周期 | 回调内允许 | 回调内禁止（默认） |
|------|------------|-------------------|
| **秒级**（`OneSecond`～`ThirtySecond`） | 内存结构轻量扫描、入队、触发 `scheduleDrain`、本机缓存标记清理 | 复杂业务、阻塞 IO、**数据库 / 文件 / Redis** 读写、远程 HTTP |
| **一分钟及以内**（含 `OneMinute`） | 同上；可做极轻量本机内存清理 | 原则上禁止 **DB / 文件 / Redis**；耗时逻辑一律异步 |
| **大于一分钟**（`FiveMinute` 及以上） | 可在回调内做适度业务；仍建议重活走 `TagTaskExecutor` | 长时间阻塞整类调度（应用 `timeout` / 异步） |

**正确拆分：**

1. **秒级 / ≤1min 回调**：只做「快速投递」——内存入队 + `scheduleDrain()`，或 **`FunctionQueues.offer(...)`**（回调内有 DB 的简单串行，见 **`docs/AI_FUNCTION_QUEUE.md`** §5）。
2. **耗时处理**：
   - **简单串行 / 降低起线程开销**：`FunctionQueue`（常驻单 worker；框架 wall / UserProfile 等已按此迁）。
   - **业务 map + 单飞 / 再调度 / 监控 UI**：`TagTaskExecutor` + `TagRunnable`（`lock=false`）+ `JobPhaseGate`；`exe()` 内再访问 Redis / DB（跨节点用 `withLockOrFallback*`）。
3. **禁止**在秒级回调里私建 `ScheduledExecutorService` / `Executors.*`。

```java
// A) FunctionQueue：≤1min 且需访问 DB 的简单滚动
@Override
public void onOneMinute() {
    FunctionQueues.offer("IpBlackService.clear", this::clear);
}

// B) TagTaskExecutor + JobPhaseGate：内存 map drain
@Override
public void onThirtySecond() {
    enqueueExpiredKeys();
    scheduleDrain();
}
```

## 5. 反模式（AI 与人工均需避免）

- 在 `execute()` 前维护 `AtomicBoolean bookRunning`，仅在 `exe()` 的 `finally` 释放。
- 队列 drain 使用 `LockOnce` 却期望每轮都执行 `exe()`。
- 覆写 `run()` 却不调用 `super.run()` 且未自行 `notifyFinished(SKIPPED)`。
- `onFinished` 中执行耗时阻塞 IO（应短逻辑：改相位、触发下一轮 schedule）。
- 与 `BaseQueueService` 混用同一业务键却不区分「持久化消息」与「内存待处理」两套语义。
- **在秒级 / `OneMinute` 的 `onXxx` 内直接读写 Redis、数据库、文件或跑复杂业务**（应入队后异步处理，见 §4.1）。
- 为「延迟 N 秒删 key」私建 `ScheduledExecutorService`，绕过 `LoopJob` + `TagTaskExecutor`。

## 5.1 Quartz `schedule_job` 与 LoopJob 线程池隔离

| 池 | Bean | 用途 |
|----|------|------|
| 主异步池 | `@Primary` `asyncTaskExecutor`（线程名 `Executor-*`） | LoopJob drain、业务 `TagRunnable` |
| Quartz 专用池 | `scheduleJobExecutor`（线程名 `ScheduleJob-*`） | 管理后台 / 表 `schedule_job` 的 cron 方法 |

- **禁止**再让 `ScheduleJob` 共用主池：长日任务会占满 `Executor-*`，饿死秒级 drain。
- 超时：`autumn.job.schedule-timeout-seconds`（**默认 0=不限制**）；仅当显式 `>0` 时 `future.get` 超时并 `cancel`。
- 池大小：`autumn.job.schedule-pool-core`（默认 2）/ `schedule-pool-max`（8）/ `schedule-pool-queue`（64）。
- 重活仍可走 `schedule_job`，但注意 Quartz 触发线程会 `future.get` 阻塞至任务结束（或超时）；LoopJob 周期任务不受影响。

## 6. 与 Redis / 熔断的关系

- `@TagValue(lock=true)` / `LockOnce`：未持锁 → `SKIPPED` → **`onFinished` 仍会调用**（`run()` 收口）。
- Redis 不可用时的**单机回退**可能在本机执行 `exe()`（多节点有重复风险），见 **`docs/REDIS_STANDALONE.md`**。
- 熔断 OPEN 时框架锁行为与 **`docs/REDIS_RESILIENCE.md`** 一致；业务 `withLockOrFallback` 与框架锁是两层，互不复用。

## 7. 相关源码与文档

- `cn.org.autumn.thread.TagRunnable`、`LockOnce`、`FinishStatus`、`TagTaskExecutor`、`JobPhase`、`JobPhaseGate`
- `cn.org.autumn.thread.FunctionQueue`、`FunctionQueues`（全局串行函数队列，见 **`docs/AI_FUNCTION_QUEUE.md`**）
- `cn.org.autumn.modules.job.task.LoopJob`（周期回调；秒级纪律见 §4.1）
- **`docs/AI_DISTRIBUTED_LOCK.md`** §10（摘要）及 §8 模板
- **`docs/AI_MAP.md`** §2.2C、§2.2D、§2.5
- 示例：`docs/examples/distributed-lock/`（`withLock*`）；业务队列参考各子项目 Service 内 `scheduleDrain` 实现

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

1. **本机调度相位**（如 `IDLE` / `DISPATCHING`）：只表示「是否已向线程池提交 drain 任务」；在 **`onFinished` 中** 置回 `IDLE`，若队列非空再 `schedule`。
2. **跨节点互斥**：在 `exe()` 内用 `withLockOrFallbackUnchecked("biz:queue:drain", …)`，抢不到锁记日志，依赖下一轮 `onFinished` 或 `LoopJob` 补偿。

```java
private enum DrainPhase { IDLE, DISPATCHING }
private final AtomicReference<DrainPhase> phase = new AtomicReference<>(DrainPhase.IDLE);
private final ConcurrentMap<String, Item> queue = new ConcurrentHashMap<>();

void enqueue(Item item) {
    queue.put(item.getKey(), item);
    scheduleDrain();
}

void scheduleDrain() {
    if (queue.isEmpty()) {
        phase.set(DrainPhase.IDLE);
        return;
    }
    if (!phase.compareAndSet(DrainPhase.IDLE, DrainPhase.DISPATCHING)) {
        return;
    }
    asyncTaskExecutor.execute(new TagRunnable() {
        @Override
        protected void onFinished(FinishStatus status) {
            phase.set(DrainPhase.IDLE);
            if (!queue.isEmpty()) {
                scheduleDrain();
            }
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
    if (!queue.isEmpty() && phase.get() == DrainPhase.IDLE) {
        scheduleDrain();
    }
}
```

`LoopJob.OneMinute` 中可调用 `recoverIfStalled()`，防止 `SKIPPED` / `NOT_DISPATCHED` 后队列积压无消费者。

## 5. 反模式（AI 与人工均需避免）

- 在 `execute()` 前维护 `AtomicBoolean bookRunning`，仅在 `exe()` 的 `finally` 释放。
- 队列 drain 使用 `LockOnce` 却期望每轮都执行 `exe()`。
- 覆写 `run()` 却不调用 `super.run()` 且未自行 `notifyFinished(SKIPPED)`。
- `onFinished` 中执行耗时阻塞 IO（应短逻辑：改相位、触发下一轮 schedule）。
- 与 `BaseQueueService` 混用同一业务键却不区分「持久化消息」与「内存待处理」两套语义。

## 6. 与 Redis / 熔断的关系

- `@TagValue(lock=true)` / `LockOnce`：未持锁 → `SKIPPED` → **`onFinished` 仍会调用**（`run()` 收口）。
- Redis 不可用时的**单机回退**可能在本机执行 `exe()`（多节点有重复风险），见 **`docs/REDIS_STANDALONE.md`**。
- 熔断 OPEN 时框架锁行为与 **`docs/REDIS_RESILIENCE.md`** 一致；业务 `withLockOrFallback` 与框架锁是两层，互不复用。

## 7. 相关源码与文档

- `cn.org.autumn.thread.TagRunnable`、`LockOnce`、`FinishStatus`、`TagTaskExecutor`
- **`docs/AI_DISTRIBUTED_LOCK.md`** §10（摘要）及 §8 模板
- **`docs/AI_MAP.md`** §2.2C
- 示例：`docs/examples/distributed-lock/`（`withLock*`）；业务队列参考各子项目 Service 内 `scheduleDrain` 实现

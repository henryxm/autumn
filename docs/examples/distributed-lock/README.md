# Distributed Lock 示例库（可直接复制）

> 说明：本目录用于沉淀“可直接拷贝到业务工程”的分布式锁模板代码。  
> 这些文件是示例源码，不参与当前工程编译。

## 文件清单

- `OrderStateStrictLockExample.java`
  - 场景：强一致写入（订单状态推进）
  - API：`withLock`
- `LoopJobReentryGuardExample.java`
  - 场景：周期任务防重入
  - API：`DistributedLockService.withLockOrFallbackUnchecked`
- `CallbackFallbackLockExample.java`
  - 场景：非继承链组件（回调处理）可降级
  - API：`DistributedLockService.withLockOrFallbackUnchecked`

## 使用步骤

- 复制最接近的示例文件到目标模块
- 按需替换包名、类名、DAO/Entity/方法名
- 按业务规则替换 `lockKey` 设计
- 根据一致性要求在“严格/降级/重试”模式间切换
- 增加日志、指标、告警，补齐多实例并发回归

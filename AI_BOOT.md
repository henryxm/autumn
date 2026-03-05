# Autumn AI 启动上下文（首轮必读）

> 用途：给 AI 的最小启动上下文。默认只读本文件，再按任务类型追加其他文档。
> 统一索引：`@AI_INDEX.md`

## 1. 核心原则（必须遵守）

- 优先复用框架能力，禁止重复造轮子。
- 默认从 `ModuleService` 继承链出发实现业务：
  - `ModuleService -> BaseService -> ShareCacheService -> BaseCacheService -> BaseQueueService`
- 业务逻辑放可维护层（`controller/*`、`service/*`），避免放 `controller/gen/*` 可重生层。

## 2. 默认技术路径（高频）

- 接口：
  - 默认：`Request<T> -> Response<T>`
  - 兼容：`CompatibleRequest<T> -> CompatibleResponse<T> + @Endpoint(compatible=true)`
- 缓存：优先 `getCache/getListCache/getShareCache`，写后必须失效。
- 队列：优先 `BaseQueueService`（发送、消费、重试、死信）。
- 任务：固定周期优先 `LoopJob.*`，复杂日历规则才用 cron。
- 生成链路：实体注解驱动建表 -> gen 生成骨架 -> 业务补在非 gen 层。

## 3. 加密最小约束

- 请求解密触发：请求体包含 `ciphertext + session`。
- 响应加密触发：请求头包含 `X-Encrypt-Session`。
- 握手入口：`/rsa/api/v1/init`（仅握手，不混入业务）。
- 无 `X-Encrypt-Session` 时返回明文；兼容响应可降级为 `data`。

## 4. 开发前 AI 自检（每次执行）

- 现有基类/模块能力是否已覆盖需求？
- 变更是否破坏缓存一致性、加密语义、权限语义？
- 是否给出回归点（明文/密文/header 有无、缓存失效、队列失败、任务观测）？

## 5. 按需追加文档（不要全量加载）

- 核心能力详解：`@AI_MAP.md`
- 加解密兼容专项：`@AI_CRYPTO.md`
- 模块任务模板：`@AI_TEMPLATES.md`
- 治理与协作规范：`@AI_GOVERNANCE.md`
- 安全专项（签名/灰度/演练）：`@AI_SECURITY.md`
- 提问模板库：`@AI_PROMPTS.md`

## 6. 推荐加载组合

- 日常开发：`AI_BOOT.md + AI_MAP.md`
- 接口加解密改造：`AI_BOOT.md + AI_MAP.md + AI_CRYPTO.md`
- 模块新建/代码生成：`AI_BOOT.md + AI_MAP.md + AI_TEMPLATES.md`
- 规范梳理/团队协作：`AI_BOOT.md + AI_MAP.md + AI_GOVERNANCE.md`
- 安全改造/攻防演练：`AI_BOOT.md + AI_MAP.md + AI_SECURITY.md`

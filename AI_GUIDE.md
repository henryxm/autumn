# Autumn 多项目 AI 开发指南（跨平台）

> 目标：当你有多个业务项目都基于 Autumn 时，让 AI 每次都能快速理解框架能力，并按框架预设方案实现业务（不依赖固定绝对路径）。

## 1. 推荐的上下文喂给方式（3 层）

每次和 AI 开发时，按这 3 层提供上下文，效果最稳定：

1. **框架层（固定）**  
   引用 Autumn 能力地图：`@<autumn-root>/AI_MAP.md`
2. **当前项目层（变化）**  
   引用当前项目的 README、模块目录、核心配置文件
3. **当前任务层（变化）**  
   明确“目标 + 约束 + 验收标准 + 禁止事项”

---

## 2. 你在 Cursor 里如何使用相对路径

推荐优先使用相对路径，兼容 Windows/macOS/Linux。

### 2.1 最小可用写法（每次都可复制）

```md
请先阅读这些上下文再实现：
- @../autumn/AI_MAP.md
- @./README.md
- @./src  （或对应后端目录）

需求目标：
- <一句话>

实现约束：
- 必须优先复用 Autumn 框架能力，不允许重复造轮子
- 不破坏现有接口与权限语义
- 涉及缓存/队列/加密时必须遵循框架默认流程
```

### 2.2 如果任务涉及特定能力，额外补充引用

- 缓存：再加 `@../autumn/autumn-modules/src/main/resources/templates/modules/docs/cache.html`
- 队列：再加 `@../autumn/autumn-modules/src/main/resources/templates/modules/docs/queue.html`
- 加解密：再加 `@../autumn/autumn-modules/src/main/resources/templates/modules/docs/hybrid-crypto.html`
- Handler 扩展：再加 `@../autumn/autumn-modules/src/main/resources/templates/modules/docs/handler.html`
- 定时任务：再加 `@../autumn/autumn-modules/src/main/resources/templates/modules/docs/job.html`

---

## 3. 标准提问模板（强烈建议）

## 3.1 新增业务功能

```md
你是我的 Autumn 架构开发助手。

先阅读：
- @../autumn/AI_MAP.md
- @./README.md
- @./<模块目录>

现在要实现：
- <功能目标>

请按以下顺序输出：
1) 先给实现方案（复用哪些 Autumn 能力、涉及哪些类）
2) 再改代码（最小改动）
3) 最后给测试清单和回归风险

强约束：
- 不重复实现已有基础能力
- 缓存/队列/加解密必须走框架既有机制
- 若发现需求与框架冲突，先提出冲突点与可选方案，不要硬改内核
```

## 3.2 修 Bug

```md
先读取：
- @../autumn/AI_MAP.md
- @./<报错相关目录>

问题现象：
- <错误日志/复现步骤>

要求：
- 优先定位是否违反 Autumn 既有机制（缓存失效、队列重试、加密触发条件等）
- 给出 root cause
- 最小修复 + 回归点
```

---

## 4. AI 需要遵守的框架规则（建议你固定写进每次提示）

- 优先使用：
  - 缓存：`BaseCacheService` / `CacheService` / `ShareCacheService`
  - 队列：`BaseQueueService` / `QueueService`
  - 加密：`EncryptArgumentResolver` / `EncryptInterceptor` / `RsaService` / `AesService`
  - 定时任务：优先 `LoopJob.OneMinute/FiveMinute/...` 接口周期，复杂时间规则再用 cron
- 禁止：
  - 自建重复缓存框架
  - 新造并行队列基础设施
  - 绕开现有加密触发规则直接改协议
  - 固定周期任务强行使用 cron 表达式（无必要不允许）
- 必做：
  - 变更点说明“为什么这样改符合 Autumn 机制”
  - 给出可验证步骤（接口/页面/数据）

---

## 4.1 定时任务专项提示词（可直接复制）

```md
请优先按 Autumn 的接口式定时任务实现，不要先写 cron。

要求：
- 固定周期任务使用 LoopJob 接口（如 OneMinute/FiveMinute）
- 用 @JobMeta 配置 skipIfRunning、timeout、maxConsecutiveErrors
- 多节点场景给出 assignTag/server.tag 建议
- 给出 /job/loop/list /stats /alerts /trigger 的验证步骤
- 仅当时间规则确实复杂时，才允许回退到 schedulejob + cronExpression
```

---

## 5. 多项目复用建议（实践）

当多个项目都基于 Autumn，建议每个项目都放一个本地 `AGENTS.md`（简版），内容只保留两件事：

1. 指向统一框架地图：`@../autumn/AI_MAP.md`（按你的目录层级调整）
2. 声明该项目自己的模块边界、禁改范围、发布规范

这样 AI 先读“统一框架规则”，再读“项目特定规则”，效果最好。

---

## 6. 一句话工作流（你可以直接照做）

每次提需求时都先贴：

1) `@../autumn/AI_MAP.md`  
2) 当前项目关键目录  
3) 任务目标 + 强约束 + 输出顺序

这能显著提升 AI 按框架实现业务功能的准确率与一致性。


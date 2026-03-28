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

## 3. 注解能力速查（高频）

- `@JobMeta`（任务治理）
  - 作用域：类 + 方法（方法级覆盖类级）。
  - 核心参数：`skipIfRunning`（防重入）、`timeout`（超时观测）、`maxConsecutiveErrors`（连续错误自动禁用）、`assign`（多节点分配）、`delay/async`（异步调度）。
  - 建议：秒级/分钟级任务默认开启 `skipIfRunning=true`，并设置合理 `timeout`。
- `@TaskAware`（任务触发）
  - 定义 cron 触发、环境模式（`mode`）、展示备注（`remark`）。
  - 职责边界：负责“什么时候触发”；运行治理交给 `@JobMeta`。
- `@Endpoint`（接口加解密语义）
  - `force=true`：强制密文会话（入参或出参）。
  - `compatible=true`：普通对象也允许按请求头走兼容加密返回。
  - `hidden/reason`：控制是否对外暴露在端点清单中。
- `@Cache / @Caches`（缓存索引）
  - 声明缓存键字段、唯一性、是否未命中自动创建（`create=true`）。
  - 约束：写操作后必须做缓存失效，避免脏读。
- `@EnvAware`（配置注入）
  - 在配置 Bean 字段上声明配置键（如 `site.domain`、`node.tag`）。
- `@Table` / `@Column` / `@Index` / `@Indexes` / `@IndexField`（注解驱动建表，见 `AI_MAP.md` 2.10 节）
  - `@Table.comment` / `@Column.comment`：`BaseService` 多语言初始化在注释含 **`:`** 时**只取冒号前**作为列表/菜单等处的**短标题**；冒号后为详述。建议 **`短标题（约 1～4 字）：详细说明`**，避免表头被长文案撑满（详见 `AI_MAP.md` 2.10.5）。
  - `@Column(isUnique = true)`：已在 DDL 中为该列生成唯一约束；**禁止**再在同一字段上叠 `@Index`，也避免用 `@Indexes` 再声明同一单列唯一/普通索引，以免重复索引与迁移对比噪音。
  - 字段上已用 `@Index` 的列：**不要**在类级 `@Indexes`（或类级 `@Index` 的 `fields`）里再声明同列的同用途索引，避免 `TableInfo` 收集到重复 `IndexInfo`、建表/变更阶段生成重复索引。
  - 字段级 `@Index`（无 `fields` 时）会把 `@Column.length`（默认 `255`）当作索引**前缀长度**写入 SQL；前缀语法仅适用于字符串类列。除 `String`、以及落库为字符串类型的枚举等字段外，**不要**使用字段级 `@Index`；若确需对数值/日期等列建索引，请用类级 `@Indexes` + `@IndexField(field = "...", length = 0)`。`IndexTypeEnum.FULLTEXT` 仅适用于字符型列（MySQL 全文索引语义）。

## 4. 加密最小约束

- 请求解密触发：请求体包含 `ciphertext + session`。
- 响应加密触发：请求头包含 `X-Encrypt-Session`。
- 握手入口：`/rsa/api/v1/init`（仅握手，不混入业务）。
- 无 `X-Encrypt-Session` 时返回明文；兼容响应可降级为 `data`。

## 5. 开发前 AI 自检（每次执行）

- 现有基类/模块能力是否已覆盖需求？
- 变更是否破坏缓存一致性、加密语义、权限语义、任务治理语义？
- 是否给出回归点（明文/密文/header 有无、缓存失效、队列失败、任务观测）？

## 6. 按需追加文档（不要全量加载）

- 核心能力详解：`@AI_MAP.md`
- 加解密兼容专项：`@AI_CRYPTO.md`
- 模块任务模板：`@AI_TEMPLATES.md`
- 治理与协作规范：`@AI_GOVERNANCE.md`
- 安全专项（签名/灰度/演练）：`@AI_SECURITY.md`
- 提问模板库：`@AI_PROMPTS.md`

## 7. 推荐加载组合

- 日常开发：`AI_BOOT.md + AI_MAP.md`
- 接口加解密改造：`AI_BOOT.md + AI_MAP.md + AI_CRYPTO.md`
- 模块新建/代码生成：`AI_BOOT.md + AI_MAP.md + AI_TEMPLATES.md`
- 规范梳理/团队协作：`AI_BOOT.md + AI_MAP.md + AI_GOVERNANCE.md`
- 安全改造/攻防演练：`AI_BOOT.md + AI_MAP.md + AI_SECURITY.md`

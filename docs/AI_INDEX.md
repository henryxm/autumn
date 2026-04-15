# Autumn AI 文档总索引（统一入口）

> 用途：团队统一引用入口。先看本页，再按任务场景加载最小文档集合。

> **分支与实现差异**：`docs/` 中的流程与纪律对 **2.0.0（master）** 与 **3.0.0（`3.0.0` 分支）** 均适用；具体 **JDK、Spring Boot、MyBatis-Plus、`javax`/`jakarta`、OpenAPI（SpringDoc）** 等以**当前检出分支**的根 `pom.xml` 与 `README.md` 为准。选用 Cursor Skill：2.x → `autumn-framework-2x`，3.x → `autumn-framework-3x`。

## 1. 文档职责一览

- `docs/AI_BOOT.md`：最小启动上下文（首轮必读）
- `docs/INSTALL_MODE_CONDITIONAL.md`：安装模式（`autumn.install.mode`）下 **`@ConditionalOnNotInstallMode` / `@ConditionalOnInstallMode`** 与 **`autumn.install.autoconfigure-exclude`** 用法；**§0** 安装占位数据源（**默认 H2 内存**、可选 **mysql**）
- `docs/REDIS_STANDALONE.md`：**可选 Redis / 单机启动**（`autumn.redis.open`、安装向导、分布式锁单机回退；**§6** 依赖方 **`RedisTemplate` 可选注入**）
- `docs/REDIS_RESILIENCE.md`：**Redis 韧性**（`RedisResilience` 熔断、`DistributedLockService` 集成、`DISTRIBUTED_LOCK_CONFIG.ignoreCircuitBreaker`）
- `docs/AI_MAP.md`：高频开发能力主图（含生成模板分层与可改/不可改边界）
- `docs/AI_STANDARDS.md`：**约束性开发规范**（分层、API、定时任务、权限、FTL、**实体/注解建表/禁止初始化 DDL**、模块表前缀、**Dao+Provider**、**Controller–Service–Dao**、**statics/pages/Site/PageAware**）
- **`docs/AI_DATABASE.md`**：**多数据库落地规范**（已支持 `DatabaseType` 清单、**§2.1 `RuntimeSqlDialect` 能力清单与故意不抽象项**、**§4.0 代码层标准写法（`RuntimeSql` / `WrapperColumns`，禁止硬编码方言符号）**、**全库兼容默认**、**Wrapper 安全边界**、**Dao+Provider 强制与推荐分层**、`RuntimeSql` 使用纪律；**§8 老旧注解 Dao / 方言化 Wrapper 升级与一键体检策略**）
- `docs/AI_DISTRIBUTED_LOCK.md`：分布式锁能力（`DistributedLockService` / `DistributedService`）、场景化加锁、降级与抗雪崩策略、`DistributedLockConfig` 配置项与默认值、业务域快捷模板（含项目示例源码入口）
- `docs/AI_POSTGRESQL.md`：PostgreSQL 专项（DDL/元数据、`PostgresQuerySql`、迁移与兼容性）；通用跨库口径以 **`docs/AI_DATABASE.md`** 为准
- `docs/AI_UPGRADE.md`：依赖方升级 autumn 时的清单、一键扫描脚本说明与自动化边界
- `docs/AI_CRYPTO.md`：接口加解密兼容与迁移
- `docs/AI_SECURITY.md`：安全强校验、灰度、演练
- `docs/AI_CODEGEN.md`：**代码生成链路（gen / GenUtils / 模板 / 库表反射）**与**推荐三步开发流程**（实体 → 生成骨架 → 业务与页面）；**`BaseCacheService` / `ShareCacheService` / `BaseQueueService`** 能力说明
- `docs/AI_TEMPLATES.md`：模块任务模板库
- `docs/AI_GOVERNANCE.md`：治理协作、术语、维护口径
- `docs/AI_PROMPTS.md`：可复制提示词模板
- `docs/AI_GUIDE.md`：多项目导航与引用方式

## 2. 推荐加载矩阵（按场景）

- 日常开发：`docs/AI_BOOT.md + docs/AI_MAP.md + docs/AI_STANDARDS.md`（涉及 SQL/Wrapper/多库时追加 **`docs/AI_DATABASE.md`**）
- 分布式互斥/跨节点任务：`docs/AI_BOOT.md + docs/AI_MAP.md + docs/AI_STANDARDS.md + docs/AI_DISTRIBUTED_LOCK.md`
- 新模块/代码生成：`docs/AI_BOOT.md + docs/AI_MAP.md + docs/AI_STANDARDS.md + docs/AI_CODEGEN.md + docs/AI_TEMPLATES.md`（先读代码生成流程与三步节奏，再确认生成层约束，后落业务层）
- 多项目模板整合（TemplateFactory）：`docs/AI_BOOT.md + docs/AI_MAP.md + docs/AI_STANDARDS.md + docs/AI_GOVERNANCE.md`
- 接口加解密改造：`docs/AI_BOOT.md + docs/AI_MAP.md + docs/AI_STANDARDS.md + docs/AI_CRYPTO.md`
- 安全改造/攻防演练：`docs/AI_BOOT.md + docs/AI_MAP.md + docs/AI_STANDARDS.md + docs/AI_SECURITY.md`
- 文档治理/多人协作：`docs/AI_BOOT.md + docs/AI_MAP.md + docs/AI_STANDARDS.md + docs/AI_GOVERNANCE.md`
- 快速起任务：在以上任一组合追加 `docs/AI_PROMPTS.md`
- **多库 / 方言 / Wrapper / Provider / 换库排查**：`docs/AI_BOOT.md` + `docs/AI_MAP.md` + `docs/AI_STANDARDS.md` + **`docs/AI_DATABASE.md`**（PostgreSQL 专项叠加 **`docs/AI_POSTGRESQL.md`**）
- 业务工程升级 autumn 版本：`docs/AI_BOOT.md + docs/AI_MAP.md + docs/AI_STANDARDS.md + docs/AI_UPGRADE.md`（必要时叠加 `docs/AI_POSTGRESQL.md`）

## 3. 标准引用顺序

- 第一步：`@docs/AI_BOOT.md`
- 第二步：`@docs/AI_MAP.md`
- 第三步：读取 `docs/AI_STANDARDS.md`（应用层强制规范），再按场景追加专项文档（如 `docs/AI_CODEGEN.md`、`docs/AI_CRYPTO.md`、`docs/AI_TEMPLATES.md`、`docs/AI_GOVERNANCE.md`、`docs/AI_SECURITY.md`、`docs/AI_PROMPTS.md` 等）
- 第四步：追加当前项目上下文（README + 目标模块目录）

## 4. 相对路径示例（可复制）

业务仓库与 **autumn 框架仓库并列**（例如 `../autumn`）时：

```md
请先读取：
- @../autumn/docs/AI_INDEX.md
- @../autumn/docs/AI_BOOT.md
- @../autumn/docs/AI_MAP.md
- @../autumn/docs/AI_STANDARDS.md
- （按需）@../autumn/docs/AI_DATABASE.md / @../autumn/docs/AI_POSTGRESQL.md / @../autumn/docs/AI_UPGRADE.md / @../autumn/docs/AI_CRYPTO.md / @../autumn/docs/AI_CODEGEN.md / @../autumn/docs/AI_TEMPLATES.md / @../autumn/docs/AI_GOVERNANCE.md / @../autumn/docs/AI_SECURITY.md / @../autumn/docs/AI_PROMPTS.md
- @./README.md
- @./<目标模块目录>
```

仅在 **autumn 本仓库**内开发时，将上文 `../autumn/docs/` 改为 `@docs/` 即可。

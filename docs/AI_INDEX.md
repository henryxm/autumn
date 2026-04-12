# Autumn AI 文档总索引（统一入口）

> 用途：团队统一引用入口。先看本页，再按任务场景加载最小文档集合。

## 1. 文档职责一览

- `AI_BOOT.md`：最小启动上下文（首轮必读）
- `AI_MAP.md`：高频开发能力主图（含生成模板分层与可改/不可改边界）
- `AI_STANDARDS.md`：**约束性开发规范**（分层、API、定时任务、权限、FTL、**实体/注解建表/禁止初始化 DDL**、模块表前缀、**Dao+Provider**、**Controller–Service–Dao**、**statics/pages/Site/PageAware**）
- **`AI_DATABASE.md`**：**多数据库落地规范**（已支持 `DatabaseType` 清单、**全库兼容默认**、**Wrapper 安全边界**、**Dao+Provider 强制与推荐分层**、`RuntimeSql` 使用纪律；**§8 老旧注解 Dao / 方言化 Wrapper 升级与一键体检策略**）
- `AI_POSTGRESQL.md`：PostgreSQL 专项（DDL/元数据、`PostgresQuerySql`、迁移与兼容性）；通用跨库口径以 **`AI_DATABASE.md`** 为准
- `AI_UPGRADE.md`：依赖方升级 autumn 时的清单、一键扫描脚本说明与自动化边界
- `AI_CRYPTO.md`：接口加解密兼容与迁移
- `AI_SECURITY.md`：安全强校验、灰度、演练
- `AI_CODEGEN.md`：**代码生成链路（gen / GenUtils / 模板 / 库表反射）**与**推荐三步开发流程**（实体 → 生成骨架 → 业务与页面）；**`BaseCacheService` / `ShareCacheService` / `BaseQueueService`** 能力说明
- `AI_TEMPLATES.md`：模块任务模板库
- `AI_GOVERNANCE.md`：治理协作、术语、维护口径
- `AI_PROMPTS.md`：可复制提示词模板
- `AI_GUIDE.md`：多项目导航与引用方式

## 2. 推荐加载矩阵（按场景）

- 日常开发：`AI_BOOT.md + AI_MAP.md + AI_STANDARDS.md`（涉及 SQL/Wrapper/多库时追加 **`AI_DATABASE.md`**）
- 新模块/代码生成：`AI_BOOT.md + AI_MAP.md + AI_STANDARDS.md + AI_CODEGEN.md + AI_TEMPLATES.md`（先读代码生成流程与三步节奏，再确认生成层约束，后落业务层）
- 多项目模板整合（TemplateFactory）：`AI_BOOT.md + AI_MAP.md + AI_STANDARDS.md + AI_GOVERNANCE.md`
- 接口加解密改造：`AI_BOOT.md + AI_MAP.md + AI_STANDARDS.md + AI_CRYPTO.md`
- 安全改造/攻防演练：`AI_BOOT.md + AI_MAP.md + AI_STANDARDS.md + AI_SECURITY.md`
- 文档治理/多人协作：`AI_BOOT.md + AI_MAP.md + AI_STANDARDS.md + AI_GOVERNANCE.md`
- 快速起任务：在以上任一组合追加 `AI_PROMPTS.md`
- **多库 / 方言 / Wrapper / Provider / 换库排查**：`AI_BOOT.md` + `AI_MAP.md` + `AI_STANDARDS.md` + **`AI_DATABASE.md`**（PostgreSQL 专项叠加 **`AI_POSTGRESQL.md`**）
- 业务工程升级 autumn 版本：`AI_BOOT.md + AI_MAP.md + AI_STANDARDS.md + AI_UPGRADE.md`（必要时叠加 `AI_POSTGRESQL.md`）

## 3. 标准引用顺序

- 第一步：`@AI_BOOT.md`
- 第二步：`@AI_MAP.md`
- 第三步：读取 `AI_STANDARDS.md`（应用层强制规范），再按场景追加专项文档（`CODEGEN/CRYPTO/TEMPLATES/GOVERNANCE/SECURITY/PROMPTS` 等）
- 第四步：追加当前项目上下文（README + 目标模块目录）

## 4. 相对路径示例（可复制）

```md
请先读取：
- @../autumn/AI_INDEX.md
- @../autumn/AI_BOOT.md
- @../autumn/AI_MAP.md
- @../autumn/AI_STANDARDS.md
- （按需）@../autumn/**AI_DATABASE.md** / @../autumn/AI_POSTGRESQL.md / @../autumn/AI_UPGRADE.md / @../autumn/AI_CRYPTO.md / @../autumn/AI_CODEGEN.md / @../autumn/AI_TEMPLATES.md / @../autumn/AI_GOVERNANCE.md / @../autumn/AI_SECURITY.md / @../autumn/AI_PROMPTS.md
- @./README.md
- @./<目标模块目录>
```

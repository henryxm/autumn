# Autumn AI 文档总索引（统一入口）

> 用途：团队统一引用入口。先看本页，再按任务场景加载最小文档集合。

## 1. 文档职责一览

- `AI_BOOT.md`：最小启动上下文（首轮必读）
- `AI_MAP.md`：高频开发能力主图（含生成模板分层与可改/不可改边界）
- `AI_STANDARDS.md`：**约束性开发规范**（分层、API、定时任务、权限、FTL、**实体/注解建表/禁止初始化 DDL**、模块表前缀、**Dao+Provider**、**Controller–Service–Dao**、**statics/pages/Site/PageAware**）
- `AI_POSTGRESQL.md`：PostgreSQL 支持方案、变更清单、兼容性与迁移策略（多库适配）
- `AI_UPGRADE.md`：依赖方升级 autumn 时的清单、一键扫描脚本说明与自动化边界
- `AI_CRYPTO.md`：接口加解密兼容与迁移
- `AI_SECURITY.md`：安全强校验、灰度、演练
- `AI_TEMPLATES.md`：模块任务模板库
- `AI_GOVERNANCE.md`：治理协作、术语、维护口径
- `AI_PROMPTS.md`：可复制提示词模板
- `AI_GUIDE.md`：多项目导航与引用方式

## 2. 推荐加载矩阵（按场景）

- 日常开发：`AI_BOOT.md + AI_MAP.md + AI_STANDARDS.md`
- 新模块/代码生成：`AI_BOOT.md + AI_MAP.md + AI_STANDARDS.md + AI_TEMPLATES.md`（先确认生成层约束，再落业务层）
- 多项目模板整合（TemplateFactory）：`AI_BOOT.md + AI_MAP.md + AI_STANDARDS.md + AI_GOVERNANCE.md`
- 接口加解密改造：`AI_BOOT.md + AI_MAP.md + AI_STANDARDS.md + AI_CRYPTO.md`
- 安全改造/攻防演练：`AI_BOOT.md + AI_MAP.md + AI_STANDARDS.md + AI_SECURITY.md`
- 文档治理/多人协作：`AI_BOOT.md + AI_MAP.md + AI_STANDARDS.md + AI_GOVERNANCE.md`
- 快速起任务：在以上任一组合追加 `AI_PROMPTS.md`
- PostgreSQL / 方言与类型兼容排查：`AI_BOOT.md + AI_MAP.md + AI_STANDARDS.md + AI_POSTGRESQL.md`
- 业务工程升级 autumn 版本：`AI_BOOT.md + AI_MAP.md + AI_STANDARDS.md + AI_UPGRADE.md`（必要时叠加 `AI_POSTGRESQL.md`）

## 3. 标准引用顺序

- 第一步：`@AI_BOOT.md`
- 第二步：`@AI_MAP.md`
- 第三步：读取 `AI_STANDARDS.md`（应用层强制规范），再按场景追加专项文档（`CRYPTO/TEMPLATES/GOVERNANCE/SECURITY/PROMPTS`）
- 第四步：追加当前项目上下文（README + 目标模块目录）

## 4. 相对路径示例（可复制）

```md
请先读取：
- @../autumn/AI_INDEX.md
- @../autumn/AI_BOOT.md
- @../autumn/AI_MAP.md
- @../autumn/AI_STANDARDS.md
- （按需）@../autumn/AI_POSTGRESQL.md / @../autumn/AI_UPGRADE.md / @../autumn/AI_CRYPTO.md / @../autumn/AI_TEMPLATES.md / @../autumn/AI_GOVERNANCE.md / @../autumn/AI_SECURITY.md / @../autumn/AI_PROMPTS.md
- @./README.md
- @./<目标模块目录>
```

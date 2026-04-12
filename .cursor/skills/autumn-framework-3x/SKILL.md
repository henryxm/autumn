---
name: autumn-framework-3x
description: >-
  Autumn 3.0.0 line ONLY: JDK 17+, Spring Boot 3.5.10, MyBatis-Plus 3.x, jakarta.* namespace.
  Use on autumn branch 3.0.0 (artifact 3.0.0) or business apps pinned to that stack.
  NOT for Autumn 2.0.0 / JDK 8 / Spring Boot 2.7 / javax-only — use autumn-framework-2x on master.
  Shiro uses jakarta classifier; Druid mybatis-plus-spring-boot3-starter; SpringDoc OpenAPI (not Springfox).
  Enforces docs/AI_STANDARDS.md + docs/AI_DATABASE.md: entity-driven schema; Dao via Provider (*DaoSql extends RuntimeSql);
  Controller must not use Dao; Service uses baseMapper; gen/Pages/list.html/js never hand-edited; statics/pages/Site/PageAware.
  Read docs/AI_CODEGEN.md, docs/AI_DATABASE.md; scripts/autumn-dependency-scan.sh for upgrades.
  Triggers on cn.org.autumn 3.0.0, Spring Boot 3.5, JDK 17, ModuleService, RuntimeSql, PageAware, SpringDoc.
---

# Autumn 3.x 框架开发（3.0.0 / `3.0.0` 分支）

## 版本矩阵（本 Skill 唯一适用）

| 项 | 版本 / 约束 |
|----|-------------|
| **Autumn** | **3.0.0**（`cn.org.autumn:*:3.0.0`，**`3.0.0` Git 分支**） |
| **JDK** | **17+**（父 POM `java.version`，勿按 JDK 8 语法或依赖写本线） |
| **Spring Boot** | **3.5.10**（`spring-boot-starter-parent`） |
| **MyBatis-Plus** | **3.5.x**（`mybatis-plus-spring-boot3-starter`、`mybatis-plus-jsqlparser`；配置见 `application.yml` 中 **`mybatis-plus`** 段） |
| **命名空间** | **`jakarta.*`**（Servlet、Validation 等；**非** `javax.servlet` 新业务代码） |
| **Shiro** | **2.x + `jakarta` classifier**（`shiro-core` / `shiro-web` / `shiro-spring`） |
| **API 文档** | **SpringDoc（OpenAPI 3）**，非 Springfox |
| **JSON** | 优先 **Fastjson2** + `fastjson2-extension-spring6`（非 1.x `fastjson`） |
| **2.x 线** | **禁用本 Skill**：**master / 2.0.0**、JDK **8**、Boot **2.7** 请用 **`autumn-framework-2x`** |

业务工程须在 `AGENTS.md` 或首轮对话中写明依赖的 Autumn 主版本，避免 2.x / 3.x 规范混用。

## 何时启用

- 当前工作区为 **autumn 且检出 `3.0.0` 分支**，或业务工程 **Maven 依赖锁定 `cn.org.autumn` 3.0.0**。
- 提到上述技术栈且需 **Jakarta / MP3 / Spring Boot 3** 行为时。

## 文档加载顺序

所有 `AI_*.md` 均在仓库 **`docs/`** 下。本仓库内用 `@docs/...`；业务工程与 autumn 并列时用 `@../autumn/docs/...`（见 `docs/AI_INDEX.md` §4）。

1. `docs/AI_INDEX.md` → 2. `docs/AI_BOOT.md` → 3. `docs/AI_MAP.md` → 4. **`docs/AI_STANDARDS.md`**  
5. **`docs/AI_DATABASE.md`**  
6. 新模块 / 代码生成：追加 **`docs/AI_CODEGEN.md`**  
按需：`docs/AI_POSTGRESQL.md`、`docs/AI_TEMPLATES.md`、`docs/AI_CRYPTO.md` 等。

**注意**：文档若出现与 **Boot 2 / MP2** 绑定的旧配置键名，以实现代码与 **当前分支 `application.yml` + `pom.xml`** 为准。

## 规范开发三步（与 `docs/AI_CODEGEN.md` 一致）

1. **先实体**：`docs/AI_STANDARDS.md` + **`@Cache` / `@Caches`**；**`ModuleService`** 继承链。  
2. **再生成**：后台 ZIP 与 **`GenUtils.getFileName`** 一致；仅非 gen 空壳写业务。  
3. **后业务**：**Service** 承载规则；**Controller** URL 隔离；**`pages` + `site/*Site` + `@PageAware`**。

## 实体与数据库（§8～§10）

- 与 2.x 相同纪律：**`autumn.table.*`**、禁止常规 **`DDL .sql`**、模块目录 = 表前缀、**`docs/AI_BOOT.md` §3.2**、**`@Index` 与 `isUnique` 不叠用**。

## 多库与 SQL（与 `docs/AI_DATABASE.md` 一致）

- **Dao**：**Provider + `RuntimeSql`**；**Wrapper** 安全边界；升级体检 **`scripts/autumn-dependency-scan.sh`** + **`docs/AI_UPGRADE.md`**。

## 生成层与业务（§11）

- **gen / `*Pages` / list.html/js** 禁止手改；改 **`template/*.vm`** 后重生成。

## SQL 与 Dao（§12～§13）

- **Controller** 禁止 **Dao**；**Service** 用 **`baseMapper`**；跨域只调 **其他 Service**。

## 资源与页面（§14）

- **`statics/`**、**`pages/`**、**`@PageAware`**。

## 自检清单

- 未误用 **`javax.servlet`** 新业务包名？未混用 **Springfox**？  
- **`mybatis-plus`** 配置与 **MP3** 文档一致？  
- 其余同 2.x：**Dao**、**gen**、**Site**、**用户视角文案**。

## 多项目一句话

**`docs/AI_BOOT.md` → `docs/AI_MAP.md` → `docs/AI_STANDARDS.md` → `docs/AI_DATABASE.md` → `docs/AI_CODEGEN.md` → …**（**仅 3.0.0 / JDK17+ / Boot 3.5 / Jakarta / MP3 栈**）。

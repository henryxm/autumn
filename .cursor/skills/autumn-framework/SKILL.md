---
name: autumn-framework
description: >-
  Guides Autumn Java admin development (Spring Boot 3, MyBatis-Plus, Shiro,
  ModuleService, codegen). Enforces AI_STANDARDS: entity-driven schema via
  framework scan (no routine DDL .sql files); module dir = package = table
  prefix (do not prefix entity class names with it); table/field comments with
  half-width colon; no @Index with @Column isUnique on same field; prefer
  primitives with defaults; never edit gen/SitePages-generated Pages/html/js;
  implement in empty shell Controller/Service/Dao; new SQL only via MyBatis
  Provider not inline on Dao; Controller must not use Dao; Service uses
  baseMapper not @Autowired own Dao; inject other Services not other Daos;
  statics/ for shared static assets; new admin pages under module pages/ with
  site/*Site @PageAware(login true/false); plus prior rules (Request/Response,
  no @Scheduled production, no @RequiresPermissions on new APIs, FreeMarker
  HTML/JS, LoopJob). Use for autumn repo or dependents, or mentions of
  cn.org.autumn, gen, Provider, statics, PageAware.
---

# Autumn 框架开发（全局）

## 何时启用

- 仓库为 **autumn** 或依赖它的业务工程。
- 提到：`cn.org.autumn`、`ModuleService`、`gen`、`Dao`、`Provider`、`statics`、`pages`、`Site`、`PageAware`、注解建表、`autumn.table` 等。

## 文档加载顺序

1. `AI_INDEX.md` → 2. `AI_BOOT.md` → 3. `AI_MAP.md` → 4. **`AI_STANDARDS.md`**（强制全文，含 §8～§14 实体/库/SQL/资源）  
按需：`AI_POSTGRESQL.md`（Provider/RuntimeSql）、`AI_TEMPLATES.md`、`AI_CRYPTO.md` 等。

## 实体与数据库（§8～§10）

- 框架**扫描已加载实体**，依 **`autumn.table.*` 开关**在启动期**检测并更新表**。**禁止**把**常规初始化/演进用的 `DDL .sql`** 当作默认交付物。
- **`modules/<子目录>/`**：**目录名 = 包段 = 表前缀**。**禁止**把该前缀再拼到**实体类名开头**（避免生成错乱）。物理表名惯例 **`前缀_实体蛇形`**，细节 **`AI_BOOT.md` §3.2**。
- **`@Table` / `@Column.comment`**：短标题 **2～4 字为宜**（建议不超过约 **8** 字）+ **半角 `:`** + 说明。**禁止**同字段 **`@Index` + `@Column(isUnique=true)`** 叠用。
- 整型/布尔：**优先基本类型 + `@Column` 默认值**，减少 `null`。

## 生成层与业务（§11）

- **`controller/gen/*`**、**`SitePages.java.vm`** 生成的 **`*Pages.java`**、**`list.html` / `list.js`**：**禁止修改、禁止加逻辑**；改 **`template/*.vm`** 后重生成。
- 生成给出的**空壳** **`Controller/Service/Dao`（非 gen）**：在此实现业务；**逻辑在 Service**。

## SQL 与 Dao（§12～§13）

- **禁止**在 **Dao 接口**上用注解**内联硬编码 SQL**（新代码）；**必须**用 **Provider**（`*DaoSql` 等），多库配合 **`AI_POSTGRESQL.md` / `RuntimeSql`**。
- **Controller**：**禁止**使用 **Dao**，只调 **Service**。
- **Service**：用继承的 **`baseMapper`**，**不要** **`@Autowired`** 本实体 **Dao**。
- **跨实体**：**注入其他 Service**，**不要**注入**其他实体的 Dao**；**禁止**在非所属 Service 中直接使用某 **Dao**。

## 资源与页面（§14）

- 资源随**本项目 jar**；**`resources/statics/`**：公共静态，**匿名可访问**；**优先复用**框架已有文件。
- 新后台页：模块资源下 **`pages/`**（无则建）；在 **`site/*Site.java`** 增加字段并标 **`@PageAware`**：`login = false` 匿名，`login = true` 需登录，供 **SPM** 注册路径（见 `cn.org.autumn.annotation.PageAware`）。

## 既有纪律（摘要）

- **§2～§7**：分层、内外 API、**禁止生产 `@Scheduled`**、新接口**不用 `@RequiresPermissions`**（登录态鉴权）、**FreeMarker**（`<!-- -->`、JS 安全、`<#noparse>`）。
- **框架能力**：`ModuleService` 链、缓存队列、**`LoopJob`**、加解密见 **`AI_MAP.md`**。

## 自检清单

- 是否新增无意义的 **`schema.sql` / `init.sql`**？  
- 新 SQL 是否误写在 **Dao 注解**上？ **Controller** 是否碰了 **Dao**？  
- 是否改了 **gen** 或生成的 **html/js**？  
- 新页面是否有 **`Site` + `@PageAware`**？ **`statics`** 是否重复造轮子？

## 多项目一句话

**BOOT → MAP → STANDARDS → 专项 → README / 模块目录 → 任务约束**。

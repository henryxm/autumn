---
name: autumn-framework
description: >-
  Guides Autumn Java admin development (Spring Boot 3, MyBatis-Plus, Shiro,
  ModuleService, codegen). Enforces AI_STANDARDS + AI_DATABASE: entity-driven
  schema via framework scan (no routine DDL .sql files); module dir = package =
  table prefix (do not prefix entity class names with it); table/field comments
  with half-width colon; no @Index with @Column isUnique on same field; prefer
  primitives with defaults; never edit gen/SitePages-generated Pages/html/js;
  implement in empty shell Controller/Service/Dao; all new Dao SQL only via MyBatis
  Provider (*DaoSql extends RuntimeSql), never inline @Select/@Update on Dao;
  default all business SQL must be compatible with every supported DatabaseType
  unless explicitly documented single-DB; Wrapper only safe predicates (eq/range/in/null/orderBy);
  use Dao+Provider instead of Wrapper for functions/joins/FIND_IN_SET/LIKE patterns—use
  RuntimeSql quote/likeContainsAny/columnValueInCommaSeparatedList; Controller must not use Dao;
  Service uses baseMapper not @Autowired own Dao; inject other Services not other Daos;
  statics/ for shared static assets; new admin pages under module pages/ with site/*Site
  @PageAware(login true/false); plus prior rules (Request/Response, no @Scheduled production,
  no @RequiresPermissions on new APIs, FreeMarker HTML/JS, LoopJob). Read AI_CODEGEN for gen
  pipeline (GenUtils, templates, DB reflection) and three-step workflow (entities → codegen →
  business). Read AI_DATABASE for DatabaseType list, cross-DB standards, and §8 legacy Dao/Wrapper
  migration (autumn-dependency-scan.sh). Use for autumn repo or dependents, or mentions of
  cn.org.autumn, gen, Provider, statics, PageAware, RuntimeSql.
---

# Autumn 框架开发（全局）

## 何时启用

- 仓库为 **autumn** 或依赖它的业务工程。
- 提到：`cn.org.autumn`、`ModuleService`、`gen`、`Dao`、`Provider`、`RuntimeSql`、`DatabaseType`、`statics`、`pages`、`Site`、`PageAware`、注解建表、`autumn.table` 等。

## 文档加载顺序

1. `AI_INDEX.md` → 2. `AI_BOOT.md` → 3. `AI_MAP.md` → 4. **`AI_STANDARDS.md`**（强制全文，含 §8～§14 实体/库/SQL/资源）  
5. **`AI_DATABASE.md`**（**多库落地**：`DatabaseType` 清单、**全库兼容默认**、**Wrapper 安全边界**、**Dao 必须 Provider**、推荐 A～D 分层、`RuntimeSql` 纪律）  
6. **新模块 / 代码生成 / AI 搭骨架**：追加 **`AI_CODEGEN.md`**（`GeneratorService` → `TableDao` → **`GenUtils`** → `resources/template/*.vm`；优先后台生成 ZIP；三步：实体与 `@Cache` → 生成空壳 → 业务与页面；`BaseCacheService` / `ShareCacheService` / `BaseQueueService` 能力，禁止重复造轮子）  
按需：`AI_POSTGRESQL.md`（PG 专项 DDL/元数据/Provider 示例）、`AI_TEMPLATES.md`、`AI_CRYPTO.md` 等。

## 规范开发三步（与 AI_CODEGEN 一致）

1. **先实体**：按 `AI_STANDARDS` 建实体与索引注释；用 **`@Cache` / `@Caches`**（`name` 区分多套缓存）；业务 Service 继承 **`ModuleService`**，用继承链上的缓存/队列/`LoopJob`，勿自建平行组件。
2. **再生成**：**优先开发者**在后台「代码生成」导出 ZIP，路径与 **`GenUtils.getFileName`** 一致；**仅**在非 gen 空壳里写业务；**禁止**在 `controller/gen`、`*Pages`、`list.html/js` 上堆逻辑。若 AI 生成骨架，须与模板产出**同目录同结构**，避免日后重生成不一致。
3. **后业务**：`Service` 实现规则与失效；可维护 `Controller` 独立 URL 空间；`pages` + `site/*Site` + `@PageAware` 接页面。

## 实体与数据库（§8～§10）

- 框架**扫描已加载实体**，依 **`autumn.table.*` 开关**在启动期**检测并更新表**。**禁止**把**常规初始化/演进用的 `DDL .sql`** 当作默认交付物。
- **`modules/<子目录>/`**：**目录名 = 包段 = 表前缀**。**禁止**把该前缀再拼到**实体类名开头**（避免生成错乱）。物理表名惯例 **`前缀_实体蛇形`**，细节 **`AI_BOOT.md` §3.2**。
- **`@Table` / `@Column.comment`**：短标题 **2～4 字为宜**（建议不超过约 **8** 字）+ **半角 `:`** + 说明。**禁止**同字段 **`@Index` + `@Column(isUnique=true)`** 叠用。
- 整型/布尔：**优先基本类型 + `@Column` 默认值**，减少 `null`。

## 多库与 SQL（与 `AI_DATABASE.md` 一致）

- **默认**：业务 SQL、Wrapper 条件与查询结果应对框架已支持的 **`DatabaseType` 全量兼容**；单库例外须在 JavaDoc 标明「仅某某库」。
- **Dao**：**禁止**在 Dao 上用 **`@Select`/`@Update` 等注解内联 SQL**（新代码）；**必须** **`@SelectProvider` 等 + `*DaoSql`**，推荐 **`extends RuntimeSql`**，用 **`quote`、`limitOne`、`likeContainsAny`、`columnValueInCommaSeparatedList`、`enabledTrueSqlLiteral`**，**禁止**手写单库引号或 `FIND_IN_SET` 等裸函数。
- **Wrapper**：仅用等值/范围/`in`/空值/简单 `orderBy`；保留字列用 **`columnInWrapper`**；**禁止** `apply` 塞单库函数；JOIN/报表/复杂模糊/列表成员判断 → **Dao + Provider**（见 **`AI_DATABASE.md` §4～§5 推荐分层 A～D**）。
- **老旧项目升级**：存量 **`@Select`/`@Update`/`@Delete`/`@Insert` 内联字符串**、**Wrapper 反引号/`apply`/`last` 拼方言** → 按 **`AI_DATABASE.md` §8** 迁 Provider + `RuntimeSql`；**一键体检**用 **`scripts/autumn-dependency-scan.sh`** + **`AI_UPGRADE.md` §3.3**（扫描只读，改造分批人工）。

## 生成层与业务（§11）

- **`controller/gen/*`**、**`SitePages.java.vm`** 生成的 **`*Pages.java`**、**`list.html` / `list.js`**：**禁止修改、禁止加逻辑**；改 **`template/*.vm`** 后重生成。
- 生成给出的**空壳** **`Controller/Service/Dao`（非 gen）**：在此实现业务；**逻辑在 Service**。

## SQL 与 Dao（§12～§13，摘要）

- **禁止**在 **Dao 接口**上用注解**内联硬编码 SQL**（新代码）；**必须**用 **Provider**（`*DaoSql` 等），多库配合 **`AI_DATABASE.md`**、**`RuntimeSql`**、**`AI_POSTGRESQL.md`**（PG 专项）。
- **Controller**：**禁止**使用 **Dao**，只调 **Service**。
- **Service**：用继承的 **`baseMapper`**，**不要** **`@Autowired`** 本实体 **Dao**。
- **跨实体**：**注入其他 Service**，**不要**注入**其他实体的 Dao**；**禁止**在非所属 Service 中直接使用某 **Dao**。

## 资源与页面（§14）

- 资源随**本项目 jar**；**`resources/statics/`**：公共静态，**匿名可访问**；**优先复用**框架已有文件。
- 新后台页：模块资源下 **`pages/`**（无则建）；在 **`site/*Site.java`** 增加字段并标 **`@PageAware`**：`login = false` 匿名，`login = true` 需登录，供 **SPM** 注册路径（见 `cn.org.autumn.annotation.PageAware`）。

## 既有纪律（摘要）

- **§2～§7**：分层、内外 API、**禁止生产 `@Scheduled`**、新接口**不用 `@RequiresPermissions`**（登录态鉴权）、**FreeMarker**（`<!-- -->`、JS 安全、`<#noparse>`）。
- **页面文案硬约束**：除非需求明确特殊说明，页面描述必须使用**用户视角**；禁止出现开发术语、后台表名/字段名/函数名、技术架构与程序员视角表达。
- **框架能力**：`ModuleService` 链、缓存队列、**`LoopJob`**、加解密见 **`AI_MAP.md`**；缓存/队列/复合键细节见 **`AI_CODEGEN.md`** 第 4 节。

## 自检清单

- 是否新增无意义的 **`schema.sql` / `init.sql`**？  
- 新 SQL 是否误写在 **Dao 注解**上？是否应用 **`RuntimeSql`** 做跨库片段？**Wrapper** 是否含单库函数或 `apply` 黑魔法？  
- **Controller** 是否碰了 **Dao**？  
- 是否改了 **gen** 或生成的 **html/js**？  
- 新页面是否有 **`Site` + `@PageAware`**？ **`statics`** 是否重复造轮子？  
- 页面描述是否全程使用**用户视角**，且未出现开发术语、后台表名/函数名、架构实现描述？  
- 生成骨架是否由后台或**与 GenUtils 路径一致**的 AI 产出，避免双轨目录？

## 多项目一句话

**BOOT → MAP → STANDARDS → DATABASE（多库 SQL）→ CODEGEN（生成场景）→ 专项 → README / 模块目录 → 任务约束**。

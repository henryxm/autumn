---
name: autumn-framework-2x
description: >-
  Autumn 2.0.0 line ONLY: JDK 8, Spring Boot 2.7.18, MyBatis-Plus 2.x, javax.* namespace.
  Use on autumn master (artifact 2.0.0) or business apps pinned to that stack.
  NOT for Autumn 3.0.0 / JDK 17+ / Spring Boot 3.5 / Jakarta — use autumn-framework-3x on the 3.0.0 branch when available.
  Enforces docs/AI_STANDARDS.md + docs/AI_DATABASE.md: entity-driven schema via framework scan (no routine DDL .sql);
  module dir = package = table prefix; Dao SQL only via MyBatis Provider (*DaoSql extends RuntimeSql), never inline @Select/@Update;
  No hardcoded SQL dialect quotes in Java (use RuntimeSql quote/columnInWrapper or WrapperColumns.columnInWrapper / orderByColumnExpression / entityWrapperAllEqQuoted per docs/AI_DATABASE.md §4.0);
  Controller must not use Dao; Service uses baseMapper; gen/Pages/list.html/js never hand-edited; statics/pages/Site/PageAware.
  Read docs/AI_CODEGEN.md, docs/AI_DATABASE.md; scripts/autumn-dependency-scan.sh for upgrades.
  Triggers on cn.org.autumn 2.0.0, Spring Boot 2.7, JDK 8, ModuleService, RuntimeSql, PageAware.
---

# Autumn 2.x 框架开发（2.0.0 / master）

## 版本矩阵（本 Skill 唯一适用）

| 项 | 版本 / 约束 |
|----|-------------|
| **Autumn** | **2.0.0**（`cn.org.autumn:*:2.0.0`，**master** 分支） |
| **JDK** | **1.8**（勿按 JDK 17+ / `var` / 新 API 写法改本线代码） |
| **Spring Boot** | **2.7.18**（父 POM `spring-boot-starter-parent`） |
| **MyBatis-Plus** | **2.x**（与 Boot 2.7 栈一致；以根 `pom.xml` 属性为准） |
| **Java EE 命名空间** | **`javax.*`**（非 `jakarta.*`） |
| **3.x 线** | **禁用本 Skill**：Autumn **3.0.0**、JDK **17+**、Spring Boot **3.5.x** 等请用 **`autumn-framework-3x`**（在 **3.0.0** 分支合并并实现后使用） |

业务工程须在 `AGENTS.md` 或首轮对话中写明依赖的 Autumn 主版本，避免 2.x / 3.x 规范混用。

## 何时启用

- 当前工作区是 **autumn 仓库且检出 master（或维护 2.0.0 标签/分支）**，或业务工程 **Maven 依赖锁定 `cn.org.autumn` 2.0.0**。
- 提到：`cn.org.autumn`、`ModuleService`、`gen`、`Dao`、`Provider`、`RuntimeSql`、`DatabaseType`、`statics`、`pages`、`Site`、`PageAware`、`autumn.table` 等，且 **栈为 JDK8 + Boot 2.7**。

## 文档加载顺序

所有 `AI_*.md` 均在仓库 **`docs/`** 下。本仓库内用 `@docs/...`；业务工程与 autumn 并列时用 `@../autumn/docs/...`（见 `docs/AI_INDEX.md` §4）。

1. `docs/AI_INDEX.md` → 2. `docs/AI_BOOT.md` → 3. `docs/AI_MAP.md` → 4. **`docs/AI_STANDARDS.md`**（强制全文，含 §8～§14）  
5. **`docs/AI_DATABASE.md`**（多库、`DatabaseType`、**§4.0 代码层方言标准写法**、`WrapperColumns`、`RuntimeSql`、Wrapper 边界、Dao **必须** Provider）  
6. 新模块 / 代码生成 / 搭骨架：追加 **`docs/AI_CODEGEN.md`**  
按需：`docs/AI_POSTGRESQL.md`、`docs/AI_TEMPLATES.md`、`docs/AI_CRYPTO.md`、`docs/AI_DISTRIBUTED_LOCK.md`、**`docs/REDIS_RESILIENCE.md`**（Redis 熔断与分布式锁稳健性）、`docs/REDIS_STANDALONE.md`、**`docs/INSTALL_MODE_CONDITIONAL.md`**（安装向导 **`autumn.install.wizard`**、**§0 占位默认 H2 / 可选 mysql**）等。

## 规范开发三步（与 `docs/AI_CODEGEN.md` 一致）

1. **先实体**：按 `docs/AI_STANDARDS.md` 建实体与索引注释；**`@Cache` / `@Caches`**；业务 Service 继承 **`ModuleService`**，勿自建平行缓存/队列/`LoopJob`。
2. **再生成**：后台「代码生成」ZIP 与 **`GenUtils.getFileName`** 路径一致；仅在非 gen 空壳写业务；**禁止**改 `controller/gen`、`*Pages`、`list.html/js`。
3. **后业务**：`Service` 承载规则与事务；可维护 `Controller` 独立 URL；`pages` + `site/*Site` + `@PageAware`。

## 实体与数据库（§8～§10）

- 框架扫描实体 + **`autumn.table.*`** 启动期对齐表结构；**禁止**把常规 **`DDL .sql`** 当默认交付物。
- **`modules/<子目录>/`**：目录名 = 包段 = 表前缀；**禁止**把前缀拼进实体类名；物理表名见 **`docs/AI_BOOT.md` §3.2**。
- **`@Table` / `@Column.comment`**：短标题 + **半角 `:`**；**禁止**同列 **`@Index` + `@Column(isUnique=true)`** 叠用。
- 整型/布尔：优先基本类型 + 默认值。

## 多库与 SQL（与 `docs/AI_DATABASE.md` 一致）

- 默认业务 SQL / Wrapper 对 **`DatabaseType` 全量兼容**；单库例外须 JavaDoc 标明。
- **禁止硬编码方言**：表/列/排序/Map 键不写死 `` ` `` / `"` / `[]`；Provider 用 **`RuntimeSql#quote` / `columnInWrapper`**；未继承 **`DialectService`** 时用 **`WrapperColumns`**（**`columnInWrapper`**、分页排序 **`orderByColumnExpression`**、Map 等值 **`entityWrapperAllEqQuoted`** 等，见 **`docs/AI_DATABASE.md` §4.0**）。
- **Dao**：新代码 **禁止** 注解内联 SQL；**必须** `@SelectProvider` + **`*DaoSql`**，推荐 **`extends RuntimeSql`**。
- **Wrapper**：安全谓词 only；复杂场景 **Dao + Provider**（见 **`docs/AI_DATABASE.md` §4～§5**）。
- 升级体检：**`scripts/autumn-dependency-scan.sh`** + **`docs/AI_UPGRADE.md` §3.3**；存量 Wrapper/Service 手写引号对照 **`docs/AI_DATABASE.md` §8.1** 与 **§8.5** `rg` 示例。

## 生成层与业务（§11）

- **`controller/gen/*`**、**`SitePages.java.vm`** 产物：**禁止**改逻辑；改 **`template/*.vm`** 后重生成。
- 空壳 **Controller/Service/Dao（非 gen）**：业务落 **Service**。

## SQL 与 Dao（§12～§13，摘要）

- **Provider**  mandatory for new SQL；配合 **`docs/AI_DATABASE.md`**、**`docs/AI_POSTGRESQL.md`**（PG）。
- **Controller** 禁止 **Dao**；**Service** 用 **`baseMapper`**，勿 **`@Autowired`** 本 **Dao**；跨域只注入 **其他 Service**。

## 分布式执行与加锁（新增）

- 涉及跨节点互斥、定时任务防重入、热点写入串行化时，必须优先复用框架锁能力。
- **已继承 `ModuleService` / `BaseService`**：直接使用 **`DistributedService`** 的 `withLock*` 能力（`withLock` / `withLockRetry` / `withLockOrFallback`）。
- **未继承框架基础能力**（独立组件、监听器、过滤器等）：直接注入 **`DistributedLockService`**。
- 配置来源统一走后台 **`DistributedLockConfig`**（键 `DISTRIBUTED_LOCK_CONFIG`），通过 `sysConfigService.getObject(...)` 获取对象配置，不走环境变量。
- **Redis 韧性**：框架 Bean **`RedisResilience`**（`autumn.redis.resilience.*`）对 Redis/Redisson 基础设施失败做计数熔断；**`DistributedLockService`** 与 **`TagRunnable` / `LockOnce`** 已对齐（熔断 OPEN 默认跳过 tryLock、单机回退）。自定义 Redis 调用应优先 **`RedisResilience#execute`**；必读 **`docs/REDIS_RESILIENCE.md`**，与 **`docs/REDIS_STANDALONE.md`**（`autumn.redis.open`、安装向导）配合。
- **依赖方 / 兄弟模块**：未启用 Redis 时 **无 `RedisTemplate` Bean**；任何 **`@Autowired RedisTemplate`**（默认必填）会导致启动失败。使用 **`@Autowired(required = false)`** 或 **`ObjectProvider`**，业务路径 **`if (redisTemplate == null)`** 降级；见 **`docs/REDIS_STANDALONE.md` §6** 与 **`docs/AI_UPGRADE.md` §2.2 行 7**。
- 默认策略：严格模式优先（锁竞争失败抛错）；需要服务降级时显式使用 `withLockOrFallback` 或开启配置降级。
- 抗雪崩策略：锁竞争重试必须使用“有限重试 + 随机退避”（`withLockRetry`），禁止业务自旋热重试。

## 资源与页面（§14）

- **`resources/statics/`** 公共静态；新后台页 **`pages/`** + **`site/*Site`** + **`@PageAware`**。

## 既有纪律（摘要）

- **禁止生产 `@Scheduled`**；新接口 **不用 `@RequiresPermissions`**（登录态）；**FreeMarker** 安全（`<!-- -->`、`<#noparse>`）。
- 页面文案：**用户视角**，禁开发术语与表名字段名堆砌。
- 能力地图：**`docs/AI_MAP.md`**；缓存/队列细节：**`docs/AI_CODEGEN.md`** 第 4 节。

## 自检清单

- 无多余 **`schema.sql` / `init.sql`**？  
- Dao 无内联 SQL？**Wrapper** 无方言黑魔法？Java 无手写 **反引号/双引号/方括号** 拼 SQL（§4.0）？  
- **Controller** 未碰 **Dao**？未手改 **gen / list.html/js**？  
- 新页有 **`Site` + `@PageAware`**？  
- 生成路径与 **GenUtils** 一致？

## 多项目一句话

**`docs/AI_BOOT.md` → `docs/AI_MAP.md` → `docs/AI_STANDARDS.md` → `docs/AI_DATABASE.md` → `docs/AI_CODEGEN.md` → 专项 → README / 模块目录 → 任务约束**（**仅 2.0.0 / JDK8 / Boot 2.7 栈**）。

---
name: autumn-framework-3x
description: >-
  Autumn 3.0.0 line ONLY: JDK 17+, Spring Boot 3.5.10, MyBatis-Plus 3.x, jakarta.* namespace.
  Use on autumn branch 3.0.0 (artifact 3.0.0) or business apps pinned to that stack.
  NOT for Autumn 2.0.0 / JDK 8 / Spring Boot 2.7 / javax-only — use autumn-framework-2x on master.
  Shiro uses jakarta classifier; Druid mybatis-plus-spring-boot3-starter; SpringDoc OpenAPI (not Springfox).
  Enforces docs/AI_STANDARDS.md + docs/AI_DATABASE.md: entity-driven schema; Dao via Provider (*DaoSql extends RuntimeSql);
  No hardcoded dialect quotes in Java (RuntimeSql quote/columnInWrapper or WrapperColumns per docs/AI_DATABASE.md §4.0);
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
| **Spring Boot** | **3.5.10**（`spring-boot-starter-parent`；小版本以根 `pom.xml` 为准） |
| **MyBatis-Plus** | **3.5.x**（`mybatis-plus-spring-boot3-starter`、`mybatis-plus-jsqlparser`；配置见 `application.yml` 中 **`mybatis-plus`** 段） |
| **命名空间** | **`jakarta.*`**（Servlet、Validation 等；**非** `javax.servlet` 新业务代码） |
| **Shiro** | **2.x + `jakarta` classifier**（`shiro-core` / `shiro-web` / `shiro-spring`） |
| **API 文档** | **SpringDoc（OpenAPI 3）**，非 Springfox |
| **JSON** | 优先 **Fastjson2** + `fastjson2-extension-spring6`（非 1.x `fastjson`） |
| **2.x 线** | **禁用本 Skill**：**master / 2.0.0**、JDK **8**、Boot **2.7** 请用 **`autumn-framework-2x`** |

业务工程须在 `AGENTS.md` 或首轮对话中写明依赖的 Autumn 主版本，避免 2.x / 3.x 规范混用。

## 何时启用

- 当前工作区为 **autumn 且检出 `3.0.0` 分支**，或业务工程 **Maven 依赖锁定 `cn.org.autumn` 3.0.0**。
- 提到：`cn.org.autumn`、`ModuleService`、`RuntimeSql`、`DatabaseType`、`WrapperColumns`、`QueryWrapper`、`PageAware`、`SpringDoc` 等，且 **栈为 JDK 17+ + Boot 3.5 + Jakarta + MP3**。

## 文档加载顺序

所有 `AI_*.md` 均在仓库 **`docs/`** 下。本仓库内用 `@docs/...`；业务工程与 autumn 并列时用 `@../autumn/docs/...`（见 `docs/AI_INDEX.md` §4）。

1. `docs/AI_INDEX.md` → 2. `docs/AI_BOOT.md` → 3. `docs/AI_MAP.md` → 4. **`docs/AI_STANDARDS.md`**
5. **`docs/AI_DATABASE.md`**（含 **§4.0 代码层方言标准写法**、`WrapperColumns`、`RuntimeSql`、Wrapper 边界、Dao **必须** Provider；存量反模式 **§8.1**、检索 **§8.5**）
6. 新模块 / 代码生成：追加 **`docs/AI_CODEGEN.md`**
按需：`docs/AI_POSTGRESQL.md`、`docs/AI_UPGRADE.md`、`docs/AI_TEMPLATES.md`、`docs/AI_CRYPTO.md`、`docs/AI_DISTRIBUTED_LOCK.md`、**`docs/REDIS_RESILIENCE.md`**（Redis 熔断与分布式锁稳健性）、`docs/REDIS_STANDALONE.md` 等。

**注意**：文档若出现与 **Boot 2 / MP2** 绑定的旧配置键名，以实现代码与 **当前分支 `application.yml` + `pom.xml`** 为准。

## 规范开发三步（与 `docs/AI_CODEGEN.md` 一致）

1. **先实体**：`docs/AI_STANDARDS.md` + **`@Cache` / `@Caches`**；**`ModuleService`** 继承链。
2. **再生成**：后台 ZIP 与 **`GenUtils.getFileName`** 一致；仅非 gen 空壳写业务。
3. **后业务**：**Service** 承载规则；**Controller** URL 隔离；**`pages` + `site/*Site` + `@PageAware`**。

## 实体与数据库（§8～§10）

- 与 2.x 相同纪律：**`autumn.table.*`**、禁止常规 **`DDL .sql`**、模块目录 = 表前缀、**`docs/AI_BOOT.md` §3.2**、**`@Index` 与 `isUnique` 不叠用**。

## 多库与 SQL（与 `docs/AI_DATABASE.md` 一致）

- 默认业务 SQL / Wrapper 对 **`DatabaseType`** 全量兼容；单库例外须 JavaDoc 标明。
- **禁止硬编码方言**：表/列/排序/`QueryWrapper` 列参、Map 键不写死 `` ` `` / `"` / `[]`；见 **`docs/AI_DATABASE.md` §4.0**。
- **Dao**：**Provider + `RuntimeSql`**（`quote` / `columnInWrapper` 等 §2.1）；**禁止**注解内联 SQL。
- **未继承 `DialectService`** 的 Java 路径：**`WrapperColumns.columnInWrapper`**、分页排序 **`orderByColumnExpression`**、Map 等值 **`queryWrapperAllEqQuoted`**。
- 升级体检：**`scripts/autumn-dependency-scan.sh`** + **`docs/AI_UPGRADE.md` §3.3**；存量 Wrapper/Service 手写引号对照 **§8.1** 与 **§8.5** `rg` 示例。
- **Wrapper** 安全边界；复杂场景 **Dao + Provider**（**§4～§5**）。

## 生成层与业务（§11）

- **gen / `*Pages` / list.html/js** 禁止手改；改 **`template/*.vm`** 后重生成。

## SQL 与 Dao（§12～§13）

- **Provider** mandatory for new SQL；配合 `docs/AI_DATABASE.md`、`docs/AI_POSTGRESQL.md`。
- **Controller** 禁止 **Dao**；**Service** 用 **`baseMapper`**，勿注入本 Dao；跨域只注入其他 Service。

## 分布式执行与加锁（新增）

- 涉及跨节点任务互斥、热点写入串行化、任务防重入时，优先复用框架锁能力，不自建锁组件。
- **已继承 `ModuleService` / `BaseService`**：使用 **`DistributedService`** 的 `withLock*` 系列（严格/降级/重试）。
- **未继承基础框架能力**：直接注入 **`DistributedLockService`**。
- 配置统一通过后台 **`DistributedLockConfig`**（`DISTRIBUTED_LOCK_CONFIG`）管理，读取方式 `sysConfigService.getObject(...)`；强一致后台任务可配置 **`ignoreCircuitBreaker`**（见 **`docs/AI_DISTRIBUTED_LOCK.md`**）。
- **Redis 韧性（3.x 与 2.x 行为对齐）**：Bean **`RedisResilience`**（`autumn.redis.resilience.*`）对 Redis/Redisson 基础设施失败做计数熔断；**`DistributedLockService`** 与 **`TagRunnable` / `TagCallable` / `LockOnce`** 已对齐（熔断 OPEN 时在配置允许下跳过 tryLock、单机回退）；**`CacheService.publish`**、**`RedisListenerService`** 等路径配合成功/失败计数。自定义 Redis 调用优先 **`RedisResilience#execute`**；必读 **`docs/REDIS_RESILIENCE.md`**，并与 **`docs/REDIS_STANDALONE.md`**（`autumn.redis.open`、安装向导、无 `spring.redis` 场景）配合。
- 强一致场景默认严格失败；非强一致场景可用 `withLockOrFallback` 做服务降级。
- 并发突发场景必须使用 `withLockRetry` 的随机退避机制，避免锁竞争雪崩。

## 资源与页面（§14）

- **`resources/statics/`** 公共静态；新后台页 **`pages/`** + **`site/*Site`** + **`@PageAware`**。

## 既有纪律（摘要）

- 禁止生产 `@Scheduled`；新接口默认不加 `@RequiresPermissions`（登录态）。
- 页面文案用户视角，禁开发术语堆砌。
- 能力地图优先看 `docs/AI_MAP.md`；缓存/队列细节看 `docs/AI_CODEGEN.md`。

## 自检清单

- 未误用 **`javax.servlet`** 新业务包名？未混用 **Springfox**？
- **`mybatis-plus`** 配置与 **MP3** 文档一致？
- Dao 无内联 SQL？**无手写方言引号（§4.0）**？
- **Controller** 未碰 **Dao**？未手改 **gen / list.html/js**？
- 其余同 2.x：**Site**、**用户视角文案**。

## 多项目一句话

**`docs/AI_BOOT.md` → `docs/AI_MAP.md` → `docs/AI_STANDARDS.md` → `docs/AI_DATABASE.md`（含 §4.0、§8）→ `docs/AI_CODEGEN.md` → 专项 → README / 模块目录 → 任务约束**（**仅 3.0.0 / JDK17+ / Boot 3.5 / Jakarta / MP3 栈**）。

---
name: autumn-framework-3x
description: >-
  Autumn 3.0.0 line ONLY: JDK 17+, Spring Boot 3.5.10, MyBatis-Plus 3.x, jakarta.* namespace.
  Use on autumn branch 3.0.0 (artifact 3.0.0) or business apps pinned to that stack.
  NOT for Autumn 2.0.0 / JDK 8 / Spring Boot 2.7 / javax-only — use autumn-framework-2x on master.
  Shiro uses jakarta classifier; Druid mybatis-plus-spring-boot3-starter; SpringDoc OpenAPI (not Springfox).
  Enforces docs/AI_STANDARDS.md + docs/AI_DATABASE.md: never combine @Column(isUnique=true) with @Index on same field (§10.2); dual-key entities (auto Long id for gen CRUD only + unique biz key via Uuid.uuid()/SnowflakeId for FK/API; never use id as association); entity-driven schema; Dao via Provider (*DaoSql extends RuntimeSql);
  No hardcoded dialect quotes in Java (RuntimeSql quote/columnInWrapper or WrapperColumns per docs/AI_DATABASE.md §4.0);
  Controller must not use Dao; Service uses baseMapper; gen/Pages/list.html/js never hand-edited; statics/pages/Site/PageAware.
  Read docs/AI_CODEGEN.md, docs/AI_DATABASE.md; Redis TTL / Redisson / pExpire: docs/REDIS_TTL_GUIDE.md + docs/REDIS_REDISSON_SPRING_DATA.md (constraints-scan group H or --redis-expire-only); scripts/autumn-dependency-scan.sh for upgrades.
  When this skill applies: agent MUST run scripts/constraints-scan on the task root, read grouped output (A–H), conclude real violations, and fix in scope; re-run after edits. See skill body "约束扫描门禁".
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

- 当前工作区是 **autumn 仓库且检出 3.0.0 分支**，或业务工程 **Maven 依赖锁定 `cn.org.autumn` 3.0.0**。
- 提到：`cn.org.autumn`、`ModuleService`、`gen`、`Dao`、`Provider`、`RuntimeSql`、`DatabaseType`、`WrapperColumns`、`QueryWrapper`、`statics`、`pages`、`Site`、`PageAware`、`autumn.table`、`SpringDoc` 等，且 **栈为 JDK 17+ + Boot 3.5 + Jakarta + MP3**。

## 约束扫描门禁（启用本 Skill 作代码/实体/库表相关任务时 **必选**）

1. **执行脚本**（须用终端实际跑，不要只读文档当已扫过）  
   - 本仓库：在**工作区根**执行 `bash scripts/constraints-scan`（或 `bash scripts/constraints-scan .`）。  
   - 业务仓库且本机并列 clone 了 autumn：可对业务根执行  
     `bash ../autumn/scripts/constraints-scan /path/to/business-root`  
     （路径按实际调整）；若无 `scripts/constraints-scan`，先到 autumn 仓库拷贝该脚本再执行。  
   - **依赖**：已安装 **`rg`（ripgrep）**。  
   - 可选环境变量：**`AUTUMN_SCAN_SKIP_GEN=1`**、**`AUTUMN_SCAN_EXTRA=1`**（见脚本头注释）。

2. **解读输出**  
   - 按脚本分组 **A～H** 阅读命中（H 为 Redis TTL，可 `AUTUMN_SCAN_SKIP_REDIS=1` 跳过）；对照 **`docs/AI_DATABASE.md` §8.5** 与 **`docs/AI_STANDARDS.md`**。  
   - **区分**真实违规与误报（注释、测试、`target/`、历史生成代码等）。**F 组**仅为 gen 清单，**不计入** TOTAL。

3. **修复**  
   - 任务范围内可确定的违规：**直接改代码**。超出范围或需决策的：**写明残留风险**，勿静默忽略。

4. **收尾**  
   - 改过相关文件后，条件允许时 **再跑一次** `constraints-scan`。

## 文档加载顺序

所有 `AI_*.md` 均在仓库 **`docs/`** 下。本仓库内用 `@docs/...`；业务工程与 autumn 并列时用 `@../autumn/docs/...`（见 `docs/AI_INDEX.md` §4）。

1. `docs/AI_INDEX.md` → 2. `docs/AI_BOOT.md` → 3. `docs/AI_MAP.md` → 4. **`docs/AI_STANDARDS.md`**（强制全文，含 §8～§14）  
5. **`docs/AI_DATABASE.md`**（多库、`DatabaseType`、**§4.0 代码层方言标准写法**、`WrapperColumns`、`RuntimeSql`、Wrapper 边界、Dao **必须** Provider）  
6. 新模块 / 代码生成 / 搭骨架：追加 **`docs/AI_CODEGEN.md`**  
按需：`docs/AI_POSTGRESQL.md`、`docs/AI_TEMPLATES.md`、`docs/AI_CRYPTO.md`、`docs/AI_DISTRIBUTED_LOCK.md`、**`docs/REDIS_RESILIENCE.md`**（Redis 熔断与分布式锁稳健性）、`docs/REDIS_STANDALONE.md`、**`docs/REDIS_TTL_GUIDE.md`**（Redis TTL / **`RedisExpireUtil`**）、**`docs/INSTALL_MODE_CONDITIONAL.md`**（安装向导 **`autumn.install.wizard`**、**§0 占位默认 H2 / 可选 mysql**）等。

**注意**：文档或示例若与 **Boot 3 / Jakarta / MP3** 或本分支 **`pom.xml` / `application.yml`** 不一致，以**仓库当前实现**为准。

## 规范开发三步（与 `docs/AI_CODEGEN.md` 一致）

1. **先实体**：按 `docs/AI_STANDARDS.md` 建实体与索引注释；**`@Cache` / `@Caches`**；业务 Service 继承 **`ModuleService`**，勿自建平行缓存/队列/`LoopJob`。
2. **再生成**：后台「代码生成」ZIP 与 **`GenUtils.getFileName`** 路径一致；仅在非 gen 空壳写业务；**禁止**改 `controller/gen`、`*Pages`、`list.html/js`。
3. **后业务**：`Service` 承载规则与事务；可维护 `Controller` 独立 URL；`pages` + `site/*Site` + `@PageAware`。

## 实体与数据库（§8～§10）

- 框架扫描实体 + **`autumn.table.*`** 启动期对齐表结构；**禁止**把常规 **`DDL .sql`** 当默认交付物。
- **`modules/<子目录>/`**：目录名 = 包段 = 表前缀；**禁止**把前缀拼进实体类名；物理表名见 **`docs/AI_BOOT.md` §3.2**。
- **`@Table` / `@Column.comment`**：短标题 + **半角 `:`**；**凡 `@Column(isUnique=true)` 的字段禁止再使用 `@Index`**（含类级索引中含该列，§10.2，无例外）。
- 整型/布尔：优先基本类型 + 默认值。
- **双键模型（默认强制，详见 `docs/AI_STANDARDS.md` §10.4）**：每个实体须有 **`@TableId` 自增 `Long id`**（仅后台代码生成 CRUD、勿作业务关联）；**另增唯一业务主键列**（插入前赋值：`Uuid.uuid()` 小写 32 位，或 **`cn.org.autumn.utils.SnowflakeId`**），用于外键、对外 API、缓存键等。**禁止**用 **`id`** 关联其它表或被引用。SQL 侧见 **`docs/AI_DATABASE.md` §1.1**。

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

## Redis 与框架 `RedisConfig`

- **`cn.org.autumn.config.RedisConfig`**：`@Configuration`；**`@Autowired(required = false) RedisConnectionFactory`**；**`@Bean`**：`RedisTemplate`（`@Primary` + JSON）、**Ops**；**不**使用 **`@ConditionalOnBean(RedisConnectionFactory)`**、**不**使用 **`@AutoConfigureAfter`**；**不**列入 **`spring.factories` → `EnableAutoConfiguration`**，随 **`cn.org.autumn`** **组件扫描**加载。
- **`autumn.redis.open`**、EPP 与 **`spring.redis.*`**：**`docs/REDIS_STANDALONE.md` §1、§2**；业务 **模式 A / B**：**§3、§8**；升级清单：**`docs/AI_UPGRADE.md` §2.2 行 7**。
- **`RedisResilience`**、**`DistributedLockService`**、**`TagRunnable` / `LockOnce`**：**`docs/REDIS_RESILIENCE.md`**、**`docs/AI_DISTRIBUTED_LOCK.md`**。
- **TTL / `RedisExpireUtil` / Redisson ↔ SDR 对齐**：**`docs/REDIS_TTL_GUIDE.md`** + **`docs/REDIS_REDISSON_SPRING_DATA.md`**（可选 **`scripts/constraints-scan --redis-expire-only`** 或全文体检 **H 组**）。

## 分布式执行与加锁（新增）

- 涉及跨节点互斥、定时任务防重入、热点写入串行化时，必须优先复用框架锁能力。
- **已继承 `ModuleService` / `BaseService`**：直接使用 **`DistributedService`** 的 `withLock*` 能力（`withLock` / `withLockRetry` / `withLockOrFallback`）。
- **未继承框架基础能力**（独立组件、监听器、过滤器等）：直接注入 **`DistributedLockService`**。
- 配置来源统一走后台 **`DistributedLockConfig`**（键 `DISTRIBUTED_LOCK_CONFIG`），通过 `sysConfigService.getObject(...)` 获取对象配置，不走环境变量。
- **Redis 与熔断**：见上一节 **Redis 与框架 `RedisConfig`**；**`RedisResilience`** 细节见 **`docs/REDIS_RESILIENCE.md`**。
- 默认策略：严格模式优先（锁竞争失败抛错）；需要服务降级时显式使用 `withLockOrFallback` 或开启配置降级。
- 抗雪崩策略：锁竞争重试必须使用“有限重试 + 随机退避”（`withLockRetry`），禁止业务自旋热重试。

## 资源与页面（§14）

- **`resources/statics/`** 公共静态；新后台页 **`pages/`** + **`site/*Site`** + **`@PageAware`**。

## 既有纪律（摘要）

- **禁止生产 `@Scheduled`**；新接口 **不用 `@RequiresPermissions`**（登录态）；**FreeMarker** 安全（`<!-- -->`、`<#noparse>`）。
- 页面文案：**用户视角**，禁开发术语与表名字段名堆砌。
- 能力地图：**`docs/AI_MAP.md`**；缓存/队列细节：**`docs/AI_CODEGEN.md`** 第 4 节。

## 自检清单

- 已按上文 **约束扫描门禁** 执行 **`bash scripts/constraints-scan`**（或指向业务根的等价命令），并对照输出完成解读/修复（或写明无法修复的原因）？
- 无多余 **`schema.sql` / `init.sql`**？  
- Dao 无内联 SQL？**Wrapper** 无方言黑魔法？Java 无手写 **反引号/双引号/方括号** 拼 SQL（§4.0）？  
- **Controller** 未碰 **Dao**？未手改 **gen / list.html/js**？  
- 新页有 **`Site` + `@PageAware`**？  
- 生成路径与 **GenUtils** 一致？
- 新实体是否有 **业务主键**且关联列未误用 **`Long id`**？多节点雪花是否配置 **`autumn.snowflake.worker-id` / `datacenter-id`**？
- **`isUnique=true` 的 `@Column` 是否未再叠 `@Index`**（§10.2）？
- 新业务包未误用 **`javax.servlet`**？未混用 **Springfox**（本线为 **SpringDoc**）？
- **`mybatis-plus`** 配置与 **`application.yml`** 中 **MP3** 段一致？

## 多项目一句话

**`docs/AI_BOOT.md` → `docs/AI_MAP.md` → `docs/AI_STANDARDS.md` → `docs/AI_DATABASE.md` → `docs/AI_CODEGEN.md` → 专项 → README / 模块目录 → 任务约束**（**仅 3.0.0 / JDK17+ / Boot 3.5 / Jakarta / MP3 栈**）。

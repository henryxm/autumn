---
name: autumn-framework-3x
description: >-
  Autumn 3.0.0 line ONLY: JDK 17+, Spring Boot 3.5.x, MyBatis-Plus 3.x, jakarta.* namespace.
  Use on autumn branch 3.0.0 (artifact 3.0.0) or business apps pinned to that stack.
  NOT for Autumn 2.0.0 / JDK 8 / Spring Boot 2.7 / javax-only — use autumn-framework-2x on master.
  Enforces docs/AI_STANDARDS.md + docs/AI_DATABASE.md + docs/AI_DUAL_KEY.md: never combine @Column(isUnique=true) with @Index on same field (§10.2); single-field indexes on field @Index only not class-level @Indexes unless explicit (§10.2); dual-key entities (auto Long id for gen CRUD only + biz uuid via UuidBased/SnowBased or UserBased/unique user or no second PK when user suffices; legacy entities default no retrofit unless task explicitly requires upgrade; never use id as association); entity-driven schema; Dao SQL only via MyBatis Provider (*DaoSql extends RuntimeSql);
  No hardcoded dialect quotes in Java (RuntimeSql quote/columnInWrapper or WrapperColumns per docs/AI_DATABASE.md §4.0);
  Prefer import over fully qualified class names unless name clash (docs/AI_CODE_STYLE.md §7);
  log.info/debug/warn/error must be one line, no line breaks in the call (docs/AI_CODE_STYLE.md §8);
  FreeMarker templates: docs/AI_STANDARDS.md §7 (HTML <!--<#if>--> per line, JS //<#if>, title bare inline FTL);
  Controller must not use Dao; Service uses baseMapper; gen/Pages/list.html/js never hand-edited; statics/pages/Site/PageAware.
  Read docs/AI_CODEGEN.md, docs/AI_DATABASE.md, docs/AI_DUAL_KEY.md. scripts/constraints-scan is optional: run only when the user explicitly asks for a constraint audit, CI-style check, or phrases like 约束扫描/规范体检; see skill section "约束扫描（按需）".
  Triggers on cn.org.autumn 3.0.0, Spring Boot 3.5, JDK 17, ModuleService, EncryptModuleService, FieldEncrypt, isEncryptCacheField, encryptCache, RuntimeSql, PageAware, SpringDoc, bot, robot, rbt_, 字段加密, field encrypt, 加密缓存.
---

# Autumn 3.x 框架开发（3.0.0 / 分支 3.0.0）

## 版本矩阵（本 Skill 唯一适用）

| 项 | 版本 / 约束 |
|----|-------------|
| **Autumn** | **3.0.0**（`cn.org.autumn:*:3.0.0`，**3.0.0** 分支） |
| **JDK** | **17+** |
| **Spring Boot** | **3.5.x**（以根 `pom.xml` 为准） |
| **MyBatis-Plus** | **3.x** |
| **命名空间** | **`jakarta.*`** |
| **2.x 线** | **禁用本 Skill**：Autumn **2.0.0**、JDK **8**、Spring Boot **2.7** 请用 **`autumn-framework-2x`**（**master**） |

业务工程须在 `AGENTS.md` 或首轮对话中写明依赖的 Autumn 主版本，避免 2.x / 3.x 规范混用。

## 何时启用

- 当前工作区是 **autumn 仓库且检出 3.0.0 分支**，或业务工程 **Maven 依赖锁定 `cn.org.autumn` 3.0.0**。
- 提到：`cn.org.autumn`、`ModuleService`、`gen`、`Dao`、`Provider`、`RuntimeSql`、`DatabaseType`、`statics`、`pages`、`Site`、`PageAware`、`autumn.table` 等，且 **栈为 JDK 17+ + Boot 3.5**。

## 约束扫描（按需）

**默认不跑**：用户未提及规范体检、合并/CI 自检、`constraints-scan`、**约束扫描**等时，**不要**把执行 **`scripts/constraints-scan`** 当作本 Skill 的必做步骤。

**何时执行**：用户或任务**明确要求**对仓库做与 **`docs/AI_STANDARDS.md` / `docs/AI_DATABASE.md`** 对齐的分组体检、或声明 CI/合并门禁需要脚本结果时，再在任务根目录实际执行（勿只读文档代替执行）。

1. **命令**  
   - 本仓库：工作区根执行 `bash scripts/constraints-scan`（或 `bash scripts/constraints-scan .`）。  
   - 业务仓库且并列 clone 了 autumn：`bash ../autumn/scripts/constraints-scan /path/to/business-root`（路径按实际调整）；若无脚本则从 autumn 仓库拷贝。  
   - **依赖**：**`rg`（ripgrep）**；可选 **`AUTUMN_SCAN_SKIP_GEN=1`**、**`AUTUMN_SCAN_EXTRA=1`**、**`AUTUMN_SCAN_SKIP_REDIS=1`**（见脚本头注释）。

2. **解读输出**  
   - 按分组 **A～H** 阅读（H 为 Redis TTL）；对照 **`docs/AI_DATABASE.md` §8.5** 与 **`docs/AI_STANDARDS.md`**。  
   - **区分**真实违规与误报（注释、测试、`target/`、历史生成代码等）。**F 组**仅为 gen 清单，**不计入** TOTAL。

3. **修复与收尾**  
   - 任务范围内可确定的违规：**直接改代码**；超出范围的在回复中**写明残留风险**。  
   - 若本次已跑过扫描且改过相关文件，可**再跑一次**确认未引入新的可修复项（仍属按需，非默认）。

## 文档加载顺序

所有 `AI_*.md` 均在仓库 **`docs/`** 下。本仓库内用 `@docs/...`；业务工程与 autumn 并列时用 `@../autumn/docs/...`（见 `docs/AI_INDEX.md` §4）。

1. `docs/AI_INDEX.md` → 2. `docs/AI_BOOT.md` → 3. `docs/AI_MAP.md` → 4. **`docs/AI_STANDARDS.md`**（强制全文，含 §8～§14）  
5. **`docs/AI_DATABASE.md`**（多库、`DatabaseType`、**§4.0 代码层方言标准写法**、`WrapperColumns`、`RuntimeSql`、Wrapper 边界、Dao **必须** Provider）  
6. 新模块 / 代码生成 / 搭骨架：追加 **`docs/AI_CODEGEN.md`**  
按需：`docs/AI_POSTGRESQL.md`、`docs/AI_TEMPLATES.md`、`docs/AI_CRYPTO.md`、**`docs/AI_FIELD_ENCRYPT.md`**（实体字段存储加密、`EncryptModuleService`）、`docs/AI_DISTRIBUTED_LOCK.md`、**`docs/AI_ASYNC_TASK.md`**（**`TagRunnable` / `FinishStatus` / `onFinished`**）、**`docs/REDIS_RESILIENCE.md`**（Redis 熔断与分布式锁稳健性）、`docs/REDIS_STANDALONE.md`、**`docs/REDIS_TTL_GUIDE.md`**（Redis TTL / `RedisExpireUtil`）、**`docs/INSTALL_MODE_CONDITIONAL.md`**（安装向导 **`autumn.install.wizard`**、**§0 占位默认 H2 / 可选 mysql**）等。

涉及「终止会话 / 记住我阻断 / 会话过期重登守卫」时，追加阅读 **`docs/AI_SESSION_GUARD.md`**（`ForceLogoutRememberMeManager`、`ShiroSessionService`、`/sys/session/self/*`、`autumn-session-guard.js`）。

涉及 **`asyncTaskExecutor`、内存队列 drain、本机调度闸门** 时，必读 **`docs/AI_ASYNC_TASK.md`**。

涉及 **支付密码 / `PayPinVerifier` / `modules.safe`** 时，追加 **`docs/AI_SAFE_CREDENTIAL_INTEGRATION.md`** + **`docs/AI_SAFE_CREDENTIAL.md`**（2.x master 已实现；3.x 分支同步前以检出代码为准）。摘要：`POST /safe/api/v1/*`、`SafeConfig`、`gate/assess` → `PayPinVerifier`、错误码 838～852。

涉及 **实体字段存储加密 / `@FieldEncrypt` / `EncryptModuleService`** 时，必读 **`docs/AI_FIELD_ENCRYPT.md`**（§0 易混概念；§7 **`@Cache` 加密缓存**；与 **`docs/AI_CRYPTO.md`** 传输加密独立）。

## 字段存储加密（at-rest）

| 任务 | 必读 |
|------|------|
| **新增 `@FieldEncrypt` 实体** | **`docs/AI_FIELD_ENCRYPT.md`** §0～§2.4 |
| **`@Cache` + searchable 加密字段** | 上列 **§7**（实体双 `@Cache`、Service、`getCache` / `getNameCache("hash", …)`） |
| **运行时开关 / 加解密测试** | §1～§4 + **`fieldencrypt.html`** / `FieldEncryptAdminController` |

**纪律（摘要）**：

- 默认 **`ModuleService`**（零加解密、零加密缓存）；实体含 `@FieldEncrypt` → **`EncryptModuleService`**（MP3 `BaseMapper` / `Wrapper` / `IPage`）。
- **`baseMapper` 直查** → **`afterRead(...)`**（实体）或 **`afterReadMap` / `afterReadMaps` / `afterReadScalar(s)`**（Map/标量）；`searchable=true` → 手写 `{field}Hash` + `@Column`。
- 列表条件：仅 **`EncryptModuleService#tryHashQueryCondition`**（`ModuleService` 走原列映射）。
- **`@Cache` + 加密**：searchable 字段与 hash 列各标 `@Cache`（hash 列 `name = FieldEncryptService.HASH_CACHE_CHANNEL`）；调用方 cache key 用**明文**或 **hash hex**，miss 回源由框架 hash 盲查（§7.2）。
- **`BaseCacheService` 不依赖 `FieldEncryptService`**；加密缓存钩子在 **`EncryptModuleService`** 覆盖：`isEncryptCacheField` / `isEncryptCacheNaming` / `isEncryptCacheEntity`、`tryEncryptCacheEq`、`mirrorEncryptCache`、`encryptCacheEvictionKeys`、`encryptCacheEvictionValue`（§7.4）。**勿**在基类或 `ModuleService` 子类手写平行逻辑。
- 约束单测：`FieldEncryptConventionTest`；缓存：`FieldEncryptCacheTest`。

## 规范开发三步（与 `docs/AI_CODEGEN.md` 一致）

1. **先实体**：按 `docs/AI_STANDARDS.md` 建实体与索引注释；**`@Cache` / `@Caches`**；业务 Service 默认继承 **`ModuleService`**；实体含 **`@FieldEncrypt`** 时改继承 **`EncryptModuleService`**，勿自建平行缓存/队列/`LoopJob`。
2. **再生成**：由开发者在后台执行「代码生成」ZIP，路径与 **`GenUtils.getFileName`** 一致；**AI 不**以仿写替代整套生成器输出；仅在非 gen 空壳写业务；**禁止**删改已落库的 `controller/gen`、`*Pages`、`list.html/js`（须改 **`template/*.vm`** 后重生成）。
3. **后业务**：`Service` 承载规则与事务；可维护 `Controller` 独立 URL；`pages` + `site/*Site` + `@PageAware`。

## 实体与数据库（§8～§10）

- 框架扫描 + **`autumn.table.*`** 对齐表结构；**禁止**常规 **`DDL .sql`**。
- **表名/类名（强制）**：见 **`docs/AI_BOOT.md` §3.2**、**`docs/AI_STANDARDS.md` §9**。摘要：`{模块名}_` + 类名去 `Entity` 蛇形；表名仅 **`@TableName`**，**`@Table` 不写 `value`**（框架自动合并）；Dao `quote` 与 `@TableName` 一致；类名不含模块前缀。示例：`PayUserPinEntity` + `@TableName("safe_pay_user_pin")` + `@Table(comment = "...")`。
- **`@Column.comment`**：简介名互异（§10.1）；**`isUnique=true` 禁止再 `@Index`**（§10.2）；**单字段索引用字段级 `@Index`**，类级 **`@Indexes` 仅组合索引（≥2 列）**（§10.2）。
- **双键**：第二主键按需 **`uuid`** / **`UserBased`** / **无（§3.4 仅 `user`）**；业务 **`user`** 可存 **`sys_user` 或 `bot_robot` 的 uuid**。**存量实体默认不升级**（**`docs/AI_DUAL_KEY.md` §1.2**），除非任务明确要求。详见 **§1.1、§3**。

## 多库与 SQL（与 `docs/AI_DATABASE.md` 一致）

- 默认业务 SQL / Wrapper 对 **`DatabaseType` 全量兼容**；单库例外须 JavaDoc 标明。
- **禁止硬编码方言**：表/列/排序/Map 键不写死 `` ` `` / `"` / `[]`；Provider 用 **`RuntimeSql#quote` / `columnInWrapper`**；未继承 **`DialectService`** 时用 **`WrapperColumns`**（**`columnInWrapper`**、分页排序 **`orderByColumnExpression`**、Map 等值 **`entityWrapperAllEqQuoted`** 等，见 **`docs/AI_DATABASE.md` §4.0**）。
- **import 优先**：除非**类名冲突**，**禁止**在方法体/字段等处写全限定类名；**必须** `import` 后用短类名（**`docs/AI_CODE_STYLE.md` §7**）。**改 Java 后跑** `bash scripts/check-java-fqn`；合法冲突登记 **`scripts/fqn-allowlist.txt`**。
- **日志单行**：`log.trace` / `log.debug` / `log.info` / `log.warn` / `log.error` **整条调用一行写完**，**禁止**为日志实参换行（**`docs/AI_CODE_STYLE.md` §8**）。
- **Dao**：新代码 **禁止** 注解内联 SQL；**必须** `@SelectProvider` + **`*DaoSql`**，推荐 **`extends RuntimeSql`**。
- **Wrapper**：安全谓词 only；复杂场景 **Dao + Provider**（见 **`docs/AI_DATABASE.md` §4～§5**）。
- 升级体检：**`scripts/autumn-dependency-scan.sh`** + **`docs/AI_UPGRADE.md` §3.3**；存量 Wrapper/Service 手写引号对照 **`docs/AI_DATABASE.md` §8.1** 与 **§8.5** `rg` 示例。

## 生成层与业务（§11）

- 可重生层：**禁止**在生成结果上删改业务逻辑；改 **`template/*.vm`** 后由开发者重生成；扩展用非 gen 壳子或独立类（**`docs/AI_STANDARDS.md` §11**）。
- 空壳 **Controller/Service/Dao（非 gen）**：业务落 **Service**。

## SQL 与 Dao（§12～§13，摘要）

- **Provider**  mandatory for new SQL；配合 **`docs/AI_DATABASE.md`**、**`docs/AI_POSTGRESQL.md`**（PG）。
- **Controller** 禁止 **Dao**；**Service** 用 **`baseMapper`**，勿 **`@Autowired`** 本 **Dao**；跨域只注入 **其他 Service**。

## Redis 与框架 `RedisConfig`

- **`cn.org.autumn.config.RedisConfig`**：`@Configuration`；**`@Autowired(required = false) RedisConnectionFactory`**；**`@Bean`**：`RedisTemplate`（`@Primary` + JSON）、**Ops**；**不**使用 **`@ConditionalOnBean(RedisConnectionFactory)`**、**不**使用 **`@AutoConfigureAfter`**；**不**列入 **`spring.factories` → `EnableAutoConfiguration`**，随 **`cn.org.autumn`** **组件扫描**加载。
- **`autumn.redis.open`**、EPP 与 **`spring.redis.*`**：**`docs/REDIS_STANDALONE.md` §1、§2**；业务 **模式 A / B**：**§3、§8**；升级清单：**`docs/AI_UPGRADE.md` §2.2 行 7**。
- **`RedisResilience`**、**`DistributedLockService`**、**`TagRunnable` / `LockOnce`**：**`docs/REDIS_RESILIENCE.md`**、**`docs/AI_DISTRIBUTED_LOCK.md`**、**`docs/AI_ASYNC_TASK.md`**。

## 异步任务与 `onFinished`（`TagRunnable`）

- **`onFinished(FinishStatus)`** 每任务必调一次；本机闸门在 **`onFinished` 释放**；队列 drain 见 **`docs/AI_ASYNC_TASK.md`** §4。

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

## FreeMarker 模板（§7 摘要）

改 **`resources/templates/**/*.html`** 时必读 **`docs/AI_STANDARDS.md` §7**；参考 **`webauthentication.html`**、**`login.html`**。

| 位置 | 条件渲染注释 | 指令换行 |
|------|----------------|----------|
| **HTML 块**（表单、列表、权限按钮） | `<!--<#if ...>-->` … `<!--</#if>-->` | **每个** FTL 指令独占一行 |
| **`<title>` 等纯文本** | **禁止** HTML 注释包裹（防 `<!---->` 进标题） | 单行裸 `<#if>…</#if>` |
| **`<script>` 内** | **`//<#if>`** … `//</#if>`；**禁止** `<!-- -->` | 每指令一行；简单标量用 `${…?js_string}` / `${(x!false)?c}` |
| **说明注释** | HTML 用 `<!-- 说明 -->`，JS 用 `//` | 注释正文**禁止**写 `${}` / `<#if>` 样例 |

## 既有纪律（摘要）

- **禁止生产 `@Scheduled`**；新接口 **不用 `@RequiresPermissions`**（登录态）；**FreeMarker** 见 **§7**（HTML `<!-- -->` 包 FTL、JS `//` 包 FTL、`<#noparse>`）。
- 页面文案：**用户视角**，禁开发术语与表名字段名堆砌。
- 能力地图：**`docs/AI_MAP.md`**；缓存/队列细节：**`docs/AI_CODEGEN.md`** 第 4 节。

## 自检清单

- 若用户**要求**跑规范扫描：已执行 **`bash scripts/constraints-scan`**（含 **I 组 FQN**）并完成解读/修复（或写明残留）？
- **改 Java 后**：已执行 **`bash scripts/check-java-fqn`**（PR CI 硬门禁）？
- 无多余 **`schema.sql` / `init.sql`**？  
- Dao 无内联 SQL？**Wrapper** 无方言黑魔法？Java 无手写 **反引号/双引号/方括号** 拼 SQL（§4.0）？  
- **Controller** 未碰 **Dao**？未手改 **gen / list.html/js**？  
- 新页有 **`Site` + `@PageAware`**？  
- 改 **`.html` 模板**：**`AI_STANDARDS` §7**（HTML `<!--<#if>` 独立行；JS `//<#if>`；`<title>` 单行裸 FTL）？
- 生成路径与 **GenUtils** 一致？
- 新实体/新字段是否按 **`docs/AI_DUAL_KEY.md`** 选型？**存量默认不 retrofit**，除非任务明确要求升级第二主键 / **`user`** 标准？
- 按用户唯一表是否 **`UserBased` + 唯一 `user`**？用户维度足够时是否避免冗余 **`uuid`**（**§3.4**）？
- **`isUnique=true` 的 `@Column` 是否未再叠 `@Index`**（§10.2）？
- 新索引是否**单字段在字段上 `@Index`**、**组合索引才用类级 `@Indexes`**（§10.2）？
- **`@FieldEncrypt`**：Service 是否 `EncryptModuleService`？`baseMapper` → `afterRead`？`searchable` + hash 列？**`@Cache`** 是否按 §7（明文键 + hash 通道）？
- **表名**：符合 §3.2？仅 `@TableName`（`@Table` 无 `value`）？Dao `quote` 一致？

## 多项目一句话

**`docs/AI_BOOT.md` → `docs/AI_MAP.md` → `docs/AI_STANDARDS.md` → `docs/AI_DATABASE.md` → `docs/AI_CODEGEN.md` → 专项 → README / 模块目录 → 任务约束**（**仅 3.0.0 / JDK17+ / Boot 3.5 栈**）。

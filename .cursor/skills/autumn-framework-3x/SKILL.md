---
name: autumn-framework-3x
description: >-
  Autumn 3.0.0 line ONLY: JDK 17+, Spring Boot 3.5.x, MyBatis-Plus 3.x, jakarta.* namespace.
  Use on autumn branch 3.0.0 (artifact 3.0.0) or business apps pinned to that stack.
  NOT for Autumn 2.0.0 / JDK 8 / Spring Boot 2.7 / javax-only — use autumn-framework-2x on master.
  Enforces docs/AI_STANDARDS.md + docs/AI_DATABASE.md + docs/AI_DUAL_KEY.md: never combine @Column(isUnique=true) with @Index on same field (§10.2); dual-key entities (auto Long id for gen CRUD only + biz uuid via UuidBased/SnowBased or UserBased/unique user; never use id as association); entity-driven schema; Dao SQL only via MyBatis Provider (*DaoSql extends RuntimeSql);
  No hardcoded dialect quotes in Java (RuntimeSql quote/columnInWrapper or WrapperColumns per docs/AI_DATABASE.md §4.0);
  Prefer import over fully qualified class names unless name clash (docs/AI_CODE_STYLE.md §7);
  log.info/debug/warn/error must be one line, no line breaks in the call (docs/AI_CODE_STYLE.md §8);
  Controller must not use Dao; Service uses baseMapper; gen/Pages/list.html/js never hand-edited; statics/pages/Site/PageAware.
  Read docs/AI_CODEGEN.md, docs/AI_DATABASE.md, docs/AI_DUAL_KEY.md. Bot/robot: read docs/AI_ROBOT.md + docs/AI_ROBOT_API.md (rbt_, Hook, message/push, cn.org.autumn.modules.bot); web 集成测试见 web/docs/INTEGRATION_TEST.md（基类 integration.base.IntegrationTest）。
  scripts/constraints-scan is optional: run only when the user explicitly asks for a constraint audit, CI-style check, or phrases like 约束扫描/规范体检; see skill section "约束扫描（按需）".
  Triggers on cn.org.autumn 3.0.0, Spring Boot 3.5, JDK 17, ModuleService, RuntimeSql, PageAware, SpringDoc, bot, robot, rbt_, RobotHook, RobotMessageSubscriber, message/push.
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
- 提到：**机器人 / Bot / `rbt_` / Hook 回调 / `message/push` / `cn.org.autumn.modules.bot`**（见下文 **机器人模块**）。

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
6. 新模块 / 代码生成 / 搭骨架：追加 **`docs/AI_CODEGEN.md`**、**`docs/AI_DUAL_KEY.md`**（双键、`UserBased`、业务 `user` 可存真人或机器人 uuid）  
按需：`docs/AI_POSTGRESQL.md`、`docs/AI_TEMPLATES.md`、`docs/AI_CRYPTO.md`、`docs/AI_DISTRIBUTED_LOCK.md`、**`docs/AI_ASYNC_TASK.md`**（**`TagRunnable` / `FinishStatus` / `onFinished`**、内存队列 drain 状态机）、**`docs/REDIS_RESILIENCE.md`**（Redis 熔断与分布式锁稳健性）、`docs/REDIS_STANDALONE.md`、**`docs/REDIS_TTL_GUIDE.md`**（Redis TTL / `RedisExpireUtil`）、**`docs/INSTALL_MODE_CONDITIONAL.md`**（安装向导 **`autumn.install.wizard`**、**§0 占位默认 H2 / 可选 mysql**）等。

涉及「终止会话 / 记住我阻断 / 会话过期重登守卫」时，追加阅读 **`docs/AI_SESSION_GUARD.md`**（`ForceLogoutRememberMeManager`、`ShiroSessionService`、`/sys/session/self/*`、`autumn-session-guard.js`）。

涉及 **`asyncTaskExecutor`、内存待处理队列、本机 DISPATCHING/IDLE 闸门** 时，必读 **`docs/AI_ASYNC_TASK.md`**（勿与 **`BaseQueueService`** 持久化队列混淆）。

**注意**：文档或示例若与 **Boot 3 / Jakarta / MP3** 或本分支 **`pom.xml` / `application.yml`** 不一致，以**仓库当前实现**为准。

## 机器人模块（Bot / 开放 API）

**区分两类任务**（勿混读文档重点）：

| 任务 | 必读文档 | 代码位置 |
|------|----------|----------|
| **业务系统 HTTP 对接** | **`docs/AI_ROBOT.md`** + **`docs/AI_ROBOT_API.md`** | 仅调 `{ORIGIN}/robot/api/v1/*` |
| **Autumn 内扩展**（改 bot、Subscriber） | 上列 + **`docs/AI_STANDARDS.md`** + **`docs/AI_DATABASE.md`** + **`docs/AI_CODEGEN.md`** | `autumn-modules` → **`cn.org.autumn.modules.bot`**（表前缀 **`bot_`**） |

**3.x 实现要点**（相对 2.x master 合并后）：

- **命名空间**：`jakarta.servlet.*`、`jakarta.validation.*`；实体 **`com.baomidou.mybatisplus.annotation.*`**（非 `annotations`）。
- **鉴权**：`ShiroConfig` 将 `/robot/api/v1/**` 设为 `anon`；`@Authenticated` + **`UserContextArgumentResolver`** + **`UserContextService`**（`Token` / `X-Robot-Token`）。
- **账号**：`AccountHandler.User#isRobot()`；人类用户 UUID 用 **`UuidNamespaceService`**（勿在 3.x 新代码里裸 `Uuid.uuid()` 造 sys_user）；业务表泛化 **`user`** 列可存 **`sys_user` 或 `bot_robot` 的 uuid**（见 **`docs/AI_DUAL_KEY.md` §1.1**）。
- **配置**：`SysConfigService.ROBOT_QUOTA_CONFIG` → **`RobotQuotaConfig`**；JSON 序列化用 **`GsonConfig.getGson()`**。
- **令牌轮换**：`RobotTokenService.rotateIssue` 清理已作废行用 **SELECT + `deleteById`**（兼容 H2，勿依赖 `DELETE … ORDER BY LIMIT`）。
- **集成测试**：`web` 模块 **`IntegrationTest`**（`@TestInstance(PER_CLASS)`）+ `application-it.yml`；`mvn -pl web -am test -Pintegration -DskipTests=false`（见 **`web/docs/INTEGRATION_TEST.md`**）。

**对接纪律（摘要）**：

- **用户令牌**：管理 API（除 `message/push`）；**禁止** `rbt_`。
- **`rbt_`**：仅 **`POST .../message/push`**；头 **`X-Robot-Token`**。
- **`disable`**：作废该机器人全部 `rbt_`（再 push 为鉴权 `-10000`）。
- Hook 验签：`HMAC_SHA256(timestamp + "." + body)`，**原始 body**。
- 扩展点（`autumn-lib`）：`RobotMessageSubscriber`、`RobotHookDispatch`。

涉及 **支付密码 / `PayPinVerifier` / `modules.safe`** 时，追加 **`docs/AI_SAFE_CREDENTIAL_INTEGRATION.md`** + **`docs/AI_SAFE_CREDENTIAL.md`**。摘要：`POST /safe/api/v1/*`、`SafeConfig`、`gate/assess` → `PayPinVerifier`、错误码 838～852；**`jakarta.*` / MP3 / `lang3`**。

## 规范开发三步（与 `docs/AI_CODEGEN.md` 一致）

1. **先实体**：按 `docs/AI_STANDARDS.md` 建实体与索引注释；**`@Cache` / `@Caches`**；业务 Service 继承 **`ModuleService`**，勿自建平行缓存/队列/`LoopJob`。
2. **再生成**：由开发者在后台执行「代码生成」ZIP，路径与 **`GenUtils.getFileName`** 一致；**AI 不**以仿写替代整套生成器输出；仅在非 gen 空壳写业务；**禁止**删改已落库的 `controller/gen`、`*Pages`、`list.html/js`（须改 **`template/*.vm`** 后重生成）。
3. **后业务**：`Service` 承载规则与事务；可维护 `Controller` 独立 URL；`pages` + `site/*Site` + `@PageAware`。

## 实体与数据库（§8～§10）

- 框架扫描 + **`autumn.table.*`** 对齐表结构；**禁止**常规 **`DDL .sql`**。
- **表名/类名（强制）**：见 **`docs/AI_BOOT.md` §3.2**、**`docs/AI_STANDARDS.md` §9**。摘要：`{模块名}_` + 类名去 `Entity` 蛇形；表名仅 **`@TableName`**，**`@Table` 不写 `value`**（框架自动合并）；Dao `quote` 与 `@TableName` 一致；类名不含模块前缀。示例：`PayUserPinEntity` + `@TableName("safe_pay_user_pin")` + `@Table(comment = "...")`。
- **`@Column.comment`**：简介名互异（§10.1）；**`isUnique=true` 禁止再 `@Index`**（§10.2）。
- **双键**：第二主键 **`uuid`** / 按真人唯一的 **`UserBased`**；业务 **`user`** 列可存 **`sys_user` 或 `bot_robot` 的 uuid**（互斥分配）。详见 **`docs/AI_DUAL_KEY.md`** §1.1、§3。

## 多库与 SQL（与 `docs/AI_DATABASE.md` 一致）

- 默认业务 SQL / Wrapper 对 **`DatabaseType` 全量兼容**；单库例外须 JavaDoc 标明。
- **禁止硬编码方言**：表/列/排序/Map 键不写死 `` ` `` / `"` / `[]`；Provider 用 **`RuntimeSql#quote` / `columnInWrapper`**；未继承 **`DialectService`** 时用 **`WrapperColumns`**（**`columnInWrapper`**、分页排序 **`orderByColumnExpression`**、Map 等值 **`entityWrapperAllEqQuoted`** 等，见 **`docs/AI_DATABASE.md` §4.0**）。
- **import 优先**：除非**类名冲突**，**禁止**在方法体/字段等处写全限定类名；**必须** `import` 后用短类名（**`docs/AI_CODE_STYLE.md` §7**）。
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
- **TTL / `RedisExpireUtil` / Redisson ↔ SDR 对齐**：**`docs/REDIS_TTL_GUIDE.md`** + **`docs/REDIS_REDISSON_SPRING_DATA.md`**（可选 **`scripts/constraints-scan --redis-expire-only`** 或全文体检 **H 组**）。

## 异步任务与 `onFinished`（`TagRunnable`）

- 注入 **`TagTaskExecutor`**（常名 `asyncTaskExecutor`）；业务写在 **`exe()`**，**禁止**业务侧直接调用 `exe()`。
- **`onFinished(FinishStatus)`**：任务结束必调一次（`COMPLETED`/`FAILED`/`SKIPPED`/`NOT_DISPATCHED`）；**本机调度闸门在 `onFinished` 释放**，不要只在 `exe()` 的 `finally` 释放。
- **内存队列 drain**：`TagRunnable` + `@TagValue(lock=false)` + `exe()` 内 **`withLockOrFallback*`**；**不要**对 drain 用 `LockOnce`。
- **`execute` 返回 `boolean`**：`false` 时已 `NOT_DISPATCHED`；可配合 `LoopJob` 做积压补偿。
- 详见 **`docs/AI_ASYNC_TASK.md`** §4。

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

- 若用户**要求**跑规范扫描：已执行 **`bash scripts/constraints-scan`** 并完成解读/修复（或写明残留）？
- 无多余 **`schema.sql` / `init.sql`**？  
- Dao 无内联 SQL？**Wrapper** 无方言黑魔法？Java 无手写 **反引号/双引号/方括号** 拼 SQL（§4.0）？  
- **Controller** 未碰 **Dao**？未手改 **gen / list.html/js**？  
- 新页有 **`Site` + `@PageAware`**？  
- 生成路径与 **GenUtils** 一致？
- 按用户唯一表是否 **`UserBased` + 唯一 `user`**（禁止 `uuid` 第二主键）？非按用户唯一表的 **`user`** 是否未标 `isUnique`？第二主键 / 外键 **`comment`** 是否符合 **`docs/AI_DUAL_KEY.md`** §3？
- **`isUnique=true` 的 `@Column` 是否未再叠 `@Index`**（§10.2）？
- 机器人：是否已读 **`docs/AI_ROBOT.md` + `docs/AI_ROBOT_API.md`**？bot 包是否仍为 **`jakarta.*` / MP3 annotation**？管理 API 与 `message/push` 鉴权是否分离？
- **表名**：符合 §3.2？仅 `@TableName`（`@Table` 无 `value`）？Dao `quote` 一致？

## 多项目一句话

**`docs/AI_BOOT.md` → `docs/AI_MAP.md` → `docs/AI_STANDARDS.md` → `docs/AI_DATABASE.md` → `docs/AI_CODEGEN.md` → 专项 → README / 模块目录 → 任务约束**（**仅 3.0.0 / JDK17+ / Boot 3.5 栈**）。

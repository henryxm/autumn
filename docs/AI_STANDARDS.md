# Autumn 应用层开发规范（约束性）

> 用途：约定**业务与数据访问分层、实体与库表、生成层边界、资源与页面**等纪律。  
> 与 `docs/AI_BOOT.md`、`docs/AI_MAP.md` 互补：**多库 SQL、Wrapper 与 Provider 落地以 `docs/AI_DATABASE.md` 为准**；**PostgreSQL 专项实现见 `docs/AI_POSTGRESQL.md`**；**团队强制口径以本文为准**。

## 1. 适用范围

- Autumn 本仓库各模块。
- 基于 Autumn 的**业务子项目 / 多模块工程**（含仅依赖 starter 的下游仓库）。

## 2. 架构原则（必须遵守）

### 2.1 高内聚、低耦合

- **高内聚**：与某实体（表 / 领域模型）相关的**业务流程、规则、事务边界**，默认实现于该实体对应的 **`XxxService`**（继承链以 `ModuleService` 为默认，见 `docs/AI_MAP.md` §2.7）。
- **低耦合**：模块之间避免直接依赖对方**具体实现类**；需要协作时优先 **接口契约**、**事件**、或团队已选型的**远程/本地服务调用**（如 Dubbo、Facade Bean 等），由项目架构统一选型，不在此重复实现细节。

### 2.2 分层职责

| 层级 | 职责 | 约束 |
|------|------|------|
| **Controller（可维护层）** | HTTP 入参校验与组装、调用 Service、返回视图或统一响应包装 | 与实体对应的业务接口写在 **`controller/*` 下可维护的 `*Controller.java`**，**不写**复杂业务规则与事务；**禁止**直接使用 **Dao**（见 §13） |
| **Service** | 该实体域内的业务流程、缓存/队列/任务与领域编排 | 逻辑落在 **`service/*` 下与该实体对应的 `*Service`**；通过继承的 **`baseMapper`** 访问本实体持久化，**禁止**再 **`@Autowired`** 本实体 **Dao**（见 §13） |
| **跨实体 / 跨模块编排** | 组合多个 Service 完成用例 | 放在**发起用例所在项目或模块**的 Service 层（如 `FooFacadeService`、`BarApplicationService`），**禁止**在 Controller 中堆叠多段业务逻辑 |

### 2.3 子项目之间协作

- **尽量减少耦合**：不直接引用对方模块内部包路径下的实现细节；通过**暴露的 API 接口**、**共享 API 模块**、或**注册的服务接口**交互。
- **聚合多个下游能力**：在**当前子项目**中增加专门的 Service 层编排类，而不是在多个子项目的 Controller 之间互相调用。

## 3. 对内接口（管理端 / 网页后台）

面向后台管理、内部页面的 HTTP 接口（非对外开放 API）须满足：

1. **不得与 gen 生成控制器冲突**  
   - **禁止**使用与 **`ControllerGen`**（或同模块代码生成产出的控制器）**相同的请求映射**（如相同的类级 `@RequestMapping` 路径组合与方法级路径、HTTP 方法组合导致与生成路由重复）。  
   - **禁止**在可维护 `Controller` 中使用与生成控制器中**已存在的、可能引起混淆的同名公开方法名**作为映射方法（团队 Code Review 以「一眼可区分、无路由歧义」为准）。  
   - 目的：避免路由覆盖、权限配置错误、Swagger/文档重复与线上排障困难。

2. **自定义业务接口**须有**独立、可识别的 URL 命名空间**（如业务前缀 `/api/app/...`、或模块内约定前缀），与生成 CRUD 的默认路径明确区分。

## 4. 对外 API（Open / 第三方调用）

1. **独立控制器**：对外 API 使用**单独的 Controller 类**（例如 `XxxOpenApiController`、`XxxApiController`），与对内页面/管理接口类分离，便于鉴权、限流与文档隔离。

2. **统一请求响应模型**：对外 body 统一使用框架约定的 **`Request<T>` / `Response<T>`**；兼容旧客户端时使用 **`CompatibleRequest<T>` / `CompatibleResponse<T>`** 及 `@Endpoint(compatible=true)`，与 `docs/AI_CRYPTO.md`、`docs/AI_MAP.md` §2.4 一致。

3. **应用层加解密**：对外接口须**接入框架应用层自动加解密语义**（请求体 `ciphertext`+`session`、响应头 `X-Encrypt-Session`、握手与业务分离等），**禁止**自造与框架并行的加解密协议。细节以 `docs/AI_MAP.md`、`docs/AI_CRYPTO.md` 为准。

## 5. 定时任务（禁止 `@Scheduled`）

- **禁止**在业务代码中使用 Spring **`@Scheduled`** 作为**生产环境**定时调度入口（避免脱离框架的任务治理、监控、多节点分配与统一启停）。

- **必须**使用 Autumn 任务体系，按场景二选一（详见 `docs/AI_MAP.md` §2.5）：  
  - **固定周期**：实现 **`LoopJob.OneSecond` … `OneWeek`** 中对应接口，配合 **`@JobMeta`**（`skipIfRunning`、`timeout`、`maxConsecutiveErrors`、`assign` 等）。  
  - **复杂日历**：使用框架 **`schedulejob` + `cronExpression`**（及 `@TaskAware` / `@JobMeta` 等既有约定），而非裸 `@Scheduled`。

- 例外：仅允许在**本地调试、单元测试、非交付脚手架**中使用 `@Scheduled`，且不得合并入主分支交付配置。

## 6. 权限注解 `@RequiresPermissions`（约束）

- **定位**：`@RequiresPermissions` 出现在**代码生成框架**产出的后台管理链路中，用于 **Shiro 后台管理菜单/按钮级鉴权**，**不面向终端普通用户开放**的「管理端语义」。

- **新增接口（禁止）**：**所有新编写的 Controller 接口**（含对内业务 API、对外 API、可维护层手写接口）**不得**使用 **`@RequiresPermissions`** 做鉴权。  
  - **普通用户 / 已登录用户**可访问的接口：采用**登录态鉴权**（Session、Token、OAuth 等与项目一致的「已登录即可访问」机制），在 Service 或安全过滤器中按业务需要补充校验，**不**套用管理端权限码注解。

- **例外**：仅当明确属于**与 gen 同构的后台管理扩展**且团队书面约定沿用菜单权限模型时，才允许按框架既有方式使用；默认假设下**新代码一律不用** `@RequiresPermissions`。

## 7. 页面模板与 FreeMarker（HTML / JavaScript）

默认页面经 **FreeMarker** 渲染。须避免与 FTL 冲突的写法，并保证渲染后的 **HTML / JavaScript 语法合法**。

### 7.1 禁止与建议

- **禁止**在页面中随意使用与 **FreeMarker 语法冲突** 的占位与指令形式（如与 `${`、`#`、`@` 等 FTL 解析规则相撞的内容），除非按本节规则处理。

### 7.2 条件与结构：HTML 注释包裹

- 在页面中使用 FreeMarker 做**条件判断、分支、循环**等时，为保持 **HTML 结构在校验器/IDE 中合法**，应用 **HTML 注释** `<!-- ... -->` **包裹**对应的 FTL 片段（含指令与表达式边界），避免裸写导致 HTML 非良构。  
- **说明**：服务端仍会**正常解析**注释内的 FTL 并输出结果；注释用于**源文件级 HTML 形态**与团队协作约定，**不**等同于关闭 FreeMarker。若需**完全禁止某段被 FTL 解析**（原样输出或内含大量冲突字符），见 §7.4。

### 7.3 JavaScript 中的 FreeMarker 变量

- 将 FreeMarker 变量插入 **`<script>`** 时，必须保证**渲染后的脚本**仍是合法 JavaScript：  
  - 字符串必须**正确加引号、转义**，避免**裸露**替换结果导致未闭合字符串、意外换行或关键字冲突。  
  - 为保持 **JS 语法不被破坏**，应对 FTL 插入点采用**注释占位、拆分到独立 `<script>`、或 `data-*` 属性 + JS 读取**等安全模式；**禁止**依赖未经验证的裸 `${...}` 插在 JS 字面量中间导致运行期语法错误。

### 7.4 无法避免的语法冲突：`<#noparse>`

- 当**无法避免**与 FreeMarker 冲突的片段（如文档示例、内嵌模板、含大量 `$`/`#` 的脚本）时，必须使用 **`<#noparse>...</#noparse>`**（或项目约定的等价机制）包裹，使内部**按字面输出**、不被 FTL 解析。

## 8. 实体扫描、注解建表与禁止手工 DDL 脚本

- 框架对**已加载的实体类型**进行扫描，在配置允许时**自动建表 / 自动对齐表结构**（启动期执行检测与更新；开关与行为见 `autumn.table.*` 等配置，`docs/AI_BOOT.md` §3 与运行库说明）。

- **禁止**在研发交付物中新增**用于初始化或变更库结构的独立 DDL `.sql` 文件**作为常规做法（例如随代码提交的 `schema.sql` / `init.sql` 专供手工执行）。**表结构以实体注解 + 框架同步为准**；若生产必须离线 DDL，走发布流程与 DBA 规范，**不**作为日常开发默认路径。

## 9. 模块目录、包名、表前缀与实体命名

- **模块根**：通常位于 `autumn-modules/src/main/java/cn/org/autumn/modules/<子目录>/`。该 **`<子目录>` 名**同时作为 **Java 包路径段**与**表名前缀**（与 `README`、代码生成流程一致）。

- **禁止**把**模块目录名 / 表前缀**当作**实体类名的前缀**拼进类名（例如模块 `spm` 时，**不要**命名为 `SpmOrderEntity` 这类「前缀重复」形态），以免与代码生成、表前缀拼接规则叠加后产生**错乱**。实体类名使用**领域短名**即可（如 `OrderEntity`），由 **`@Table` / `@TableName` + `prefix`** 表达物理表前缀（细则见 `docs/AI_BOOT.md` §3.2）。

- **物理表名**：在遵守 `docs/AI_BOOT.md` §3.2 前提下，惯例为 **`{表前缀}_{实体核心蛇形}`**（实体类去 `Entity` 后缀再驼峰转下划线，再与模块前缀拼接）。

## 10. 表/字段注释、索引与字段类型

### 10.1 注释格式（`@Table` / `@Column.comment`）

- **短标题**宜 **2～4 个汉字**（或等价长度），**不做硬性上限**时建议**不超过约 8 个字符**；随后使用**半角冒号 `:`** 再接详细说明，形如 **`名称:详细说明`**。框架对列表/菜单等会**只取冒号前**作为短标题（详见 `docs/AI_BOOT.md` §3、`docs/AI_MAP.md` 注解建表章节）。

### 10.2 索引与唯一约束

- **禁止**在同一字段上**同时**使用 **`@Index`** 与 **`@Column(isUnique = true)`**（及同类重复唯一声明），避免 DDL 重复与迁移噪音（与 `docs/AI_BOOT.md` §3 一致）。

### 10.3 整型、布尔与空值

- **无特殊原因**时，字段尽量使用 **基本类型**（如 `int`、`boolean`）并在 **`@Column`** 上声明**默认值**，减少 **`null`** 分支与 **`NullPointerException`** 风险。

## 11. 生成层边界与业务落点

- **`controller/gen/*`**、由 **`SitePages.java.vm`** 生成的 **`site/*Pages.java`**、以及生成产出的 **`list.html` / `list.js`**：**禁止修改、禁止添加任何业务逻辑**；变更须走 **Velocity 模板 `resources/template/*.vm` 并重生成**（与 `docs/AI_MAP.md` 生成矩阵一致）。

- 代码生成产出的**可维护壳子**：**空的 / 骨架的** **`Controller.java`、`Service.java`、`Dao.java`**（非 gen 路径）是**日常实现业务与流程的位置**；**所有业务逻辑在对应 `Service`**，**Controller** 只做编排与调用 **Service**。

## 12. Dao、Provider 与多数据库 SQL

- **默认目标**：除非在代码中**明确声明**仅支持某一种库，业务 SQL 与持久化写法应对框架已接入的 **`DatabaseType` 全量兼容**（见 **`docs/AI_DATABASE.md`** §1、§2）。

- **禁止**在 **Dao 接口**的方法上**内联硬编码 SQL**（如 **`@Select`/`@Update` 等注解中直接写死 SQL 字符串**）作为**新代码**的默认写法。

- **必须**通过与之对应的 **MyBatis `Provider`**（如 **`XxxDaoSql`**、`*Provider` 类及 `@SelectProvider` 等）拼接与维护 SQL；推荐 **`extends RuntimeSql`**，统一使用 **`quote`、`limitOne`、`likeContainsAny`、`columnValueInCommaSeparatedList`** 等可移植片段（见 **`docs/AI_DATABASE.md`** §3、§5 与 **`docs/AI_POSTGRESQL.md`** 示例）。

- **Java 里禁止硬编码方言符号**（反引号、双引号、方括号等）：表/列引用、分页排序列、Map 等值键须走 **`RuntimeSql` / `DialectService` / `WrapperColumns`**（**`docs/AI_DATABASE.md` §4.0**），未继承方言基类时**优先** `WrapperColumns.columnInWrapper`、`orderByColumnExpression`、`entityWrapperAllEqQuoted` / `queryWrapperAllEqQuoted`（以当前分支 API 为准）。

- **EntityWrapper / Condition**：仅使用跨库相对安全的条件（等值、范围、`in`、空值、简单 `orderBy`）；**禁止**在 `apply` / 自定义片段中写死单库函数（`FIND_IN_SET`、`IFNULL`、`DATE_FORMAT` 等）。复杂查询、JOIN、报表、强依赖方言的模糊/列表匹配 **必须** 改为 **Dao + Provider**（**`docs/AI_DATABASE.md`** §4～§5）。

- **例外**：仅框架内置或历史存量可在治理计划中逐步迁移；**新开发一律走 Provider**。

## 13. Controller、Service、Dao 协作纪律

- **Controller**：**禁止**注入或直接使用 **Dao**；**只**依赖 **Service** 完成业务。

- **本实体 Service**：通过继承链获得的 **`baseMapper`**（或框架等价入口）调用本模块持久化能力，**禁止**再 **`@Autowired`** 本实体 **Dao**（避免重复入口）。

- **跨实体访问**：**尽量不**在 Service 中 **`@Autowired` 其他实体的 Dao**；应 **注入其他实体的 Service** 并调用其公开方法。**禁止**在**非该 Dao 所属域的 Service** 中直接使用该 **Dao**。

## 14. 资源、`statics`、`pages`、`Site` 与 `@PageAware`

- 框架支持从**多个 jar** 加载模板与静态资源；各子项目须把**页面与静态资源放在本项目资源目录**下，随 jar 发布，**不**依赖在入口工程手工拷贝聚合（与 `docs/AI_GOVERNANCE.md` TemplateFactory 约定一致）。

- **`resources/statics/`**：跨子模块共享的**公共静态资源**目录；放置于此的静态资源按框架约定**匿名可访问**，**无需**再单独配置匿名 URL。**若框架已有同类资源，须优先复用**，避免重复拷贝。

- **新后台页面**：放在**当前模块资源树下与模块对应的 `pages` 目录**（如 `resources/.../modules/<模块>/pages/`）；**若不存在则创建**。

- **站点入口**：每个新页面须在 **`site` 包**中对应 **`Site` 类（如 `XxxSite`）** 增加**页面指引**（字段 + 注解），与现有模块（如 `oauth` 的 `OauthSite`）一致，供框架扫描并经 **SPM** 形成可访问路径。

- **`@PageAware`**（标注在 **Site 类的字段**上）：  
  - **匿名公共页**：`@PageAware(login = false)`（按需填 `resource` / `url` / `page` 等指向资源路径）。  
  - **需登录后台页**：`@PageAware(login = true)`。  

  具体属性以 `cn.org.autumn.annotation.PageAware` 为准。

- **页面文案与功能描述（新增硬约束）**：除非需求中有**特殊说明**，所有页面开发都必须以**用户与使用者视角**进行描述与设计，禁止从开发实现视角表达。具体要求：
  - **禁止**出现开发相关专业术语（如接口、数据库、缓存、线程、方法、函数、参数、对象、JSON、SQL、鉴权链路等）。
  - **禁止**出现后台表名、字段名、后台函数名、类名、包名、实现路由等技术标识。
  - **禁止**描述后台开发体系结构、分层实现、调用链路，或“开发者/程序员视角”说明。
  - 页面描述必须聚焦“用户能做什么、看到什么、得到什么反馈”，使用业务语义与操作结果表达。

## 15. Redis TTL / Redisson 与 `RedisExpireUtil`

业务工程若使用 **Redisson + Spring Data Redis**，建议在处理 **键过期、带 TTL 的写入、滑动窗口计数续期** 或排查 **`StackOverflowError`（栈含 `DefaultedRedisConnection` / `pExpire`）** 时：

1. 先阅读 **`docs/REDIS_REDISSON_SPRING_DATA.md`**（依赖对齐，治本）与 **`docs/REDIS_TTL_GUIDE.md`**（何时适合用 **`RedisExpireUtil`**、API 对照、推荐处理顺序）。
2. **推荐**用 **`cn.org.autumn.utils.RedisExpireUtil`** 统一表达 TTL 语义（Lua 或服务端语义），避免各项目复制零散脚本；是否替换存量 `RedisTemplate.expire` / `set(..., TimeUnit)` 由团队按风险与迭代安排决定。
3. 可选使用 **`scripts/redis-expire-forbidden-scan.sh`** 做静态检索，辅助代码评审。

说明：§15 不否定 Spring 默认写法在「依赖已正确对齐」时的可用性；文档重点是**讲清风险场景**并给出 **Autumn 侧的一致推荐路径**。

## 16. 与 AI 文档的交叉引用

| 主题 | 文档 |
|------|------|
| `@Table` / `@Column` / 字符集 / 表名推导细节 | `docs/AI_BOOT.md` §3 |
| 多库兼容、Wrapper 边界、Provider 标准写法 | **`docs/AI_DATABASE.md`**（权威） |
| PostgreSQL 专项 DDL/元数据、`RuntimeSql` 示例 | `docs/AI_POSTGRESQL.md` |
| 升级扫描与迁移 | `docs/AI_UPGRADE.md` |
| ModuleService、缓存、队列、LoopJob、生成矩阵 | `docs/AI_MAP.md` |
| 代码生成端到端流程、三步开发节奏、缓存/队列基类详解 | `docs/AI_CODEGEN.md` |
| 加解密 | `docs/AI_CRYPTO.md` |
| 多项目协作与术语 | `docs/AI_GOVERNANCE.md` |
| 索引与加载组合 | `docs/AI_INDEX.md` |
| Redis TTL、Redisson、`RedisExpireUtil` 使用说明 | **`docs/REDIS_TTL_GUIDE.md`**、**`docs/REDIS_REDISSON_SPRING_DATA.md`** |

## 17. 维护约定

- 新增与「实体/库表/Dao/资源/页面」相关的团队规则时，**优先更新本文**，并同步 `docs/AI_INDEX.md`、`docs/AI_GUIDE.md` 摘要；**多库类型、方言、Wrapper/Provider 纪律**以 **`docs/AI_DATABASE.md`** 为落地专篇，变更时同步该文 §2；**老旧项目注解 Dao / 方言 Wrapper 迁移与扫描清单**见 **`docs/AI_DATABASE.md` §8** 与 **`docs/AI_UPGRADE.md` §3.3**；与代码生成流程或三步节奏相关的补充可落在 **`docs/AI_CODEGEN.md`**。**Redis TTL 与 `RedisExpireUtil`** 以 **`docs/REDIS_TTL_GUIDE.md`** 为专篇，变更 API 或扫描脚本时同步 **`autumn-lib`** 与 **`scripts/redis-expire-forbidden-scan.sh`**。
- 若与 `docs/AI_MAP.md` §4「开发决策规则」表述重叠，以**本文 §2～§14**、**§15** 为应用层与数据访问纪律的权威表述；MAP 保留框架能力级硬约束与类索引。

# Autumn AI 框架能力地图

> 首轮建议先读：`@AI_BOOT.md`（最小启动上下文）
> 全量索引入口：`@AI_INDEX.md`

> 目标：给 AI 提供统一、结构化的框架上下文，减少“不了解项目能力”导致的误判与重复实现。

## 0. AI 最小上下文（先读这段再开发）

- 一句话原则：
  - 优先复用平台能力，禁止重复造轮子；默认从 `ModuleService` 继承链出发实现业务。
- Service 默认能力：
  - `ModuleService -> BaseService -> ShareCacheService -> BaseCacheService -> BaseQueueService`
  - 即：CRUD + 菜单/多语言初始化 + 缓存 + 队列（开箱可用）。
- 接口默认模式：
  - 普通：`Request<T> -> Response<T>`
  - 兼容：`CompatibleRequest<T> -> CompatibleResponse<T> + @Endpoint(compatible=true)`
- 混合加密触发条件：
  - 请求解密：请求体含 `ciphertext + session`
  - 响应加密：请求头含 `X-Encrypt-Session`
  - 握手入口：`/rsa/api/v1/init`（仅握手，不混入业务）
- 定时任务默认模式：
  - 固定周期优先 `LoopJob.*`，复杂日历才使用 cron。
- 生成代码默认模式：
  - 实体注解驱动建表 -> gen 模块模板生成 -> 业务只写非 gen 层（防覆盖）。
- 开发前 3 问（AI 自检）：
  - 1) 现有基类/模块能力是否已覆盖？
  - 2) 是否会破坏缓存一致性、加密语义、权限语义？
  - 3) 生成后业务代码是否放在可维护层（非 `controller/gen`）？

## 1. 项目结构（AI 先看）

- `autumn-lib`：框架基础能力（缓存、队列、加解密、通用服务抽象）。
- `autumn-modules`：业务模块与控制器实现（sys/gen/job/db/oauth/usr/oss/lan/spm/wall 等）。
- `autumn-web`：启动与页面入口。

### 1.1 多数据库（PostgreSQL）

- **`autumn.database`** 与 MySQL 基线并存；注解建表、分页方言、运行时 SQL 方言、实体 `tinyint`/`boolean`/`smallint` 兼容、`BaseService` 分页 `COUNT` 等与 PG 相关的约定与变更过程，见 **`AI_POSTGRESQL.md`**（与 `AI_BOOT.md` §8 摘要互补）。业务工程 **升级 autumn 版本** 的通用流程与只读扫描脚本见 **`AI_UPGRADE.md`**。

## 2. 核心能力索引（按开发高频）

### 2.1 缓存体系

- 核心类：
  - `cn.org.autumn.service.CacheService`
  - `cn.org.autumn.service.BaseCacheService`
  - `cn.org.autumn.service.ShareCacheService`
  - `cn.org.autumn.modules.sys.controller.CacheController`
- 关键点：
  - 两级缓存：EhCache + Redis。
  - `getCache*` 未命中自动调用 `getEntity*` 回源。
  - 默认回源支持注解+反射，业务可覆盖 `getEntity/getNameEntity`。
  - 失效同步通道：`cache:invalidation`（Redis Pub/Sub）。

### 2.2 队列体系

- 核心类：
  - `cn.org.autumn.service.QueueService`
  - `cn.org.autumn.service.BaseQueueService`
  - `cn.org.autumn.modules.sys.controller.QueueController`
- 关键点：
  - 队列类型：`MEMORY` / `REDIS_LIST` / `REDIS_STREAM` / `DELAY` / `PRIORITY`。
  - 支持延迟、定时、优先级、批量发送。
  - 支持自动消费者启停（空闲超时）。
  - 支持重试、死信、历史消息运维。

### 2.3 混合加解密（RSA + AES）

- 核心类：
  - `cn.org.autumn.modules.oauth.controller.RsaController`
  - `cn.org.autumn.modules.oauth.resolver.EncryptArgumentResolver`
  - `cn.org.autumn.modules.oauth.interceptor.EncryptInterceptor`
  - `cn.org.autumn.service.RsaService`
  - `cn.org.autumn.service.AesService`
- 关键点（务必准确）：
  - 请求解密不是全量触发：仅当请求体包含 `ciphertext + session` 时解密。
  - 响应加密不是全量触发：仅当请求头有 `X-Encrypt-Session` 时进入加密流程。
  - 返回兼容增强：若返回体不是 `Encrypt` 但为 JSON，拦截器会按返回类型规则决定包装后再加密（`Response`/`CompatibleResponse`/`DefaultEncrypt`）。
  - `@Endpoint(compatible=true)` 表示“支持兼容加密”，不直接决定包装与否；包装形态由声明返回类型决定。
  - 无 `X-Encrypt-Session` 时一律不加密并返回原始值；若实际返回为 `CompatibleResponse`，会解包为 `data` 返回（旧客户端兼容）。
  - `/rsa/*` 接口在响应侧排除加密（避免密钥交换循环加密）。
  - 非 JSON 响应（文件流/文本）不会做自动包装，保持原语义。
  - `@Endpoint(force=true)` 接口缺少 `X-Encrypt-Session` 时返回 `FORCE_ENCRYPT_SESSION_REQUIRED`。

### 2.4 加密兼容方案（跨项目复用）

- 本节已拆分为独立专项文档：`@AI_CRYPTO.md`。
- 日常开发只需记住 4 条：
  - 默认接口：`Request<T> -> Response<T>`，兼容场景再用 `CompatibleRequest/CompatibleResponse`。
  - 触发条件：请求体含 `ciphertext + session` 才解密；请求头含 `X-Encrypt-Session` 才加密响应。
  - 约束：不要自建平行加密协议；握手接口与业务接口分离。
  - 回归：至少覆盖“明文/密文/header有无”三组调用路径。
- 安全强校验（`agent/auth/签名/灰度/演练`）请看：`@AI_SECURITY.md`。

### 2.5 接口式定时任务（LoopJob，常用推荐）

- 核心目标：
  - 用“接口周期”替代“字符串 cron”，降低配置错误率与维护成本。
  - 用统一管理接口完成任务启停、触发、统计、告警和分配。
- 核心类：
  - `cn.org.autumn.modules.job.task.LoopJob`
  - `cn.org.autumn.modules.job.controller.LoopJobController`
- 选型规则（给 AI 的硬约束）：
  - 固定周期任务：优先实现 `LoopJob.OneMinute/FiveMinute/...`。
  - 仅当时间规则复杂（如每月、节假日）时，才使用 `schedulejob + cronExpression`。
- 关键机制：
  - 周期接口覆盖：`OneSecond` 到 `OneWeek`。
  - 运行保护：`skipIfRunning` 防重入，`timeout` 超时观测，`maxConsecutiveErrors` 连续错误自动禁用。
  - 多节点分配：`assignTag` 配合 `server.tag` 控制任务归属。
  - 批量性能：支持并行执行开关与 overrun（批量耗时超间隔）监控。
- 最小模板（跨项目可直接套用）：

```java
@Component
@JobMeta(
    name = "Demo Job",
    skipIfRunning = true,
    timeout = 15000,
    maxConsecutiveErrors = 5
)
public class DemoJob implements LoopJob.OneMinute {
    @Override
    public void onOneMinute() {
        // business logic
    }
}
```

- 验证接口（至少覆盖）：
  - `GET /job/loop/list?category=OneMinute`
  - `POST /job/loop/trigger`
  - `GET /job/loop/stats`
  - `GET /job/loop/alerts`

### 2.6 Handler 扩展机制

- 核心目录：`autumn-handler`
- 关键点：
  - 用接口扩展主流程，模块与框架解耦。
  - 常见接口：页面、拦截器、解析器、插件、过滤链等。
  - 常见模式：默认实现 + `@ConditionalOnMissingBean` + `@Order`。

### 2.7 ModuleService 默认继承能力（AI 必看）

- 继承链（非常关键）：
  - `ModuleService` -> `BaseService` -> `ShareCacheService` -> `BaseCacheService` -> `BaseQueueService` -> `ServiceImpl`
- 结论：
  - 任何 `extends ModuleService<Dao, Entity>` 的业务 Service，默认就有 CRUD + 缓存 + 队列 + 菜单语言初始化能力。
  - 定时任务不需要额外基类，直接在 Service 上 `implements LoopJob.OneMinute/...` 即可接入调度。
- `ModuleService` 提供的模块化能力：
  - 自动菜单与多语言初始化：`init()` 会调用 `sysMenuService.put(...)` 和 `language.put(...)`。
  - 自动解析模块信息：根据实体 `@Table(module/prefix/value)` 解析模块、前缀、菜单 key。
  - 约定优于配置：生成模块时，默认可通过实体命名与注解推导菜单、语言、权限基础项。
- `BaseCacheService` 默认可用能力（无需重复封装）：
  - 单值缓存：`getCache(...) / putCache(...) / removeCache(...) / clearCache(...)`。
  - 列表缓存：`getListCache(...) / putListCache(...) / removeListCache(...) / clearListCache(...)`。
  - 命名缓存与类型缓存：支持 `name` 维度和 `Class<X>` 维度。
  - 回源约定：缓存未命中自动调用 `getEntity* / getListEntity*`，业务只需覆写回源方法。
  - 一致性约定：写操作后优先调用缓存失效方法（至少同时处理单值和列表缓存）。
- `ShareCacheService` 默认可用能力（跨项目共享缓存）：
  - 共享单值/列表/Map：`getShareCache/getShareListCache/getShareMapCache`。
  - 支持 `shareName`、泛型类型、复合 key、supplier 回源。
  - 适用于子项目间共享数据，不要再单独造“跨模块缓存工具类”。
- `BaseQueueService` 默认可用能力（无需再封装消息中间层）：
  - 发送：`sendQueue / sendMessage / sendDelay / sendScheduled / sendPriority / sendBatch`。
  - 消费：`pollQueue / pollBatch / consume / startQueue / stopQueue`。
  - 自动队列配置：可通过 `getQueueConfig()/register()` 自动注册与自动启停消费者。
  - 失败处理扩展点：`onQueueMessage / onErrorMessage / onDeadMessage`。
- `LoopJob` 接入约定（Service 级）：
  - 固定周期任务：在 Service 直接 `implements LoopJob.OneSecond...OneWeek` 并覆写对应 `onXxx()`。
  - 任务治理：可结合 `@JobMeta` 使用 `skipIfRunning/timeout/maxConsecutiveErrors/assign`。
  - 仅复杂时间表达式场景才退回 `schedulejob + cronExpression`。
- 给 AI 的硬约束：
  - 新建 Service 时默认继承 `ModuleService`，禁止绕开继承链重复实现缓存/队列/分页/菜单语言初始化。
  - 需要缓存时，先选 `getCache/getListCache/getShareCache` 体系；需要异步时，先选 `BaseQueueService` 体系。
  - 需要周期任务时，先让当前 Service `implements LoopJob.*`，不要先设计新的调度器或线程轮询器。

### 2.8 ModuleService 实战模板（AI 直接套用）

- 目标：
  - 让 AI 在新增业务 Service 时直接复用平台能力，一次性完成“缓存 + 队列 + 周期任务 + 业务扩展点”。
- Service 骨架模板（建议优先）：

```java
@Service
public class DemoService extends ModuleService<DemoDao, DemoEntity> implements LoopJob.OneMinute {

    @Override
    public boolean onQueueMessage(DemoEntity entity) {
        // 队列消息消费逻辑（失败返回 false，触发重试/死信链路）
        return true;
    }

    @Override
    public void onOneMinute() {
        // 周期任务逻辑（固定周期优先）
    }

    @Override
    public DemoEntity getEntity(Object key) {
        // 单值缓存回源（key -> entity）
        return baseMapper.selectById((Long) key);
    }
}
```

- 缓存调用优先级（AI 必须遵守）：
  - 单实体查询：优先 `getCache(key)`，命中失败由 `getEntity` 回源。
  - 列表查询：优先 `getListCache(key)`，命中失败由 `getListEntity` 回源。
  - 变更后失效：优先 `removeCache/removeListCache/removeCacheAll`，禁止只改库不清缓存。
  - 跨项目共享：优先 `getShareCache/getShareListCache/getShareMapCache`，不要新增自定义共享缓存组件。
- 队列调用优先级（AI 必须遵守）：
  - 普通异步：`sendQueue/sendMessage`。
  - 延迟/定时/优先级：`sendDelay/sendScheduled/sendPriority`。
  - 批处理：`sendBatch`。
  - 消费端优先覆写：`onQueueMessage/onErrorMessage/onDeadMessage`，或使用 `startQueue(..., handler)`。
- 定时任务调用优先级（AI 必须遵守）：
  - 固定周期：`implements LoopJob.OneSecond...OneWeek` + `onXxx`。
  - 任务治理：按需增加 `@JobMeta(skipIfRunning/timeout/maxConsecutiveErrors/assign)`。
  - 复杂日历规则才使用 cron，且要说明为何 LoopJob 不适用。
- 生成代码后的扩展落点（防覆盖）：
  - 可重生层：`controller/gen/*`（尽量不放业务）
  - 可维护层：`controller/*` + `service/*`（业务逻辑放这里）
  - 页面入口层：`site/*Site` + `site/*Menu`
  - 前端页面层：`templates/modules/{module}/*.html|*.js`

### 2.8A 代码生成模板分层约束（MVC 生成骨架）

> 模板目录：`autumn-modules/src/main/resources/template/*`。

#### 2.8A.1 生成产物分层（必须区分）

- **可重生层（生成覆盖层，禁止业务改动）**：
  - 后端：`ControllerGen.java.vm` -> `controller/gen/*ControllerGen.java`
  - 页面聚合：`SitePages.java.vm` -> `site/*Pages.java`
  - 前端 CRUD 页面：`list.html.vm` / `list.js.vm` -> `templates/modules/{module}/{entity}.html|.js`
- **可维护层（业务扩展层，允许改动）**：
  - 后端控制器壳：`Controller.java.vm` -> `controller/*Controller.java`
  - 服务层：`Service.java.vm` -> `service/*Service.java`
  - 站点入口：`Site.java.vm` -> `site/*Site.java`
  - 菜单入口：`Menu.java.vm` -> `site/*Menu.java`
- **基础结构模板（按实体重生成，谨慎手改）**：
  - `Entity.java.vm`、`Dao.java.vm`、`menu.sql.vm`

#### 2.8A.2 AI 开发硬约束（生成体系）

- `controller/gen/*` 与 `site/*Pages.java` 视为**生成源**：默认不承载业务逻辑，不在其上做需求改造。
- 业务接口改造放在 `controller/*Controller.java`（或新增业务 Controller），通过继承/覆盖调用 `*ControllerGen`。
- 页面业务改造优先放在自定义页面/组件；若必须改生成页，需在需求中明确“允许改模板并接受重生成影响”。
- 生成后允许 AI 自动更新的优先顺序：`Service` -> `Controller` -> `Site` -> `Menu`；避免先改 `gen` 层。

#### 2.8A.3 典型落地方式（建议）

- 新增查询/聚合接口：在 `controller/*Controller.java` 增加新 API，不改 `*ControllerGen`。
- 复杂表单/交互：新增独立页面而非直接改 `list.html.js` 模板产物。
- 通用 CRUD 能力升级：改模板文件（`*.vm`）并统一重生成，而不是逐个改生成结果文件。

#### 2.8A.4 模板文件级速查表（11 个模板）

| 模板文件 | 典型生成产物 | 分层 | 默认策略 | 备注 |
|---|---|---|---|---|
| `ControllerGen.java.vm` | `controller/gen/*ControllerGen.java` | 可重生 | **不改生成结果**；需统一行为时改模板 | CRUD 细节与权限注解在此 |
| `SitePages.java.vm` | `site/*Pages.java` | 可重生 | **不改生成结果**；改模板后重生成 | 页面 key 聚合层 |
| `list.html.vm` | `templates/modules/{module}/{entity}.html` | 可重生 | 默认不直接改生成页；复杂需求建独立页 | CRUD 列表与表单壳 |
| `list.js.vm` | `templates/modules/{module}/{entity}.js` | 可重生 | 默认不直接改生成脚本；需统一改模板 | jqGrid + Vue 通用交互 |
| `Controller.java.vm` | `controller/*Controller.java` | 可维护 | **可改**（业务 API 落点） | 继承 `*ControllerGen` |
| `Service.java.vm` | `service/*Service.java` | 可维护 | **可改**（业务逻辑首选） | 默认继承 `ModuleService` |
| `Site.java.vm` | `site/*Site.java` | 可维护 | **可改**（自定义页面入口） | 继承 `*Pages` |
| `Menu.java.vm` | `site/*Menu.java` | 可维护 | **可改**（模块菜单扩展） | 菜单入口定义 |
| `Entity.java.vm` | `entity/*Entity.java` | 结构层 | 谨慎改；若改模板需评估全量重生成影响 | 注解驱动建表基础 |
| `Dao.java.vm` | `dao/*Dao.java` | 结构层 | 谨慎改；优先保持稳定 | `BaseMapper` 基础访问层 |
| `menu.sql.vm` | 菜单初始化 SQL 片段 | 结构层 | 谨慎改；仅框架级规则变化时改模板 | 生成菜单与按钮权限骨架 |

> 判定原则：  
> 1) 单模块业务改造，优先改可维护层；  
> 2) 多模块统一行为升级，改 `*.vm` 并重生成；  
> 3) 避免在可重生层写业务逻辑，防止后续生成覆盖。

### 2.9 多项目模板资源聚合（TemplateFactory 方案）

- 目标：
  - 支持“多项目独立开发、运行时统一装配模板资源”。
  - 每个项目可将自身 `templates` 资源打包进本项目 jar，无需集中到单一入口工程。
- 关键实现：
  - `TemplateFactory.Template`：每个 artifact 仅需提供一个实现即可参与模板装载。
  - 默认模板根路径：`/templates`（可覆写 `getBasePackagePath()`）。
  - `TemplateFactory` 会收集所有 `TemplateFactory.Template` Bean，并按 `@Order` 从小到大装配为 `TemplateLoader` 链。
  - 最终使用 `DynamicTemplateLoader` 做统一查找；同时兼容 `LoaderFactory.Loader`（旧扩展方式，已废弃）。
- 运行时整合能力：
  - `Plugin` 默认继承 `TemplateFactory.Template`，插件安装/卸载时可动态 `add/remove` 模板加载器。
  - 卸载后调用 `resetState()` 清理缓存，避免读取到失效模板源。
  - `DynamicTemplateLoader.exists(...)` 会校验插件 jar 是否仍存在，防止“模板源句柄存在但文件已删除”的脏引用。
- 冲突与覆盖约定（AI 必须遵守）：
  - 多项目存在同名模板时，优先通过 `@Order` 显式控制优先级，不要依赖隐式加载顺序。
  - 若业务需要覆盖框架默认模板，使用“同名模板 + 更高优先级（更小 order 值）”实现。
  - 新项目接入时，不要要求把模板复制到主工程；应在本项目内提供 `TemplateFactory.Template` 实现并随 jar 发布。
  - 非必要不要继续扩展 `LoaderFactory.Loader`，新代码统一走 `TemplateFactory.Template`。

### 2.10 实体建表注解（`@Table` / `@Column` / `@Index` / `@Indexes`）

> 包名：`cn.org.autumn.table.annotation.*`；收集与 SQL 拼装入口：`TableInfo`、`QuerySql`、`IndexInfo`。

#### 2.10.1 注解职责速览

- **`@Table`（类）**：表名、前缀、模块、注释、存储引擎、字符集等；与 MyBatis-Plus `@TableName` 等共同参与表名推导（见 `TableInfo.getTableName`）。
- **`@Column`（字段）**：列名、类型、长度、是否可空、主键/自增、默认值、**`comment`（建议「短标题：详述」格式，见 2.10.5）**；**`isUnique = true`** 时在建表 SQL 中会额外生成该列的 `UNIQUE KEY`（见 `QuerySql.createTable` 中对 `columnInfo.isUnique()` 的处理）。
- **`@Index`（类 / 字段 / 方法 / 注解类型）**：声明二级索引或唯一索引、全文/空间索引等；属性包括 `name`、`fields`（`@IndexField` 数组）、`indexType`（`IndexTypeEnum`）、`indexMethod`（`IndexMethodEnum`）、`comment`。
- **`@Indexes`（类）**：打包多个 `@Index`，用于组合索引或一次声明多组索引。
- **`@IndexField`**：`field` 为 Java 属性名（框架内会转为下划线列名），`length` 为索引前缀长度；**`length = 0` 表示不对列加前缀长度**（整列参与索引）。

#### 2.10.2 框架如何收集索引（避免重复声明）

`TableInfo.initFrom(Class)` 会合并以下来源的索引定义，全部进入 `indexInfos`，最终由 `buildIndexSql()` 拼进建表语句：

- 类上的单个 `@Index`、以及 `@Indexes` 里的每一个 `@Index`；
- 字段上的 `@Index`（通过 `new IndexInfo(k, field)` 解析）。

同时，`@Column(isUnique = true)` 会把对应列记入 **`indexColumn`**（`IndexInfo` 视为唯一索引形态），在 **`getIndexInfosCombine()`** 中还会与 `indexInfos` 合并，供迁移阶段比对索引差异（`MysqlTableService.buildModifyIndex`）。

因此：

1. **`@Column(isUnique = true)` 与 `@Index` 不要叠在同一列**  
   建表时：`isUnique` 已在列定义处生成 `UNIQUE KEY`。**字段级**仍写 `@Index` 时，`TableInfo` 会**忽略**该 `@Index` 并打 **WARN**。**类级** `@Index` / `@Indexes` 的 `fields` 若包含已 `isUnique` 的列，会从该索引定义中**剔除**该列（`isUnique` 优先）并 **WARN**；剔除后若索引无剩余列则整段索引忽略并 **WARN**。

2. **字段上已有 `@Index` 时，不要在类级 `@Indexes` / 类级 `@Index` 里再包含同一列的等价索引**  
   否则同一列会被注册两次，建表或变更时可能生成两条含义重叠的 `INDEX`，属于重复声明。

3. **与 `@UniqueKey` / `@UniqueKeys` 的边界**  
   多列唯一约束应优先用 `@UniqueKey(s)`；若已与 `@Column(isUnique)` 或 `@Index` 表达同一约束，不要再用另一套注解重复描述。

#### 2.10.3 字段级 `@Index` 与列类型（前缀长度）

**说明**：MySQL 对数值、日期时间、`DECIMAL` 等类型**支持** B-tree 整列索引；**仅** CHAR/VARCHAR/TEXT/BLOB/BINARY 等类型支持索引定义中的**前缀**语法 `` `col`(n) ``。旧实现曾把 `@Column.length()`（默认 255）误当前缀，导致数值列生成非法 DDL。

**当前实现**（`IndexPrefixRules` + `TableInfo.initFrom` 末尾对 `IndexInfo` / `UniqueKeyInfo` 的 `applyPrefixLengthPolicy`）：

- 根据 **`@Column.type()`**（若有）判断是否允许前缀；不允许时前缀长度强制为 **0**，生成 `` `col` ``，**可在任意可索引列上声明 `@Index` / `@Indexes` / `@UniqueKey`** 而不因前缀报错。
- 无 `Column.type` 时，按 Java 类型保守处理：`String` / `byte[]` 可保留前缀长度，其余类型前缀为 0。
- 类级 `@IndexField` / `UniqueKeyFields` 上误写的非零 `length` 也会按实体字段与 `Column` 收敛。

**仍建议**：需要前缀索引时显式在 `@IndexField` 中写 `length`；字符串列用 `@Column.length` 参与字段级 `@Index` 时，确保 `type` 为字符串类，避免歧义。

#### 2.10.4 `IndexTypeEnum` / `IndexMethodEnum` 提示

- **`FULLTEXT`**：仅适用于 MySQL 全文索引支持的字符型列；不要对数值/日期列使用。
- **`SPATIAL`**：空间索引需列类型与 MySQL 空间类型语义一致（本框架不替你校验业务是否匹配）。
- **`HASH`**：与存储引擎及 MySQL 版本能力相关；默认 **`BTREE`** 为最常见选择。

#### 2.10.5 `@Column.comment` / `@Table.comment` 与「短标题：详述」约定（多语言 / 生成列表表头）

**代码依据**：`BaseService.getLanguageItemsInternal()`、`BaseService.getMenuItemsInternal()`（`autumn-lib`）。

- 当 `comment` **含有英文冒号 `:`** 时，框架在注册多语言条目、菜单展示名时，只取 **第一个 `:` 之前** 的片段作为**短标题**（用于列表/表单列头、菜单名等生成链路中的「展示用名称」）；**冒号之后**的整段视为**详述**（说明字段含义、业务规则、注意事项等），**不会**作为上述短标题展示。
- **`@Table(comment = "...")`** 与 **`@Column(comment = "...")`** 使用**同一套规则**（表级、字段级均按 `:` 分割）。

**给 AI / 开发者的写法约定（建议）**：

- **短标题**：放在 **`:` 之前**，一般用 **1～4 个汉字**（或同等长度的简短词组），保证列表/表头不换行、不溢出。
- **详述**：放在 **`:` 之后**，用完整句子说明字段用途、取值含义、与其它字段关系等。
- **示例**：`状态：0=禁用 1=启用，同步上游审核结果`、`邮箱：用于登录与找回密码，唯一`。

**注意**：完整字符串仍会进入实体上的 `@Column(comment = "...")` / 表注释（如 `ColumnInfo.buildAnnotation`、库表 COMMENT），详述不会丢失；仅**自动多语言初始化、菜单名等**使用冒号前段，避免把长说明塞进「表头」位。

## 3. 模块能力总览（业务域）

- `sys`：用户/角色/菜单/部门/配置/日志/系统运维能力。
- `gen`：代码生成（表 -> 代码）。
- `job`：定时任务管理与执行日志（优先 `LoopJob` 接口周期，复杂时间规则再使用 cron）。
- `db`：数据库备份与恢复。
- `oauth/client`：认证授权与客户端管理。
- `usr`：用户域开放能力与 token。
- `oss`：对象存储与文件管理。
- `lan`：多语言资源管理。
- `spm`：超级位置模型与埋点统计。
- `wall`：防火墙策略（IP/URL/主机访问控制）。

## 4. 开发决策规则（给 AI 的硬约束）

- 能复用框架能力时，禁止重复造轮子（优先找 `Base*Service` / `*Service` 现有能力）。
- 改业务逻辑优先“覆盖扩展点”而非修改内核流程。
- 涉及缓存更新必须同步考虑失效策略（删除单值 + 列表缓存）。
- 涉及加解密必须先判断触发条件（header/请求体字段/接口排除）。
- 涉及兼容改造时，先判断请求/返回类型是否已实现 `Encrypt`；若已实现则禁止再做兼容包装。
- 涉及队列消费必须给出失败、重试、死信处理策略。
- 涉及定时任务时，优先接口式任务（`LoopJob.OneMinute/FiveMinute/...`），避免不必要的 cron 表达式。
- 所有 `ModuleService` 子类默认具备缓存/队列/基础 CRUD 能力，禁止重复封装同类基础组件。
- 新增基础能力前，先检查 `BaseCacheService/ShareCacheService/BaseQueueService/LoopJob` 是否已覆盖需求。
- 涉及页面/模板资源扩展时，优先通过 `TemplateFactory.Template` 在本项目 jar 内提供模板，不要求集中拷贝到入口工程。

## 5. AI 交互输入模板（建议每次需求都带）

### 5.1 需求描述模板（给开发者/产品）

```md
项目：Autumn

需求目标：
- （一句话描述业务目标）

业务范围：
- 模块：（如 sys / oauth / usr / job）
- 实体：（如 XxxEntity）
- 页面/接口：（如 modules/xxx/yyy、/xxx/yyy/*）

关键约束：
- 是否涉及缓存：（是/否，若是说明缓存键与失效时机）
- 是否涉及队列：（是/否，若是说明发送点与消费点）
- 是否涉及定时任务：（是/否，若是说明周期与超时要求）
- 是否涉及加解密兼容：（是/否）
- 是否必须兼容旧接口：（是/否）
```

### 5.2 执行指令模板（给 AI）

```md
你是 Autumn 项目的开发助手，必须优先复用现有框架能力，不得重复造轮子。

任务目标：
- （一句话描述）

涉及模块：
- （如 sys / oauth / cache / queue / job）

需要复用的框架能力（必须优先）：
- Service 继承链：ModuleService -> BaseService -> ShareCacheService -> BaseCacheService -> BaseQueueService
- 默认能力：CRUD + 菜单/多语言初始化 + 缓存 + 队列（无需重复造轮子）
- 缓存：BaseCacheService / CacheService / ShareCacheService（若需要）
- 队列：BaseQueueService / QueueService（若需要）
- 加密：EncryptArgumentResolver / EncryptInterceptor / RsaService / AesService（若需要）
- 协议兼容：CompatibleRequest + Response（同接口支持明文/密文）
- 定时任务：LoopJob 接口周期（固定周期优先，复杂规则再 cron）

执行约束：
- 不要新增重复基础设施
- 不要破坏现有接口返回结构
- 保持权限、缓存、加密、队列语义一致
- 新 Service 默认继承 ModuleService；除非明确说明，否则不要新建平行基础层
- 涉及接口改造时，优先采用 CompatibleRequest<T> + Response<T> 组合
- 涉及定时任务时，优先 LoopJob 接口，不要默认写 cronExpression
- 若未复用 ModuleService 继承能力，必须先解释原因并给替代方案

输出要求：
- 先给实现方案（涉及类、接口、复用点）
- 再给代码改动清单
- 最后给测试与回归检查点
```

### 5.3 最小填写示例（可直接复制）

```md
项目：Autumn
需求目标：新增安全请求审计能力，支持缓存加速与异步落库。
业务范围：模块 oauth；实体 SecurityRequestEntity；接口 /oauth/securityrequest/*
关键约束：涉及缓存（按id与auth失效）；涉及队列（save后异步审计）；不涉及加解密协议改造。
```

## 6. AI 开发执行清单（落地顺序）

- 第一步：确认目标 Service 是否继承 `ModuleService`；若不是，优先改为继承后再开发。
- 第二步：按需求映射能力：
  - 查缓存：`getCache/getListCache/getShareCache`
  - 异步消息：`sendQueue/sendMessage/sendDelay/sendScheduled/sendPriority`
  - 周期任务：`LoopJob.* + onXxx`
- 第三步：实现回源与失效：
  - 覆写 `getEntity/getListEntity`（必要时）
  - 写操作后执行 `removeCache/removeListCache/removeCacheAll`
- 第四步：补全治理能力：
  - 队列失败处理 `onErrorMessage/onDeadMessage`
  - 任务治理 `@JobMeta`（超时/重入/连续错误阈值）
- 第五步：回归检查：
  - 缓存命中/失效是否正确
  - 队列发送/消费/重试是否正确
  - 定时任务是否按周期执行、是否可观测（list/stats/alerts）

## 7. AI 常见反模式（禁止）

- 缓存反模式：
  - 直接 `baseMapper` 查询后手写 `Map` 缓存，绕过 `getCache/getListCache/getShareCache`。
  - 写库后不做 `removeCache/removeListCache/removeCacheAll`，导致脏读。
- 队列反模式：
  - 业务中手动新建线程池或阻塞队列替代 `BaseQueueService`。
  - 仅发送消息不定义失败处理策略（`onErrorMessage/onDeadMessage`）。
- 定时任务反模式：
  - 固定周期任务直接上 cron，不实现 `LoopJob.*` 周期接口。
  - 任务无超时、无防重入、无连续错误保护，不配置 `@JobMeta`。
- 分层反模式：
  - 把业务逻辑堆在 `controller/gen/*` 可重生层，导致再次生成被覆盖。
  - 新建与 `ModuleService` 平行的基础服务层，重复平台能力。
- 建表注解反模式：
  - `Column(isUnique = true)` 与同一列的 `@Index` / `@Indexes` 重复声明。
  - 字段 `@Index` 与类级 `@Indexes` 对同一列重复声明。
  - `@Column.type()` 与真实库类型不一致，导致前缀长度收敛不符合预期（见 2.10.3、`IndexPrefixRules`）。
  - `@Column.comment` / `@Table.comment` 写成整段长说明却**无冒号**，或**冒号前**写成整句长标题，导致多语言/列表表头位展示过长或难以对齐（见 2.10.5）。

## 8. 一页式速查卡（Cheat Sheet）

- 标准 Service 继承：
  - `class XxxService extends ModuleService<XxxDao, XxxEntity>`
  - 固定周期任务直接 `implements LoopJob.OneMinute/...`
- 标准接口签名：
  - 默认：`Request<T> -> Response<R>`
  - 兼容：`CompatibleRequest<T> -> CompatibleResponse<R> + @Endpoint(compatible=true)`
- 加密握手顺序（客户端）：
  - `POST /rsa/api/v1/init` -> 保存 `session/AES`
  - `POST|GET /rsa/api/v1/endpoints` -> 保存端点能力映射
  - 业务请求按映射决定是否加密并携带 `X-Encrypt-Session`
- 响应加密触发：
  - 有 `X-Encrypt-Session` -> 响应按 JSON 加密返回
  - 无 `X-Encrypt-Session` -> 返回明文；`CompatibleResponse` 自动降级为 `data`
- 缓存最小实践：
  - 查：优先 `getCache/getListCache/getShareCache`
  - 改：写库后必须 `removeCache/removeListCache/removeCacheAll`
- 队列最小实践：
  - 发：`sendQueue/sendDelay/sendScheduled/sendPriority/sendBatch`
  - 消：覆写 `onQueueMessage`，并补 `onErrorMessage/onDeadMessage`
- 错误恢复（客户端）：
  - `FORCE_ENCRYPT_SESSION_REQUIRED` / `RSA_CLIENT_PUBLIC_KEY_NOT_FOUND` -> 重新执行 `/rsa/api/v1/init`
  - 解密失败 -> 清理本地会话与 AES 缓存后重握手
- 提测前 8 项检查：
  - 1) 是否复用 `ModuleService` 继承链
  - 2) 是否使用 `Request/Response` 或 `Compatible*`
  - 3) 是否验证明文调用路径
  - 4) 是否验证密文调用路径
  - 5) 是否验证 header 有/无两种响应
  - 6) 是否覆盖缓存命中与失效
  - 7) 是否覆盖队列失败/重试/死信路径
  - 8) 是否覆盖 LoopJob 周期执行与可观测接口（list/stats/alerts）
## 9. 按需扩展索引（低频，不默认加载）

- 模板库（模块任务指令、组合索引）：
  - `@AI_TEMPLATES.md`
- 加解密兼容专项（接口迁移与客户端对接）：
  - `@AI_CRYPTO.md`
- 治理与维护（规则优先级、术语、多项目协作、维护规范）：
  - `@AI_GOVERNANCE.md`
- 安全专项（签名接入、灰度上线、攻防演练）：
  - `@AI_SECURITY.md`
- 提问模板库（可复制指令）：
  - `@AI_PROMPTS.md`
- 默认加载建议：
  - 日常开发：仅 `AI_MAP.md`
  - 接口加解密改造：`AI_MAP.md + AI_CRYPTO.md`
  - 模块新建/批量生成：`AI_MAP.md + AI_TEMPLATES.md`
  - 规范梳理/文档治理：`AI_MAP.md + AI_GOVERNANCE.md`
  - 安全改造/演练：`AI_MAP.md + AI_SECURITY.md`

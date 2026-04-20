# Autumn 代码生成与规范开发流程

> 用途：说明 **gen 模块代码生成链路**、**数据库反射驱动生成**、以及 **推荐的三步开发节奏**（实体 → 生成骨架 → 业务与页面）。  
> 与 **`docs/AI_STANDARDS.md`**（分层与禁止项）、**`docs/AI_MAP.md`**（继承链与代码生成模板分层，约 2.7～2.8A）互补：**约束以 STANDARDS 为准，流程与能力细节以本文为准**。

## 1. 代码生成框架：位置与端到端流程

### 1.1 模块与入口

- **实现位置**：`autumn-modules` 下的 **`gen`** 模块（包路径如 `cn.org.autumn.modules.gen`）。
- **后台入口**：菜单「代码生成」对应页面路径 **`modules/gen/generator`**；HTTP 下载 ZIP 的映射为 **`GET /gen/generator/code`**（参数 `tables`、`genId`，见 `GeneratorController`）；服务端在 **`GeneratorService.generatorCode`** 中组装字节流（与权限串 `gen:generator:code` 等配合，见 `GeneratorService.init()` 注册的菜单）。
- **核心工具类**：**`GenUtils`**（`cn.org.autumn.modules.gen.utils.GenUtils`）负责：
  - 声明 Velocity **模板清单**（`getTemplates()` / `getSiteTemplates()`）；
  - 将 **`TableInfo` + `GenTypeWrapper`** 填入 **`VelocityContext`** 并逐模板渲染；
  - 按模板类型计算 ZIP 内 **相对路径**（`getFileName`），输出标准 Maven 目录结构（`main/java/...`、`main/resources/templates/...`）。

### 1.2 数据库反射：从表元数据到 `TableInfo`

- **数据访问**：`GeneratorService` 注入 **`TableDao`**（`cn.org.autumn.table.dao.TableDao`），通过 JDBC **反射当前连接库**中的表、列、索引、唯一键等元数据。
- **构建过程**（概念顺序）：
  1. 用户选择表名列表与生成方案（**`GenTypeEntity` / `GenTypeWrapper`**：根包、模块包、模块名、表前缀、作者等）。
  2. **`build(tableName, wrapper)`**：`queryTable` / `queryColumns` / `showIndex` / `showKeys` 等拉取元数据，组装为框架统一的 **`TableInfo`**（含主键、列列表、`ColumnInfo` 类型映射、是否含 `BigDecimal` 等）。
  3. 每张表调用 **`GenUtils.generatorCode(tableInfo, wrapper, zip, GenUtils.getTemplates(), tables)`**；全部表处理完后，再调用一次 **`GenUtils.generatorCode(null, wrapper, zip, GenUtils.getSiteTemplates(), tables)`** 生成 **Site / Menu / SitePages** 等聚合文件（第二次调用携带已收集的 `tables` 列表供模板使用）。

**要点**：生成器以 **库表真实结构** 为输入，能在已有表上快速导出与 Autumn 约定一致的 **Entity / Dao / Service / Controller / 页面骨架**，与启动期 **实体注解驱动建表**（`autumn.table.*`）形成「表 ↔ 实体 ↔ 代码」闭环；日常演进仍以 **实体 + 框架同步表** 为主，见 **`docs/AI_STANDARDS.md`** 第 8 节。

### 1.3 模板目录与产物路径

- **模板根目录**：`autumn-modules/src/main/resources/template/`，扩展名为 **`.vm`**（Apache Velocity）。
- **`getTemplates()` 默认清单**（与 `GenUtils` 源码一致）：

| 模板 | 典型产物（ZIP 内） |
|------|-------------------|
| `Entity.java.vm` | `{package}/{module}/entity/{Class}Entity.java` |
| `Dao.java.vm` | `{package}/{module}/dao/{Class}Dao.java` |
| `Service.java.vm` | `{package}/{module}/service/{Class}Service.java` |
| `Controller.java.vm` | `{package}/{module}/controller/{Class}Controller.java` |
| `ControllerGen.java.vm` | `{package}/{module}/controller/gen/{Class}ControllerGen.java` |
| `list.html.vm` | `main/resources/templates/modules/{module}/{entity小写}.html` |
| `list.js.vm` | `main/resources/templates/modules/{module}/{entity小写}.js` |

- **`getSiteTemplates()`**：`SitePages.java.vm`、`Site.java.vm`、`Menu.java.vm` → `site/{Module}Pages.java`、`{Module}Site.java`、`{Module}Menu.java`（模块名首字母大写等规则见 `getFileName`）。

**统一调整生成行为**：修改对应 **`.vm`** 后，通过后台重新生成或团队约定的生成任务输出 ZIP，**不要**在「可重生产物」上长期手写差异（见本文第 3 节）。

### 1.4 与 `docs/AI_MAP.md` 的关系

- 生成物分层（可重生 / 可维护 / 结构层）的 **速查表与 AI 硬约束** 见 **`docs/AI_MAP.md`** 代码生成模板分层（约 2.8A）。
- 本文第 1 节补充 **调用链（GeneratorService → TableDao → GenUtils → Velocity）** 与 **反射输入**；分层纪律仍以 **STANDARDS + MAP** 为权威。

---

## 2. 推荐三步开发流程（降低 AI 随意性与维护成本）

### 第一步：需求总览与实体建模（先于生成）

1. **对齐需求与边界**，列出领域实体、关键字段、查询维度、缓存与异步是否需要。
2. **编写实体类**（放在目标模块 `entity` 包下），严格遵循：
   - **`docs/AI_STANDARDS.md`** 第 8～10 节：**`@Table` / `@Column` / `@Index` / `@Indexes`**、注释格式（短标题 + 半角 `:`）、模块目录 = 包段 = 表前缀且**不把前缀写进类名**；**凡 `isUnique=true` 的 `@Column` 禁止再对该字段使用 `@Index`**（§10.2）等。
   - **`docs/AI_STANDARDS.md` §10.4**：自增 **`Long id`** 仅服务后台生成 CRUD；**须另有唯一业务主键**（`Uuid.uuid()` / **`SnowflakeId`**），插入前赋值；关联与对外 ID **禁止**用 **`id`**。
3. **缓存声明**：在实体上使用 **`cn.org.autumn.annotation.Cache`** / **`@Caches`**（见本文第 4.1 节），声明字段级或类级复合键、**`name`** 区分同一实体上的多套缓存策略、**`unique`** 区分单值与列表语义、**`create`** 是否与自动建记录行为配合。
4. **依赖框架能力，禁止重复造轮子**：业务 Service 默认继承 **`ModuleService`**，自动具备 **CRUD、菜单/多语言初始化、缓存、队列** 等能力（继承链见 **`docs/AI_MAP.md`** 与本文第 4 节）。**不要**自建平行缓存中间层、消息封装或调度线程替代 **`LoopJob`**。

### 第二步：生成骨架代码（优先人工触发生成）

1. **优先由开发者在后台使用代码生成**（或团队 CI 任务）导出 ZIP，解压到与生成器一致的目录结构，减少 AI 手写大面积骨架带来的路径、包名、命名不一致。
2. **生成代码放置规则**：
   - 解压路径应与 **`GenUtils.getFileName`** 约定一致（即与「再生成一次」时覆盖的文件一致），避免同一模块存在两套平行目录。
3. **可编辑边界**（与 **`docs/AI_STANDARDS.md`** 第 11 节一致）：
   - **`controller/gen/*`、`*Pages.java`、生成的 `list.html` / `list.js`**：视为生成源，**禁止**在上面叠加业务逻辑；需要改行为时改 **`.vm` 模板**并重生成。
   - **`Controller.java` / `Service.java` / `Dao.java`（非 gen）** 等模板中的 **空壳或薄骨架**：**业务逻辑只写在 `Service`**，`Controller` 只做编排；**在此实现**。
   - 若模板产出的某文件**已含完整生成逻辑（非空壳）**，则**不要**在该文件中堆业务；应改可维护层或独立类/接口。
4. **若必须由 AI 按模板生成**：AI 应 **严格参照 `template/*.vm` 的占位符与目录约定** 生成，输出路径与后台生成器 **完全一致**，以便日后「重生成」时 diff 可控、避免混用两套风格。

### 第三步：业务、接口与页面整合

1. 在 **可维护 `Service`** 中实现领域规则、事务、缓存失效、队列发送、`LoopJob` 周期任务等。
2. 在 **可维护 `Controller`**（或独立业务 Controller）中增加 **与 `ControllerGen` 不冲突** 的 URL 设计，见 **`docs/AI_STANDARDS.md`** 第 3 节。
3. **页面**：复杂交互优先 **独立页面**（模块 `pages/` + **`site/*Site`** 上 **`@PageAware`**）；与生成列表页的整合遵循 **FreeMarker** 规则（**`docs/AI_STANDARDS.md`** 第 7 节）。

---

## 3. 代码生成使用原则（摘要）

- **改通用行为**：改 **`resources/template/*.vm`** → 再生成；**不要**逐文件改 `ControllerGen` / `*Pages` / `list.html` / `list.js`。
- **改单模块业务**：改 **非 gen** 的 `Controller` / `Service` / `Site` / `Menu`。
- **实体与表**：以实体与框架开关为权威；生成器用于 **加速初始骨架**，不是绕过 **`docs/AI_STANDARDS.md`** 第 8 节的理由。

---

## 4. 基类能力详解（实现前必读，避免重复实现）

以下类均在 **`autumn-lib`**，业务 **`ModuleService`** 继承链已包含 **`ShareCacheService` → `BaseCacheService` → `BaseQueueService`**（见 **`docs/AI_MAP.md`** 继承链说明）。

### 4.1 `BaseCacheService` 与 `@Cache` / `@Caches`

- **职责**：围绕**当前实体类型**提供 **本地（EhCache）+ Redis** 两级缓存、统一 **`CacheService.compute`** 回源、以及 **`save/update/remove`** 等写路径上的 **按实体 / 按 key 失效**（减少手写删缓存遗漏）。
- **配置维度**：子类可覆盖 **`getCacheExpire` / `getRedisExpire` / `getCacheMax` / `getCacheUnit`** 等；支持按 **`naming`** 或 **`Class<X>`** 细分（与 **`getConfig` / `getListConfig` 系列** 对应）。
- **注解协作**（`cn.org.autumn.annotation.Cache`）：
  - **类上 `value`**：复合键字段名列表；**`name`**：同一实体多套缓存通道名；**`unique`**：是否单键单实体；**`create`**：未命中时是否与「自动建记录」语义配合（与 **`getCached` / `getEntitied`** 系列关系见注解 Javadoc）。
  - **`@Caches`**：打包多个 **`@Cache`**；与单个类上 **`@Cache`** 的合并规则见 **`BaseCacheService.findAllCompositeCaches`**（单个 `@Cache` 优先于同名 `@Caches` 项）。
- **常用 API 族**（命名随重载变化，具体以源码为准）：
  - **单值**：`getCache` / `putCache` / `removeCache` / `clearCache`；**按命名或类型**的 `getNameCache`、`getTypeCache` 等。
  - **列表**：`getListCache` / `putListCache` / `removeListCache` / `clearListCache` 及命名变体。
  - **回源**：覆盖 **`getEntity` / `getListEntity` / `getNameEntity`** 等，使「未命中则加载」走统一路径。
  - **批量失效**：`**removeCacheAll**`、`**removeCacheByEntity**`、写后务必同步列表缓存。
- **原则**：读走 **`getCache*`**，写后 **`remove*`**；不要复制一套 TTL 或 Redis 键规则。

### 4.2 `ShareCacheService`

- **定位**：在 **多个独立子项目 / 不同实体定义** 之间，按 **约定缓存名** 共享数据；**值类型默认 `Object.class`**，也可按类型覆写 **`getShareModelClass`** 等。
- **配置**：**`getShareCacheName` / `getShareCacheName(Class)`**、**`getShareCacheExpire`**、**`isShareCacheNull`** 等可覆盖；**`getShareConfig` / `getShareConfig(shareName)` / `getShareConfig(shareName, valueType)`** 构建独立 **`CacheConfig`**。
- **常用 API**：**`getShareCache` / `putShareCache` / `removeShareCache`**；列表与 Map 变体 **`getShareListCache` / `getShareMapCache`** 等；支持 **复合 key**、**`Supplier` 回源**（未命中时回调），或覆写 **`getShareEntity` / `getShareEntity(shareName, key)`** 提供数据源。
- **原则**：跨应用复用数据优先 **同名 share 区 + 上述 API**，不要新建「全局静态 Map」或自建 Redis 工具类替代。

### 4.3 `BaseQueueService`

- **职责**：基于 **`QueueService`** 提供 **队列名推导**、**`QueueConfig` 构建**、**注册消费者**、**发送**（即时 / 延迟 / 定时 / 优先级 / 批量）等；默认 **`getQueueName()`** 由实体类名派生（如 `{entity}queue`），并支持 **suffix / 消息体类型** 维度区分多队列。
- **消费扩展**：覆写 **`onQueueMessage(T body)`** 返回 `boolean` 表示是否成功；**`onErrorMessage` / `onDeadMessage`** 处理错误与死信；**`register()` / `register(QueueConfig)`** 与 **`isAutoQueue` / `getIdleTime`** 控制自动启停消费者。
- **发送 API 族**（以源码为准）：**`sendQueue` / `sendMessage` / `sendDelay` / `sendScheduled` / `sendPriority` / `sendBatch`**，以及带 **suffix / Class** 的重载。
- **原则**：异步、削峰、延迟任务优先走 **队列体系**（类型含 MEMORY、REDIS_LIST、REDIS_STREAM、DELAY、PRIORITY 等，见 **`docs/AI_MAP.md`** 队列体系小节），不要自建无治理的 `Executor` 长轮询替代生产链路。

---

## 5. 文档交叉引用

| 主题 | 文档 |
|------|------|
| 应用层强制规范与生成层禁止项 | **`docs/AI_STANDARDS.md`** 第 11～14 节 |
| ModuleService 继承链与生成矩阵速查 | **`docs/AI_MAP.md`** 约 2.7、2.8A |
| 模块任务提示模板（含 gen） | **`docs/AI_TEMPLATES.md`** 第 2.5 节 |
| 文档索引与加载组合 | **`docs/AI_INDEX.md`** |
| 多项目喂给顺序 | **`docs/AI_GUIDE.md`** |

---

## 6. 维护约定

新增与「生成链路、三步流程、基类能力说明」相关的规则时，**优先更新本文**，并同步 **`docs/AI_INDEX.md`**、**`docs/AI_GUIDE.md`** 中的加载组合说明；与 **`docs/AI_STANDARDS.md`** 冲突时 **以 STANDARDS 为约束权威**，本文侧重 **流程与能力说明**。

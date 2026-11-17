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
   - Lombok：**`@Getter` + `@Setter`**（**禁止**手写 getter/setter，**禁止**表实体 `@Data`，见 **`docs/AI_STANDARDS.md` §2.0**）。
   - **`docs/AI_STANDARDS.md`** 第 8～10 节：**`@TableName` + `@Table(comment=…)`（`@Table` 不写 `value`）、**`@Column` / `@Index`**、`comment` **2～8 字短标题:`说明`**、表前缀不进类名等。
   - **`docs/AI_STANDARDS.md` §10.4** / **`docs/AI_DUAL_KEY.md`**：双键按需；触结构变更/新外键/新开放 API 时须对齐。
3. **缓存声明**：在实体上使用 **`@Cache` / `@Caches`**；**`create=true`** 仅白名单场景（§13.1）。
4. **Service 承载全部实体业务**：空壳 **`XxxService extends ModuleService`**（或 **`EncryptModuleService`**）；**禁止**把业务写进 Controller。需日志时类上 **`@Slf4j`**。勿自建平行缓存/队列/`LoopJob`。

### 第二步：空壳 + 可选 ZIP（纪律见 **`docs/AI_STANDARDS.md` §11**）

1. 创建 §11.1 表中必产文件；对内路径 `{module}/{entitylower}`。  
2. 未 ZIP 前若需模板同款 CRUD：按 **§11.2** 写入 `*ControllerGen`（须可整文件替换）。  
3. **禁止 AI** 写 `list.html` / `list.js` / `*Pages`；开发者可选 ZIP 覆盖 Gen/资源层（§11.0）。  
4. 特有逻辑 → `*Service` / 实体 `*Controller` / 独立 Api Controller；**不进**模板产物。

### 第三步：业务与接口

1. 实体业务 → **`*Service`**（§2.1）。对内薄 Controller（§3）；对外 `POST /{module}/api/v1/{action}`（§4）。  
2. 复杂页 → `pages/` + `Site` + `@PageAware`；list 可选 ZIP；FTL 见 §7。  
3. `ControllerGen` 模板每个 CRUD 入口调用 **`AdminAccessGuard#enforceAdminIpApiIfRequired`**（系统管理员 + 管理 IP；自助模块自动跳过），并与 `GenAdminAccessAspect` 双保险（§14 / C-20 / **`docs/AI_ADMIN_IP_BIZ.md`**）。

---

## 3. 代码生成使用原则（摘要）

权威：**`docs/AI_STANDARDS.md` §11.0**（可替换 · 禁特有逻辑 · 空壳除外）。  
改通用行为改 **`.vm` 再生成**；改业务改非 gen 的 Controller/Service/Site/Api Controller；**禁 AI** 写 list/Pages。

---

## 4. 基类能力详解（实现前必读，避免重复实现）

以下类均在 **`autumn-lib`**，业务 **`ModuleService`** 继承链已包含 **`ShareCacheService` → `BaseCacheService` → `BaseQueueService` → `AutoIdService`**（见 **`docs/AI_MAP.md`** 继承链说明；双键详见 **`docs/AI_DUAL_KEY.md`**）。

### 4.0 `AutoIdService` 与第二主键 `uuid`

- **职责**：写库前为 **`UuidBased`** / **`SnowBased`** 实体自动补全空 **`uuid`**（不覆盖已有值）。  
- **默认生效**：凡 **`extends ModuleService`** 的 Service 已具备；实体只需 `implements UuidBased` 或 `SnowBased`。  
- **手动调用**：`AutoIdService.autoId(entity)`（非继承链场景）。  
- **纪律**：**`id`** 仅 gen CRUD；关联与 API 只用 **`uuid`**。

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
- **禁止**：手写 **`redisTemplate.keys("cache:" + name + ":*")`** 或等价 **`KEYS`** 清 Redis；按名清理用 **`clearCache` / `cacheService.clear(name)`**（框架内已 **SCAN 分批**）。见 **`docs/REDIS_KEYS_AND_SCAN.md`**。

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
- **与 `TagTaskExecutor` 区分**：Service 内 **内存 `ConcurrentMap` + 本机 drain**（如补签待处理表）用 **`TagRunnable` + `onFinished(FinishStatus)`**，见 **`docs/AI_ASYNC_TASK.md`**；**不要**与 `BaseQueueService` 混为一谈。

---

## 5. 文档交叉引用

| 主题 | 文档 |
|------|------|
| 命名与 `comment`（§3.1～§3.2） | **`docs/AI_DUAL_KEY.md`** |
| **`UserBased`** / 按用户唯一 `user` | **`docs/AI_DUAL_KEY.md` §3.3、§4.3** |
| 应用层强制规范与生成层禁止项 | **`docs/AI_STANDARDS.md`** 第 11～14 节 |
| ModuleService 继承链与生成矩阵速查 | **`docs/AI_MAP.md`** 约 2.7、2.8A |
| **`TagRunnable` / `onFinished` / 内存队列状态机** | **`docs/AI_ASYNC_TASK.md`** |
| 模块任务提示模板（含 gen） | **`docs/AI_TEMPLATES.md`** 第 2.5 节 |
| 文档索引与加载组合 | **`docs/AI_INDEX.md`** |
| 多项目喂给顺序 | **`docs/AI_GUIDE.md`** |

---

## 6. 维护约定

新增与「生成链路、三步流程、基类能力说明」相关的规则时，**优先更新本文**，并同步 **`docs/AI_INDEX.md`**、**`docs/AI_GUIDE.md`** 中的加载组合说明；与 **`docs/AI_STANDARDS.md`** 冲突时 **以 STANDARDS 为约束权威**，本文侧重 **流程与能力说明**。

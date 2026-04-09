# Autumn AI 启动上下文（首轮必读）

> 用途：给 AI 的最小启动上下文。默认只读本文件，再按任务类型追加其他文档。
> 统一索引：`@AI_INDEX.md`

## 1. 核心原则（必须遵守）

- 优先复用框架能力，禁止重复造轮子。
- 默认从 `ModuleService` 继承链出发实现业务：
  - `ModuleService -> BaseService -> ShareCacheService -> BaseCacheService -> BaseQueueService`
- 业务逻辑放可维护层（`controller/*`、`service/*`），避免放 `controller/gen/*` 可重生层。
- 页面开发默认使用**用户视角**描述功能与交互；除非需求有特殊说明，禁止在页面文案中出现开发术语、后台表名/函数名、技术架构描述。
- **应用层与数据访问强制规范**（高内聚低耦合、内外 API、gen 路由隔离、禁止生产 `@Scheduled`、新接口禁用 `@RequiresPermissions`、FreeMarker、**禁止常规 DDL `.sql`、注解建表**、**Dao 仅经 Provider 写 SQL**、**Controller 禁用 Dao**、**statics/pages/Site/PageAware** 等）见 **`AI_STANDARDS.md`**，与本文同步遵守。

## 2. 默认技术路径（高频）

- 接口：
  - 默认：`Request<T> -> Response<T>`
  - 兼容：`CompatibleRequest<T> -> CompatibleResponse<T> + @Endpoint(compatible=true)`
- 缓存：优先 `getCache/getListCache/getShareCache`，写后必须失效。
- 队列：优先 `BaseQueueService`（发送、消费、重试、死信）。
- 任务：固定周期优先 `LoopJob.*`，复杂日历规则才用 cron。
- 生成链路：实体注解驱动建表 -> gen 生成骨架 -> 业务补在非 gen 层。
- 生成分层约束：`ControllerGen/SitePages/list.html/list.js` 属于可重生层（默认不改）；业务开发优先落在 `Controller/Service/Site/Menu` 可维护层。

## 3. 注解能力速查（高频）

- `@JobMeta`（任务治理）
  - 作用域：类 + 方法（方法级覆盖类级）。
  - 核心参数：`skipIfRunning`（防重入）、`timeout`（超时观测）、`maxConsecutiveErrors`（连续错误自动禁用）、`assign`（多节点分配）、`delay/async`（异步调度）。
  - 建议：秒级/分钟级任务默认开启 `skipIfRunning=true`，并设置合理 `timeout`。
- `@TaskAware`（任务触发）
  - 定义 cron 触发、环境模式（`mode`）、展示备注（`remark`）。
  - 职责边界：负责“什么时候触发”；运行治理交给 `@JobMeta`。
- `@Endpoint`（接口加解密语义）
  - `force=true`：强制密文会话（入参或出参）。
  - `compatible=true`：普通对象也允许按请求头走兼容加密返回。
  - `hidden/reason`：控制是否对外暴露在端点清单中。
- `@Cache / @Caches`（缓存索引）
  - 声明缓存键字段、唯一性、是否未命中自动创建（`create=true`）。
  - 约束：写操作后必须做缓存失效，避免脏读。
- `@EnvAware`（配置注入）
  - 在配置 Bean 字段上声明配置键（如 `site.domain`、`node.tag`）。
- `@Table` / `@Column` / `@Index` / `@Indexes` / `@IndexField`（注解驱动建表，见 `AI_MAP.md` 2.10 节；**表名 / 前缀**见 **§3.2**，**存储引擎 / 字符集 / 排序规则**见 **§3.1**）
  - `@Table.comment` / `@Column.comment`：`BaseService` 多语言初始化在注释含 **`:`** 时**只取冒号前**作为列表/菜单等处的**短标题**；冒号后为详述。建议 **`短标题（约 1～4 字）：详细说明`**，避免表头被长文案撑满（详见 `AI_MAP.md` 2.10.5）。
  - `@Column(isUnique = true)`：已在 DDL 中为该列生成唯一约束；**禁止**再在同一字段上叠 `@Index`，也避免用 `@Indexes` 再声明同一单列唯一/普通索引，以免重复索引与迁移对比噪音。
  - 字段上已用 `@Index` 的列：**不要**在类级 `@Indexes`（或类级 `@Index` 的 `fields`）里再声明同列的同用途索引，避免 `TableInfo` 收集到重复 `IndexInfo`、建表/变更阶段生成重复索引。
  - 索引前缀：`IndexPrefixRules` 会按 `@Column.type()` 与 Java 类型收敛前缀长度，非字符串/二进制串列不会生成非法 `` `col`(n) ``；数值、日期等列可正常加 `@Index`（整列索引）。`IndexTypeEnum.FULLTEXT` 仍仅适用于字符型列（MySQL 全文索引语义）。详见 `AI_MAP.md` 2.10.3。

### 3.1 表结构 SQL 语义枚举（`annotation.sql`）与字符集约定

包路径：`cn.org.autumn.table.annotation.sql`。注解层使用**语义化枚举**，`getSqlName()` 在**当前实现**下给出 **MySQL/MariaDB** DDL 字面量；其它方言由后续适配映射或忽略（见各枚举类注释与 `Dialect`）。

| 枚举 | 用途 |
|------|------|
| `Engine` | `@Table.engine()`：InnoDB、MyISAM 等；非 MySQL 系数据库可忽略或另行映射。 |
| `CharacterSet` | `@Table.charset()` / `@Column.charset()`：utf8、utf8mb4、gbk 等。 |
| `Collation` | `@Table.collation()` / `@Column.collation()`：具体排序规则名，或 **不写**（见默认）。 |
| `Dialect` | 预留：目标 SQL 方言路由（与 `autumn.database` 等配置协同扩展）。 |

**`@Table` 默认（未写属性时）**

- `engine = Engine.INNODB`
- `charset = CharacterSet.UTF8`（historic utf8 / utf8mb3 语义，**不含 Emoji**）
- `collation = Collation.INHERIT` → DDL **不拼** `COLLATE`，由服务器按字符集默认处理

**表级 `CharacterSet.INHERIT`**：请勿使用；若误写，`TableInfo` **回退为 `UTF8`**，避免表无字符集。

**`@Column` 默认**

- `charset = CharacterSet.INHERIT` → 列上**不生成** `CHARACTER SET`，继承表默认
- `collation = Collation.INHERIT` → 列上**不生成** `COLLATE`，继承表默认

**运行时行为（`MysqlTableService` + `QuerySql`，`autumn.table.auto=update`）**

- 新建表：`CREATE TABLE ... ENGINE= ... DEFAULT CHARACTER SET ...`（`Collation` 非 `INHERIT` 时带 `COLLATE`）；列上仅在显式指定时追加 `CHARACTER SET` / `COLLATE`。
- 表字符集与实体不一致：`autumn.table.sync-charset`（默认 `true`）下对已有表执行 `ALTER TABLE ... CONVERT TO ...`（大表可能锁表，可关开关后改手动迁移）。
- 列级字符串类型：与库中 `information_schema` 比较字符集/排序规则，不一致则 `MODIFY` 对齐。

**与 JDBC 的关系（存储 ≠ 连接编码名）**

- URL 使用 **`characterEncoding=UTF-8`**（Java 标准名）；**不要**写 `characterEncoding=utf8mb4`。
- 需要会话级 **utf8mb4**（Emoji 等）时，在 URL 增加例如 **`connectionCollation=utf8mb4_unicode_ci`** 或 **`utf8mb4_0900_ai_ci`**（MySQL 8），与表上 `CharacterSet.UTF8MB4` 对齐。
- 仅部分表需要 Emoji：连接统一 utf8mb4 会话通常**不影响**仅 utf8 列的存量数据；表/列仍应用 `UTF8MB4` 存 4 字节字符。

**兼容与注意**

- 比较字符集时 **`utf8` 与 `utf8mb3` 视为等价**（MySQL 8 元数据可能返回 `utf8mb3`）。
- 升级到 `utf8mb4` 时注意 **索引字节长度**（如 `VARCHAR(255)` 唯一索引）与 DDL 耗时；生产大表谨慎使用自动 `CONVERT`。
- 标识符经校验后再拼入 DDL，降低注入风险。

### 3.2 表名（`@Table` / `@TableName`）约定

**推荐物理表名形态**：`{模块短前缀}_{实体核心蛇形}`。

- **模块短前缀**：与 Maven 模块及包路径 `cn.org.autumn.modules.<模块>/` 对应，通常取目录名 + 下划线，如 `spm` → **`spm_`**、`sys` → **`sys_`**，用于区分业务域、避免跨模块撞表名。
- **实体核心蛇形**：类名去掉后缀 **`Entity`**，再将剩余驼峰转为**小写 + 下划线**（`TableInfo.getTableName` 与 `HumpConvert` 规则一致）。

**示例**（`SuperPositionModelEntity`，模块 `spm`）：

- 去后缀 → `SuperPositionModel` → 蛇形 → `super_position_model`
- 加前缀 → **`spm_super_position_model`**

**定义方式（按团队习惯选一；推荐减少重复）**

1. **单点表名（推荐）**：只写一处物理表名——**`@Table` 的 `value` 留空**，并写 MyBatis-Plus 的 **`@TableName("spm_super_position_model")`**。Autumn 的 `TableInfo.getTableName` 在 `value` 为空时会**读取 `@TableName` 的值**作为物理表名，与 MP 一致，无需两段相同字符串。仍需保留 **`@Table`**（至少带 `comment` 等），否则不参与注解建表扫描。
2. **显式双写**：`@Table(value = "spm_super_position_model", comment = "...")` 与 `@TableName("spm_super_position_model")` 相同；适合不依赖 MP 表名、或希望 Autumn 侧自包含表名的场景。
3. **前缀 + 推导**：`@Table(prefix = "spm_", comment = "...")` 且 **`value` 与 `@TableName` 均留空**时，按「前缀 + 去 `Entity` 蛇形」生成；若只填了 `@TableName` 则同 **1**。

**`@Table.module()`**：可选，多用于生成/展示元数据，**不参与**物理表名字符串拼接；表名前缀以 `prefix` / `value` 为准。

## 4. 加密最小约束

- 请求解密触发：请求体包含 `ciphertext + session`。
- 响应加密触发：请求头包含 `X-Encrypt-Session`。
- 握手入口：`/rsa/api/v1/init`（仅握手，不混入业务）。
- 无 `X-Encrypt-Session` 时返回明文；兼容响应可降级为 `data`。

## 5. 开发前 AI 自检（每次执行）

- 现有基类/模块能力是否已覆盖需求？
- 变更是否破坏缓存一致性、加密语义、权限语义、任务治理语义？
- 是否给出回归点（明文/密文/header 有无、缓存失效、队列失败、任务观测）？

## 6. 按需追加文档（不要全量加载）

- 应用层开发规范：`@AI_STANDARDS.md`
- 核心能力详解：`@AI_MAP.md`
- **PostgreSQL 支持（完整方案、兼容性、变更清单）：`@AI_POSTGRESQL.md`**
- **依赖方升级 autumn（清单、扫描脚本、自动化边界）：`@AI_UPGRADE.md`**
- 加解密兼容专项：`@AI_CRYPTO.md`
- 模块任务模板：`@AI_TEMPLATES.md`
- 代码生成流程与三步开发、基类缓存/队列说明：`@AI_CODEGEN.md`
- 治理与协作规范：`@AI_GOVERNANCE.md`
- 安全专项（签名/灰度/演练）：`@AI_SECURITY.md`
- 提问模板库：`@AI_PROMPTS.md`

## 7. 推荐加载组合

- 日常开发：`AI_BOOT.md + AI_MAP.md + AI_STANDARDS.md`
- 接口加解密改造：`AI_BOOT.md + AI_MAP.md + AI_STANDARDS.md + AI_CRYPTO.md`
- 模块新建/代码生成：`AI_BOOT.md + AI_MAP.md + AI_STANDARDS.md + AI_CODEGEN.md + AI_TEMPLATES.md`
- 规范梳理/团队协作：`AI_BOOT.md + AI_MAP.md + AI_STANDARDS.md + AI_GOVERNANCE.md`
- 安全改造/攻防演练：`AI_BOOT.md + AI_MAP.md + AI_STANDARDS.md + AI_SECURITY.md`
- **PostgreSQL / 多库适配排查：`AI_BOOT.md + AI_MAP.md + AI_STANDARDS.md + AI_POSTGRESQL.md`**
- **新增 Mapper 手写 SQL / SqlProvider：`AI_BOOT.md + AI_POSTGRESQL.md`（§ RuntimeSql）**
- **业务仓库升级 autumn：`AI_BOOT.md + AI_MAP.md + AI_STANDARDS.md + AI_UPGRADE.md`（多库/PG 时叠加 `AI_POSTGRESQL.md`）**
- 仅对照 PG 清单与兼容性、少读其它文档时：可直接打开 `AI_POSTGRESQL.md`（仍建议与 `AI_MAP.md` 中表结构/方言章节交叉核对）。

## 8. PostgreSQL 支持（摘要）

> **完整说明、变更过程与兼容性矩阵见 `AI_POSTGRESQL.md`。**

- **配置**：`autumn.database: postgresql`，数据源与 `pagehelper` 方言对齐；示例见 `application-postgresql.yml`。
- **路由**：`AutumnDatabaseType` + `RoutingRelationalTableOperations` / `RoutingRuntimeSqlDialect`；注解建表仅对 mysql/mariadb/postgresql 执行同步。
- **方言与 SQL**：底层为 `RuntimeSqlDialect`（引号、`LIMIT`、`FIND_IN_SET` 替代等）；**业务手写 SQL 推荐**继承 **`RuntimeSql`**（`cn.org.autumn.database.runtime.RuntimeSql`），在 `*DaoSql` 中调用 `quote`、`limitOne`、`likeContainsAny` 等，避免各处重复 `RuntimeSqlDialectRegistry.get()`。详见 **`AI_POSTGRESQL.md` §「RuntimeSql 与 MyBatis Provider」** 与 **`AI_UPGRADE.md` 跨库手写 SQL**。`PostgresQuerySql` 负责 PG DDL/元数据；业务侧逐步用 `*Sql` Provider 替换非移植 SQL。
- **类型兼容**：新建 PG 库时 `tinyint(1)` 布尔元数据可落 **`boolean`** 列；**已有 `smallint` 列**可与实体 **`int` 0/1** 对齐，或 **`ALTER ... TYPE boolean USING (...)`**。
- **MyBatis**：避免同一字段 **`getX(int)` + `boolean isX()`** 并存导致 `Reflector` 冲突；分页 **`COUNT` 不带 `ORDER BY`**（见 `BaseService.selectCountWithoutOrderBy`）。
- **构建**：JDK 9+ 需 Lombok **`annotationProcessorPaths`**；多模块运行前建议 **`mvn clean install -pl … -am`**，避免本地仓库旧 JAR 与源码不一致。
- **备份**：内置导出仍以 MySQL 为主；PG 生产备份建议 **`pg_dump`** 等外部方案。

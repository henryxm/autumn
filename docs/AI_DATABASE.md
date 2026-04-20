# Autumn 多数据库与 SQL 落地规范

> **用途**：约定**全库兼容**前提下的 Dao / Provider / Wrapper / 手写 SQL 纪律，与 `DatabaseType`、`RuntimeSql`、`PageHelper` 对齐。  
> **PostgreSQL 专项**（DDL、元数据、`PostgresQuerySql`、迁移细节）仍以 **`docs/AI_POSTGRESQL.md`** 为准；**通用跨库口径以本文为准**。

## 1. 总原则（默认必须遵守）

1. **全库兼容默认**  
   除非需求或代码中**明确声明**「仅支持某一种 `DatabaseType`」（须在类/方法 JavaDoc 写明），业务侧**所有**可执行 SQL、Wrapper 条件、排序与分页结果，应在框架已接入的类型上**语义一致或可接受的等价行为**。禁止在通用路径上静默依赖某一库的宽松语法（例如仅 MySQL 允许的写法）。

2. **方言与标识符**  
   表名、列名、保留字冲突处理必须通过 **`RuntimeSql`**（或 **`DialectService`** / **`ModuleService#sql()`** 同源能力）的 **`quote` / `columnInWrapper`** 生成，**禁止**手写反引号 `` ` ``、双引号 `"` 等与单库绑定的引用。

3. **可移植片段**  
   模糊查询用 **`likeContainsAny(#{param})`**；逗号分隔列表成员判断用 **`columnValueInCommaSeparatedList(列, csv)`**（语义对齐 MySQL `FIND_IN_SET`）；布尔开关字面量用 **`enabledTrueSqlLiteral()`**；单行结果集限制用 **`limitOne()`**（**勿**用于 `COUNT(*)` / `MAX` 等聚合）。**勿**在 Provider 外手写各库不同的 `CONCAT`、`||`、`LIMIT`/`FETCH` 组合。

4. **注解建表**  
   仅 **`MYSQL` / `MARIADB` / `POSTGRESQL`** 会执行框架注解同步（`DatabaseType#supportsAnnotationTableSync()`）。其余类型须用 Flyway / Liquibase / 厂商工具或手工 DDL；不得假设 TableInit 会建表。

5. **外键与 WHERE 中的实体引用**  
   表间关联、子查询与业务条件中引用的「对方实体标识」必须使用 **`docs/AI_STANDARDS.md` §10.4** 定义的 **业务主键列**（`Uuid.uuid()` / **`SnowflakeId`** 等），**禁止**使用自增 **`Long id`** 作为关联键或对外资源 ID。**`id`** 仅限生成 CRUD 与技术主键回填场景（见 §1.1）。

### 1.1 关联列与 Wrapper 写法（承接 STANDARDS §10.4）

- **FK 列命名**：与被引用实体的业务主键列语义一致（如 `xxx_biz_key`、`xxx_uuid`），存 **业务主键值**，不存对方 **`id`**。  
- **JOIN / `eq`**：条件侧使用上述业务列 + **`RuntimeSql` / `WrapperColumns`** 按 **`docs/AI_DATABASE.md` §4.0** 引用列名；禁止假设「全世界都用 `id` 关联」。  
- **雪花 ID**：长整型业务主键生成使用 **`cn.org.autumn.utils.SnowflakeId`**（多节点须配置 **`autumn.snowflake.worker-id`** / **`autumn.snowflake.datacenter-id`**）；字符串业务主键优先 **`Uuid.uuid()`**。

---

## 2. 框架已支持的 `DatabaseType`（与代码一致）

以下与 `cn.org.autumn.database.DatabaseType` 枚举一致；**推断顺序**见 `DatabaseHolder#resolveType`（JDBC URL + `autumn.database`，含 TiDB / OceanBase 等与 `jdbc:mysql` 重叠时的配置约定）。

| 类型 | 典型 JDBC / 说明 | 注解建表 | PageHelper 方言别名（5.1.x） |
|------|------------------|----------|------------------------------|
| `MYSQL` | `jdbc:mysql:` | ✓ | `mysql` |
| `MARIADB` | `jdbc:mariadb:` | ✓ | `mariadb` |
| `POSTGRESQL` | `jdbc:postgresql:`、`jdbc:pgsql:` | ✓ | `postgresql` |
| `ORACLE` | `jdbc:oracle:` | ✗ | `oracle` |
| `SQLSERVER` | `jdbc:sqlserver:` 等 | ✗ | `sqlserver` |
| `SQLITE` | `jdbc:sqlite:` | ✗ | `sqlite`（映射 MySQL 分页实现） |
| `H2` / `HSQLDB` | `jdbc:h2:`、`jdbc:hsqldb:` | ✗ | `h2` / `hsqldb` |
| `DB2` / `DERBY` | `jdbc:db2:`、`jdbc:derby:` | ✗ | `db2` / `derby` |
| `FIREBIRD` | `jdbc:firebirdsql:` 等 | ✗ | `sqlserver2012`（5.1.x 无独立别名时的折中） |
| `INFORMIX` | `jdbc:informix-sqli:` 等 | ✗ | `informix-sqli` |
| `DAMENG` | `jdbc:dm:`、`jdbc:dm8:` | ✗ | `dm` |
| `KINGBASE` | `jdbc:kingbase8:`、`jdbc:kingbase86:` | ✗ | `postgresql` |
| `TIDB` | 官方多为 `jdbc:mysql://`；需 `autumn.database=tidb` 或 `jdbc:tidb:` | ✗ | `mysql` |
| `OCEANBASE_MYSQL` | `jdbc:oceanbase:`（无 Oracle 兼容参数）；`jdbc:mysql` 连 OB 时需配置 | ✗ | `mysql` |
| `OCEANBASE_ORACLE` | `jdbc:oceanbase:` + `compatibleMode=oracle` 等，或配置 `oceanbase_oracle` | ✗ | `oracle` |
| `OTHER` | 未识别配置 | ✗ | 回退 `mysql`（须人工核对） |

**备份**：内置 SQL 导出仍仅 **MySQL/MariaDB**；其余库使用各厂商工具。

**内嵌演示库（H2）**：`web` 提供 **`application-h2.yml`** 等 profile（URL 须含 **`MODE=MySQL`**），运行期由 **`H2EmbeddedMysqlDialect`** 将 **`DatabaseType` 视为 `MYSQL`**；**`QuerySql#createTable` 在该组合下会剥离 H2 不支持的表级 DDL、索引 `USING …`，并将 MySQL 索引前缀长度 `` `col`(n) `` 改为整列 `` `col` ``**（见 `QuerySql#embeddedH2MysqlCompatDdl`）。`TableInit` 启动前会确保 **`Config` 已绑定 `Environment`**。种子数据仍来自 **`InitFactory`**；建表失败在内嵌模式下 **`WARN`** 日志。

### 2.1 `RuntimeSqlDialect` / `RuntimeSql` / `DialectService` 能力清单（方言化边界）

以下能力由 **`RuntimeSqlDialect`** 按库实现（或接口 `default`），业务侧经 **`RuntimeSql`**（`*DaoSql extends RuntimeSql`）或 **`DialectService` / `ModuleService#sql()`** 同源调用，**避免在 Provider 中重复** `RuntimeSqlDialectRegistry.get()` 与各库函数名。

| 能力 | `RuntimeSql` 等方法名 | 说明与纪律 |
|------|------------------------|------------|
| 标识符引用 | `quote`、`columnInWrapper` | 禁止手写反引号/双引号 |
| 当前时间、清表 | `currentTimestamp`、`truncateTable` | 清表语义因库而异，慎用 |
| 单行 `SELECT` 后缀 | `limitOne` | **勿**用于 `COUNT(*)` / `MAX` 等聚合 |
| `LIKE` 两端通配 | `likeContainsAny(#{p})` | 勿手写三参 `CONCAT`（Oracle 等限制） |
| 逗号列表成员判断 | `columnValueInCommaSeparatedList` | 语义对齐 MySQL `FIND_IN_SET` |
| 开关字面量 | `enabledTrueSqlLiteral`、`enabledFalseSqlLiteral` | PG `boolean` 与整型库差异 |
| 布尔/开关列 → 聚合用 0/1 | `booleanColumnAsTinyInt01(quotedCol)` | `null→0`、真/非零→1、假/零→0；列须已 `quote` |
| 手写分页后缀（无 `ORDER BY`） | `limitOffsetSuffix(limit, offset)` | **SQL Server**：主查询在拼接前**必须**已有 `ORDER BY`，否则 `OFFSET/FETCH` 非法 |
| 大小写不敏感子串 | `lowerColumnContainsNeedle(quotedCol, #{needle})` | 绑定值建议**小写** |
| 日/月/年/ISO 周桶 | `timestampBucketDay` 等 | 列须已 `quote`；勿手写 `DATE_FORMAT`/`to_char` |

**路由**：`RoutingRuntimeSqlDialect` 按 **`DatabaseHolder#getType()`** 的 **`DatabaseType`** 选择实现；**`null`** 与 **`MYSQL` / `MARIADB` / `OTHER`** 走 **`MysqlRuntimeSqlDialect`**（与历史一致）。**`OTHER`** 表示未识别配置，生产环境应显式设置 **`autumn.database`**，勿依赖默认 MySQL 语法。**Java 8** 下 `switch` 对枚举不做穷尽校验，实现末尾另有 **`return mysql`** 兜底；**新增 `DatabaseType` 枚举常量时必须在路由中接线**，避免静默走错方言。

**暂不纳入方言、须在 Provider 中单库声明或专用工具的能力**（换库即需人工改造）：

- 字符串聚合：`GROUP_CONCAT`、`STRING_AGG`、`LISTAGG`（分隔符、排序、`DISTINCT` 差异大）
- 条件插入/合并：`INSERT … ON DUPLICATE`、`MERGE`、`REPLACE INTO`
- 复杂 JSON/XML 函数、厂商全文检索语法
- 其它「一库一写法」的 DDL 细节（仍以 Flyway/厂商工具或 §7 单库声明为准）

新增与日期分桶、布尔字面量**同类**的横切需求时，优先补 **`RuntimeSqlDialect` + 各方言实现 + `RoutingRuntimeSqlDialect` 委托 + `RuntimeSql`/`DialectService` 封装**，再改业务 Provider，减少反复打补丁。

---

## 3. Dao 与 Provider（强制）

1. **禁止**在 **Dao 接口**上使用 **`@Select` / `@Update` / `@Insert` / `@Delete` 等注解内联硬编码 SQL** 作为**新代码**的交付形态。

2. **必须**使用 **`@SelectProvider` / `@UpdateProvider` …`**，实现类放在与 Dao 同域的 **`*DaoSql`**（或团队统一命名的 Provider）中；SQL 字符串在 Provider 方法内拼接。

3. **推荐**业务侧 `*DaoSql` **`extends RuntimeSql`**，通过 **`quote`、`limitOne`、`likeContainsAny`、`columnValueInCommaSeparatedList`、`enabledTrueSqlLiteral`、`enabledFalseSqlLiteral`、`booleanColumnAsTinyInt01`、`limitOffsetSuffix`、`lowerColumnContainsNeedle`、`currentTimestamp`、`truncateTable`**，以及 **`timestampBucketDay` / `timestampBucketMonth` / `timestampBucketYear` / `timestampBucketIsoWeek`**（时间桶与布尔规范化入参均为 **`quote` 后的单列**）拼装，保证与 `RoutingRuntimeSqlDialect` / `RuntimeSqlDialectRegistry` 一致。**禁止**在 Provider 中手写 `DATE_FORMAT`、`to_char` 等单库日期分桶函数。

4. **历史与框架内置**注解 SQL 仅允许在治理计划中逐步迁移；**新增与变更一律 Provider**。

---

## 4. EntityWrapper / Condition 使用规范（跨库安全）

MyBatis-Plus `EntityWrapper` / `Wrapper` 生成的 SQL 仍经当前数据源执行，**不等于**自动跨库。须遵守：

> **与升级章节互参**：历史代码里同类反模式的对照表与检索命令见 **§8.1**、**§8.5**；**正向标准写法**以本节 **§4.0** 为准。

### 4.0 代码层标准写法（禁止硬编码方言符号）

**目标**：凡会进入 SQL 的**表名、列名、排序片段、Map 条件键**，一律经 **`RuntimeSqlDialect`** 同源能力生成，**禁止**在 Java 字符串里手写 `` ` ``、`"`、`[]` 等与**当前** `DatabaseType` 强绑定的引用，否则换库（如 MySQL → PostgreSQL / H2 / Derby）即出现解析错误、列找不到、或 `ORDER BY` 与 `SELECT` 列引用不一致。

#### 4.0.1 能力分层（按场景选用）

| 场景 | 标准入口 | 禁止 |
|------|----------|------|
| **手写 SQL / Provider** | `*DaoSql extends RuntimeSql`，用 **`quote` / `columnInWrapper` / `likeContainsAny` / `limitOne` / `limitOffsetSuffix`** 等与 §2.1 一致的方法 | 在注解、`apply`、`last`、字符串模板里写死反引号、双引号、方括号或单库函数名 |
| **已继承 `DialectService` / `ModuleService#sql()`** | 使用 **`columnInWrapper`、`quote`** 等与 `RuntimeSql` 同源 API | 手写方言引用后再拼进 Wrapper |
| **未继承方言基类**的 Service / 工具类（含 `BaseService`、`Query` 等框架路径） | **`cn.org.autumn.database.runtime.WrapperColumns`**：`columnInWrapper(逻辑列名)`；分页/列表排序用 **`orderByColumnExpression(列, sqlAlreadySanitized)`**；MP 2.x 下需从 **Map 构造整段等值条件**时用 **`entityWrapperAllEqQuoted(map)`**（避免 `selectByMap` / `allEq` 裸键） | 把裸 `user_id` 直接传给 `orderBy` / `setOrderByField` / `setDescs` / `eq("user_id", v)`（在未确认 MP 全局列格式作用于该路径时） |
| **Autumn 3.0.0 线（MyBatis-Plus 3.x）** | 同上用 **`WrapperColumns`**；整表 Map 等值优先 **`queryWrapperAllEqQuoted(map)`**（以该分支 `WrapperColumns` 实际方法名为准） | 同上 |

**说明**：`WrapperColumns` 内部走 **`RuntimeSqlDialectRegistry`**，与 **`RoutingRuntimeSqlDialect` / `DatabaseHolder`** 解析的当前数据源一致；内嵌 H2（`MODE=MySQL`）等特例仍以 JDBC URL 与配置为准，勿在业务里写死「我是 MySQL」的引号。

#### 4.0.2 排序与用户输入

- **来自请求参数的排序列**（如 jqGrid `sidx`、分页 `descs`）：须先 **`SQLFilter.sqlInject`**（或由 **`orderByColumnExpression(..., false)`** 统一过滤），再对**简单单列逻辑名**做 **`columnInWrapper`**；已由调用方生成且**成对**方言引用的片段可传入 **`orderByColumnExpression`** 的「已引用」分支（见实现注释），**禁止**二次 `quote`。
- **复杂排序表达式**（函数、子查询、多列未抽象）：默认**不**走自动引用；须 **Dao + Provider** 或单库声明（§7），不得在通用 Service 中拼接半套方言。

#### 4.0.3 Code Review 快速拒收清单

- 出现 `` `[a-z_]+` ``、`"[a-z_]+"`（手写列引用）且不在测试数据字符串字面量中、而是拼 SQL 的 Java 代码路径上。
- `Wrapper` / `Page` 的 `orderBy` / `setOrderByField` / `setDescs` 参数为**未经过** `columnInWrapper` / `orderByColumnExpression` 的裸标识符，且该路径**未**依赖框架已统一处理的入口（如未使用框架提供的 `Query` / `BaseService#getPage`）。
- `FIND_IN_SET`、`DATE_FORMAT`、`IFNULL`、`LIMIT n` 等写在 **Wrapper.apply** 或 **非 Provider** 的 Java 字符串里。

与 **§8.1** 表中「**Java 手写方言引用拼 Wrapper / Page**」一行互参；升级扫描命中后按 **§8.3** 分类改造。

### 4.1 建议仅使用的条件形态（相对可移植）

- **等值与范围**：`eq`、`ne`、`gt`、`ge`、`lt`、`le`、`between`
- **空值**：`isNull`、`isNotNull`
- **枚举集合**：`in`（注意列表过长与参数上限）
- **排序**：`orderBy` 仅 **简单列名**；列名若可能为保留字，使用 **`columnInWrapper("col")`** 或 **`DialectService#columnInWrapper`** 传入 Wrapper

### 4.2 禁止或慎用（极易单库化）

- **`apply` / 自定义 `sqlSegment`** 中写死 **函数名**（如 `FIND_IN_SET`、`IFNULL`、`DATE_FORMAT`、`to_char`、`GROUP_CONCAT`、专有类型转换）；报表「按日/月/年/ISO 周」分桶须走 **`RuntimeSql` 时间桶方法**或 **`RuntimeSqlDialect#sqlTimestampBucket*`**，不得在 Wrapper 中直写单库日期函数
- **依赖 MySQL 宽松规则** 的 `GROUP BY`、`ORDER BY` 与聚合混用（PostgreSQL 等会直接报错）
- **手写片段中的引号**（反引号/双引号）

### 4.3 复杂场景改走 Dao + Provider

满足以下**任一**时，**不要**用 Wrapper 硬凑，应 **定义 Dao 方法 + `*DaoSql` Provider**：

- 多表 **JOIN**、子查询、**EXISTS**、报表级 **GROUP BY / HAVING**
- **LIKE** 需与方言一致的拼接（使用 **`likeContainsAny`**）
- **逗号分隔列表**权限 / 数据范围（使用 **`columnValueInCommaSeparatedList`**）
- **分页语义**依赖数据库专属语法（已由 PageHelper 处理的简单列表查询除外）

---

## 5. 推荐实施标准（可读性与换库成本）

按复杂度递增，团队优先按下列**固定套路**选型，便于 Code Review：

| 层级 | 场景 | 推荐写法 |
|------|------|----------|
| A | 单表主键/简单条件 CRUD | **`Service` + 继承链上的 `baseMapper`**（`selectById`、`updateById` 等） |
| B | 单表、条件均为 §4.1 安全形态 | **`baseMapper` + `Wrapper`**；保留字列用 **`columnInWrapper`** |
| C | 需方言引用或少量动态片段 | **`DialectService` / `ModuleService#sql()`** 取 **`quote`、`likeContainsAny`** 等，再配合 `Wrapper` 或字符串列片段（仍避免 `apply` 塞整段方言 SQL） |
| D | 多表、函数、强动态、或任何 §4.2 风险 | **`XxxDao` + `XxxDaoSql extends RuntimeSql`**，**禁止**在 Service 里拼接整段 SQL 字符串 |

**目标**：换 `DatabaseType` 时，主要 diff 应在 **`RuntimeSqlDialect` 实现**与**少量 Provider**，而不是散落在各 Service 的 Wrapper hack。

---

## 6. 类型与分页注意点

- **布尔**：`BooleanNumericTypeHandler` 对 **`POSTGRESQL` / `KINGBASE`** 走 `setBoolean`，其余多为整型 0/1；新实体避免同一列混用两种语义。
- **分页**：依赖 **`DatabaseType#pageHelperDialectName()`** 与 `JdbcEnvironmentPostProcessor` 注入；特殊 URL 见 §2 脚注。
- **`limitOne()`**：仅追加在**可能多行**的 `SELECT` 末尾；聚合查询**不要**追加。
- **手写 `LIMIT`/`OFFSET`**：若不走 PageHelper、须在 Provider 拼分页后缀，用 **`limitOffsetSuffix`**；SQL Server 等见 §2.1。

---

## 7. 单库例外（如何写）

若业务**确实**只需某一库（如仅用 PG 的 JSON 算子），须：

1. 在 **Service 或 DaoSql 类** JavaDoc 标明：**「仅 `DatabaseType.XXX`」**  
2. 在发布/配置说明中列出**不支持的数据库**  
3. 仍优先把 SQL 放在 **Provider** 中，便于审计；避免在 Controller 或 Service 内联巨型字符串

---

## 8. 老旧项目升级：注解 Dao 与方言化 Wrapper

> **目标**：把历史代码中 **Dao/Mapper 注解内联 SQL**、**Wrapper 里写死单库语法**（反引号、`FIND_IN_SET`、`apply` 拼接等）迁到 **Provider + `RuntimeSql`** 或安全 Wrapper，以支持多库与长期维护。  
> **正向写法（新代码默认）**：表/列/排序/Map 键一律 **`RuntimeSql` / `DialectService` / `WrapperColumns`**，见 **§4.0**。  
> **「一键升级」边界**：工程上可实现 **一键扫描报告（只读）** + **分阶段人工改造**；**不能**指望无人值守自动改完所有 SQL 语义（见 **`docs/AI_UPGRADE.md` §3**）。

### 8.1 常见老旧模式与处置

| 模式 | 典型痕迹 | 风险 | 建议处置 |
|------|----------|------|----------|
| **注解内联 SQL** | `@Select("...")` / `@Update("...")` / `@Insert` / `@Delete` 字符串字面量 | 无法随方言切换引号/分页/函数 | 改为 `@SelectProvider(type=XxxDaoSql.class, method="…")`，`XxxDaoSql extends RuntimeSql` |
| **MySQL 反引号** | `` `user` ``、`WHERE `order` =`` 写在注解、`apply`、`last("…")` | PG/Oracle 等解析失败或语义错误 | 全部改为 **`quote("col")`** / **`columnInWrapper("col")`** |
| **Java 手写方言引用拼 Wrapper / Page** | `Wrapper.eq` / `orderBy` / `Page#setOrderByField` / `setDescs` 等入参为**手写反引号、双引号或方括号**包列名，或 Java 拼接出的含方言引号的排序片段 | 换库后引号规则与 SELECT 列引用不一致；Derby/H2 等裸标识符被折大写导致列找不到 | 未继承 **`DialectService`** 时统一用 **`WrapperColumns.columnInWrapper`**、**`orderByColumnExpression`**、**`entityWrapperAllEqQuoted` / `queryWrapperAllEqQuoted`**（见 **§4.0**）；Provider 内仍用 **`RuntimeSql#quote` / `columnInWrapper`** |
| **Wrapper.apply / last 塞 SQL** | `.apply("FIND_IN_SET(...)")`、`.last("LIMIT 1")` 带方言 | 换库即挂 | 复杂条件 **下沉 Dao + Provider**；简单排序若必须 `last`，须评审是否可改为标准 `orderBy` |
| **MySQL 专有函数** | `FIND_IN_SET`、`IFNULL`、`DATE_FORMAT`、`GROUP_CONCAT`、`STR_TO_DATE` | 非 MySQL 族不兼容 | 列表成员用 **`columnValueInCommaSeparatedList`**；模糊用 **`likeContainsAny`**；其它用各库等价写法或 **单库声明**（§7） |
| **手写 LIMIT / FETCH** | 注解里 `"… LIMIT 1"` | 与 Oracle/SQL Server 等不一致 | 可移植片段用 **`limitOne()`**；分页交给 PageHelper |
| **三参 CONCAT 模糊** | `concat('%',#{x},'%')` | Oracle `CONCAT` 双参限制等 | **`likeContainsAny("#{x}")`** |
| **布尔字面量** | 与 `enabled` 比较写死 `= 1` 或 `= true` | PG `boolean` 与整型库差异 | **`enabledTrueSqlLiteral()`** |

### 8.2 注解 Dao → Provider 的推荐步骤（单方法迭代）

1. **新建** `XxxDaoSql extends RuntimeSql`（与 Mapper 同模块，常见包路径 `.../dao/sql/`）。  
2. **原注解 SQL** 整段迁入 Provider 的 **`public String methodName()`**，返回拼接字符串。  
3. **替换**：反引号列/表 → **`quote("identifier")`**；`LIMIT 1`（非聚合）→ 末尾 **`+ limitOne()`**；`LIKE` 模糊 → **`likeContainsAny("#{field}")`**；`FIND_IN_SET` → **`columnValueInCommaSeparatedList("t.col", csv)`**（注意 `csv` 为**不含外层引号**的内层字面量拼接规则与防注入，与现有 `RuntimeSqlDialect` 契约一致）。  
4. Mapper 上改为 **`@SelectProvider(type = XxxDaoSql.class, method = "methodName")`**（`Update`/`Delete`/`Insert` 同理）。  
5. **在目标库**（至少 MySQL + 若上线 PG 则 PG）各跑一遍单测或手工验证；关注 **大小写敏感标识符**（Oracle 双引号）与 **日期/时区**。  
6. **删除**原注解内联字符串；禁止长期保留「Provider 里再拼一层反引号」。

### 8.3 Wrapper 方言化痕迹 → 改造策略

1. **扫描**：在 `*Service` / `*Dao` 使用处搜 **`.apply(`**、**`.last(`**、**`.having(`** 及字符串中的 **`` ` ``**、**`FIND_IN_SET`**、**`IFNULL`** 等。  
2. **分类**：  
   - **简单条件**（eq/in/between/isNull）：改为无字符串 SQL 的 Wrapper API；列名/Map 键须方言化时见 **§4.0**（**`WrapperColumns`** / **`entityWrapperAllEqQuoted` / `queryWrapperAllEqQuoted`**）。  
   - **需引用保留字列**：`orderBy(true, columnInWrapper("order"), true)` 等（通过 **`DialectService`** / **`ModuleService#sql()`** 取 `columnInWrapper`，或未继承基类时用 **`WrapperColumns.columnInWrapper`**）。  
   - **无法用 API 表达**：**新增 Dao 方法 + Provider**，不要在 Wrapper 上堆 `apply`。  
3. **禁止**在 Java 字符串里拼 **`` `table` ``** 再塞进 `apply`；若必须动态表名，须在 **Provider** 内 **`quote(table)`**。  

### 8.4 「一键」在流程中的位置

1. **阶段 0**：依赖与配置升级（**`docs/AI_UPGRADE.md` §2**）。  
2. **阶段 1**：运行 **`scripts/autumn-dependency-scan.sh`**（或等价 `rg`/`grep`），生成 **命中清单**（含本节相关规则，见 **`docs/AI_UPGRADE.md` §3.1**）。  
3. **阶段 2**：按模块/包 **分批**改 Provider 与 Wrapper，每批 **编译 + 核心用例回归**。  
4. **阶段 3**：在 **非 MySQL** 环境做一次全量冒烟（或 CI 矩阵）。  
5. **门禁**：合并前 Code Review 检查 **新增**代码不得再引入 §8.1 中的模式（与 **`docs/AI_STANDARDS.md`** §12 一致）。

### 8.5 只读检索示例（依赖方仓库内）

以下在业务工程根目录执行，**不修改文件**；命中后需人工打开文件判断（注释、字符串常量可能误报）。

```bash
# MyBatis 注解内联 SQL（常见形态：单字符串）
rg -n '@(Select|Update|Insert|Delete)\(\s*"' --glob '*.java' .

# 多行注解 @Select({ "…", "…" })（需另搜）
rg -n '@(Select|Update|Insert|Delete)\(\s*\{' --glob '*.java' .

# Wrapper 自定义片段（重点人工审）
rg -n '\.(apply|last|having)\(' --glob '*Service*.java' --glob '*Dao*.java' .

# 疑 MySQL 反引号（排除部分误报）
rg -n '`[a-zA-Z_][a-zA-Z0-9_]*`' --glob '*.java' .

# 业务 Service 中手写反引号列（与 §4.0、§8.1「Java 手写方言引用」对照审）
rg -n '`[a-zA-Z_][a-zA-Z0-9_]*`' --glob '*Service*.java' .

# 已知高风险函数（与扫描脚本部分重叠）
rg -n 'FIND_IN_SET|IFNULL\(|DATE_FORMAT\(|GROUP_CONCAT\(|LAST_INSERT_ID\(' --glob '*.java' .
```

**仓库脚本（分组体检）**：根目录执行 **`scripts/constraints-scan`**（只读、`rg` 必选），按 **实体命名 / 索引唯一 / Dao 注解 SQL / Wrapper / 分层 / DDL 文件名** 等分组输出，规则与 **`docs/AI_STANDARDS.md`**、本文 §1～§5、§8 及 **`docs/AI_BOOT.md`** 表名约定等对读；详见脚本头部注释与环境变量 **`AUTUMN_SCAN_SKIP_GEN`** / **`AUTUMN_SCAN_EXTRA`**。

---

## 9. 文档与代码同步

- 新增或调整 **`DatabaseType`**、方言实现或路由规则时：**同步更新本文 §2** 与 **`docs/AI_INDEX.md`**。  
- **实现类索引**：`DatabaseHolder`、`RoutingRuntimeSqlDialect`（按 `DatabaseType` 路由，见 §2.1）、`RoutingRelationalTableOperations`、`RuntimeSqlDialectRegistry`、`RuntimeSql`、`DialectService`。

---

## 10. 交叉引用

| 主题 | 文档 |
|------|------|
| 分层、Dao 禁止项、Controller 纪律 | **`docs/AI_STANDARDS.md`** §12～§13 |
| **代码层方言标准写法**（`RuntimeSql` / `WrapperColumns`） | **本文 §4.0**；反模式对照 **§8.1** |
| PostgreSQL DDL、元数据、`PostgresQuerySql` | **`docs/AI_POSTGRESQL.md`** |
| 启动配置、profile、环境变量 | **`docs/AI_BOOT.md`** §3、§8 |
| 升级清单、扫描脚本、「一键」边界 | **`docs/AI_UPGRADE.md`** |
| **老旧 Dao/Wrapper 迁移步骤与检索** | **本文 §8** |

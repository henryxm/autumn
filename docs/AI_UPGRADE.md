# Autumn 作为基础库：依赖方升级清单与自动化扫描

> **定位**：`autumn`（本仓库）作为 **Maven 依赖 / 二方库** 被其它业务工程引用时，版本升级所需的检查项、兼容性说明，以及**只读扫描脚本**能覆盖与不能覆盖的范围。  
> 与 **PostgreSQL 专项** 的关系：多库、方言、实体类型等细节见 `docs/AI_POSTGRESQL.md`；本文件侧重 **跨项目升级流程** 与 **清单化验收**。

---

## 1. 依赖关系模型

| 角色 | 说明 |
|------|------|
| **基础项目（本仓库）** | 产出 `autumn-lib`、`autumn-modules`、`autumn-handler`、`web` 等 GAV，版本号在根 `pom.xml` 统一（如 `2.0.0`）。 |
| **依赖方工程** | 业务仓库：在自身 `pom.xml` 中声明 `cn.org.autumn:*` 依赖，可能还有本地 `application*.yml`、自研模块与 `autumn` 的扩展点实现。 |
| **升级** | 将依赖方中所有 `cn.org.autumn` 构件版本 **对齐到目标版本**，重新 **全量编译 / 安装**，再按清单做 **配置、数据库、回归**。 |

---

## 2. 依赖方升级清单（建议按顺序执行）

### 2.1 版本与构建

| # | 项 | 说明 |
|---|-----|------|
| 1 | **统一 GAV** | 依赖方所有模块中 `cn.org.autumn` 的 `artifactId` + `version` 与目标发布版本 **一致**，避免混用多个版本。 |
| 2 | **传递依赖** | 若依赖方排除了 `spring-boot`、`mybatis-plus` 等，升级后核对 **与 autumn 父 POM 管理版本** 是否冲突（以 autumn 发布说明或 BOM 为准）。 |
| 3 | **JDK** | 与所依赖的 **autumn 主版本** 对齐：**2.0.0 / master** 多为 **Java 8**；**3.0.0 / `3.0.0` 分支** 为 **Java 17+**（以依赖方引入的 autumn 根 `pom.xml` 中 `java.version` 为准）。使用 **JDK 9+** 编译依赖方时须保证 **Lombok** 走 `annotationProcessorPaths`（见 `docs/AI_POSTGRESQL.md` 工程化小节）。 |
| 4 | **构建命令** | 建议使用 `mvn clean install -pl <启动模块> -am -DskipTests` **从根反应堆安装**，避免仅子模块 `spring-boot:run` 使用 `~/.m2` **陈旧 JAR**。 |

### 2.2 配置

| # | 项 | 说明 |
|---|-----|------|
| 5 | **`autumn.database`** | 使用 PostgreSQL 时设为 `postgresql`，并配置数据源、PageHelper 方言（见 `application-postgresql.yml` 示例）。 |
| 6 | **多数据源 / Druid** | 校验 `spring.datasource` 与 autumn 期望结构一致；升级后注意 **validation-query**（PG 常用 `SELECT 1`）。 |
| 7 | **Redis / 其它中间件** | 默认 **`autumn.redis.open=false`** 时不装配 Redis 栈（无 `RedisConnectionFactory` / 框架 `RedisTemplate`）。**模式 A**：任意一处 **`@Autowired RedisTemplate`**（默认 `required=true`）则**必须** **`autumn.redis.open=true`** + **`spring.redis.*`**；向导 **`autumn.install.wizard=true`** 时须在安装流程中启用 Redis，否则占位阶段仍无 Bean 会**启动失败**。**模式 B**：全部 **`@Autowired(required = false)`**（或 `ObjectProvider`）且调用前判空，则可关 Redis 启动。框架 **`RedisConfig`**：**`docs/REDIS_STANDALONE.md` §2**；业务注入：**§3、§8**。若使用 **Redisson**（如 `redisson-spring-boot-starter`），依赖方须在根 POM 保证 **`redisson-spring-data-XX`** 与 **`spring-data-redis`** 主版本一致；TTL 写法与 **`RedisExpireUtil`** 说明见 **`docs/REDIS_TTL_GUIDE.md`**，原理见 **`docs/REDIS_REDISSON_SPRING_DATA.md`**。 |
| 7a | **`autumn.install.wizard=true`** | 占位数据源**默认 H2 内存**（无需外部 DB）；`autumn-modules` 传递 **`h2`**。若设 **`autumn.install.bootstrap-datasource.flavor=mysql`** 则须本机 MySQL 已启动。见 **`docs/INSTALL_MODE_CONDITIONAL.md` §0**。 |

### 2.3 数据库与数据

| # | 项 | 说明 |
|---|-----|------|
| 8 | **注解建表** | 若开启 `autumn.table.auto`，升级后首次启动关注日志；PG 与 MySQL 语义差异见 `docs/AI_POSTGRESQL.md`。 |
| 9 | **已有 PG 库** | `smallint` 标志列 vs Java `boolean` 已用 **`int` 0/1** 或 **`ALTER ... TYPE boolean`** 策略处理，升级后勿回退实体类型导致 JDBC 类型不匹配。 |
| 10 | **迁移脚本** | 若 autumn 发布说明要求执行 SQL，在依赖方环境 **先备份再执行**。 |

### 2.4 代码与 API（自动化弱、需人工）

| # | 项 | 说明 |
|---|-----|------|
| 11 | **二方扩展** | 依赖方若 **继承/实现** autumn 的 `ModuleService`、`BaseMenu`、`LoopJob` 等，对照 **CHANGELOG** 或 **Diff** 检查重载签名、废弃方法。 |
| 12 | **手写 SQL** | 硬编码 `LIMIT`/`FIND_IN_SET`/三参 `concat('%',#{x},'%')`/保留字列名等，在多库下易出错；**老旧 Dao 上 `@Select`/`@Update`/`@Insert`/`@Delete` 内联字符串**、**Wrapper 中反引号/`apply` 拼方言**、**Java 里手写引号拼 `eq`/`orderBy`/分页排序**须按 **`docs/AI_DATABASE.md` §8** 迁到 **`RuntimeSql` + Provider** 或 **`WrapperColumns`（§4.0）**（见下「跨库手写 SQL 与 RuntimeSql」）。 |
| 13 | **实体布尔与 MyBatis** | 避免同一字段 **`getX(int)` + `boolean isX()`** 引发 `Reflector` 冲突；分页若自定义 count，避免 **`COUNT(...) ORDER BY`**（PG 非法）。 |

### 2.5 回归与发布

| # | 项 | 说明 |
|---|-----|------|
| 14 | **冒烟用例** | 登录、权限、核心 CRUD、定时任务、文件/备份（若使用）等与业务相关的路径。 |
| 15 | **灰度** | 生产环境建议先 **单实例 / 低流量** 验证再全量。 |

#### 跨库手写 SQL 与 `RuntimeSql`（依赖方与基础项目一致）

**推荐写法（以本仓库 `autumn-lib` 为依赖时）**

1. 新建 SQL 构建类：`public class XxxDaoSql extends cn.org.autumn.database.runtime.RuntimeSql`（与 Mapper 同模块、常放 `.../dao/sql/`）。
2. Mapper 使用：`@SelectProvider(type = XxxDaoSql.class, method = "方法名")`（或 `@UpdateProvider` / `@DeleteProvider` 等同理）。
3. Provider **方法**：**实例方法**、`public String xxx()`，返回拼接好的 SQL 字符串；**不要**写成 `static` 后仍调用实例上的 `quote()`（无法编译）；若坚持 `static`，须在方法内显式 `RuntimeSqlDialectRegistry.get().quote(...)`，一般无必要。
4. 在构建类中优先使用 **`RuntimeSql`** 的封装（内部经 `RuntimeSqlDialectRegistry` 取当前方言）：
   - **`quote("列或表名")`**：标识符引用，勿手写反引号/双引号/方括号。
   - **`limitOne()`**：仅用于「可能多行」的 `SELECT` 取单行；**勿**与 `COUNT(*)`、`MAX/MIN` 等聚合同用（见下）。
   - **`likeContainsAny("#{param}")`**：LIKE 两端 `%`，勿手写三参 `concat('%',#{x},'%')`（Oracle 仅双参 `CONCAT`）。
   - **`enabledTrueSqlLiteral()`**：手写 SQL 中与开关列比较时的字面量（PG `boolean` 与 MySQL 整型 0/1 差异）。
   - **`currentTimestamp()`**、**`truncateTable("表名")`**、**`columnInWrapper("列名")`**、**`columnValueInCommaSeparatedList(qualifiedColumn, csvInner)`**（替代 `FIND_IN_SET`）。
   - 需要直接访问方言实现时：**`dialect()`** 返回 `RuntimeSqlDialect`（与直接 `RuntimeSqlDialectRegistry.get()` 等价，择一即可）。

**与仅使用 `RuntimeSqlDialect` 的关系**：`RuntimeSql` 是对方言的薄封装，**不替代** `RuntimeSqlDialect` 接口；依赖方若从旧代码迁移，把「每类重复的 `private ... d() { return RuntimeSqlDialectRegistry.get(); }`」收敛为 **`extends RuntimeSql`** 即可。

**仍须人工审**：动态 SQL 中的函数名、日期格式、分页子句是否与目标库一致。

---

#### 跨库手写 SQL 约定（要点速查，与 `docs/AI_POSTGRESQL.md` 一致）

- **列/表引用**：`quote` / `columnInWrapper`，勿手写 `` ` `` / `"` / `[]`。
- **取一行**：`limitOne()`，勿手写固定 `LIMIT 1`。**`SELECT COUNT(*)`、`MAX/MIN` 等聚合勿再追加** `limitOne()`。Mapper 上勿再写 `@Select("... limit 1")`，应改为 `@SelectProvider` + 含 `limitOne()` 的构建类。
- **逗号列表成员判断**：`columnValueInCommaSeparatedList(qualifiedColumn, csvInner)`。
- **清空表**：`truncateTable("表名")`；外键/复制等环境下各库行为不同，需自行评估。

---

## 3. 「一键升级」可行性说明

### 3.1 可以脚本化（建议自动化）

- 在 **依赖方仓库根目录** 扫描所有 `pom.xml` / `*.gradle`，检查 `cn.org.autumn` **版本号是否统一**、是否缺失 **Lombok 注解处理器** 配置（启发式 grep）。
- 扫描 `application*.yml` / `application*.properties` 是否包含 **`autumn.database`**、数据源驱动与方言关键词（**提示性**，不修改文件）。
- **Grep 级** 规则（**仅报告**，不自动改代码）：`sun.misc.Launcher`、明显 `FIND_IN_SET`、三参 `concat('%'`、手写 `LIMIT`、**`@Select`/`@Update`/`@Insert`/`@Delete` 后紧跟字符串 SQL**、**`.apply(` / `.last(` / `.having(`**、常见 MySQL 函数 **`IFNULL`/`DATE_FORMAT`/`GROUP_CONCAT`/`LAST_INSERT_ID`** 等。依赖方宜对照 **`docs/AI_DATABASE.md` §8** 分批改为 **`RuntimeSql` + Provider** 或标准 Wrapper。
- **本仓库脚本**：`scripts/autumn-dependency-scan.sh` 已包含部分上述规则；**§8.5** 提供可复制 `rg` 命令作为补充。

### 3.2 难以或不应「一键」自动完成

- **业务语义**：是否改用新 API、是否删除废弃调用，必须由 **人** 判断。
- **数据库 DDL**：自动执行 `ALTER` 风险高，脚本至多生成 **待审核 SQL**，不应默认连接库执行。
- **合并冲突**：Git 升级分支冲突需人工解决。
- **全项目语义搜索**：「所有手写 SQL 已移植」无法仅靠正则 **证明**，需测试与代码评审。

**结论**：可提供 **一键扫描报告（只读）** + **可选的版本号批量替换（需显式确认）**；所谓「一键升级」在工程上应理解为 **一键体检 + 清单勾选**，而非无人值守替换全部代码与数据。

---

### 3.3 老旧项目：注解 Dao 与方言化 Wrapper（升级路径摘要）

| 阶段 | 动作 | 产出 |
|------|------|------|
| 体检 | 运行 **`autumn-dependency-scan.sh`** + **`docs/AI_DATABASE.md` §8.5** 检索 | 待改造文件列表 |
| 改造 | 按 **`docs/AI_DATABASE.md` §8.2～§8.3** 逐项替换为 Provider / 安全 Wrapper | 可编译、可回归的 PR |
| 验收 | 在 **目标 `DatabaseType`**（含非 MySQL）下冒烟与核心用例 | 合并门禁 |

**详细对照表、单方法迁移步骤、Wrapper 策略**以 **`docs/AI_DATABASE.md` §8** 为准。

---

## 4. 仓库内脚本：`scripts/autumn-dependency-scan.sh`

- **位置**：本仓库 `scripts/autumn-dependency-scan.sh`。
- **用法**：在 **依赖方项目根目录** 执行（或将该脚本复制到依赖方后执行）：

```bash
# 在业务工程根目录
bash /path/to/autumn/scripts/autumn-dependency-scan.sh
# 或设置扫描根目录
AUTUMN_SCAN_ROOT=/path/to/business-repo bash /path/to/autumn/scripts/autumn-dependency-scan.sh
```

- **行为**：只读；输出 **WARN / INFO**；**不修改** 任何文件。
- **覆盖**：`pom.xml` 中 `cn.org.autumn` 版本、多版本冲突；`application*.yml|yaml|properties` 中 `autumn.database`、jdbc 线索；源码启发式规则含：`FIND_IN_SET`、固定串 `concat('%'`（三参模糊）、`COUNT` 与 `limitOne` 同句、手写 `LIMIT n`、`sun.misc.Launcher` 等（**grep -E / 固定串**，注释亦可能命中，需人工判断）。

升级 autumn 版本号仍建议在依赖方使用 **IDE / sed / 脚本** 在 **评审后** 批量替换，本扫描脚本不包含默认改写。

---

## 5. 与其它文档的交叉引用

| 文档 | 内容 |
|------|------|
| `docs/AI_POSTGRESQL.md` | 多库、方言、`RuntimeSql` 与 Provider、`BaseService` 分页 count 等 |
| `docs/AI_BOOT.md` | 最小上下文；含 PostgreSQL 摘要与 `RuntimeSql` 索引 |
| `docs/AI_MAP.md` | 框架能力与模块边界 |

---

（完）

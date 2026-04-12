# Autumn PostgreSQL 支持方案（实现说明与兼容性）

> 目标：在**不破坏现有 MySQL/MariaDB 基线**的前提下，为 PostgreSQL 提供可运行的元数据、DDL、分页、手写 SQL 方言与运行时类型兼容。  
> **通用多库纪律**（全库兼容默认、`DatabaseType` 清单、Wrapper 边界、Dao **必须 Provider**）：见 **`AI_DATABASE.md`**。  
> 关联启动说明摘要：见 `AI_BOOT.md` §8「多数据库支持（摘要）」。

## 1. 背景与问题模型

### 1.1 动机

- 默认基线为 **MySQL**；扩展目标优先 **PostgreSQL**。
- PostgreSQL 与 MySQL 在以下方面差异显著，需分层处理：
  - **标识符引用**（`"` vs `` ` ``）
  - **LIMIT / 分页**（语义相近，但生成 SQL 需一致）
  - **布尔与整型**：JDBC 对 Java `boolean` 绑定为 PG **`boolean`**，而历史/注解同步常见列为 **`smallint`**（由 `tinyint` 映射），导致 `smallint` vs `boolean` 报错。
  - **聚合与排序**：`COUNT(...) ... ORDER BY` 在 PG 中严格非法（MySQL 往往宽松）。
  - **元数据查询**：索引、列信息需使用 PG 系统目录与 `information_schema`，与 MySQL 不同。

### 1.2 设计原则

1. **按库拆分实现**：MySQL 与 PostgreSQL 各自实现类，通过 **`autumn.database`**、JDBC URL 与 **`DatabaseHolder`**（`resolveType`）路由；全库类型见 **`AI_DATABASE.md` §2**。
2. **扩展点清晰**：方言接口、表操作接口、Provider SQL 类可独立演进。
3. **兼容优先**：未单独实现的数据库类型可回退到 MySQL 风格（仅保证启动，语义需后续核对）。

---

## 2. 配置与开关

### 2.1 `autumn.database`

- **位置**：`application.yml` 注释 + `additional-spring-configuration-metadata.json` 说明。
- **取值**：以 `DatabaseType#fromConfig` 为准（含 `mysql`、`mariadb`、`postgresql`、`oracle`、`sqlserver`、`sqlite`、`h2`、`kingbase`、`tidb`、`oceanbase_mysql`、`oceanbase_oracle`、`dm` 等）；完整表见 **AI_DATABASE.md** §2。
- **作用**：驱动 **`DatabaseType`**、分页方言、`RoutingRelationalTableOperations`、注解建表是否执行、运行时 SQL 方言等（与 **`DatabaseHolder`** 联用）。

### 2.2 数据源与分页

- 示例：`application-postgresql.yml`
  - `spring.datasource.driver-class-name: org.postgresql.Driver`
  - JDBC URL 可带 `stringtype=unspecified`（按团队约定）
  - `pagehelper.helper-dialect: postgresql`
- **说明**：内置「数据库备份导出」仍仅支持 MySQL/MariaDB；PG 请使用 `pg_dump` 等外部工具。

### 2.3 生产环境仅用 `prod` + 环境变量（推荐）

线上若固定 `spring.profiles.active=prod`，**不必**再挂 `postgresql` profile：在 `application-prod.yml` 中已通过 **`SPRING_DATASOURCE_DRUID_FIRST_URL` / `SPRING_DATASOURCE_DRUID_SECOND_URL`、`SPRING_DATASOURCE_DRIVER_CLASS_NAME`、`AUTUMN_DATABASE`、`PAGEHELPER_DIALECT`、`DRUID_VALIDATION_QUERY`** 等与 `dev` / `application-postgresql.yml` **同名变量**切换库；文件顶部注释有 MySQL / PostgreSQL 示例。`postgresql` profile 仍可用于本地 `dev,postgresql` 等组合，与上述变量一致。

---

## 3. 架构与代码变更总览

### 3.1 数据库类型与持有者

| 组件 | 说明 |
|------|------|
| `DatabaseType` | 枚举：库类型 + `supportsAnnotationTableSync()`（仅 mysql/mariadb/postgresql 为 true）；全量见 **`AI_DATABASE.md` §2** |
| `DatabaseHolder` | `getType()` / `resolveType`：JDBC URL + `autumn.database` |

### 3.2 注解建表与表操作路由

| 组件 | 说明 |
|------|------|
| `RelationalTableOperations` | 抽象：表是否存在、列元数据、索引、DDL 执行等 |
| `MysqlRelationalTableOperations` | 委托原 `TableDao` / MySQL 路径 |
| `PostgresRelationalTableOperations` | 委托 `PostgresTableDao` + PG 脚本执行 |
| `RoutingRelationalTableOperations`（`@Primary`） | 按 `DatabaseHolder` 选择实现 |
| `MysqlTableService` | 注入 `RelationalTableOperations` 而非直接使用 `TableDao` |
| `TableInit` | 仅在 `supportsAnnotationTableSync()` 为 true 时执行同步；否则告警跳过 |

### 3.3 PostgreSQL 专用 DDL / 元数据

| 组件 | 说明 |
|------|------|
| `cn.org.autumn.table.platform.postgresql` | `PostgresQuerySql`、`PostgresTableDao`、多语句 DDL 拆分执行等 |
| `PostgresQuerySql` | 类型映射：`tinyint(1)`（Java 布尔元数据）→ PG **`boolean`**；默认值 `0/1` → `false/true`；索引查询修复 `u.ordinality` 等 |
| `AutumnApplication` | `@MapperScan` 包含 `cn.org.autumn.table.platform.postgresql` |

### 3.4 运行时 SQL 方言（手写 SQL / Provider）

| 组件 | 说明 |
|------|------|
| **`RuntimeSql`**（**依赖方推荐入口**） | 基类：`cn.org.autumn.database.runtime.RuntimeSql`。业务 `*DaoSql` 类 **`extends RuntimeSql`**，在实例方法里调用 **`quote`、`limitOne`、`likeContainsAny`、`enabledTrueSqlLiteral`、`currentTimestamp`、`truncateTable`、`columnInWrapper`、`columnValueInCommaSeparatedList`** 等；内部通过 **`dialect()`** → `RuntimeSqlDialectRegistry.get()` 取当前方言，**避免**每个类重复编写 `RuntimeSqlDialectRegistry.get()` 样板代码。 |
| `RuntimeSqlDialect` | 底层接口：`quote`、`columnInWrapper`、`currentTimestamp`、`truncateTable`、`limitOne`（**勿**用于 `COUNT(*)` 等聚合）、`likeContainsAny`、`enabledTrueSqlLiteral`、`columnValueInCommaSeparatedList` 等 |
| `MysqlRuntimeSqlDialect` / `PostgresqlRuntimeSqlDialect` | 各库实现 |
| `RoutingRuntimeSqlDialect`（`@Primary`） | 按 `DatabaseHolder` 委托 |
| `RuntimeSqlDialectRegistry` + `RuntimeSqlDialectBootstrap` | MyBatis 实例化 Provider 类时通常非 Spring 注入；通过静态注册表在运行期解析当前方言（与 `RuntimeSql` / 直接 `get()` 一致） |

#### RuntimeSql 与 MyBatis Provider（给「以本仓库为基础项目」的依赖方）

**目的**：多库（至少 MySQL + PostgreSQL）下，手写 SQL 的标识符引号、单行限制、LIKE 模糊、`TRUE`/`1` 开关字面量等必须随 **`autumn.database`** 变化；统一继承 **`RuntimeSql`** 可减少重复、降低漏改风险。

**典型写法**：

```java
package com.example.biz.dao.sql;

import cn.org.autumn.database.runtime.RuntimeSql;

public class FooDaoSql extends RuntimeSql {

    public String findOne() {
        return "SELECT * FROM " + quote("foo") + " WHERE " + quote("id") + " = #{id}" + limitOne();
    }

    public String searchName() {
        return "SELECT * FROM " + quote("foo") + " WHERE " + quote("name")
                + " LIKE " + likeContainsAny("#{name}");
    }
}
```

```java
// Mapper
@SelectProvider(type = FooDaoSql.class, method = "findOne")
FooEntity findOne(@Param("id") Long id);
```

**约定**：

- Provider 方法使用 **实例方法**（`public String xxx()`）。MyBatis 会 `new` 一次 SQL 构建类再反射调用；**不要**在 `static` 方法里调用实例上的 `quote()`（编译失败）。若未继承 `RuntimeSql` 且使用 `static`，需在方法内自行 `RuntimeSqlDialectRegistry.get()`。
- **`limitOne()`** 仅用于「可能返回多行」的 `SELECT` 需要单行时；**不要**在 `SELECT COUNT(*)`、`MAX`/`MIN` 等聚合 SQL 后追加（见接口注释与各仓库已修案例）。
- **`enabledTrueSqlLiteral()`** 用于手写 SQL 中与开关列比较（无 `#{param}` 时）；PostgreSQL `boolean` 与 MySQL 整型列语义不同，由方言返回 `TRUE` 或 `1`。
- 需要完整方言 API 时调用 **`dialect()`**，与 `RuntimeSqlDialectRegistry.get()` 等价。

**业务侧已接入示例（本仓库）**：各模块 `**/dao/sql/*DaoSql`（如 `SysUserDaoSql`、`SysConfigDaoSql`、`WallDaoSql`、`OauthInlineSql`、`UserOpenDaoSql` 等）均继承 **`RuntimeSql`**；切面等处的 `FIND_IN_SET` 替代见 `DataFilterAspect`。原先 Mapper 上硬编码 `limit 1` / `LIMIT 1` 的 `@Select` 已逐步改为 `@SelectProvider` + **`limitOne()`**。

**依赖方升级与清单**：见 **`AI_UPGRADE.md`**「跨库手写 SQL 与 `RuntimeSql`」。

### 3.5 MyBatis-Plus 分页

- `MybatisPlusConfig`：分页插件（如 `MybatisPlusInterceptor` + `PaginationInnerInterceptor`）按 `autumn.database` 设置 `DbType`（含 `POSTGRE_SQL` / `ORACLE` / `SQL_SERVER` / `MYSQL` 等），与 `autumn.database` 对齐。

### 3.6 备份服务

- `DatabaseBackupService`：非 MySQL/MariaDB 时 `exportDatabase` 抛 `UnsupportedOperationException`（避免误用 mysqldump 语义）。

### 3.7 构建与运行（横向）

| 项 | 说明 |
|----|------|
| **Lombok** | 根 `pom.xml` 为 `maven-compiler-plugin` 配置 `annotationProcessorPaths`（JDK 9+ 必须，否则 `@Slf4j`/`@Data`/`@Builder` 不生成代码） |
| **`PluginManager`** | 去掉对 `sun.misc.Launcher` 的编译期依赖，JDK 9+ 用反射尝试扩展 classpath，失败则跳过 |
| **`web` spring-boot-maven-plugin** | 配置 `mainClass: cn.org.autumn.Web` |
| **`Common`（handler）** | 实现 `IResult.setResult/getResult`，避免未实现接口导致编译失败 |

---

## 4. 业务模块变更（按主题）

### 4.1 防火墙 / Wall

- `WallDaoSql` / `WallCounterSql` 等：继承 **`RuntimeSql`**，`limitOne()`、`currentTimestamp()`、`enabledTrueSqlLiteral()` 等经 **`dialect()`** 路由。
- 各 Wall DAO：`@UpdateProvider` 修正误用 `@Select` 执行 `UPDATE`。
- `ShieldEntity` / `JumpEntity` 的 `enable`：使用 **`int` 0/1** + `isEnabled()` 语义方法，避免 **`getEnable` + `isEnable` 双 getter** 触发 MyBatis `Reflector` 冲突。

### 4.2 系统配置

- `SysConfigEntity.readonly`：**`int` 0/1**；`SysConfigService.put` 中修正 **`entity` 分支误写 `config.setReadonly`**，并改为 `setReadonly(0/1)`。

### 4.3 用户 / 登录 / OAuth

- `UserOpenEntity.deleted`、`UserLoginLogEntity`（`white`/`logout`/`allow`）、`SysCategoryEntity.frozen`、`SecurityRequestEntity.enabled`：统一为 **`int` 0/1** 及调用处调整，避免 PG `smallint` 与 JDBC `boolean` 冲突；注意 **勿**再为同一属性提供冲突的 `isXxx`+`getXxx`（布尔语义）组合。
- `UserTokenService.queryByToken`：委托 `getToken(token)`，与 DAO SQL 一致。

### 4.4 分页总数统计（PostgreSQL）

- **`BaseService.queryPage`**：`selectPage` 后通过 **`SqlHelper.fillWrapper`** 会把 **Page 排序写入 Wrapper**，直接 `selectCount(wrapper)` 会生成 **`COUNT(...) ORDER BY ...`**，PostgreSQL 报错。
- **处理**：`selectCountWithoutOrderBy`：从 `getSqlSegment()` 去掉 **`ORDER BY`** 段，必要时去掉 **`WHERE`** 前缀后 `where(...)`，再 `selectCount`。

---

## 5. 兼容性与迁移策略

### 5.1 MySQL / MariaDB（基线）

- 行为保持：**`tinyint(1)` + Java `int` 0/1** 或 **布尔 DDL 策略**按库分支；现有 MySQL 库**无需**因本方案强制变更。
- 继续使用原 `QuerySql`、原 `TableDao` 路径（经 `MysqlRelationalTableOperations`）。

### 5.2 PostgreSQL（新建库）

- **注解同步**：`tinyint(1)`（来自 Java `boolean` 元数据）在 `PostgresQuerySql` 中生成 **`boolean`** 列，与 JDBC `boolean` 一致。
- **默认值**：布尔含义的 `0/1` 在 DDL 中写为 **`false`/`true`**。

### 5.3 PostgreSQL（已有库 / 从 MySQL 迁出）

| 问题 | 策略 |
|------|------|
| 列仍为 **`smallint`**，实体曾用 **`boolean`** | **推荐 A**：`ALTER COLUMN ... TYPE boolean USING (col <> 0)`；**推荐 B**：实体改为 **`int` 0/1**（本仓库对多处表已采用 B） |
| 索引元数据 SQL 错误 | 已修 `PostgresQuerySql.showIndex` 中 `ordinality` 别名引用 |
| 分页 count 带 `ORDER BY` | 已修 `BaseService.selectCountWithoutOrderBy` |

### 5.4 构建与依赖

- 全量构建请使用 **`mvn clean install -pl <模块> -am`**，避免仅 `web` 运行时使用 **`~/.m2` 旧 `autumn-modules` JAR** 导致运行时类与源码不一致。

### 5.5 其他数据库（Oracle / SQL Server / other）

- `columnValueInCommaSeparatedList` 等可能仍路由到 MySQL 风格实现，需在对应方言中补全后再用于生产。

---

## 6. 变更过程（时间线式归纳）

1. **基础设施**：`DatabaseType`、`DatabaseHolder`、路由表操作、PG 包、`TableInit` 条件执行、`MapperScan`、示例 `application-postgresql.yml`；多库规范见 **`AI_DATABASE.md`**。
2. **运行时方言**：`RuntimeSqlDialect` 体系与业务 Provider/SQL 分批替换 `limit`、`FIND_IN_SET`、保留字列名等。
3. **实体与 JDBC**：PG 上 `smallint` vs `boolean` 问题 → 注解层 **`tinyint(1)`→`boolean` DDL** + 存量/业务侧 **`int` 标志位** 双轨。
4. **MyBatis 反射**：避免 **`getX`/`isX`** 对同一逻辑列冲突。
5. **分页**：`BaseService` 统计条数去掉 `ORDER BY`。
6. **工程化**：Lombok AP、`PluginManager` JDK 模块、`spring-boot-maven-plugin` mainClass、IResult 实现类等，保障 **JDK 9+ 与完整 reactor 编译**。

---

## 7. 验证建议

- **编译**：`mvn clean install -pl web -am -DskipTests`
- **PG**：`spring.profiles.active` 含 `postgresql`（或与 `dev` 组合），连接真实实例跑建表/备份列表/登录与 OAuth 相关路径。
- **回归 MySQL**：`dev`  profile 下核心 CRUD、分页、注解同步。

---

## 8. 文档与配置索引

| 文件 | 内容 |
|------|------|
| `application-postgresql.yml` | PG 可选 profile；与 prod/dev 共用环境变量名 |
| `application-prod.yml` | 生产数据源与 `AUTUMN_DATABASE` 等可由环境变量覆盖（见文件内注释） |
| `AI_BOOT.md` | PostgreSQL 摘要、`RuntimeSql` 索引 + 指向本文件 |
| `AI_INDEX.md` / `AI_GUIDE.md` | 索引与导航中增加本文件条目 |
| `AI_UPGRADE.md` | 依赖方升级 autumn 的通用清单、只读扫描脚本与自动化边界（不限于 PG） |

---

## 9. 依赖方升级（与 PG 的关系）

将 autumn 作为 **Maven 依赖** 升级到新版本时，除本文件中的 **多库 / PG** 项外，还需统一 GAV、构建链路与业务回归；完整清单与 **`scripts/autumn-dependency-scan.sh`** 用法见 **`AI_UPGRADE.md`**。

（完）

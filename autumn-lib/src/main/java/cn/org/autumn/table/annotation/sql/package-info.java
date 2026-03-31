/**
 * 与具体 SQL 方言相关的注解取值（存储引擎、字符集、排序规则等）。
 * <p>
 * 设计目标：注解层使用<strong>语义化、可移植的枚举名</strong>；各数据库的实际 DDL 字面量由
 * {@link cn.org.autumn.table.annotation.sql.Dialect 方言} 对应的建表/迁移实现负责映射。
 * 当前 Autumn 内置的 {@code QuerySql} / {@code MysqlTableService} 以 <b>MySQL/MariaDB</b> 为主实现，
 * {@link cn.org.autumn.table.annotation.sql.Engine#getSqlName()}、{@link cn.org.autumn.table.annotation.sql.CharacterSet#getSqlName()}、{@link cn.org.autumn.table.annotation.sql.Collation#getSqlName()}
 * 返回该兼容语法下的标识符；接入 PostgreSQL、SQL Server 等时应在对应方言模块中做转换或忽略无关项。
 */
package cn.org.autumn.table.annotation.sql;

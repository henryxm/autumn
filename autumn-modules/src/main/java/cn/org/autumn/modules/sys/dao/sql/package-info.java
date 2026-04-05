/**
 * 按模块存放可移植 SQL 构建类（{@code spm}/{@code lan}/{@code client} 等模块亦含 {@code dao/sql}），供 MyBatis {@code *Provider} 引用。
 * 统一通过 {@link cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry} 取 {@link cn.org.autumn.database.runtime.RuntimeSqlDialect}：
 * {@code quote}、{@code limitOne}、{@code likeContainsAny}、{@code currentTimestamp} 等。
 */
package cn.org.autumn.modules.sys.dao.sql;

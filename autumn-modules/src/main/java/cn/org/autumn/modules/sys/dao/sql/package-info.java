/**
 * 按模块存放可移植 SQL 构建类（其它模块的 {@code dao/sql} 同理），供 MyBatis {@code *Provider} 引用。
 * <p>
 * 推荐继承 {@link cn.org.autumn.database.runtime.RuntimeSql}，通过 {@link cn.org.autumn.database.runtime.RuntimeSql#quote(String)}、
 * {@link cn.org.autumn.database.runtime.RuntimeSql#limitOne()}、{@link cn.org.autumn.database.runtime.RuntimeSql#likeContainsAny(String)} 等
 * 拼装 SQL；底层仍由 {@link cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry} 解析为
 * {@link cn.org.autumn.database.runtime.RuntimeSqlDialect}。
 */
package cn.org.autumn.modules.sys.dao.sql;

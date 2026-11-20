package cn.org.autumn.database.runtime.query;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记 {@code *DaoSql}：启动发现时反射调用所有<strong>无参且返回 String</strong>的 public 方法，
 * 以便 {@link IndexedSelect#sql()} 登记索引。方法须可无参安全调用（仅拼 SQL）。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface QueryIndexScan {
}

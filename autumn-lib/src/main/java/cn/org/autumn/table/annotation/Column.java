package cn.org.autumn.table.annotation;

import cn.org.autumn.table.data.DataType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Column{

	/**
	 * 字段名
     *
	 * @return 字段名
	 */
	public String value() default "";

	/**
	 * 字段类型
	 * 
	 * @return 字段类型
	 */
	public String type() default DataType.VARCHAR;

	/**
	 * 字段长度，默认是255
	 * 
	 * @return 字段长度，默认是255
	 */
	public int length() default 255;

	/**
	 * 小数点长度，默认是-1
	 * 
	 * @return 小数点长度，默认是-1
	 */
	public int decimalLength() default -1;

	/**
	 * 是否为可以为null，true是可以，false是不可以，默认为true
	 * 
	 * @return 是否为可以为null，true是可以，false是不可以，默认为true
	 */
	public boolean isNull() default true;

	/**
	 * 是否是主键，默认false
	 * 
	 * @return 是否是主键，默认false
	 */
	public boolean isKey() default false;

	/**
	 * 是否自动递增，默认false 只有主键才能使用
	 * 
	 * @return 是否自动递增，默认false 只有主键才能使用
	 */
	public boolean isAutoIncrement() default false;

	/**
	 * 默认值，默认为null
	 * 
	 * @return 默认值，默认为null
	 */
	public String defaultValue() default "NULL";
	
	/**
	 * 是否是唯一，默认false
	 * 
	 * @return 是否是唯一，默认false
	 */
	public boolean isUnique() default false;

	/**
	 * 字段注释
	 * @return 字段注释
	 */
	public String comment() default "";
}

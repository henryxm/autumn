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
	 * 字段注释（会写入库表 COMMENT 与生成实体注解）。
	 * <p>与框架多语言/列表表头约定：若需「短标题 + 详细说明」，使用英文冒号分隔——
	 * {@code BaseService#getLanguageItemsInternal()} 等对 {@code comment} 含 {@code :} 时仅取<strong>第一个冒号之前</strong>作为展示用短标题，
	 * 冒号之后为详述（不进入该短标题位）。建议冒号前 1～4 个汉字（或等价短词）作表头，冒号后写完整业务说明。
	 *
	 * @return 字段注释
	 */
	public String comment() default "";
}

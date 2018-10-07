package cn.org.autumn.table.data;

import cn.org.autumn.table.annotation.LengthCount;

public class DataType {

	@LengthCount
	public static final  String INT = "int";
	
	@LengthCount
	public static final  String VARCHAR = "varchar";
	
	@LengthCount(LengthCount=0)
	public static final  String TEXT = "text";

	@LengthCount(LengthCount=0)
	public static final  String MEDIUMTEXT = "mediumtext";

	@LengthCount(LengthCount=0)
	public static final  String LONGTEXT = "longtext";

	@LengthCount(LengthCount=0)
	public static final  String DATETIME = "datetime";
	
	@LengthCount(LengthCount=2)
	public static final  String DECIMAL = "decimal";
	
	@LengthCount(LengthCount=2)
	public static final  String DOUBLE = "double";
	
	@LengthCount
	public static final  String CHAR = "char";
	
	/**
	 * 等于java中的long
	 */
	@LengthCount
	public static final  String BIGINT = "bigint";
}

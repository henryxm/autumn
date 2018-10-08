package cn.org.autumn.modules.gen.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {
	public final static String DATE_PATTERN = "yyyy-MM-dd";
	public final static String DATE_TIME_PATTERN = "yyyy-MM";//-dd HH:mm:ss
	
	public static String format(Date date) {
        return format(date, DATE_PATTERN);
    }

    public static String format(Date date, String pattern) {
        if(date != null){
            SimpleDateFormat df = new SimpleDateFormat(pattern);
            return df.format(date);
        }
        return null;
    }
}

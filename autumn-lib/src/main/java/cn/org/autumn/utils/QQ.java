package cn.org.autumn.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QQ {
    public static boolean isQQ(String str) {
        try {
            String regex = "[1-9][0-9]{4,14}";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(str);
            return m.matches();
        } catch (Exception e) {
            return false;
        }
    }
}
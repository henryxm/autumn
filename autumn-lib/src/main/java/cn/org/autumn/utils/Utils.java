package cn.org.autumn.utils;

public class Utils {
    public static boolean parseBoolean(String s) {
        return ((s != null) && (s.equalsIgnoreCase("true")
                || s.equalsIgnoreCase("1")
                || s.equalsIgnoreCase("yes")
                || s.equalsIgnoreCase("on")
                || s.equalsIgnoreCase("是")
                || s.equalsIgnoreCase("好")
        ));
    }
}

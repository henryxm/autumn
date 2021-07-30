package cn.org.autumn.utils;

import javax.servlet.http.HttpServletRequest;
import java.net.URL;
import java.net.URLDecoder;

import static com.baomidou.mybatisplus.toolkit.StringUtils.UTF8;

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

    public static String getCallback(HttpServletRequest request) {
        try {
            String refer = request.getHeader("referer");
            URL uri = new URL(refer);
            String query = uri.getQuery();
            String[] dd = query.split("&");
            for (String b : dd) {
                if (b.startsWith("callback=")) {
                    String[] a = b.split("=");
                    if (a.length == 2)
                        return URLDecoder.decode(a[1], UTF8);
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }
}

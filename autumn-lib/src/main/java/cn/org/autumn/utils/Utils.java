package cn.org.autumn.utils;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Random;

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

    public static String print(HttpServletRequest request) {
        Enumeration<String> e = request.getHeaderNames();
        boolean h = e.hasMoreElements();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ip:").append(IPUtils.getIp(request)).append(",");
        while (h) {
            String header = e.nextElement();
            String h_value = request.getHeader(header);
            stringBuilder.append(header).append(":").append(h_value);
            stringBuilder.append(",");
            h = e.hasMoreElements();
        }
        return stringBuilder.toString();
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

    public static String replaceSchemeHost(String scheme, String host, String original) {
        try {
            URI uri = new URI(original);
            uri = new URI(scheme.toLowerCase(Locale.US), uri.getUserInfo(), host, uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
            return uri.toString();
        } catch (Exception e) {
            return original;
        }
    }

    public static String getRandomCode(int c) {
        String t = "0123456789";
        int l = t.length();
        String r = "";
        Random random = new Random();
        while (r.length() < c) {
            int i = random.nextInt(l);
            r = r + t.charAt(i);
        }
        return r;
    }
}

package cn.org.autumn.table.utils;

import io.netty.util.internal.StringUtil;

/**
 * Camel to underline convert
 */
public class HumpConvert {

    /***
     * 下划线命名转为驼峰命名
     *
     * @param para
     *        下划线命名的字符串
     */
    public static String UnderlineToHump(String para) {
        StringBuilder result = new StringBuilder();
        String a[] = para.split("_");
        for (String s : a) {
            if (result.length() == 0) {
                result.append(s.toLowerCase());
            } else {
                result.append(s.substring(0, 1).toUpperCase());
                result.append(s.substring(1).toLowerCase());
            }
        }
        return result.toString();
    }

    /***
     * 驼峰命名转为下划线命名
     *
     * @param para
     *        驼峰命名的字符串
     */

    public static String HumpToUnderline(String para) {
        StringBuilder sb = new StringBuilder(para);
        int temp = 0;//定位
        for (int i = 1; i < para.length(); i++) {
            if (Character.isUpperCase(para.charAt(i))) {
                sb.insert(i + temp, "_");
                temp += 1;
            }
        }
        return sb.toString().toLowerCase();
    }

    public static String HumpToName(String para) {
        para = toFirstStringUpper(para);
        StringBuilder sb = new StringBuilder(para);
        int temp = 0;//定位
        for (int i = 1; i < para.length(); i++) {
            if (Character.isUpperCase(para.charAt(i))) {
                sb.insert(i + temp, " ");
                temp += 1;
            }
        }
        return sb.toString();
    }

    public static String toFirstStringUpper(String str) {
        if (StringUtil.isNullOrEmpty(str))
            return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}

package cn.org.autumn.utils;

import org.apache.commons.lang.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Phone {

    public static boolean isPhone(String phone) {
        try {
            if (StringUtils.isBlank(phone))
                return false;
            String regex = "^((13[0-9])|(14[0-9])|(15([0-9]))|(16[0-9])|(17[0-9])|(18[0-9])|(19[0-9]))\\d{8}$";
            if (phone.length() != 11) {
                return false;
            } else {
                Pattern p = Pattern.compile(regex);
                Matcher m = p.matcher(phone);
                return m.matches();
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 过滤字符串中的电话号码，将电话号码替换为空字符串
     * 支持匹配中国手机号（11位，13/14/15/16/17/18/19开头）
     * 支持匹配带分隔符的格式（如：138-1234-5678, 138 1234 5678, 138.1234.5678等）
     *
     * @param text 待过滤的文本
     * @return 过滤后的文本（电话号码被替换为空字符串）
     */
    public static String filter(String text) {
        if (StringUtils.isBlank(text)) {
            return text;
        }
        try {
            // 匹配11位中国手机号，支持多种格式：
            // 1. 连续11位数字：13812345678
            // 2. 带分隔符：138-1234-5678, 138 1234 5678, 138.1234.5678
            // 3. 混合格式：138-1234 5678, 138 1234-5678
            // 正则表达式：1[3-9]开头，后面10位数字，中间可能有0个或多个分隔符（空格、横线、点）
            // 使用非捕获组和可选分隔符来匹配各种格式
            String regex = "1[3-9]\\d(?:[\\s\\-.]?\\d{4}){2}|1[3-9]\\d{9}";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(text);
            // 替换所有匹配的电话号码为空字符串
            String result = matcher.replaceAll("");
            // 清理可能留下的多余空格（将多个连续空格替换为单个空格）
            result = result.replaceAll("\\s+", " ").trim();
            return result;
        } catch (Exception e) {
            // 如果出现异常，返回原文本
            return text;
        }
    }
}
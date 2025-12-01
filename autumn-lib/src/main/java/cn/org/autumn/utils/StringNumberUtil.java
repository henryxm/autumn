package cn.org.autumn.utils;

import org.apache.commons.lang.StringUtils;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * 字符串数字工具类
 * 提供字符串数字格式转换相关功能
 */
public class StringNumberUtil {

    /**
     * 将科学计数法表示的字符串转换为普通数字字符串
     * 如果输入是科学计数法（如 1.234E+10），则转换为普通数字字符串（如 12340000000）
     * 如果输入不是科学计数法，则原样返回
     *
     * @param numberStr 可能包含科学计数法的数字字符串
     * @return 转换后的普通数字字符串，如果输入为空或转换失败则返回原字符串
     */
    public static String convertScientificNotationToString(String numberStr) {
        if (StringUtils.isBlank(numberStr)) {
            return numberStr;
        }
        // 判断是否是科学计数法（包含 E 或 e）
        if (!numberStr.contains("E") && !numberStr.contains("e")) {
            return numberStr;
        }
        try {
            // 使用BigDecimal解析科学计数法，避免精度丢失
            BigDecimal decimal = new BigDecimal(numberStr);
            // 转换为long类型
            long longValue = decimal.longValue();
            // 转换回字符串
            return String.valueOf(longValue);
        } catch (Exception e) {
            // 转换失败时返回原字符串
            return numberStr;
        }
    }

    /**
     * 判断一个字符串是否是科学计数法表示的
     * 科学计数法格式：数字部分 + E/e + 指数部分
     * 支持格式如：1.23E+10, 1.23e-5, 1E10, 1.23e10, 1.23E-10, -1.23E+10 等
     *
     * @param numberStr 待判断的字符串
     * @return true 如果是科学计数法格式，false 否则
     */
    public static boolean isScientificNotation(String numberStr) {
        if (StringUtils.isBlank(numberStr)) {
            return false;
        }
        // 去除首尾空格
        String trimmed = numberStr.trim();
        // 科学计数法正则表达式：
        // 可选的正负号 + 数字部分（整数或小数）+ E或e + 可选的正负号 + 指数部分（整数）
        // 示例：1.23E+10, 1.23e-5, 1E10, -1.23E-10, 1.23e10
        String regex = "^[+-]?\\d+(\\.\\d+)?[Ee][+-]?\\d+$";
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(trimmed).matches();
    }

    /**
     * 判断一个字符串是否包含除大小写字母和数字以外的字符
     * 如果字符串只包含大小写字母（a-z, A-Z）和数字（0-9），返回 false
     * 如果字符串包含其他字符（如空格、标点符号、特殊字符等），返回 true
     *
     * @param str 待判断的字符串
     * @return true 如果包含除字母数字以外的字符，false 如果只包含字母和数字
     */
    public static boolean containsNonAlphanumeric(String str) {
        if (StringUtils.isBlank(str)) {
            return false;
        }
        // 使用正则表达式匹配非字母数字字符
        // [^a-zA-Z0-9] 表示匹配任何不是字母或数字的字符
        Pattern pattern = Pattern.compile("[^a-zA-Z0-9]");
        return pattern.matcher(str).find();
    }
}
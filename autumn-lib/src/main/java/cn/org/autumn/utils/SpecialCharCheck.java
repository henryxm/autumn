package cn.org.autumn.utils;

import java.util.regex.Pattern;

public class SpecialCharCheck {
    // 定义特殊字符的正则表达式
    private static final String SPECIAL_CHARS_REGEX = "[^a-zA-Z0-9]";

    // 定义一个静态的编译好的Pattern对象
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile(SPECIAL_CHARS_REGEX);

    public static boolean containsSpecialChar(String input) {
        if (null == input)
            return false;
        // 使用Pattern的matcher方法检查输入是否包含特殊字符
        return SPECIAL_CHARS_PATTERN.matcher(input).find();
    }
}
package cn.org.autumn.utils;

import org.apache.commons.lang3.StringUtils;

public class Emoji {
    private static boolean isNotEmoji(char codePoint) {
        return codePoint == 0x0 || codePoint == 0x9 || codePoint == 0xA || codePoint == 0xD || codePoint >= 0x20 && codePoint <= 0xD7FF || codePoint >= 0xE000 && codePoint <= 0xFFFD;
    }

    /**
     * 过滤emoji 或者 其他非文字类型的字符
     */
    public static String remove(String source) {
        if (StringUtils.isBlank(source)) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        int len = source.length();
        for (int i = 0; i < len; i++) {
            char codePoint = source.charAt(i);
            if (isNotEmoji(codePoint)) {
                builder.append(codePoint);
            }
        }
        return builder.toString();
    }
}

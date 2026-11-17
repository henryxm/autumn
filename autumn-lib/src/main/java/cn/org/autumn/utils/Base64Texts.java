package cn.org.autumn.utils;

import java.util.Base64;

/**
 * Base64 文本规范化与解码（登录 form-urlencoded / PEM 兼容）。
 * <p>
 * {@code application/x-www-form-urlencoded} 会把密文中的 {@code +} 解析成空格，
 * 直接 {@link Base64#getDecoder()} 会报 {@code Illegal base64 character 20}。
 */
public final class Base64Texts {

    private Base64Texts() {
    }

    /**
     * 去掉 CR/LF/TAB；将空格还原为 {@code +}（form 解码副作用）。
     */
    public static String normalize(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == ' ') {
                sb.append('+');
            } else if (c != '\r' && c != '\n' && c != '\t') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 规范化后标准 Base64 解码；失败时再尝试 URL_SAFE（部分客户端用 {@code -_}）。
     */
    public static byte[] decode(String input) {
        String normalized = normalize(input);
        if (normalized == null || normalized.isEmpty()) {
            throw new IllegalArgumentException("Base64 内容为空");
        }
        try {
            return Base64.getDecoder().decode(normalized);
        } catch (IllegalArgumentException first) {
            try {
                return Base64.getUrlDecoder().decode(normalized);
            } catch (IllegalArgumentException ignored) {
                throw first;
            }
        }
    }
}

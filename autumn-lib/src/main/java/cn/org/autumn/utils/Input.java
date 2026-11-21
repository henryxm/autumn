package cn.org.autumn.utils;

import java.util.Arrays;

/**
 * 用户可见输入归一化：trim、折叠连续空白、按场景限制空格词数。
 * <p>
 * 标准写法（按场景选方法，勿一刀切）：
 * <pre>
 * Input.nick(s)       // 昵称 / 群名 / 标签
 * Input.remark(s)     // 备注 / label
 * Input.none(s)       // 电话 / 邮箱 / 网址 / account
 * Input.body(s)       // 标题等单行长句
 * Input.bodyLines(s)  // 评论 / 简介 / 正文（多行）
 * </pre>
 * 不用于 uuid / symbol 等标识字段。
 */
public final class Input {

    /** 昵称 / 群名 / 标签：最多 2 空格（约 3 词） */
    public static final int NICK = 2;
    /** 备注 / label：最多 5 空格（约 6 词） */
    public static final int REMARK = 5;
    /** 电话 / 邮箱 / 网址 / account：禁止空格 */
    public static final int NONE = 0;
    /** 长句：不限词数（仍 trim + 折叠连续空格） */
    public static final int BODY = -1;

    private Input() {
    }

    /** 单行：trim + 折叠连续空白；spaces&lt;0 不限词数，0 去空格，&gt;0 保留前 spaces+1 词 */
    public static String of(String raw, int spaces) {
        if (raw == null) {
            return "";
        }
        String s = raw.replace('\u3000', ' ').replace('\u00A0', ' ');
        s = trimWhitespace(s);
        s = s.replaceAll("[ \\t]+", " ").trim();
        if (spaces < 0) {
            return s;
        }
        if (spaces == 0) {
            return s.replace(" ", "");
        }
        if (s.isEmpty()) {
            return s;
        }
        String[] parts = s.split(" ");
        if (parts.length <= spaces + 1) {
            return s;
        }
        return String.join(" ", Arrays.copyOf(parts, spaces + 1));
    }

    /** 多行：保留换行，每行 {@link #of}，去掉首尾空行 */
    public static String lines(String raw, int spaces) {
        if (raw == null) {
            return "";
        }
        String normalized = raw.replace("\r\n", "\n").replace('\r', '\n');
        String[] rows = normalized.split("\n", -1);
        StringBuilder out = new StringBuilder();
        int start = 0;
        int end = rows.length - 1;
        while (start <= end && of(rows[start], spaces).isEmpty()) {
            start++;
        }
        while (end >= start && of(rows[end], spaces).isEmpty()) {
            end--;
        }
        for (int i = start; i <= end; i++) {
            if (out.length() > 0) {
                out.append('\n');
            }
            out.append(of(rows[i], spaces));
        }
        return out.toString();
    }

    public static String nick(String raw) {
        return of(raw, NICK);
    }

    public static String remark(String raw) {
        return of(raw, REMARK);
    }

    public static String none(String raw) {
        return of(raw, NONE);
    }

    public static String body(String raw) {
        return of(raw, BODY);
    }

    public static String bodyLines(String raw) {
        return lines(raw, BODY);
    }

    private static String trimWhitespace(String s) {
        int len = s.length();
        int start = 0;
        while (start < len && Character.isWhitespace(s.charAt(start))) {
            start++;
        }
        int end = len;
        while (end > start && Character.isWhitespace(s.charAt(end - 1))) {
            end--;
        }
        return start == 0 && end == len ? s : s.substring(start, end);
    }
}

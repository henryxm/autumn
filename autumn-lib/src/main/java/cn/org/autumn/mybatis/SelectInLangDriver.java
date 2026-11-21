package cn.org.autumn.mybatis;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;

/**
 * 自定义 Select IN 注解：将 {@code IN (#{param})} 展开为 {@code <foreach>}。
 * <p>
 * Provider 返回的 SQL 会包进 {@code <script>} 再按 XML 解析。须先对 SQL 文本做 XML 转义
 *（{@code <=}、{@code <>}、裸 {@code &} 等），再注入 foreach 标签；否则会出现
 * {@code SAXParseException: 元素内容必须由格式正确的字符数据或标记组成}。
 * <p>
 * 已是实体引用的片段（{@code &lt;} {@code &gt;} {@code &amp;} {@code &#…;}）不会二次转义，
 * 兼容历史手写 {@code &lt;=} 的 Provider。
 * <p>
 * 业务侧闭区间优先 {@link cn.org.autumn.database.runtime.RuntimeSql#columnBetween} /
 * {@code BETWEEN}，见 {@code docs/AI_DATABASE.md} §3.1。
 */
public class SelectInLangDriver extends XMLLanguageDriver implements LanguageDriver {

    private final Pattern inPattern = Pattern.compile("\\(#\\{(\\w+)\\}\\)");

    @Override
    public SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType) {
        if (script == null) {
            script = "";
        }
        // 先转义 SQL 文本，再注入 foreach（标签本身不可被转义）
        String body = escapeXmlText(script);
        Matcher matcher = inPattern.matcher(body);
        body = matcher.replaceAll("<foreach collection=\"$1\" item=\"_item\" open=\"(\" "
                + "separator=\",\" close=\")\">#{_item}</foreach>");
        return super.createSqlSource(configuration, "<script>" + body + "</script>", parameterType);
    }

    /**
     * XML 文本节点转义。{@code &} 优先；已是 {@code &…;} 实体则原样保留，避免 {@code &lt;=} 变成 {@code &amp;lt=}。
     */
    static String escapeXmlText(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '&') {
                int end = xmlEntityEndExclusive(s, i);
                if (end > i) {
                    out.append(s, i, end);
                    i = end - 1;
                } else {
                    out.append("&amp;");
                }
            } else if (c == '<') {
                out.append("&lt;");
            } else if (c == '>') {
                out.append("&gt;");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * 若 {@code s[ampIndex]} 起为合法 XML 实体，返回实体结束下标（不含）；否则 -1。
     */
    static int xmlEntityEndExclusive(String s, int ampIndex) {
        int n = s.length();
        if (ampIndex < 0 || ampIndex >= n || s.charAt(ampIndex) != '&' || ampIndex + 2 >= n) {
            return -1;
        }
        if (s.charAt(ampIndex + 1) == '#') {
            int j = ampIndex + 2;
            if (j < n && (s.charAt(j) == 'x' || s.charAt(j) == 'X')) {
                j++;
            }
            int digits = j;
            while (j < n && isXmlNumericEntityChar(s.charAt(j))) {
                j++;
            }
            if (j > digits && j < n && s.charAt(j) == ';') {
                return j + 1;
            }
            return -1;
        }
        int j = ampIndex + 1;
        while (j < n && isXmlNamedEntityChar(s.charAt(j))) {
            j++;
        }
        if (j > ampIndex + 1 && j < n && s.charAt(j) == ';') {
            return j + 1;
        }
        return -1;
    }

    private static boolean isXmlNamedEntityChar(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    private static boolean isXmlNumericEntityChar(char c) {
        return (c >= '0' && c <= '9')
                || (c >= 'A' && c <= 'F')
                || (c >= 'a' && c <= 'f');
    }
}

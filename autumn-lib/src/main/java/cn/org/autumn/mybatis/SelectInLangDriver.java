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
 * Provider / 注解 SQL 会包进 {@code <script>} 再按 XML 解析。须先对「普通 SQL 文本」做 XML 转义
 *（裸 {@code <=}、{@code <>}、{@code &} 等），再注入 foreach；否则会出现
 * {@code SAXParseException: 元素内容必须由格式正确的字符数据或标记组成}（见 83cc2e7b / dimina）。
 * <p>
 * <strong>升级兼容</strong>（相对转义前旧驱动，依赖方零改）：
 * <ul>
 *   <li>已是实体的 {@code &lt;} {@code &gt;} {@code &amp;} {@code &#…;} —— 不二次转义</li>
 *   <li>完整 {@code <![CDATA[…]]>} —— 段原样保留（历史用 CDATA 包比较符）</li>
 *   <li>{@code <!--…-->} 注释、MyBatis/XML 元素标签（{@code <if>}、{@code </foreach>} 等）—— 原样保留</li>
 *   <li>段/标签外的裸比较符 —— 转义防 SAX</li>
 * </ul>
 * 勿把 {@code IN (#{param})} 写进 CDATA 内，否则 foreach 注入会落在 CDATA 文本中无法展开。
 * 业务侧闭区间优先 {@link cn.org.autumn.database.runtime.RuntimeSql#columnBetween} /
 * {@code BETWEEN}；新代码不要写 CDATA。见 {@code docs/AI_DATABASE.md} §3.1。
 */
public class SelectInLangDriver extends XMLLanguageDriver implements LanguageDriver {

    private static final String CDATA_OPEN = "<![CDATA[";
    private static final String CDATA_CLOSE = "]]>";
    private static final String COMMENT_OPEN = "<!--";
    private static final String COMMENT_CLOSE = "-->";

    private final Pattern inPattern = Pattern.compile("\\(#\\{(\\w+)\\}\\)");

    @Override
    public SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType) {
        if (script == null) {
            script = "";
        }
        // 先转义普通 SQL 文本，再注入 foreach（标签本身不可被转义）
        String body = escapeXmlText(script);
        Matcher matcher = inPattern.matcher(body);
        body = matcher.replaceAll("<foreach collection=\"$1\" item=\"_item\" open=\"(\" "
                + "separator=\",\" close=\")\">#{_item}</foreach>");
        return super.createSqlSource(configuration, "<script>" + body + "</script>", parameterType);
    }

    /**
     * 对普通 SQL 文本做 XML 转义；CDATA / 注释 / 元素标签原样保留。
     */
    static String escapeXmlText(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length() + 16);
        int n = s.length();
        for (int i = 0; i < n; ) {
            if (s.startsWith(CDATA_OPEN, i)) {
                int close = s.indexOf(CDATA_CLOSE, i + CDATA_OPEN.length());
                if (close >= 0) {
                    int end = close + CDATA_CLOSE.length();
                    out.append(s, i, end);
                    i = end;
                    continue;
                }
            } else if (s.startsWith(COMMENT_OPEN, i)) {
                int close = s.indexOf(COMMENT_CLOSE, i + COMMENT_OPEN.length());
                if (close >= 0) {
                    int end = close + COMMENT_CLOSE.length();
                    out.append(s, i, end);
                    i = end;
                    continue;
                }
            } else if (isXmlElementTagStart(s, i)) {
                int end = xmlElementTagEndExclusive(s, i);
                if (end > i) {
                    out.append(s, i, end);
                    i = end;
                    continue;
                }
            }
            char c = s.charAt(i);
            if (c == '&') {
                int end = xmlEntityEndExclusive(s, i);
                if (end > i) {
                    out.append(s, i, end);
                    i = end;
                } else {
                    out.append("&amp;");
                    i++;
                }
            } else if (c == '<') {
                out.append("&lt;");
                i++;
            } else if (c == '>') {
                out.append("&gt;");
                i++;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    /**
     * {@code <} 后为可选 {@code /} + 字母，视为 XML/MyBatis 元素标签开端（非 CDATA/注释）。
     */
    static boolean isXmlElementTagStart(String s, int ltIndex) {
        int n = s.length();
        if (ltIndex < 0 || ltIndex >= n || s.charAt(ltIndex) != '<') {
            return false;
        }
        int j = ltIndex + 1;
        if (j >= n) {
            return false;
        }
        // <![CDATA[ / <!-- 由调用方优先分支处理；此处排除其它 <!
        if (s.charAt(j) == '!') {
            return false;
        }
        if (s.charAt(j) == '?') {
            // <?xml …?> 极少见于 SQL；不按元素标签保留，走普通转义更安全
            return false;
        }
        if (s.charAt(j) == '/') {
            j++;
            if (j >= n) {
                return false;
            }
        }
        char c = s.charAt(j);
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    /**
     * 从 {@code <} 起找标签结束 {@code >}（属性内引号中的 {@code >} 不计），返回结束下标（不含）；未闭合返回 -1。
     */
    static int xmlElementTagEndExclusive(String s, int ltIndex) {
        int n = s.length();
        boolean inSingle = false;
        boolean inDouble = false;
        for (int i = ltIndex + 1; i < n; i++) {
            char c = s.charAt(i);
            if (inSingle) {
                if (c == '\'') {
                    inSingle = false;
                }
                continue;
            }
            if (inDouble) {
                if (c == '"') {
                    inDouble = false;
                }
                continue;
            }
            if (c == '\'') {
                inSingle = true;
            } else if (c == '"') {
                inDouble = true;
            } else if (c == '>') {
                return i + 1;
            }
        }
        return -1;
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

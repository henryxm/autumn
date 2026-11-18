package cn.org.autumn.table.data.index;

import cn.org.autumn.table.annotation.IndexTypeEnum;
import cn.org.autumn.table.data.IndexInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 索引注解安全校验：where/expression 白名单；空名派生；超长截断；同名冲突提示。
 */
public final class IndexAnnotationValidator {

    private static final Logger log = LoggerFactory.getLogger(IndexAnnotationValidator.class);

    /** 允许标识符、比较、数字、空白、括号、逗号、点号、单引号字面量、白名单函数名。 */
    private static final Pattern SAFE_EXPR = Pattern.compile(
            "^[a-zA-Z0-9_\\s\\.\\,\\(\\)\\=\\<\\>\\!\\'\\\"\\+\\-]*$");

    private static final Set<String> BLOCKED = new HashSet<>();

    static {
        BLOCKED.add(";");
        BLOCKED.add("--");
        BLOCKED.add("/*");
        BLOCKED.add("*/");
        BLOCKED.add(" select ");
        BLOCKED.add(" insert ");
        BLOCKED.add(" update ");
        BLOCKED.add(" delete ");
        BLOCKED.add(" drop ");
        BLOCKED.add(" union ");
    }

    private IndexAnnotationValidator() {
    }

    public static void validate(IndexInfo index) {
        if (index == null) {
            return;
        }
        rejectUnsafe("where", index.getWherePredicate());
        for (IndexColumnSpec spec : index.getColumnSpecs()) {
            if (spec.isExpression()) {
                rejectUnsafe("expression", spec.getExpression());
            }
        }
        if (IndexTypeEnum.FULLTEXT.name().equals(index.getIndexType())
                || IndexTypeEnum.SPATIAL.name().equals(index.getIndexType())) {
            if (index.getColumnSpecs().isEmpty()) {
                log.warn("Index {} type {} has no columns", index.getName(), index.getIndexType());
            }
        }
        if (index.getIncludeColumns() != null) {
            Set<String> fieldCols = new HashSet<>();
            for (IndexColumnSpec s : index.getColumnSpecs()) {
                if (!s.isExpression()) {
                    fieldCols.add(s.getColumn().toLowerCase(Locale.ROOT));
                }
            }
            for (String inc : index.getIncludeColumns()) {
                if (inc != null && fieldCols.contains(inc.toLowerCase(Locale.ROOT))) {
                    log.warn("Index {} INCLUDE column {} duplicates key column", index.getName(), inc);
                }
            }
        }
    }

    public static void validateAll(List<IndexInfo> indexes) {
        normalizeAndValidateAll(indexes, null);
    }

    /**
     * 空名派生、超长截断（WARN）、安全校验、实体内重名检测。不阻断启动（除 unsafe expression）。
     */
    public static void normalizeAndValidateAll(List<IndexInfo> indexes, String tableName) {
        if (indexes == null) {
            return;
        }
        int max = IndexDialectCapability.maxIndexNameLength();
        String table = tableName == null ? "" : tableName;
        for (IndexInfo i : indexes) {
            if (i == null) {
                continue;
            }
            String before = i.getName();
            boolean blankBefore = StringUtils.isBlank(before);
            if (i.ensurePhysicalName(max)) {
                if (blankBefore) {
                    log.warn("索引名为空已按列派生: table={} to={}", table, i.getName());
                } else if (before != null && !before.equals(i.getName())) {
                    log.warn("索引名超长已截断: table={} from={} to={} max={}", table, before, i.getName(), max);
                }
            }
            validate(i);
        }
        Set<String> names = new HashSet<>();
        for (IndexInfo i : indexes) {
            if (i == null || StringUtils.isBlank(i.getName())) {
                continue;
            }
            String n = IndexInfo.normalizeIndexName(i.getName());
            if (!names.add(n)) {
                log.warn("Duplicate index name in entity: table={} name={}", table, i.getName());
            }
        }
    }

    public static void rejectUnsafe(String kind, String raw) {
        if (StringUtils.isBlank(raw)) {
            return;
        }
        String s = raw.trim();
        String lower = " " + s.toLowerCase(Locale.ROOT) + " ";
        for (String b : BLOCKED) {
            if (lower.contains(b) || s.contains(b.trim())) {
                throw new IllegalArgumentException("Unsafe index " + kind + " rejected: " + raw);
            }
        }
        if (!SAFE_EXPR.matcher(s).matches()) {
            throw new IllegalArgumentException("Unsafe index " + kind + " characters: " + raw);
        }
    }
}

package cn.org.autumn.modules.db.service;

import cn.org.autumn.database.runtime.RuntimeSql;
import cn.org.autumn.modules.db.model.IndexedColumnTarget;
import cn.org.autumn.modules.db.model.TableScanColumnMeta;
import cn.org.autumn.modules.db.service.TableScanValueBinder.BindOutcome;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 在 @Table 实体索引列上精确匹配特征值，定位命中表。
 */
@Slf4j
@Service
public class DatabaseTableScanService {

    private static final Pattern COLUMN_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");
    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");
    private static final int QUERY_TIMEOUT_SECONDS = 3;
    private static final int DEFAULT_PREVIEW_LIMIT = 10;
    private static final int MAX_PREVIEW_LIMIT = 50;

    @Autowired
    private EntityIndexedColumnResolver indexedColumnResolver;

    @Autowired
    private DataSource dataSource;

    private final RuntimeSql runtimeSql = RuntimeSql.sql;

    public Map<String, Object> search(String columnName, String value, Integer previewLimit) {
        validateColumnName(columnName);
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException("特征值不能为空");
        }
        normalizePreviewLimit(previewLimit);
        long started = System.currentTimeMillis();
        List<IndexedColumnTarget> candidates = indexedColumnResolver.resolve(columnName);
        List<Map<String, Object>> hits = new ArrayList<>();
        int skippedCount = 0;
        for (IndexedColumnTarget target : candidates) {
            BindOutcome bindOutcome = TableScanValueBinder.canBind(value.trim(), target.getColumnMeta());
            if (!bindOutcome.isBound()) {
                skippedCount++;
                log.debug("Table scan skipped table={} column={}: {}", target.getTableName(), target.getColumnName(), bindOutcome.getSkipReason());
                continue;
            }
            try {
                long hitCount = countExact(target.getTableName(), target.getColumnName(), value.trim(), target.getColumnMeta());
                if (hitCount <= 0) {
                    continue;
                }
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("tableName", target.getTableName());
                row.put("entityClass", target.getEntityClass());
                row.put("entityFullName", target.getEntityFullName());
                row.put("columnName", target.getColumnName());
                row.put("columnType", target.getColumnType());
                row.put("bindKind", bindOutcome.getBindKind());
                row.put("hitCount", hitCount);
                hits.add(row);
            } catch (Exception e) {
                skippedCount++;
                log.warn("Table scan skipped table={} column={}: {}", target.getTableName(), target.getColumnName(), e.getMessage());
            }
        }
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("candidateCount", candidates.size());
        stats.put("hitCount", hits.size());
        stats.put("skippedCount", skippedCount);
        stats.put("elapsedMs", System.currentTimeMillis() - started);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("hits", hits);
        out.put("stats", stats);
        return out;
    }

    public List<Map<String, Object>> preview(String tableName, String columnName, String value, Integer limit) {
        validateTableName(tableName);
        validateColumnName(columnName);
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException("特征值不能为空");
        }
        int rowLimit = normalizePreviewLimit(limit);
        TableScanColumnMeta columnMeta = indexedColumnResolver.findColumnMeta(tableName, columnName);
        BindOutcome bindOutcome = TableScanValueBinder.canBind(value.trim(), columnMeta);
        if (!bindOutcome.isBound()) {
            throw new IllegalArgumentException(bindOutcome.getSkipReason());
        }
        String quotedTable = runtimeSql.quote(tableName);
        String quotedColumn = runtimeSql.quote(columnName);
        String sql = "SELECT * FROM " + quotedTable + " WHERE " + quotedColumn + " = ?" + runtimeSql.limitOffsetSuffix(rowLimit, 0);
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            TableScanValueBinder.bind(ps, 1, value.trim(), columnMeta);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(meta.getColumnLabel(i), TableScanResultFormatter.format(rs.getObject(i)));
                    }
                    rows.add(row);
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("预览失败: " + e.getMessage(), e);
        }
        return rows;
    }

    public List<Map<String, Object>> listCandidates(String columnName) {
        validateColumnName(columnName);
        List<Map<String, Object>> out = new ArrayList<>();
        for (IndexedColumnTarget target : indexedColumnResolver.resolve(columnName)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tableName", target.getTableName());
            row.put("entityClass", target.getEntityClass());
            row.put("entityFullName", target.getEntityFullName());
            row.put("columnName", target.getColumnName());
            row.put("columnType", target.getColumnType());
            row.put("scannable", !target.getColumnMeta().isFieldEncrypted());
            out.add(row);
        }
        return out;
    }

    private long countExact(String tableName, String columnName, String value, TableScanColumnMeta columnMeta) throws Exception {
        String quotedTable = runtimeSql.quote(tableName);
        String quotedColumn = runtimeSql.quote(columnName);
        String sql = "SELECT COUNT(*) FROM " + quotedTable + " WHERE " + quotedColumn + " = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            BindOutcome outcome = TableScanValueBinder.bind(ps, 1, value, columnMeta);
            if (!outcome.isBound()) {
                throw new IllegalArgumentException(outcome.getSkipReason());
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0L;
    }

    private static void validateColumnName(String columnName) {
        if (StringUtils.isBlank(columnName)) {
            throw new IllegalArgumentException("列名不能为空");
        }
        if (!COLUMN_NAME_PATTERN.matcher(columnName.trim()).matches()) {
            throw new IllegalArgumentException("列名仅允许字母、数字与下划线");
        }
    }

    private static void validateTableName(String tableName) {
        if (StringUtils.isBlank(tableName)) {
            throw new IllegalArgumentException("表名不能为空");
        }
        if (!TABLE_NAME_PATTERN.matcher(tableName.trim()).matches()) {
            throw new IllegalArgumentException("表名仅允许字母、数字与下划线");
        }
    }

    private static int normalizePreviewLimit(Integer previewLimit) {
        if (previewLimit == null || previewLimit <= 0) {
            return DEFAULT_PREVIEW_LIMIT;
        }
        return Math.min(previewLimit, MAX_PREVIEW_LIMIT);
    }
}

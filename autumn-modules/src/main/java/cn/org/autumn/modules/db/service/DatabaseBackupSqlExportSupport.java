package cn.org.autumn.modules.db.service;

import cn.org.autumn.database.DatabaseHolder;
import cn.org.autumn.database.DatabaseType;
import cn.org.autumn.database.runtime.RuntimeSqlDialect;
import cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 数据库逻辑备份：真实 MySQL 协议库走 {@code SHOW CREATE TABLE}；其它库（含内嵌 H2 的 {@code MODE=MySQL}，此时
 * {@link DatabaseType} 可能仍为 {@link DatabaseType#MYSQL}）走 JDBC {@link DatabaseMetaData} 拼装 DDL + INSERT，
 * 标识符引用与 {@link RuntimeSqlDialectRegistry} 对齐，避免破坏各库方言规则。
 * <p>
 * <b>完整性</b>：各支持库在相同应用初始化下应导出<b>同一套业务表</b>（FULL 模式仅排除备份元数据表）；Derby/DB2 不得因表名
 * {@code sys_*} 误当作系统表而漏导。CLOB 列须导出文本内容而非 {@code toString()}。H2 下 DROP/CREATE/INSERT 对表使用一致的 schema 限定。
 * 跨厂商的 DDL/字面量形态仍可能不同，属 JDBC 近似极限，恢复应在同大类库上进行。
 */
public final class DatabaseBackupSqlExportSupport {

    private static final Logger log = LoggerFactory.getLogger(DatabaseBackupSqlExportSupport.class);

    private static final int MAX_INSERT_BYTES = 2 * 1024 * 1024;

    /** 单单元格 CLOB 导出上限，避免 OOM；超出部分截断（极端长文本列需另行迁移方案）。 */
    private static final int MAX_EXPORT_CLOB_CHARS = 16_777_216;

    /**
     * 全量备份排除：避免导出 DROP/重建备份元数据表，恢复后把备份功能自身表删掉导致应用不可用。
     */
    private static final Set<String> EXCLUDED_FROM_FULL_BACKUP = new HashSet<>(Arrays.asList(
            "db_database_backup",
            "db_database_backup_strategy",
            "db_database_backup_upload"));

    @FunctionalInterface
    public interface ExportProgressConsumer {
        void accept(Long backupId, int completed, int total, String currentTable);
    }

    public interface BackupExportTaskHandle {
        Long getBackupId();

        String getMode();

        String getTableList();

        boolean isStopped();

        void waitIfPaused();
    }

    /**
     * 从当前 JDBC 连接 URL 推断物理库类型；无法识别时返回 {@code null}。
     * 用于 H2 {@code MODE=MySQL} 等场景：逻辑 {@link DatabaseType} 可能为 {@link DatabaseType#MYSQL}，导出仍须按 H2 规则引用标识符。
     */
    public static DatabaseType inferPhysicalJdbcType(Connection conn) throws SQLException {
        if (conn == null) {
            return null;
        }
        DatabaseMetaData meta = conn.getMetaData();
        if (meta == null) {
            return null;
        }
        String url = meta.getURL();
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        return DatabaseHolder.inferFromJdbcUrl(url.trim());
    }

    /**
     * 是否对<b>当前连接</b>使用 MySQL {@code SHOW CREATE TABLE} 快速路径。
     * <p>
     * 内嵌 {@code jdbc:h2:…;MODE=MySQL} 时框架常将类型解析为 {@link DatabaseType#MYSQL}，但 H2 不支持
     * {@code SHOW CREATE TABLE}，须返回 {@code false} 以走 {@link #exportJdbcMetadata}。
     */
    public static boolean usesShowCreateTable(DatabaseType t, Connection conn) throws SQLException {
        if (!(t == DatabaseType.MYSQL || t == DatabaseType.MARIADB
                || t == DatabaseType.TIDB || t == DatabaseType.OCEANBASE_MYSQL)) {
            return false;
        }
        if (conn != null) {
            DatabaseMetaData meta = conn.getMetaData();
            if (meta != null) {
                String url = meta.getURL();
                if (url != null && url.trim().toLowerCase(Locale.ROOT).startsWith("jdbc:h2:")) {
                    return false;
                }
            }
        }
        return true;
    }

    public static List<String> listTableNames(Connection conn, DatabaseType dbType) throws SQLException {
        List<TableRef> refs = listTableRefs(conn, dbType);
        List<String> names = new ArrayList<>(refs.size());
        for (TableRef r : refs) {
            names.add(r.name);
        }
        return names;
    }

    public static List<String> resolveTableList(Connection conn, DatabaseType dbType, String mode, String tableListCsv) throws SQLException {
        if ("TABLES".equals(mode) && tableListCsv != null && !tableListCsv.trim().isEmpty()) {
            List<String> tables = new ArrayList<>();
            for (String t : tableListCsv.split(",")) {
                String x = t.trim();
                if (!x.isEmpty()) {
                    tables.add(x);
                }
            }
            return tables;
        }
        return excludeBackupSelfTablesFromFull(listTableNames(conn, dbType));
    }

    private static List<String> excludeBackupSelfTablesFromFull(List<String> tables) {
        List<String> out = new ArrayList<>(tables.size());
        for (String t : tables) {
            if (t != null && EXCLUDED_FROM_FULL_BACKUP.contains(t.toLowerCase(Locale.ROOT))) {
                if (log.isDebugEnabled()) {
                    log.debug("Full backup skips backup-module metadata table: {}", t);
                }
                continue;
            }
            out.add(t);
        }
        return out;
    }

    public static int[] exportMysqlFamily(Connection conn, BufferedWriter writer, BackupExportTaskHandle task,
                                          List<String> tablesToBackup, DatabaseType dbType,
                                          ExportProgressConsumer onProgress) throws Exception {
        int tableCount = tablesToBackup.size();
        int totalRecords = 0;
        String dbName = safeGetCatalog(conn);
        writeHeader(writer, dbName != null ? dbName : "(default)", task, tableCount, true, dbType);
        int completedCount = 0;
        for (String table : tablesToBackup) {
            if (task.isStopped()) {
                log.info("Backup task stopped during export: id={}, completedTables={}/{}", task.getBackupId(), completedCount, tableCount);
                break;
            }
            task.waitIfPaused();
            if (task.isStopped()) {
                break;
            }
            onProgress.accept(task.getBackupId(), completedCount, tableCount, table);
            writer.write("-- ----------------------------\n");
            writer.write("-- Table structure for " + table + "\n");
            writer.write("-- ----------------------------\n");
            writer.write("DROP TABLE IF EXISTS `" + table + "`;\n");
            String createTableSql = getCreateTableSqlMysql(conn, table);
            writer.write(createTableSql + ";\n\n");
            int records = exportTableDataMysql(conn, writer, table, task);
            totalRecords += records;
            writer.write("\n");
            completedCount++;
        }
        if (!task.isStopped()) {
            writer.write("SET FOREIGN_KEY_CHECKS = 1;\n");
        }
        writer.flush();
        if (!task.isStopped()) {
            onProgress.accept(task.getBackupId(), tableCount, tableCount, null);
        }
        return new int[]{tableCount, totalRecords};
    }

    public static int[] exportJdbcMetadata(Connection conn, BufferedWriter writer, BackupExportTaskHandle task,
                                           List<String> tablesToBackup, DatabaseType dbType,
                                           ExportProgressConsumer onProgress) throws Exception {
        RuntimeSqlDialect dialect = RuntimeSqlDialectRegistry.statelessDialectFor(dbType);
        int tableCount = tablesToBackup.size();
        int totalRecords = 0;
        String dbLabel = safeGetCatalog(conn);
        if (dbLabel == null || dbLabel.isEmpty()) {
            String sch = safeGetSchema(conn);
            dbLabel = sch != null ? sch : "(default)";
        }
        List<TableRef> allRefs = listTableRefs(conn, dbType);
        Map<String, TableRef> refByName = new HashMap<>();
        for (TableRef r : allRefs) {
            if (r.name != null) {
                refByName.putIfAbsent(r.name.toLowerCase(Locale.ROOT), r);
            }
        }
        writeHeader(writer, dbLabel, task, tableCount, false, dbType);
        int completedCount = 0;
        for (String table : tablesToBackup) {
            if (task.isStopped()) {
                log.info("Backup task stopped during export: id={}, completedTables={}/{}", task.getBackupId(), completedCount, tableCount);
                break;
            }
            task.waitIfPaused();
            if (task.isStopped()) {
                break;
            }
            onProgress.accept(task.getBackupId(), completedCount, tableCount, table);
            TableRef ref = table != null ? refByName.get(table.toLowerCase(Locale.ROOT)) : null;
            if (ref == null) {
                for (TableRef r : allRefs) {
                    if (r.name != null && table != null && r.name.equalsIgnoreCase(table)) {
                        ref = r;
                        break;
                    }
                }
            }
            if (ref == null) {
                ref = new TableRef(safeGetCatalog(conn), safeGetSchema(conn), table);
            }
            String quotedTable = qualifiedQuotedTable(ref, dbType, dialect);
            writer.write("-- ----------------------------\n");
            writer.write("-- Table structure for " + table + "\n");
            writer.write("-- ----------------------------\n");
            writeDropTable(writer, quotedTable, dbType);
            String ddl = buildCreateTableFromMetadata(conn, ref, dbType, dialect);
            writer.write(ddl);
            writer.write(";\n\n");
            int records = exportTableDataJdbc(conn, writer, quotedTable, task, dbType, dialect);
            totalRecords += records;
            writer.write("\n");
            completedCount++;
        }
        writer.write("-- End of export\n");
        writer.flush();
        if (!task.isStopped()) {
            onProgress.accept(task.getBackupId(), tableCount, tableCount, null);
        }
        return new int[]{tableCount, totalRecords};
    }

    private static void writeHeader(BufferedWriter writer, String dbName, BackupExportTaskHandle task, int tableCount, boolean mysqlPrologue,
                                    DatabaseType backupDialect) throws IOException {
        writer.write("-- ========================================\n");
        writer.write("-- Database Backup (Autumn)\n");
        writer.write("-- Database: " + dbName + "\n");
        writer.write("-- Backup dialect: " + backupDialect.name() + "\n");
        writer.write("-- Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "\n");
        writer.write("-- Mode: " + task.getMode() + "\n");
        writer.write("-- Tables: " + tableCount + "\n");
        writer.write("-- ========================================\n\n");
        if (mysqlPrologue) {
            writer.write("SET NAMES utf8mb4;\n");
            writer.write("SET FOREIGN_KEY_CHECKS = 0;\n\n");
        } else {
            writer.write("-- JDBC metadata export; restore on same vendor major type recommended.\n\n");
        }
    }

    private static void writeDropTable(BufferedWriter w, String quotedTable, DatabaseType db) throws IOException {
        if (db == DatabaseType.ORACLE || db == DatabaseType.DAMENG || db == DatabaseType.OCEANBASE_ORACLE) {
            String lit = quotedTable.replace("'", "''");
            w.write("BEGIN\n");
            w.write("  EXECUTE IMMEDIATE 'DROP TABLE " + lit + " PURGE';\n");
            w.write("EXCEPTION\n");
            w.write("  WHEN OTHERS THEN\n");
            w.write("    IF SQLCODE != -942 THEN RAISE;\n");
            w.write("    END IF;\n");
            w.write("END;\n/\n");
        } else if (db == DatabaseType.DERBY) {
            // Derby 10.14 及常见嵌入式版本对 DROP TABLE IF EXISTS 解析报错（Encountered "EXISTS"）；
            // 恢复侧对「表不存在」的 DROP 会忽略，故仅输出 DROP TABLE。
            w.write("DROP TABLE ");
            w.write(quotedTable);
            w.write(";\n");
        } else {
            w.write("DROP TABLE IF EXISTS ");
            w.write(quotedTable);
            w.write(";\n");
        }
    }

    /**
     * H2 / HSQLDB 下无 schema 限定时，{@code SELECT * FROM "name"} 可能与元数据返回的大小写不一致导致找不到表；
     * 使用元数据中的 schema + name，并与 {@link H2RuntimeSqlDialect} 双引号规则一致。
     */
    private static String qualifiedQuotedTable(TableRef ref, DatabaseType dbType, RuntimeSqlDialect dialect) {
        if (ref == null || ref.name == null) {
            return dialect.quote("");
        }
        if (ref.schema != null && !ref.schema.trim().isEmpty()) {
            if (dbType == DatabaseType.H2 || dbType == DatabaseType.HSQLDB) {
                return dialect.quote(ref.schema) + "." + dialect.quote(ref.name);
            }
        }
        return dialect.quote(ref.name);
    }

    private static String getCreateTableSqlMysql(Connection conn, String table) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE `" + table + "`")) {
            if (rs.next()) {
                return rs.getString(2);
            }
        }
        return "";
    }

    private static int exportTableDataMysql(Connection conn, BufferedWriter writer, String table, BackupExportTaskHandle task) throws Exception {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM `" + table + "`")) {
            return writeInsertResultSet(writer, rs, task, DatabaseType.MYSQL, "`" + table + "`", true, null);
        }
    }

    private static int exportTableDataJdbc(Connection conn, BufferedWriter writer, String quotedTable,
                                           BackupExportTaskHandle task, DatabaseType dbType,
                                           RuntimeSqlDialect columnQuoteDialect) throws Exception {
        String sql = "SELECT * FROM " + quotedTable;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return writeInsertResultSet(writer, rs, task, dbType, quotedTable, false, columnQuoteDialect);
        }
    }

    private static int writeInsertResultSet(BufferedWriter writer, ResultSet rs, BackupExportTaskHandle task,
                                            DatabaseType dbType, String quotedTableForInsert, boolean mysqlStyleColumns,
                                            RuntimeSqlDialect columnQuoteDialect) throws Exception {
        int count = 0;
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();
        if (!rs.next()) {
            writer.write("-- No data for table " + quotedTableForInsert + "\n");
            return 0;
        }
        writer.write("-- ----------------------------\n");
        writer.write("-- Records\n");
        writer.write("-- ----------------------------\n");
        StringBuilder columns = new StringBuilder();
        for (int i = 1; i <= columnCount; i++) {
            if (i > 1) {
                columns.append(", ");
            }
            String colName = meta.getColumnName(i);
            if (mysqlStyleColumns) {
                columns.append("`").append(colName).append("`");
            } else {
                columns.append(columnQuoteDialect != null ? columnQuoteDialect.quote(colName)
                        : RuntimeSqlDialectRegistry.statelessDialectFor(dbType).quote(colName));
            }
        }
        String insertPrefix = "INSERT INTO " + quotedTableForInsert + " (" + columns + ") VALUES\n";
        int batchCount = 0;
        StringBuilder values = new StringBuilder();
        do {
            if (task.isStopped()) {
                return count;
            }
            task.waitIfPaused();
            if (task.isStopped()) {
                return count;
            }
            StringBuilder row = new StringBuilder();
            row.append("(");
            for (int i = 1; i <= columnCount; i++) {
                if (i > 1) {
                    row.append(", ");
                }
                int sqlType = meta.getColumnType(i);
                // Derby 等：LOB 列先 getObject 再 getClob 会触发「Stream or LOB value cannot be retrieved more than once」
                if (sqlType == Types.CLOB || sqlType == Types.NCLOB) {
                    Clob clob = sqlType == Types.NCLOB ? rs.getNClob(i) : rs.getClob(i);
                    row.append(clob == null ? "NULL" : clobToSqlStringLiteral(clob, dbType, mysqlStyleColumns));
                } else {
                    Object val = rs.getObject(i);
                    row.append(formatSqlLiteral(val, sqlType, dbType, mysqlStyleColumns));
                }
            }
            row.append(")");
            int projectedSize = values.length() + row.length() + 3;
            if (batchCount == 0) {
                projectedSize += insertPrefix.length();
            }
            if (batchCount > 0 && projectedSize > MAX_INSERT_BYTES) {
                values.append(";\n");
                writer.write(values.toString());
                values.setLength(0);
                batchCount = 0;
            }
            if (batchCount == 0) {
                values.append(insertPrefix);
            } else {
                values.append(",\n");
            }
            values.append(row);
            count++;
            batchCount++;
        } while (rs.next());
        if (batchCount > 0) {
            values.append(";\n");
            writer.write(values.toString());
        }
        return count;
    }

    private static String formatSqlLiteral(Object val, int sqlType, DatabaseType dbType, boolean mysqlStringEscape)
            throws SQLException {
        if (val == null) {
            return "NULL";
        }
        if (val instanceof Clob) {
            return clobToSqlStringLiteral((Clob) val, dbType, mysqlStringEscape);
        }
        if (val instanceof byte[]) {
            return formatBinaryLiteral((byte[]) val, dbType);
        }
        if (val instanceof Boolean) {
            return formatBooleanSqlLiteral((Boolean) val, dbType);
        }
        if ((sqlType == Types.BOOLEAN || sqlType == Types.BIT) && val instanceof Number) {
            return formatBooleanSqlLiteral(((Number) val).intValue() != 0, dbType);
        }
        if (val instanceof Number && !(val instanceof BigDecimal)) {
            return val.toString();
        }
        if (val instanceof BigDecimal) {
            return val.toString();
        }
        if (val instanceof Timestamp || val instanceof Time || val instanceof java.sql.Date) {
            return "'" + escapeStringLiteral(val.toString(), mysqlStringEscape) + "'";
        }
        if (val instanceof java.util.Date) {
            return "'" + escapeStringLiteral(new Timestamp(((java.util.Date) val).getTime()).toString(), mysqlStringEscape) + "'";
        }
        return "'" + escapeStringLiteral(val.toString(), mysqlStringEscape) + "'";
    }

    private static String clobToSqlStringLiteral(Clob c, DatabaseType dbType, boolean mysqlStringEscape) throws SQLException {
        long length = c.length();
        if (length == 0) {
            return "''";
        }
        int n = (int) Math.min(length, (long) MAX_EXPORT_CLOB_CHARS);
        String raw = c.getSubString(1, n);
        if (length > MAX_EXPORT_CLOB_CHARS && log.isDebugEnabled()) {
            log.debug("CLOB column truncated to {} characters in backup export", MAX_EXPORT_CLOB_CHARS);
        }
        return "'" + escapeStringLiteral(raw, mysqlStringEscape) + "'";
    }

    private static String formatBooleanSqlLiteral(boolean b, DatabaseType dbType) {
        if (dbType == DatabaseType.POSTGRESQL || dbType == DatabaseType.KINGBASE
                || dbType == DatabaseType.DERBY || dbType == DatabaseType.DB2
                || dbType == DatabaseType.H2 || dbType == DatabaseType.HSQLDB) {
            return b ? "TRUE" : "FALSE";
        }
        return b ? "1" : "0";
    }

    /**
     * MySQL 备份/恢复沿用反斜杠转义；其它库用 SQL 标准单引号加倍。
     */
    private static String escapeStringLiteral(String str, boolean mysqlBackslashEscape) {
        if (str == null) {
            return "";
        }
        if (mysqlBackslashEscape) {
            return str.replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\r", "\\r")
                    .replace("\n", "\\n")
                    .replace("\t", "\\t")
                    .replace("\0", "\\0");
        }
        return str.replace("'", "''");
    }

    private static String formatBinaryLiteral(byte[] bytes, DatabaseType dbType) {
        String hex = bytesToHex(bytes);
        if (dbType == DatabaseType.POSTGRESQL || dbType == DatabaseType.KINGBASE) {
            return "E'\\x" + hex + "'";
        }
        return "X'" + hex + "'";
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    /**
     * Druid 及部分 JDBC 驱动未实现 {@link Connection#getSchema()} / {@link Connection#getCatalog()}，会抛
     * {@link SQLFeatureNotSupportedException}，此处降级为 null，由 {@link DatabaseMetaData#getTables} 的多种参数组合兜底。
     */
    private static String safeGetCatalog(Connection conn) {
        try {
            return conn.getCatalog();
        } catch (SQLException e) {
            return null;
        }
    }

    private static String safeGetSchema(Connection conn) {
        try {
            return conn.getSchema();
        } catch (SQLException e) {
            return null;
        }
    }

    private static final class TableRef {
        final String catalog;
        final String schema;
        final String name;

        TableRef(String catalog, String schema, String name) {
            this.catalog = catalog;
            this.schema = schema;
            this.name = name;
        }
    }

    private static List<TableRef> listTableRefs(Connection conn, DatabaseType dbType) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        LinkedHashMap<String, TableRef> map = new LinkedHashMap<>();
        String catalog = safeGetCatalog(conn);
        String schema = safeGetSchema(conn);
        String[] types = new String[]{"TABLE"};
        collectTables(meta, catalog, schema, types, dbType, map);
        if (map.isEmpty() && schema != null) {
            collectTables(meta, catalog, null, types, dbType, map);
        }
        if (map.isEmpty()) {
            collectTables(meta, null, null, types, dbType, map);
        }
        List<TableRef> list = new ArrayList<>(map.values());
        list.sort(Comparator.comparing((TableRef t) -> t.name, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    private static void collectTables(DatabaseMetaData meta, String catalog, String schema, String[] types,
                                      DatabaseType dbType, Map<String, TableRef> out) throws SQLException {
        try (ResultSet rs = meta.getTables(catalog, schema, "%", types)) {
            while (rs.next()) {
                String sc = rs.getString("TABLE_SCHEM");
                String name = rs.getString("TABLE_NAME");
                if (!isUserTable(sc, name, dbType)) {
                    continue;
                }
                String cat = rs.getString("TABLE_CAT");
                String key = (sc != null ? sc + "." : "") + name;
                out.putIfAbsent(key.toLowerCase(Locale.ROOT), new TableRef(cat, sc, name));
            }
        }
    }

    private static boolean isUserTable(String schema, String name, DatabaseType db) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        String s = schema == null ? "" : schema.toUpperCase(Locale.ROOT);
        String n = name.toUpperCase(Locale.ROOT);
        // Derby/DB2：仅按 schema 排除系统目录（如 SYS、SYSIBM），勿按表名前缀 SYS 过滤——否则会漏掉业务表
        // sys_user、sys_config 等（大写后为 SYS_*），导致备份与其它库表集不一致。
        if (db == DatabaseType.DERBY || db == DatabaseType.DB2) {
            if (s.startsWith("SYS")) {
                return false;
            }
        }
        if (db == DatabaseType.POSTGRESQL || db == DatabaseType.KINGBASE) {
            if ("INFORMATION_SCHEMA".equals(s) || "PG_CATALOG".equals(s) || "PG_TOAST".equals(s)) {
                return false;
            }
        }
        if (db == DatabaseType.H2 || db == DatabaseType.HSQLDB) {
            if ("INFORMATION_SCHEMA".equalsIgnoreCase(s)) {
                return false;
            }
        }
        if (db == DatabaseType.SQLSERVER) {
            if ("INFORMATION_SCHEMA".equalsIgnoreCase(s) || "SYS".equalsIgnoreCase(s)) {
                return false;
            }
        }
        if (db == DatabaseType.SQLITE) {
            if (n.startsWith("SQLITE_")) {
                return false;
            }
        }
        if (db == DatabaseType.ORACLE || db == DatabaseType.DAMENG || db == DatabaseType.OCEANBASE_ORACLE) {
            if (n.startsWith("BIN$")) {
                return false;
            }
        }
        return true;
    }

    private static String buildCreateTableFromMetadata(Connection conn, TableRef ref, DatabaseType dbType, RuntimeSqlDialect dialect) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        List<Col> cols = new ArrayList<>();
        try (ResultSet rs = meta.getColumns(ref.catalog, ref.schema, ref.name, null)) {
            while (rs.next()) {
                Col c = new Col();
                c.name = rs.getString("COLUMN_NAME");
                c.dataType = rs.getInt("DATA_TYPE");
                c.typeName = rs.getString("TYPE_NAME");
                c.size = rs.getInt("COLUMN_SIZE");
                c.decimals = rs.getInt("DECIMAL_DIGITS");
                c.nullable = rs.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls;
                c.def = rs.getString("COLUMN_DEF");
                c.ordinal = rs.getInt("ORDINAL_POSITION");
                c.autoInc = "YES".equalsIgnoreCase(rs.getString("IS_AUTOINCREMENT"));
                cols.add(c);
            }
        }
        cols.sort(Comparator.comparingInt(c -> c.ordinal));
        Map<Integer, String> pkBySeq = new TreeMap<>();
        Set<String> pkNamesLower = new HashSet<>();
        try (ResultSet pk = meta.getPrimaryKeys(ref.catalog, ref.schema, ref.name)) {
            while (pk.next()) {
                String cn = pk.getString("COLUMN_NAME");
                pkBySeq.put(pk.getInt("KEY_SEQ"), cn);
                if (cn != null) {
                    pkNamesLower.add(cn.toLowerCase(Locale.ROOT));
                }
            }
        }
        List<String> pkCols = new ArrayList<>(pkBySeq.values());
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(qualifiedQuotedTable(ref, dbType, dialect)).append(" (\n");
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) {
                sb.append(",\n");
            }
            Col c = cols.get(i);
            sb.append("  ").append(dialect.quote(c.name)).append(" ");
            sb.append(mapSqlTypeDeclaration(c, dbType));
            boolean inPk = c.name != null && pkNamesLower.contains(c.name.toLowerCase(Locale.ROOT));
            boolean pgNextvalPk = (dbType == DatabaseType.POSTGRESQL || dbType == DatabaseType.KINGBASE)
                    && inPk && isPostgresNextvalColumnDefault(c.def);
            boolean identityCol = c.autoInc || isIdentityLikeColumnDef(c.def) || pgNextvalPk;
            if (identityCol && (dbType == DatabaseType.POSTGRESQL || dbType == DatabaseType.KINGBASE)) {
                // 勿导出 DEFAULT nextval('…_seq')：备份内无 CREATE SEQUENCE，恢复会报 sequence does not exist
                sb.append(" NOT NULL GENERATED BY DEFAULT AS IDENTITY");
            } else if (identityCol && supportsGeneratedByDefaultIdentity(dbType)) {
                // Derby/DB2/H2 等：JDBC 的 COLUMN_DEF 常为 GENERATED…IDENTITY，不能写成 DEFAULT …，否则语法错误
                sb.append(" NOT NULL GENERATED BY DEFAULT AS IDENTITY");
            } else {
                if (!c.nullable) {
                    sb.append(" NOT NULL");
                }
                if (c.def != null && !c.def.trim().isEmpty() && !isIdentityLikeColumnDef(c.def)) {
                    if ((dbType == DatabaseType.POSTGRESQL || dbType == DatabaseType.KINGBASE) && isPostgresNextvalColumnDefault(c.def)) {
                        // 非主键上的 nextval 不写入（无序列定义）；数据由 INSERT 还原
                    } else {
                        sb.append(" DEFAULT ").append(c.def.trim());
                    }
                }
            }
        }
        if (!pkCols.isEmpty()) {
            sb.append(",\n  PRIMARY KEY (");
            for (int i = 0; i < pkCols.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(dialect.quote(pkCols.get(i)));
            }
            sb.append(")");
        }
        sb.append("\n)");
        return sb.toString();
    }

    private static boolean isIdentityLikeColumnDef(String def) {
        if (def == null) {
            return false;
        }
        String u = def.trim().toUpperCase(Locale.ROOT);
        return (u.contains("GENERATED") && u.contains("IDENTITY"))
                || u.contains("AUTOINCREMENT")
                || "IDENTITY".equals(u);
    }

    /** PostgreSQL SERIAL/BIGSERIAL 在元数据中常表现为 DEFAULT nextval('…_seq'::regclass)。 */
    private static boolean isPostgresNextvalColumnDefault(String def) {
        if (def == null) {
            return false;
        }
        return def.toLowerCase(Locale.ROOT).contains("nextval(");
    }

    /**
     * 与 JDBC 元数据里自增/标识列的常见 DDL 片段一致的数据库（勿用于 PostgreSQL SERIAL 等）。
     */
    private static boolean supportsGeneratedByDefaultIdentity(DatabaseType db) {
        return db == DatabaseType.DERBY || db == DatabaseType.DB2
                || db == DatabaseType.H2 || db == DatabaseType.HSQLDB;
    }

    private static String mapSqlTypeDeclaration(Col c, DatabaseType db) {
        String tn = c.typeName == null ? "" : c.typeName.toUpperCase(Locale.ROOT);
        switch (c.dataType) {
            case Types.BIT:
            case Types.BOOLEAN:
                return "BOOLEAN";
            case Types.TINYINT:
                return "SMALLINT";
            case Types.SMALLINT:
                return "SMALLINT";
            case Types.INTEGER:
                return "INTEGER";
            case Types.BIGINT:
                return "BIGINT";
            case Types.FLOAT:
            case Types.REAL:
                return "REAL";
            case Types.DOUBLE:
                // PostgreSQL 无独立 DOUBLE 类型，须使用 DOUBLE PRECISION
                if (db == DatabaseType.POSTGRESQL || db == DatabaseType.KINGBASE) {
                    return "DOUBLE PRECISION";
                }
                return "DOUBLE";
            case Types.NUMERIC:
            case Types.DECIMAL:
                if (c.size > 0 && c.decimals > 0) {
                    return "DECIMAL(" + c.size + "," + c.decimals + ")";
                }
                if (c.size > 0) {
                    return "DECIMAL(" + c.size + ")";
                }
                return "DECIMAL(38,0)";
            case Types.CHAR:
                return c.size > 0 ? "CHAR(" + c.size + ")" : "CHAR(1)";
            case Types.NCHAR:
                return c.size > 0 ? "NCHAR(" + c.size + ")" : "NCHAR(1)";
            case Types.VARCHAR:
            case Types.NVARCHAR:
            case Types.LONGVARCHAR:
            case Types.LONGNVARCHAR:
                if (tn.contains("UUID") && db == DatabaseType.POSTGRESQL) {
                    return "UUID";
                }
                if (db == DatabaseType.SQLITE) {
                    if (c.size > 0 && c.size < 1048576) {
                        return "VARCHAR(" + c.size + ")";
                    }
                    return "TEXT";
                }
                if (c.size > 0 && c.size < 1048576) {
                    return "VARCHAR(" + c.size + ")";
                }
                if (db == DatabaseType.POSTGRESQL || db == DatabaseType.KINGBASE) {
                    return "TEXT";
                }
                if (db == DatabaseType.DERBY) {
                    return "LONG VARCHAR";
                }
                return "VARCHAR(32672)";
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
                if (db == DatabaseType.POSTGRESQL || db == DatabaseType.KINGBASE) {
                    return "BYTEA";
                }
                if (db == DatabaseType.SQLSERVER) {
                    return c.size > 0 ? "VARBINARY(" + c.size + ")" : "VARBINARY(MAX)";
                }
                if (db == DatabaseType.ORACLE || db == DatabaseType.DAMENG || db == DatabaseType.OCEANBASE_ORACLE) {
                    return c.size > 0 ? "RAW(" + c.size + ")" : "BLOB";
                }
                if (db == DatabaseType.DERBY || db == DatabaseType.DB2) {
                    if (c.size > 0) {
                        return "VARCHAR(" + c.size + ") FOR BIT DATA";
                    }
                    return "BLOB";
                }
                return "BLOB";
            case Types.BLOB:
            case Types.CLOB:
            case Types.NCLOB:
                if (db == DatabaseType.POSTGRESQL || db == DatabaseType.KINGBASE) {
                    if (tn.contains("CLOB") || c.dataType == Types.CLOB || c.dataType == Types.NCLOB) {
                        return "TEXT";
                    }
                    return "BYTEA";
                }
                if (tn.contains("CLOB") || c.dataType == Types.CLOB || c.dataType == Types.NCLOB) {
                    return "CLOB";
                }
                return "BLOB";
            case Types.DATE:
                return "DATE";
            case Types.TIME:
            case Types.TIME_WITH_TIMEZONE:
                return "TIME";
            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                if (tn.contains("TIMEZONE") || c.dataType == Types.TIMESTAMP_WITH_TIMEZONE) {
                    return "TIMESTAMP WITH TIME ZONE";
                }
                return "TIMESTAMP";
            case Types.SQLXML:
                return "XML";
            default:
                if (db == DatabaseType.POSTGRESQL || db == DatabaseType.KINGBASE) {
                    String pg = mapPostgresTypeNameFallback(tn);
                    if (pg != null) {
                        return pg;
                    }
                }
                if (!tn.isEmpty()) {
                    return tn;
                }
                return "VARCHAR(255)";
        }
    }

    /**
     * JDBC TYPE_NAME 落入 default 时，将 PostgreSQL 常见名称映射为合法 DDL。
     */
    private static String mapPostgresTypeNameFallback(String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            return null;
        }
        String u = typeName.toUpperCase(Locale.ROOT);
        switch (u) {
            case "FLOAT8":
            case "_FLOAT8":
                return "DOUBLE PRECISION";
            case "FLOAT4":
            case "_FLOAT4":
                return "REAL";
            case "DOUBLE":
            case "DOUBLE PRECISION":
                return "DOUBLE PRECISION";
            case "INT2":
                return "SMALLINT";
            case "INT4":
                return "INTEGER";
            case "INT8":
                return "BIGINT";
            case "BOOL":
                return "BOOLEAN";
            default:
                return null;
        }
    }

    private static final class Col {
        String name;
        int dataType;
        String typeName;
        int size;
        int decimals;
        boolean nullable;
        String def;
        int ordinal;
        boolean autoInc;
    }

    private DatabaseBackupSqlExportSupport() {
    }
}

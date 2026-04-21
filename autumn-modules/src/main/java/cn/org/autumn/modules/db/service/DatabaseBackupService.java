package cn.org.autumn.modules.db.service;

import cn.org.autumn.database.DatabaseHolder;
import cn.org.autumn.database.DatabaseType;
import cn.org.autumn.base.ModuleService;
import cn.org.autumn.modules.db.dao.DatabaseBackupDao;
import cn.org.autumn.modules.db.entity.DatabaseBackupEntity;
import cn.org.autumn.modules.db.entity.DatabaseBackupStrategyEntity;
import cn.org.autumn.modules.db.entity.DatabaseBackupUploadEntity;
import cn.org.autumn.thread.TagRunnable;
import cn.org.autumn.thread.TagTaskExecutor;
import cn.org.autumn.thread.TagValue;
import cn.org.autumn.utils.PageUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 使用注入的 {@link DataSource}（通常为动态路由数据源）；连接与 {@link DatabaseHolder#getType()} 均受
 * {@link cn.org.autumn.datasources.DynamicDataSource} 线程 lookup key 影响，请在异步/定时任务中注意与切面一致的清理。
 */
@Slf4j
@Service
public class DatabaseBackupService extends ModuleService<DatabaseBackupDao, DatabaseBackupEntity> {

    private static final Pattern DERBY_DROP_IF_EXISTS = Pattern.compile("(?i)\\bIF\\s+EXISTS\\s+");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private DatabaseHolder databaseHolder;

    @Autowired
    private DatabaseBackupUploadService databaseBackupUploadService;

    @Autowired
    TagTaskExecutor asyncTaskExecutor;

    @Autowired
    private DatabaseBackupExecutionGuard databaseBackupExecutionGuard;

    @Value("${autumn.backup.dir:#{systemProperties['user.home'] + '/database'}}")
    private String backupDir;

    private volatile String databaseName;
    private volatile boolean initialized = false;

    /**
     * 运行中的备份任务: backupId -> BackupTask
     */
    private final ConcurrentHashMap<Long, BackupTask> runningTasks = new ConcurrentHashMap<>();

    /**
     * 懒初始化：首次使用时自动创建目录、获取数据库名、清理中断任务
     * 线程安全，仅执行一次
     */
    private synchronized void ensureInitialized() {
        if (initialized) return;
        try {
            Path path = Paths.get(backupDir, "backups");
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                if (log.isDebugEnabled())
                    log.debug("Backup directory created: {}", path.toAbsolutePath());
            }
            try (Connection conn = dataSource.getConnection()) {
                try {
                    databaseName = conn.getCatalog();
                } catch (SQLException e) {
                    databaseName = "default";
                }
                if (databaseName == null || databaseName.isEmpty()) {
                    databaseName = "default";
                }
                if (log.isDebugEnabled())
                    log.debug("Database backup service initialized, database: {}, backupDir: {}", databaseName, path.toAbsolutePath());
            }
            markInterruptedTasks();
            initialized = true;
        } catch (Exception e) {
            log.error("Failed to initialize backup directory: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void shutdown() {
        for (Map.Entry<Long, BackupTask> entry : runningTasks.entrySet()) {
            entry.getValue().stop();
        }
    }

    /**
     * 将之前进行中/暂停的任务标记为失败
     */
    private void markInterruptedTasks() {
        try {
            List<DatabaseBackupEntity> interrupted = list(new QueryWrapper<DatabaseBackupEntity>().in(columnInWrapper("status"), Arrays.asList(0, 3, 4)));
            for (DatabaseBackupEntity entity : interrupted) {
                entity.setStatus(2);
                entity.setError("应用重启导致任务中断");
                updateById(entity);
                if (log.isDebugEnabled())
                    log.debug("Marked interrupted backup task: id={}", entity.getId());
            }
        } catch (Exception e) {
            if (log.isDebugEnabled())
                log.debug("Failed to mark interrupted tasks: {}", e.getMessage());
        }
    }

    /**
     * 分页查询备份记录
     */
    public PageUtils queryPage(Map<String, Object> params) {
        return queryPage(params, columnInWrapper("id"));
    }

    // ========================================
    // 备份执行（支持异步、进度、暂停、停止）
    // ========================================

    /**
     * 执行备份（异步），返回初始记录
     *
     * @param remark     备注
     * @param mode       备份模式: FULL / TABLES
     * @param tableList  指定表(逗号分隔)，mode=TABLES 时有效
     * @param strategyId 关联策略ID，可为null
     */
    public DatabaseBackupEntity backupAsync(String remark, String mode, String tableList, Long strategyId) {
        ensureInitialized();
        databaseBackupExecutionGuard.assertBackupAllowed();
        DatabaseBackupEntity entity = new DatabaseBackupEntity();
        entity.setDatabase(databaseName);
        entity.setBackupDialect(databaseHolder.getType().name());
        entity.setRemark(remark);
        entity.setMode(mode != null ? mode : "FULL");
        entity.setBackupTables(tableList);
        entity.setStrategyId(strategyId);
        entity.setStatus(0); // 等待中
        entity.setProgress(0);
        entity.setCompletedTables(0);
        entity.setCreateTime(new Date());
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
        String filename = databaseName + "_" + timestamp + ".sql";
        entity.setFilename(filename);
        Path filePath = Paths.get(backupDir, "backups", filename);
        entity.setFilepath(filePath.toAbsolutePath().toString());
        // 先保存记录
        save(entity);
        // 提交异步执行
        BackupTask task = new BackupTask(entity.getId(), entity.getFilepath(), entity.getMode(), entity.getBackupTables());
        runningTasks.put(entity.getId(), task);
        asyncTaskExecutor.execute(new TagRunnable() {
            @Override
            @TagValue(method = "backupAsync", type = DatabaseBackupService.class, tag = "数据库备份")
            public void exe() {
                try {
                    executeBackupTask(task);
                } catch (Exception e) {
                    log.error("Backup task failed unexpectedly: id={}", task.backupId, e);
                } finally {
                    runningTasks.remove(task.backupId);
                }
            }
        });
        return entity;
    }

    /**
     * 兼容原有的同步备份方法（全量备份）
     */
    public DatabaseBackupEntity backup(String remark) {
        return backupAsync(remark, "FULL", null, null);
    }

    /**
     * 通过策略执行备份
     */
    public DatabaseBackupEntity backupByStrategy(DatabaseBackupStrategyEntity strategy) {
        return backupAsync("策略自动备份: " + strategy.getName(), strategy.getMode(), strategy.getTables(), strategy.getId());
    }

    /**
     * 实际执行备份任务
     */
    private void executeBackupTask(BackupTask task) {
        DatabaseBackupEntity entity = getById(task.backupId);
        if (entity == null) return;
        entity.setStatus(3); // 进行中
        updateById(entity);
        long startTime = System.currentTimeMillis();
        try {
            int[] result = exportDatabase(task);
            entity = getById(task.backupId);
            if (entity == null)
                return;
            if (task.isStopped()) {
                entity.setStatus(5); // 已停止
                entity.setError("用户手动停止");
            } else {
                entity.setTables(result[0]);
                entity.setRecords(result[1]);
                try {
                    entity.setFilesize(Files.size(Paths.get(task.outputPath)));
                } catch (IOException ignored) {
                }
                entity.setStatus(1); // 成功
                entity.setProgress(100);
            }
            entity.setDuration(System.currentTimeMillis() - startTime);
            entity.setCurrentTable(null);
            updateById(entity);
            if (entity.getStatus() == 1 && log.isDebugEnabled()) {
                log.debug("Backup completed: id={}, tables={}, records={}, duration={}ms", task.backupId, result[0], result[1], entity.getDuration());
            }
        } catch (Exception e) {
            entity = getById(task.backupId);
            if (entity != null) {
                entity.setStatus(2); // 失败
                entity.setError(e.getMessage());
                entity.setDuration(System.currentTimeMillis() - startTime);
                entity.setCurrentTable(null);
                updateById(entity);
            }
            log.error("Backup failed: id={}, error={}", task.backupId, e.getMessage(), e);
        }
    }

    /**
     * 暂停备份任务
     */
    public boolean pauseTask(Long backupId) {
        BackupTask task = runningTasks.get(backupId);
        if (task == null) return false;
        task.pause();
        DatabaseBackupEntity entity = getById(backupId);
        if (entity != null) {
            entity.setStatus(4); // 已暂停
            updateById(entity);
        }
        if (log.isDebugEnabled())
            log.debug("Backup task paused: id={}", backupId);
        return true;
    }

    /**
     * 恢复备份任务
     */
    public boolean resumeTask(Long backupId) {
        BackupTask task = runningTasks.get(backupId);
        if (task == null) return false;
        task.resume();
        DatabaseBackupEntity entity = getById(backupId);
        if (entity != null) {
            entity.setStatus(3); // 进行中
            updateById(entity);
        }
        if (log.isDebugEnabled())
            log.debug("Backup task resumed: id={}", backupId);
        return true;
    }

    /**
     * 停止备份任务
     */
    public boolean stopTask(Long backupId) {
        BackupTask task = runningTasks.get(backupId);
        if (task == null) return false;
        task.stop();
        if (log.isDebugEnabled())
            log.debug("Backup task stop requested: id={}", backupId);
        return true;
    }

    /**
     * 获取所有运行中的任务进度
     */
    public List<Map<String, Object>> getRunningTasks() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<Long, BackupTask> entry : runningTasks.entrySet()) {
            BackupTask task = entry.getValue();
            DatabaseBackupEntity entity = getById(entry.getKey());
            if (entity != null) {
                Map<String, Object> info = new HashMap<>();
                info.put("id", entity.getId());
                info.put("filename", entity.getFilename());
                info.put("status", entity.getStatus());
                if (task.getLiveTotalTables() > 0) {
                    info.put("progress", task.getLiveProgress());
                    info.put("totalTables", task.getLiveTotalTables());
                    info.put("completedTables", task.getLiveCompletedTables());
                    info.put("currentTable", task.getLiveCurrentTable());
                } else {
                    info.put("progress", entity.getProgress());
                    info.put("totalTables", entity.getTotalTables());
                    info.put("completedTables", entity.getCompletedTables());
                    info.put("currentTable", entity.getCurrentTable());
                }
                info.put("mode", entity.getMode());
                info.put("backupDialect", entity.getBackupDialect());
                info.put("paused", task.isPaused());
                info.put("createTime", entity.getCreateTime());
                list.add(info);
            }
        }
        return list;
    }

    /**
     * 获取单个任务进度
     */
    public Map<String, Object> getTaskProgress(Long backupId) {
        DatabaseBackupEntity entity = getById(backupId);
        if (entity == null) return null;
        Map<String, Object> info = new HashMap<>();
        info.put("id", entity.getId());
        info.put("filename", entity.getFilename());
        info.put("status", entity.getStatus());
        BackupTask task = runningTasks.get(backupId);
        if (task != null && task.getLiveTotalTables() > 0) {
            info.put("progress", task.getLiveProgress());
            info.put("totalTables", task.getLiveTotalTables());
            info.put("completedTables", task.getLiveCompletedTables());
            info.put("currentTable", task.getLiveCurrentTable());
        } else {
            info.put("progress", entity.getProgress());
            info.put("totalTables", entity.getTotalTables());
            info.put("completedTables", entity.getCompletedTables());
            info.put("currentTable", entity.getCurrentTable());
        }
        info.put("mode", entity.getMode());
        info.put("backupDialect", entity.getBackupDialect());
        info.put("paused", task != null && task.isPaused());
        info.put("running", task != null);
        return info;
    }

    // ========================================
    // 数据库导出核心逻辑
    // ========================================

    /**
     * 使用纯JDBC导出数据库
     */
    private int[] exportDatabase(BackupTask task) throws Exception {
        DatabaseType logicalType = databaseHolder.getType();
        try (Connection conn = dataSource.getConnection();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(task.outputPath), StandardCharsets.UTF_8))) {
            DatabaseType physicalType = DatabaseBackupSqlExportSupport.inferPhysicalJdbcType(conn);
            DatabaseType exportType = physicalType != null ? physicalType : logicalType;
            List<String> tablesToBackup = DatabaseBackupSqlExportSupport.resolveTableList(conn, exportType, task.mode, task.tableList);
            int tableCount = tablesToBackup.size();
            DatabaseBackupEntity entity = getById(task.backupId);
            if (entity != null) {
                entity.setTotalTables(tableCount);
                entity.setTables(tableCount);
                updateById(entity);
            }
            if (DatabaseBackupSqlExportSupport.usesShowCreateTable(logicalType, conn)) {
                return DatabaseBackupSqlExportSupport.exportMysqlFamily(conn, writer, task, tablesToBackup, logicalType, this::updateProgress);
            }
            return DatabaseBackupSqlExportSupport.exportJdbcMetadata(conn, writer, task, tablesToBackup, exportType, this::updateProgress);
        }
    }

    /**
     * 更新备份进度
     */
    private void updateProgress(Long backupId, int completed, int total, String currentTable) {
        BackupTask task = runningTasks.get(backupId);
        if (task != null) {
            task.recordLiveProgress(completed, total, currentTable);
        }
        DatabaseBackupEntity entity = getById(backupId);
        if (entity == null) return;
        entity.setCompletedTables(completed);
        entity.setTotalTables(total);
        entity.setCurrentTable(currentTable);
        entity.setProgress(total > 0 ? (int) ((completed * 100.0) / total) : 0);
        updateById(entity);
    }

    // ========================================
    // 布尔值兼容处理（备份/恢复兼容性）
    // ========================================

    /**
     * 判断 SQLException 是否为布尔字符串导致的整数列不兼容错误
     * MySQL Error 1366: Incorrect integer value: 'false'/'true' for column ...
     */
    private boolean isBooleanCompatError(SQLException e) {
        String msg = e.getMessage();
        if (msg == null) return false;
        return (e.getErrorCode() == 1366 || msg.contains("Incorrect integer value"))
                && (msg.contains("'false'") || msg.contains("'true'"));
    }

    /**
     * 检查 SQL 中是否包含布尔字面量字符串
     */
    private boolean containsBooleanLiterals(String sql) {
        return sql.contains("'false'") || sql.contains("'true'")
                || sql.contains("'FALSE'") || sql.contains("'TRUE'");
    }

    /**
     * 将 SQL 中 VALUES 子句里的 'false'/'true' 替换为 0/1
     * 仅对 INSERT ... VALUES 语句生效，安全地处理布尔兼容问题
     */
    private String fixBooleanLiterals(String sql) {
        // 只对 INSERT 语句做替换，避免误改 WHERE 条件中的合法字符串
        String upper = sql.toUpperCase();
        if (!upper.contains("INSERT") || !upper.contains("VALUES")) {
            return sql;
        }
        // 在 VALUES 部分进行替换
        int valuesIdx = upper.indexOf("VALUES");
        if (valuesIdx < 0) return sql;
        String prefix = sql.substring(0, valuesIdx);
        String valuesPart = sql.substring(valuesIdx);
        // 替换布尔字面量（大小写不敏感）
        valuesPart = valuesPart.replace("'false'", "0").replace("'FALSE'", "0").replace("'False'", "0");
        valuesPart = valuesPart.replace("'true'", "1").replace("'TRUE'", "1").replace("'True'", "1");
        return prefix + valuesPart;
    }

    // ========================================
    // Packet too large 兼容处理
    // ========================================

    /**
     * 判断是否为 Packet too large 错误
     * MySQL Error 1153 / ER_NET_PACKET_TOO_LARGE
     */
    private boolean isPacketTooLargeError(SQLException e) {
        String msg = e.getMessage();
        if (msg == null) return false;
        return e.getErrorCode() == 1153
                || msg.contains("Packet for query is too large")
                || msg.contains("max_allowed_packet");
    }

    /**
     * 判断 SQL 是否为可拆分的 INSERT...VALUES 多行语句
     */
    private boolean isSplittableInsert(String sql) {
        String upper = sql.trim().toUpperCase();
        if (!upper.startsWith("INSERT")) return false;
        int valuesIdx = upper.indexOf("VALUES");
        if (valuesIdx < 0) return false;
        // 检查 VALUES 后面是否有多组 (...),(...) 值
        String valuesPart = sql.substring(valuesIdx + 6).trim();
        // 至少应该有两个 '(' 才能拆分
        int firstParen = valuesPart.indexOf('(');
        if (firstParen < 0) return false;
        // 找到第一个完整的值组的结尾 ')' 后面是否有 ','
        int depth = 0;
        boolean inString = false;
        char prev = 0;
        for (int i = firstParen; i < valuesPart.length(); i++) {
            char c = valuesPart.charAt(i);
            if (inString) {
                if (c == '\'' && prev != '\\') inString = false;
            } else {
                if (c == '\'') inString = true;
                else if (c == '(') depth++;
                else if (c == ')') {
                    depth--;
                    if (depth == 0) {
                        // 找到第一组值的结尾，看后面是否有逗号（即更多组）
                        for (int j = i + 1; j < valuesPart.length(); j++) {
                            char nc = valuesPart.charAt(j);
                            if (nc == ',') return true;  // 多组值，可拆分
                            if (!Character.isWhitespace(nc)) break;
                        }
                        return false; // 只有一组值，无法拆分
                    }
                }
            }
            prev = c;
        }
        return false;
    }

    /**
     * 将大的 INSERT...VALUES (...),(...),... 拆分为多条小 INSERT 语句执行
     * 使用二分法递归拆分，确保每条子语句都在 max_allowed_packet 范围内
     */
    private int executeSplitInsert(Connection conn, String sql, RestoreTask task, long currentLine) {
        String upper = sql.trim().toUpperCase();
        int valuesIdx = upper.indexOf("VALUES");
        String insertPrefix = sql.substring(0, valuesIdx + 6).trim(); // "INSERT INTO ... VALUES"

        // 解析出所有值组
        String valuesPart = sql.substring(valuesIdx + 6).trim();
        List<String> valueGroups = parseValueGroups(valuesPart);

        if (valueGroups.size() <= 1) {
            // 只有一组值但仍然太大，说明单行数据就超限，无法进一步拆分
            task.addWarning("Line " + currentLine + ": 单行数据超过 max_allowed_packet 限制，无法执行");
            log.warn("Single row exceeds max_allowed_packet at line {}", currentLine);
            return 0;
        }

        // 二分拆分执行
        return executeSplitBatch(conn, insertPrefix, valueGroups, 0, valueGroups.size(), task, currentLine);
    }

    /**
     * 递归二分执行拆分后的 INSERT 批次
     */
    private int executeSplitBatch(Connection conn, String insertPrefix, List<String> groups, int from, int to,
                                  RestoreTask task, long currentLine) {
        if (from >= to) return 0;

        // 构建子 INSERT
        StringBuilder sb = new StringBuilder(insertPrefix);
        sb.append("\n");
        for (int i = from; i < to; i++) {
            if (i > from) sb.append(",\n");
            sb.append(groups.get(i));
        }
        String subSql = sb.toString();

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(subSql);
            return to - from; // 成功执行的行数
        } catch (SQLException e) {
            if (isPacketTooLargeError(e) && (to - from) > 1) {
                // 继续拆成两半
                int mid = from + (to - from) / 2;
                int left = executeSplitBatch(conn, insertPrefix, groups, from, mid, task, currentLine);
                int right = executeSplitBatch(conn, insertPrefix, groups, mid, to, task, currentLine);
                return left + right;
            } else if (isBooleanCompatError(e) && containsBooleanLiterals(subSql)) {
                // 处理布尔兼容
                String fixed = fixBooleanLiterals(subSql);
                try (Statement retryStmt = conn.createStatement()) {
                    retryStmt.execute(fixed);
                    return to - from;
                } catch (SQLException retryEx) {
                    task.addWarning("Line " + currentLine + " (split): " + retryEx.getMessage());
                    return 0;
                }
            } else {
                task.addWarning("Line " + currentLine + " (split): " + e.getMessage());
                return 0;
            }
        }
    }

    /**
     * 从 VALUES 部分解析出各个值组: (val1, val2, ...), (val1, val2, ...), ...
     * 正确处理字符串中的括号、逗号和转义引号
     */
    private List<String> parseValueGroups(String valuesPart) {
        List<String> groups = new ArrayList<>();
        int depth = 0;
        boolean inString = false;
        char prev = 0;
        int groupStart = -1;

        for (int i = 0; i < valuesPart.length(); i++) {
            char c = valuesPart.charAt(i);
            if (inString) {
                if (c == '\'' && prev != '\\') inString = false;
            } else {
                if (c == '\'') {
                    inString = true;
                } else if (c == '(') {
                    if (depth == 0) groupStart = i;
                    depth++;
                } else if (c == ')') {
                    depth--;
                    if (depth == 0 && groupStart >= 0) {
                        groups.add(valuesPart.substring(groupStart, i + 1));
                        groupStart = -1;
                    }
                }
            }
            prev = c;
        }
        return groups;
    }

    // ========================================
    // 文件 & 记录管理
    // ========================================

    /**
     * 获取备份文件
     */
    public File getBackupFile(Long id) {
        DatabaseBackupEntity entity = getById(id);
        if (entity == null || entity.getFilepath() == null) {
            return null;
        }
        File file = new File(entity.getFilepath());
        if (!file.exists()) {
            return null;
        }
        return file;
    }

    /**
     * 更新备注
     */
    public boolean updateRemark(Long id, String remark) {
        DatabaseBackupEntity entity = getById(id);
        if (entity == null) {
            return false;
        }
        entity.setRemark(remark);
        return updateById(entity);
    }

    /**
     * 删除备份（含文件）
     */
    public boolean deleteBackup(Long id) {
        // 先停止运行中的任务
        BackupTask task = runningTasks.get(id);
        if (task != null) {
            task.stop();
            runningTasks.remove(id);
        }
        DatabaseBackupEntity entity = getById(id);
        if (entity == null) {
            return false;
        }
        // 删除文件
        if (entity.getFilepath() != null) {
            try {
                Files.deleteIfExists(Paths.get(entity.getFilepath()));
            } catch (IOException e) {
                log.warn("Failed to delete backup file: {}", entity.getFilepath(), e);
            }
        }
        return removeById(id);
    }

    /**
     * 批量删除备份（含文件）
     */
    public boolean deleteBatch(Long[] ids) {
        for (Long id : ids) {
            deleteBackup(id);
        }
        return true;
    }

    /**
     * 获取备份统计信息
     */
    public Map<String, Object> getStatistics() {
        ensureInitialized();
        Map<String, Object> stats = new HashMap<>();
        stats.put("database", databaseName);
        stats.put("backupDir", Paths.get(backupDir, "backups").toAbsolutePath().toString());
        try {
            long total = count(new QueryWrapper<>());
            stats.put("total", total);
            long success = count(new QueryWrapper<DatabaseBackupEntity>().eq(columnInWrapper("status"), 1));
            stats.put("success", success);
            long failed = count(new QueryWrapper<DatabaseBackupEntity>().eq(columnInWrapper("status"), 2));
            stats.put("failed", failed);
            List<DatabaseBackupEntity> list = list(new QueryWrapper<DatabaseBackupEntity>().eq(columnInWrapper("status"), 1));
            long totalSize = 0;
            for (DatabaseBackupEntity e : list) {
                if (e.getFilesize() != null) {
                    totalSize += e.getFilesize();
                }
            }
            stats.put("totalSize", totalSize);
        } catch (Exception e) {
            // 恢复备份若 DROP 了业务库表（含 db_database_backup），统计查询会失败，避免整页不可用
            log.warn("Backup statistics unavailable (missing table or DB error): {}", e.getMessage());
            stats.put("total", 0);
            stats.put("success", 0);
            stats.put("failed", 0);
            stats.put("totalSize", 0L);
            stats.put("statsDegraded", true);
        }
        stats.put("running", runningTasks.size());
        return stats;
    }

    /**
     * 获取数据库所有表名（供前端选表用）
     */
    public List<String> getDatabaseTables() {
        try (Connection conn = dataSource.getConnection()) {
            return DatabaseBackupSqlExportSupport.listTableNames(conn, databaseHolder.getType());
        } catch (Exception e) {
            log.error("Failed to get database tables: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 切换备份永久存储状态
     */
    public boolean togglePermanent(Long id) {
        DatabaseBackupEntity entity = getById(id);
        if (entity == null) return false;
        boolean newVal = !Boolean.TRUE.equals(entity.getPermanent());
        entity.setPermanent(newVal);
        updateById(entity);
        if (log.isDebugEnabled())
            log.info("Backup permanent toggled: id={}, permanent={}", id, newVal);
        return true;
    }

    /**
     * 设置备份永久存储状态
     */
    public boolean setPermanent(Long id, boolean permanent) {
        DatabaseBackupEntity entity = getById(id);
        if (entity == null) return false;
        entity.setPermanent(permanent);
        updateById(entity);
        if (log.isDebugEnabled())
            log.info("Backup permanent set: id={}, permanent={}", id, permanent);
        return true;
    }

    /**
     * 滚动清理策略过期备份
     * 跳过标记为永久存储(permanent=true)的备份，只对非永久备份计数和清理
     */
    public void cleanupByStrategy(Long strategyId, int maxKeep) {
        if (maxKeep <= 0) return;
        // 查询该策略下所有成功的、非永久存储的备份，按创建时间降序
        List<DatabaseBackupEntity> backups = list(
                new QueryWrapper<DatabaseBackupEntity>()
                        .eq(columnInWrapper("strategy_id"), strategyId)
                        .eq(columnInWrapper("status"), 1)
                        .and(i -> i.eq(columnInWrapper("permanent"), false).or().isNull(columnInWrapper("permanent")))
                        .orderByDesc(columnInWrapper("create_time")));
        if (backups.size() > maxKeep) {
            for (int i = maxKeep; i < backups.size(); i++) {
                deleteBackup(backups.get(i).getId());
                if (log.isDebugEnabled())
                    log.info("Rolling cleanup old backup: strategyId={}, backupId={}, filename={}", strategyId, backups.get(i).getId(), backups.get(i).getFilename());
            }
        }
    }

    // ========================================
    // 数据库恢复逻辑
    // ========================================

    /**
     * 运行中的恢复任务: entityId -> Future
     */
    private final ConcurrentHashMap<String, RestoreTask> runningRestoreTasks = new ConcurrentHashMap<>();

    /**
     * 恢复线程结束后会从 {@link #runningRestoreTasks} 移除任务，若前端最后一次轮询发生在移除之后，
     * 会拿不到 executedStatements/completed；此处保留终态快照直至同一 taskKey 再次发起恢复。
     */
    private final ConcurrentHashMap<String, Map<String, Object>> finishedRestoreProgress = new ConcurrentHashMap<>();

    /**
     * 从已有备份恢复（异步）
     */
    public Map<String, Object> restoreFromBackup(Long backupId) {
        DatabaseBackupEntity entity = getById(backupId);
        if (entity == null) {
            throw new RuntimeException("备份记录不存在");
        }
        if (entity.getStatus() != 1) {
            throw new RuntimeException("只能从成功的备份中恢复");
        }
        File file = new File(entity.getFilepath());
        if (!file.exists()) {
            throw new RuntimeException("备份文件不存在: " + entity.getFilepath());
        }
        String taskKey = "backup_" + backupId;
        if (runningRestoreTasks.containsKey(taskKey)) {
            throw new RuntimeException("该备份正在恢复中，请勿重复操作");
        }
        RestoreTask task = new RestoreTask(taskKey, file.getAbsolutePath());
        finishedRestoreProgress.remove(taskKey);
        runningRestoreTasks.put(taskKey, task);
        asyncTaskExecutor.execute(new TagRunnable("restore-backup-" + backupId) {
            @Override
            public void exe() {
                try {
                    executeRestore(task, null, backupId);
                } catch (Exception e) {
                    log.error("Restore from backup failed: backupId={}", backupId, e);
                } finally {
                    finishedRestoreProgress.put(taskKey, snapshotRestoreProgress(task));
                    runningRestoreTasks.remove(taskKey);
                }
            }
        });
        Map<String, Object> result = new HashMap<>();
        result.put("taskKey", taskKey);
        result.put("sourceType", "backup");
        result.put("sourceId", backupId);
        return result;
    }

    /**
     * 从上传的SQL文件恢复（异步）
     */
    public Map<String, Object> restoreFromUpload(Long uploadId) {
        DatabaseBackupUploadEntity entity = databaseBackupUploadService.getById(uploadId);
        if (entity == null) {
            throw new RuntimeException("上传记录不存在");
        }
        if (entity.getStatus() != null && entity.getStatus() == 1) {
            throw new RuntimeException("该文件正在恢复中，请勿重复操作");
        }
        File file = new File(entity.getFilepath());
        if (!file.exists()) {
            throw new RuntimeException("上传文件不存在: " + entity.getFilepath());
        }
        String taskKey = "upload_" + uploadId;
        if (runningRestoreTasks.containsKey(taskKey)) {
            throw new RuntimeException("该文件正在恢复中，请勿重复操作");
        }
        // 更新上传记录状态为恢复中
        entity.setStatus(1);
        entity.setError(null);
        databaseBackupUploadService.updateById(entity);
        RestoreTask task = new RestoreTask(taskKey, file.getAbsolutePath());
        finishedRestoreProgress.remove(taskKey);
        runningRestoreTasks.put(taskKey, task);
        asyncTaskExecutor.execute(new TagRunnable("restore-upload-" + uploadId) {
            @Override
            public void exe() {
                try {
                    executeRestore(task, uploadId, null);
                } catch (Exception e) {
                    log.error("Restore from upload failed: uploadId={}", uploadId, e);
                } finally {
                    finishedRestoreProgress.put(taskKey, snapshotRestoreProgress(task));
                    runningRestoreTasks.remove(taskKey);
                }
            }
        });
        Map<String, Object> result = new HashMap<>();
        result.put("taskKey", taskKey);
        result.put("sourceType", "upload");
        result.put("sourceId", uploadId);
        return result;
    }

    /**
     * 执行SQL恢复
     */
    private void executeRestore(RestoreTask task, Long uploadId, Long backupId) {
        long startTime = System.currentTimeMillis();
        try {
            if (backupId != null) {
                DatabaseBackupEntity src = selectById(backupId);
                if (src != null && src.getBackupDialect() != null
                        && !src.getBackupDialect().equals(databaseHolder.getType().name())) {
                    task.addWarning("备份方言为 " + src.getBackupDialect() + "，当前数据源为 " + databaseHolder.getType().name()
                            + "，建议在相同方言库上恢复以避免 SQL 不兼容");
                }
            }
            executeSqlFile(task);
            long duration = System.currentTimeMillis() - startTime;
            task.setCompleted(true);
            // 更新上传记录状态
            if (uploadId != null) {
                DatabaseBackupUploadEntity uploadEntity = databaseBackupUploadService.getById(uploadId);
                if (uploadEntity != null) {
                    uploadEntity.setStatus(2); // 恢复成功
                    uploadEntity.setRestoreDuration(duration);
                    uploadEntity.setRestoreTime(new Date());
                    uploadEntity.setError(null);
                    databaseBackupUploadService.updateById(uploadEntity);
                }
            }
            log.info("Database restore completed: taskKey={}, duration={}ms, statements={}", task.taskKey, duration, task.getExecutedStatements());
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            task.setError(e.getMessage());
            // 更新上传记录状态
            if (uploadId != null) {
                DatabaseBackupUploadEntity uploadEntity = databaseBackupUploadService.getById(uploadId);
                if (uploadEntity != null) {
                    uploadEntity.setStatus(3); // 恢复失败
                    uploadEntity.setRestoreDuration(duration);
                    uploadEntity.setRestoreTime(new Date());
                    uploadEntity.setError(e.getMessage());
                    databaseBackupUploadService.updateById(uploadEntity);
                }
            }
            log.error("Database restore failed: taskKey={}, error={}", task.taskKey, e.getMessage(), e);
        }
    }

    /**
     * 执行SQL文件
     */
    private void executeSqlFile(RestoreTask task) throws Exception {
        DatabaseType restoreDbType = databaseHolder.getType();
        boolean autocommitPerStatement = restoreUsesAutocommitPerStatement(restoreDbType);
        try (Connection conn = dataSource.getConnection();
             BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(task.filePath), StandardCharsets.UTF_8))) {
            conn.setAutoCommit(autocommitPerStatement);
            StringBuilder statement = new StringBuilder();
            String line;
            int executedCount = 0;
            boolean inMultiLineComment = false;
            // 先统计总行数用于进度
            long totalLines = 0;
            try (BufferedReader countReader = new BufferedReader(new InputStreamReader(new FileInputStream(task.filePath), StandardCharsets.UTF_8))) {
                while (countReader.readLine() != null) totalLines++;
            }
            task.setTotalLines(totalLines);
            long currentLine = 0;
            while ((line = reader.readLine()) != null) {
                currentLine++;
                task.setCurrentLine(currentLine);
                // 跳过空行
                String trimmedLine = line.trim();
                if (trimmedLine.isEmpty()) continue;
                // 处理多行注释
                if (inMultiLineComment) {
                    if (trimmedLine.contains("*/")) {
                        inMultiLineComment = false;
                    }
                    continue;
                }
                if (trimmedLine.startsWith("/*")) {
                    if (!trimmedLine.contains("*/")) {
                        inMultiLineComment = true;
                    }
                    continue;
                }
                // 跳过单行注释
                if (trimmedLine.startsWith("--") || trimmedLine.startsWith("#")) {
                    continue;
                }
                statement.append(line);
                // 检查语句是否结束（以分号结尾）
                if (trimmedLine.endsWith(";")) {
                    String sql = statement.toString().trim();
                    // 去掉末尾分号
                    if (sql.endsWith(";")) {
                        sql = sql.substring(0, sql.length() - 1).trim();
                    }
                    if (!sql.isEmpty()) {
                        executedCount += executeWithCompatFix(conn, sql, task, currentLine);
                        task.setExecutedStatements(executedCount);
                    }
                    statement.setLength(0);
                    // PostgreSQL 等: 事务内任一条失败会中止整个事务；按语句自动提交，单条失败不影响后续
                    if (!autocommitPerStatement && executedCount % 100 == 0) {
                        conn.commit();
                    }
                } else {
                    statement.append("\n");
                }
            }
            // 处理最后一条没有分号的语句
            String lastSql = statement.toString().trim();
            if (!lastSql.isEmpty()) {
                executedCount += executeWithCompatFix(conn, lastSql, task, currentLine);
                task.setExecutedStatements(executedCount);
            }
            if (!autocommitPerStatement) {
                conn.commit();
            }
            conn.setAutoCommit(true);
            log.info("SQL file executed: file={}, statements={}, autocommitPerStatement={}", task.filePath, executedCount, autocommitPerStatement);
        }
    }

    /**
     * 恢复时是否按语句独立提交。PostgreSQL/Kingbase 在事务失败后会拒绝同事务内后续语句，需避免长事务+忽略错误混用。
     */
    private static boolean restoreUsesAutocommitPerStatement(DatabaseType t) {
        return t == DatabaseType.POSTGRESQL || t == DatabaseType.KINGBASE;
    }

    /**
     * 统一的 SQL 执行方法，自动处理：
     * 1. Packet too large -> 拆分 INSERT 重试
     * 2. Boolean 兼容 -> 替换 'false'/'true' 重试
     *
     * @return 成功执行的语句数（1=成功, 0=失败/跳过, >1=拆分执行）
     */
    private int executeWithCompatFix(Connection conn, String sql, RestoreTask task, long currentLine) {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            return 1;
        } catch (SQLException e) {
            // Derby：DROP TABLE（无 IF EXISTS）在表不存在时期望忽略；含 IF EXISTS 的旧备份先去掉子句再执行
            if (databaseHolder.getType() == DatabaseType.DERBY && isDropTableStatement(sql)) {
                if (isDerbyDropTargetMissing(e)) {
                    return 1;
                }
                if (tryDerbyDropWithoutIfExists(conn, sql, currentLine)) {
                    return 1;
                }
            }
            // 1. Packet too large -> 拆分 INSERT 重试
            if (isPacketTooLargeError(e) && isSplittableInsert(sql)) {
                if (log.isDebugEnabled())
                    log.debug("Packet too large at line {}, splitting INSERT into smaller batches", currentLine);
                int count = executeSplitInsert(conn, sql, task, currentLine);
                if (count > 0) return count;
                // 拆分也全部失败了，记录警告
                task.addWarning("Line " + currentLine + ": " + e.getMessage());
                return 0;
            }
            // 2. Boolean 兼容问题 -> 替换后重试
            if (isBooleanCompatError(e) && containsBooleanLiterals(sql)) {
                String fixedSql = fixBooleanLiterals(sql);
                try (Statement retryStmt = conn.createStatement()) {
                    retryStmt.execute(fixedSql);
                    if (log.isDebugEnabled())
                        log.debug("SQL fixed boolean literals and retried at line {}", currentLine);
                    return 1;
                } catch (SQLException retryEx) {
                    // 修复后仍然太大？尝试拆分
                    if (isPacketTooLargeError(retryEx) && isSplittableInsert(fixedSql)) {
                        int count = executeSplitInsert(conn, fixedSql, task, currentLine);
                        if (count > 0) return count;
                    }
                    log.warn("SQL execution warning at line {} (after compat fix): {}", currentLine, retryEx.getMessage());
                    task.addWarning("Line " + currentLine + ": " + retryEx.getMessage());
                    return 0;
                }
            }
            // 3. 其他错误
            log.warn("SQL execution warning at line {}: {}", currentLine, e.getMessage());
            task.addWarning("Line " + currentLine + ": " + e.getMessage());
            return 0;
        }
    }

    private static boolean isDropTableStatement(String sql) {
        if (sql == null) {
            return false;
        }
        String t = sql.trim();
        return t.regionMatches(true, 0, "DROP TABLE", 0, "DROP TABLE".length());
    }

    private static boolean isDerbyDropTargetMissing(SQLException e) {
        if (e == null) {
            return false;
        }
        if ("42X05".equals(e.getSQLState())) {
            return true;
        }
        String m = e.getMessage();
        if (m == null) {
            return false;
        }
        String u = m.toUpperCase(Locale.ROOT);
        return u.contains("DOES NOT EXIST") || u.contains("NOT FOUND");
    }

    /**
     * Derby 对 {@code DROP TABLE IF EXISTS} 常报语法错；去掉 IF EXISTS 后执行，表已不存在则视为成功。
     */
    private boolean tryDerbyDropWithoutIfExists(Connection conn, String sql, long currentLine) {
        if (databaseHolder.getType() != DatabaseType.DERBY) {
            return false;
        }
        String trimmed = sql.trim();
        if (!trimmed.toUpperCase(Locale.ROOT).contains("IF EXISTS")) {
            return false;
        }
        String simplified = DERBY_DROP_IF_EXISTS.matcher(trimmed).replaceFirst("");
        if (simplified.equals(trimmed)) {
            return false;
        }
        try (Statement st = conn.createStatement()) {
            st.execute(simplified);
            if (log.isDebugEnabled()) {
                log.debug("Derby restore: DROP retried without IF EXISTS at line {}", currentLine);
            }
            return true;
        } catch (SQLException e2) {
            return isDerbyDropTargetMissing(e2);
        }
    }

    /**
     * 获取恢复任务进度
     */
    public Map<String, Object> getRestoreProgress(String taskKey) {
        RestoreTask task = runningRestoreTasks.get(taskKey);
        if (task != null) {
            return buildRestoreProgressMap(task, true);
        }
        Map<String, Object> done = finishedRestoreProgress.get(taskKey);
        if (done != null) {
            return new HashMap<>(done);
        }
        return Collections.singletonMap("running", false);
    }

    private static Map<String, Object> buildRestoreProgressMap(RestoreTask task, boolean running) {
        Map<String, Object> info = new HashMap<>();
        info.put("running", running);
        info.put("taskKey", task.taskKey);
        info.put("totalLines", task.getTotalLines());
        info.put("currentLine", task.getCurrentLine());
        info.put("executedStatements", task.getExecutedStatements());
        info.put("completed", task.isCompleted());
        info.put("error", task.getError());
        info.put("warnings", running ? task.getWarnings() : new ArrayList<>(task.getWarnings()));
        int pct;
        if (task.isCompleted() && task.getError() == null) {
            pct = 100;
        } else if (task.getTotalLines() > 0) {
            pct = (int) ((task.getCurrentLine() * 100.0) / task.getTotalLines());
        } else {
            pct = 0;
        }
        info.put("progress", pct);
        return info;
    }

    /** 终态快照（供 {@link #finishedRestoreProgress}），与 {@link #buildRestoreProgressMap} 一致但固定 running=false。 */
    private static Map<String, Object> snapshotRestoreProgress(RestoreTask task) {
        return buildRestoreProgressMap(task, false);
    }

    /**
     * 获取所有运行中的恢复任务
     */
    public List<Map<String, Object>> getRunningRestoreTasks() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, RestoreTask> entry : runningRestoreTasks.entrySet()) {
            list.add(getRestoreProgress(entry.getKey()));
        }
        return list;
    }

    // ========================================
    // 内部类：恢复任务控制
    // ========================================

    /**
     * 恢复任务状态
     */
    static class RestoreTask {
        final String taskKey;
        final String filePath;
        private volatile long totalLines = 0;
        private volatile long currentLine = 0;
        private volatile int executedStatements = 0;
        private volatile boolean completed = false;
        private volatile String error = null;
        private final List<String> warnings = Collections.synchronizedList(new ArrayList<>());

        RestoreTask(String taskKey, String filePath) {
            this.taskKey = taskKey;
            this.filePath = filePath;
        }

        long getTotalLines() {
            return totalLines;
        }

        void setTotalLines(long totalLines) {
            this.totalLines = totalLines;
        }

        long getCurrentLine() {
            return currentLine;
        }

        void setCurrentLine(long currentLine) {
            this.currentLine = currentLine;
        }

        int getExecutedStatements() {
            return executedStatements;
        }

        void setExecutedStatements(int executedStatements) {
            this.executedStatements = executedStatements;
        }

        boolean isCompleted() {
            return completed;
        }

        void setCompleted(boolean completed) {
            this.completed = completed;
        }

        String getError() {
            return error;
        }

        void setError(String error) {
            this.error = error;
        }

        List<String> getWarnings() {
            return new ArrayList<>(warnings.size() > 50 ? warnings.subList(warnings.size() - 50, warnings.size()) : warnings);
        }

        void addWarning(String warning) {
            if (warnings.size() < 200) warnings.add(warning);
        }
    }

    // ========================================
    // 内部类：备份任务控制
    // ========================================

    /**
     * 备份任务状态控制
     */
    static class BackupTask implements DatabaseBackupSqlExportSupport.BackupExportTaskHandle {
        final Long backupId;
        final String outputPath;
        final String mode;
        final String tableList;
        private volatile boolean stopped = false;
        private volatile boolean paused = false;
        private final Object pauseLock = new Object();
        /** 与导出线程同步的进度快照，供轮询接口读取，避免依赖实体缓存或读库延迟 */
        private volatile int liveProgress = 0;
        private volatile int liveCompletedTables = 0;
        private volatile int liveTotalTables = 0;
        private volatile String liveCurrentTable = null;

        BackupTask(Long backupId, String outputPath, String mode, String tableList) {
            this.backupId = backupId;
            this.outputPath = outputPath;
            this.mode = mode;
            this.tableList = tableList;
        }

        void recordLiveProgress(int completed, int total, String currentTable) {
            this.liveCompletedTables = completed;
            this.liveTotalTables = total;
            this.liveCurrentTable = currentTable;
            this.liveProgress = total > 0 ? (int) ((completed * 100.0) / total) : 0;
        }

        int getLiveProgress() {
            return liveProgress;
        }

        int getLiveCompletedTables() {
            return liveCompletedTables;
        }

        int getLiveTotalTables() {
            return liveTotalTables;
        }

        String getLiveCurrentTable() {
            return liveCurrentTable;
        }

        void pause() {
            this.paused = true;
        }

        void resume() {
            synchronized (pauseLock) {
                this.paused = false;
                pauseLock.notifyAll();
            }
        }

        void stop() {
            this.stopped = true;
            // 如果暂停中，先恢复让线程能退出
            resume();
        }

        boolean isPaused() {
            return paused;
        }

        public boolean isStopped() {
            return stopped;
        }

        /**
         * 如果暂停则等待恢复
         */
        public void waitIfPaused() {
            synchronized (pauseLock) {
                while (paused && !stopped) {
                    try {
                        pauseLock.wait(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        @Override
        public Long getBackupId() {
            return backupId;
        }

        @Override
        public String getMode() {
            return mode;
        }

        @Override
        public String getTableList() {
            return tableList;
        }
    }
}

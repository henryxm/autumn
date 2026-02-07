package cn.org.autumn.modules.db.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.modules.db.dao.DatabaseBackupDao;
import cn.org.autumn.modules.db.entity.DatabaseBackupEntity;
import cn.org.autumn.modules.db.entity.DatabaseBackupStrategyEntity;
import cn.org.autumn.utils.PageUtils;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class DatabaseBackupService extends ModuleService<DatabaseBackupDao, DatabaseBackupEntity> {

    @Autowired
    private DataSource dataSource;

    @Value("${autumn.backup.dir:backups}")
    private String backupDir;

    private String databaseName;

    /**
     * 备份任务执行线程池，支持并行备份
     */
    private final ExecutorService backupExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "backup-worker-" + new AtomicInteger().incrementAndGet());
        t.setDaemon(true);
        return t;
    });

    /**
     * 运行中的备份任务: backupId -> BackupTask
     */
    private final ConcurrentHashMap<Long, BackupTask> runningTasks = new ConcurrentHashMap<>();

    @PostConstruct
    public void initBackupDir() {
        try {
            Path path = Paths.get(backupDir);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                if (log.isDebugEnabled())
                    log.debug("Backup directory created: {}", path.toAbsolutePath());
            }
            // 获取数据库名称
            try (Connection conn = dataSource.getConnection()) {
                databaseName = conn.getCatalog();
                if (log.isDebugEnabled())
                    log.debug("Database backup service initialized, database: {}, backupDir: {}", databaseName, path.toAbsolutePath());
            }
            // 将之前未完成的任务标记为失败（应用重启场景）
            markInterruptedTasks();
        } catch (Exception e) {
            log.error("Failed to initialize backup directory: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void shutdown() {
        // 停止所有运行中的任务
        for (Map.Entry<Long, BackupTask> entry : runningTasks.entrySet()) {
            entry.getValue().stop();
        }
        backupExecutor.shutdownNow();
    }

    /**
     * 将之前进行中/暂停的任务标记为失败
     */
    private void markInterruptedTasks() {
        try {
            List<DatabaseBackupEntity> interrupted = selectList(new EntityWrapper<DatabaseBackupEntity>().in("status", Arrays.asList(0, 3, 4)));
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
        return queryPage(params, "id");
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
        DatabaseBackupEntity entity = new DatabaseBackupEntity();
        entity.setDatabase(databaseName);
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
        Path filePath = Paths.get(backupDir, filename);
        entity.setFilepath(filePath.toAbsolutePath().toString());
        // 先保存记录
        insert(entity);
        // 提交异步执行
        BackupTask task = new BackupTask(entity.getId(), entity.getFilepath(), entity.getMode(), entity.getBackupTables());
        runningTasks.put(entity.getId(), task);
        backupExecutor.submit(() -> {
            try {
                executeBackupTask(task);
            } catch (Exception e) {
                log.error("Backup task failed unexpectedly: id={}", task.backupId, e);
            } finally {
                runningTasks.remove(task.backupId);
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
        DatabaseBackupEntity entity = selectById(task.backupId);
        if (entity == null) return;
        entity.setStatus(3); // 进行中
        updateById(entity);
        long startTime = System.currentTimeMillis();
        try {
            int[] result = exportDatabase(task);
            entity = selectById(task.backupId);
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
            entity = selectById(task.backupId);
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
        DatabaseBackupEntity entity = selectById(backupId);
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
        DatabaseBackupEntity entity = selectById(backupId);
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
            DatabaseBackupEntity entity = selectById(entry.getKey());
            if (entity != null) {
                Map<String, Object> info = new HashMap<>();
                info.put("id", entity.getId());
                info.put("filename", entity.getFilename());
                info.put("status", entity.getStatus());
                info.put("progress", entity.getProgress());
                info.put("totalTables", entity.getTotalTables());
                info.put("completedTables", entity.getCompletedTables());
                info.put("currentTable", entity.getCurrentTable());
                info.put("mode", entity.getMode());
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
        DatabaseBackupEntity entity = selectById(backupId);
        if (entity == null) return null;
        Map<String, Object> info = new HashMap<>();
        info.put("id", entity.getId());
        info.put("filename", entity.getFilename());
        info.put("status", entity.getStatus());
        info.put("progress", entity.getProgress());
        info.put("totalTables", entity.getTotalTables());
        info.put("completedTables", entity.getCompletedTables());
        info.put("currentTable", entity.getCurrentTable());
        info.put("mode", entity.getMode());
        BackupTask task = runningTasks.get(backupId);
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
        int tableCount = 0;
        int totalRecords = 0;
        try (Connection conn = dataSource.getConnection(); BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(task.outputPath), StandardCharsets.UTF_8))) {
            String dbName = conn.getCatalog();
            // 确定要备份的表
            List<String> tablesToBackup;
            if ("TABLES".equals(task.mode) && task.tableList != null && !task.tableList.isEmpty()) {
                tablesToBackup = new ArrayList<>();
                for (String t : task.tableList.split(",")) {
                    String trimmed = t.trim();
                    if (!trimmed.isEmpty()) {
                        tablesToBackup.add(trimmed);
                    }
                }
            } else {
                tablesToBackup = getAllTables(conn);
            }
            tableCount = tablesToBackup.size();
            // 更新总表数
            DatabaseBackupEntity entity = selectById(task.backupId);
            if (entity != null) {
                entity.setTotalTables(tableCount);
                entity.setTables(tableCount);
                updateById(entity);
            }
            // 写入文件头
            writer.write("-- ========================================\n");
            writer.write("-- Database Backup\n");
            writer.write("-- Database: " + dbName + "\n");
            writer.write("-- Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n");
            writer.write("-- Mode: " + task.mode + "\n");
            writer.write("-- Tables: " + tableCount + "\n");
            writer.write("-- ========================================\n\n");
            writer.write("SET NAMES utf8mb4;\n");
            writer.write("SET FOREIGN_KEY_CHECKS = 0;\n\n");
            int completedCount = 0;
            for (String table : tablesToBackup) {
                // 检查是否被停止
                if (task.isStopped()) {
                    log.info("Backup task stopped during export: id={}, completedTables={}/{}", task.backupId, completedCount, tableCount);
                    break;
                }
                // 检查暂停，等待恢复
                task.waitIfPaused();
                // 再次检查停止（暂停恢复后可能被停止了）
                if (task.isStopped()) break;
                // 更新当前表进度
                updateProgress(task.backupId, completedCount, tableCount, table);
                writer.write("-- ----------------------------\n");
                writer.write("-- Table structure for " + table + "\n");
                writer.write("-- ----------------------------\n");
                writer.write("DROP TABLE IF EXISTS `" + table + "`;\n");
                // 导出建表语句
                String createTableSql = getCreateTableSql(conn, table);
                writer.write(createTableSql + ";\n\n");
                // 导出表数据
                int records = exportTableData(conn, writer, table, task);
                totalRecords += records;
                writer.write("\n");
                completedCount++;
            }
            if (!task.isStopped()) {
                writer.write("SET FOREIGN_KEY_CHECKS = 1;\n");
            }
            writer.flush();
            // 最终更新进度
            if (!task.isStopped()) {
                updateProgress(task.backupId, tableCount, tableCount, null);
            }
        }
        return new int[]{tableCount, totalRecords};
    }

    /**
     * 更新备份进度
     */
    private void updateProgress(Long backupId, int completed, int total, String currentTable) {
        DatabaseBackupEntity entity = selectById(backupId);
        if (entity == null) return;
        entity.setCompletedTables(completed);
        entity.setTotalTables(total);
        entity.setCurrentTable(currentTable);
        entity.setProgress(total > 0 ? (int) ((completed * 100.0) / total) : 0);
        updateById(entity);
    }

    /**
     * 获取所有表名
     */
    private List<String> getAllTables(Connection conn) throws SQLException {
        List<String> tables = new ArrayList<>();
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }
        Collections.sort(tables);
        return tables;
    }

    /**
     * 获取建表语句
     */
    private String getCreateTableSql(Connection conn, String table) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE `" + table + "`")) {
            if (rs.next()) {
                return rs.getString(2);
            }
        }
        return "";
    }

    /**
     * 导出表数据
     */
    private int exportTableData(Connection conn, BufferedWriter writer, String table, BackupTask task) throws Exception {
        int count = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM `" + table + "`")) {
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            if (!rs.isBeforeFirst()) {
                writer.write("-- No data for table `" + table + "`\n");
                return 0;
            }
            writer.write("-- ----------------------------\n");
            writer.write("-- Records of " + table + "\n");
            writer.write("-- ----------------------------\n");
            // 构建列名
            StringBuilder columns = new StringBuilder();
            for (int i = 1; i <= columnCount; i++) {
                if (i > 1) columns.append(", ");
                columns.append("`").append(meta.getColumnName(i)).append("`");
            }
            // 批量INSERT，每100条一批
            int batchSize = 100;
            int batchCount = 0;
            StringBuilder values = new StringBuilder();
            while (rs.next()) {
                // 检查暂停/停止
                if (task.isStopped()) return count;
                task.waitIfPaused();
                if (task.isStopped()) return count;
                if (batchCount == 0) {
                    values = new StringBuilder();
                    values.append("INSERT INTO `").append(table).append("` (")
                            .append(columns).append(") VALUES\n");
                } else {
                    values.append(",\n");
                }
                values.append("(");
                for (int i = 1; i <= columnCount; i++) {
                    if (i > 1) values.append(", ");
                    Object val = rs.getObject(i);
                    if (val == null) {
                        values.append("NULL");
                    } else if (val instanceof Number) {
                        values.append(val);
                    } else if (val instanceof byte[]) {
                        values.append("X'").append(bytesToHex((byte[]) val)).append("'");
                    } else {
                        values.append("'").append(escapeSQL(val.toString())).append("'");
                    }
                }
                values.append(")");
                count++;
                batchCount++;

                if (batchCount >= batchSize) {
                    values.append(";\n");
                    writer.write(values.toString());
                    batchCount = 0;
                }
            }
            // 写入剩余数据
            if (batchCount > 0) {
                values.append(";\n");
                writer.write(values.toString());
            }
        }
        return count;
    }

    /**
     * 转义SQL字符串
     */
    private String escapeSQL(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                .replace("\0", "\\0");
    }

    /**
     * 字节数组转十六进制
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    // ========================================
    // 文件 & 记录管理
    // ========================================

    /**
     * 获取备份文件
     */
    public File getBackupFile(Long id) {
        DatabaseBackupEntity entity = selectById(id);
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
        DatabaseBackupEntity entity = selectById(id);
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
        DatabaseBackupEntity entity = selectById(id);
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
        return deleteById(id);
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
        Map<String, Object> stats = new HashMap<>();
        int total = selectCount(new EntityWrapper<>());
        stats.put("total", total);
        int success = selectCount(new EntityWrapper<DatabaseBackupEntity>().eq("status", 1));
        stats.put("success", success);
        int failed = selectCount(new EntityWrapper<DatabaseBackupEntity>().eq("status", 2));
        stats.put("failed", failed);
        int running = runningTasks.size();
        stats.put("running", running);
        List<DatabaseBackupEntity> list = selectList(new EntityWrapper<DatabaseBackupEntity>().eq("status", 1));
        long totalSize = 0;
        for (DatabaseBackupEntity e : list) {
            if (e.getFilesize() != null) {
                totalSize += e.getFilesize();
            }
        }
        stats.put("totalSize", totalSize);
        stats.put("database", databaseName);
        stats.put("backupDir", Paths.get(backupDir).toAbsolutePath().toString());
        return stats;
    }

    /**
     * 获取数据库所有表名（供前端选表用）
     */
    public List<String> getDatabaseTables() {
        try (Connection conn = dataSource.getConnection()) {
            return getAllTables(conn);
        } catch (Exception e) {
            log.error("Failed to get database tables: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 切换备份永久存储状态
     */
    public boolean togglePermanent(Long id) {
        DatabaseBackupEntity entity = selectById(id);
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
        DatabaseBackupEntity entity = selectById(id);
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
        List<DatabaseBackupEntity> backups = selectList(
                new EntityWrapper<DatabaseBackupEntity>()
                        .eq("strategy_id", strategyId)
                        .eq("status", 1)
                        .andNew()
                        .eq("permanent", false)
                        .or()
                        .isNull("permanent")
                        .orderBy("create_time", false));
        if (backups.size() > maxKeep) {
            for (int i = maxKeep; i < backups.size(); i++) {
                deleteBackup(backups.get(i).getId());
                if (log.isDebugEnabled())
                    log.info("Rolling cleanup old backup: strategyId={}, backupId={}, filename={}", strategyId, backups.get(i).getId(), backups.get(i).getFilename());
            }
        }
    }

    // ========================================
    // 内部类：备份任务控制
    // ========================================

    /**
     * 备份任务状态控制
     */
    static class BackupTask {
        final Long backupId;
        final String outputPath;
        final String mode;
        final String tableList;
        private volatile boolean stopped = false;
        private volatile boolean paused = false;
        private final Object pauseLock = new Object();

        BackupTask(Long backupId, String outputPath, String mode, String tableList) {
            this.backupId = backupId;
            this.outputPath = outputPath;
            this.mode = mode;
            this.tableList = tableList;
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

        boolean isStopped() {
            return stopped;
        }

        /**
         * 如果暂停则等待恢复
         */
        void waitIfPaused() {
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
    }
}

package cn.org.autumn.modules.db.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.modules.db.dao.DatabaseBackupDao;
import cn.org.autumn.modules.db.entity.DatabaseBackupEntity;
import cn.org.autumn.utils.PageUtils;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
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

@Slf4j
@Service
public class DatabaseBackupService extends ModuleService<DatabaseBackupDao, DatabaseBackupEntity> {

    @Autowired
    private DataSource dataSource;

    @Value("${autumn.backup.dir:backups}")
    private String backupDir;

    private String databaseName;

    @PostConstruct
    public void initBackupDir() {
        try {
            Path path = Paths.get(backupDir);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Backup directory created: {}", path.toAbsolutePath());
            }
            // 获取数据库名称
            try (Connection conn = dataSource.getConnection()) {
                databaseName = conn.getCatalog();
                log.info("Database backup service initialized, database: {}, backupDir: {}", databaseName, path.toAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Failed to initialize backup directory: {}", e.getMessage(), e);
        }
    }

    /**
     * 分页查询备份记录
     */
    public PageUtils queryPage(Map<String, Object> params) {
        return queryPage(params, "id");
    }

    /**
     * 执行数据库备份（使用纯JDBC方式）
     */
    public DatabaseBackupEntity backup(String remark) {
        DatabaseBackupEntity entity = new DatabaseBackupEntity();
        entity.setDatabase(databaseName);
        entity.setRemark(remark);
        entity.setStatus(0);
        entity.setCreateTime(new Date());
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filename = databaseName + "_" + timestamp + ".sql";
        entity.setFilename(filename);
        // 先保存进行中的记录
        insert(entity);
        long startTime = System.currentTimeMillis();
        try {
            Path filePath = Paths.get(backupDir, filename);
            entity.setFilepath(filePath.toAbsolutePath().toString());
            int[] result = exportDatabase(filePath.toAbsolutePath().toString());
            entity.setTables(result[0]);
            entity.setRecords(result[1]);
            entity.setFilesize(Files.size(filePath));
            entity.setStatus(1);
            entity.setDuration(System.currentTimeMillis() - startTime);
            updateById(entity);
            log.info("Database backup completed: {}, tables={}, records={}, size={}", filename, result[0], result[1], entity.getFilesize());
        } catch (Exception e) {
            entity.setStatus(2);
            entity.setError(e.getMessage());
            entity.setDuration(System.currentTimeMillis() - startTime);
            updateById(entity);
            log.error("Database backup failed: {}", e.getMessage(), e);
        }

        return entity;
    }

    /**
     * 使用纯JDBC导出数据库
     */
    private int[] exportDatabase(String outputPath) throws Exception {
        int tableCount = 0;
        int totalRecords = 0;
        try (Connection conn = dataSource.getConnection(); BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath), StandardCharsets.UTF_8))) {
            String dbName = conn.getCatalog();
            // 写入文件头
            writer.write("-- ========================================\n");
            writer.write("-- Database Backup\n");
            writer.write("-- Database: " + dbName + "\n");
            writer.write("-- Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n");
            writer.write("-- ========================================\n\n");
            writer.write("SET NAMES utf8mb4;\n");
            writer.write("SET FOREIGN_KEY_CHECKS = 0;\n\n");
            // 获取所有表
            List<String> tables = getAllTables(conn);
            tableCount = tables.size();
            for (String table : tables) {
                writer.write("-- ----------------------------\n");
                writer.write("-- Table structure for " + table + "\n");
                writer.write("-- ----------------------------\n");
                writer.write("DROP TABLE IF EXISTS `" + table + "`;\n");
                // 导出建表语句
                String createTableSql = getCreateTableSql(conn, table);
                writer.write(createTableSql + ";\n\n");
                // 导出表数据
                int records = exportTableData(conn, writer, table);
                totalRecords += records;
                writer.write("\n");
            }
            writer.write("SET FOREIGN_KEY_CHECKS = 1;\n");
            writer.flush();
        }
        return new int[]{tableCount, totalRecords};
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
    private int exportTableData(Connection conn, BufferedWriter writer, String table) throws Exception {
        int count = 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM `" + table + "`")) {
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            if (!rs.isBeforeFirst()) {
                // 表为空
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
        // 总备份数
        int total = selectCount(new EntityWrapper<>());
        stats.put("total", total);
        // 成功数
        int success = selectCount(new EntityWrapper<DatabaseBackupEntity>().eq("status", 1));
        stats.put("success", success);
        // 失败数
        int failed = selectCount(new EntityWrapper<DatabaseBackupEntity>().eq("status", 2));
        stats.put("failed", failed);
        // 备份总大小
        List<DatabaseBackupEntity> list = selectList(new EntityWrapper<DatabaseBackupEntity>().eq("status", 1));
        long totalSize = 0;
        for (DatabaseBackupEntity e : list) {
            if (e.getFilesize() != null) {
                totalSize += e.getFilesize();
            }
        }
        stats.put("totalSize", totalSize);
        // 数据库名
        stats.put("database", databaseName);
        // 备份目录
        stats.put("backupDir", Paths.get(backupDir).toAbsolutePath().toString());
        return stats;
    }
}

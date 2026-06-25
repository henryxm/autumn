package cn.org.autumn.modules.db.service;

import cn.org.autumn.database.DatabaseHolder;
import cn.org.autumn.database.DatabaseType;
import cn.org.autumn.modules.sys.service.CrudGuardService;
import cn.org.autumn.table.config.TableProperties;
import cn.org.autumn.table.platform.RelationalTableOperations;
import cn.org.autumn.table.service.MysqlTableService;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 数据库管理页：概览、实体表清单与运行参数。
 */
@Service
public class DatabaseAdminService {

    @Autowired
    private DatabaseHolder databaseHolder;

    @Autowired
    private MysqlTableService mysqlTableService;

    @Autowired
    private RelationalTableOperations tableOperations;

    @Autowired
    private CrudGuardService crudGuardService;

    @Autowired
    private TableProperties tableProperties;

    public Map<String, Object> overview() {
        Map<String, Object> map = new LinkedHashMap<>();
        DatabaseType type = databaseHolder.getType();
        map.put("databaseType", type.name());
        map.put("databaseLabel", type.name());
        map.put("jdbcUrl", maskJdbcUrl(databaseHolder.getRoutedJdbcUrl()));
        map.put("annotationTableSync", type.supportsAnnotationTableSync());
        map.put("tablePack", tableProperties.getPack());
        map.put("tableAutoMode", tableProperties.getAuto().name());
        map.put("entityCount", countEntities());
        map.put("dbTableCount", safeTableCount());
        map.put("crud", crudStatus());
        map.put("links", quickLinks());
        return map;
    }

    public List<Map<String, Object>> listManagedTables() {
        List<Map<String, Object>> entities = mysqlTableService.listTableEntities();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> row : entities) {
            Map<String, Object> item = new LinkedHashMap<>(row);
            String tableName = String.valueOf(row.get("tableName"));
            boolean exists = false;
            try {
                exists = tableOperations.hasTable(tableName);
            } catch (Exception ignored) {
            }
            item.put("existsInDb", exists);
            Object cols = row.get("columns");
            item.put("columnCount", cols instanceof List ? ((List<?>) cols).size() : 0);
            out.add(item);
        }
        return out;
    }

    public Map<String, Object> crudStatus() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", crudGuardService.snapshot());
        map.put("statusLabel", crudGuardService.statusLabel());
        map.put("writableForUser", crudGuardService.isWritableForUser());
        return map;
    }

    private int countEntities() {
        try {
            return mysqlTableService.listTableEntities().size();
        } catch (Exception e) {
            return 0;
        }
    }

    private int safeTableCount() {
        try {
            Integer count = tableOperations.getTableCount();
            return count == null ? 0 : count;
        } catch (Exception e) {
            return 0;
        }
    }

    private List<Map<String, String>> quickLinks() {
        List<Map<String, String>> links = new ArrayList<>();
        links.add(link("数据库备份", "database.html", "备份、恢复与策略调度"));
        links.add(link("重建数据表", "reinit.html", "按实体同步表结构"));
        links.add(link("字段加密", "fieldencrypt.html", "存储加密与迁移"));
        links.add(link("SQL 监控", "druid/sql.html", "Druid SQL 分析"));
        links.add(link("Redis 管理", "redis.html", "缓存键空间运维"));
        return links;
    }

    private Map<String, String> link(String title, String url, String desc) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("title", title);
        m.put("url", url);
        m.put("description", desc);
        return m;
    }

    static String maskJdbcUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return "";
        }
        String trimmed = url.trim();
        int q = trimmed.indexOf('?');
        String base = q > 0 ? trimmed.substring(0, q) : trimmed;
        if (q > 0) {
            return base + "?***";
        }
        return base;
    }
}

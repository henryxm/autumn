package cn.org.autumn.table;

import cn.org.autumn.bean.EnvBean;
import cn.org.autumn.config.Config;
import cn.org.autumn.database.DatabaseHolder;
import cn.org.autumn.database.DatabaseType;
import cn.org.autumn.install.InstallMode;
import cn.org.autumn.table.data.InitType;
import cn.org.autumn.table.service.MysqlTableService;
import java.util.Locale;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Initialize table, scan the pointed destination package, generate the tables.
 */
@Service
@Slf4j
public class TableInit {
    @Autowired
    EnvBean envBean;

    @Value("${autumn.database:}")
    private String databaseType;

    @Autowired
    private DatabaseHolder databaseHolder;

    @Value("${autumn.table.init:true}")
    private boolean init;

    public String getDatabaseType() {
        if (StringUtils.isNotBlank(databaseType)) {
            return databaseType.trim();
        }
        return databaseHolder.getType().name().toLowerCase(Locale.ROOT);
    }

    public boolean isInit() {
        return init;
    }

    @Autowired
    private MysqlTableService mysqlTableService;

    @Autowired
    private Environment springEnvironment;

    @PostConstruct
    public void start() {
        if (Config.getInstance().getEnvironment() == null && springEnvironment != null) {
            Config.getInstance().setEnv(springEnvironment);
        }
        if (InstallMode.isActive(springEnvironment)) {
            return;
        }
        if (!init || !envBean.isTableInit()) {
            return;
        }
        DatabaseType t = databaseHolder.getType();
        if (t.supportsAnnotationTableSync()) {
            mysqlTableService.create();
        } else {
            log.warn("Database type={} has no annotation DDL support, TableInit skipped (use Flyway/manual DDL or extend dialect).", t);
        }
    }

    /**
     * 用于用户重新对单个实体类型进行重新创建表，比如某些日志表，或者临时数据表需要清理后重新创建表
     *
     * @param clazz 实体类型
     */
    public void reinit(Class<?> clazz) {
        create(clazz, InitType.create);
    }

    public void create(Class<?> clazz, InitType type) {
        if (InstallMode.isActive(springEnvironment)) {
            log.warn("Install wizard mode, skipped single-table annotation DDL: {}", clazz.getName());
            return;
        }
        if (!databaseHolder.getType().supportsAnnotationTableSync()) {
            log.warn("Skipped annotation DDL for table {}: database type not supported", clazz.getName());
            return;
        }
        mysqlTableService.create(clazz, type);
    }
}

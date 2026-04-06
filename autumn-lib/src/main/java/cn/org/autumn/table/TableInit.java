package cn.org.autumn.table;

import javax.annotation.PostConstruct;
import java.util.Locale;

import cn.org.autumn.bean.EnvBean;
import cn.org.autumn.table.data.InitType;
import cn.org.autumn.database.DatabaseHolder;
import cn.org.autumn.database.DatabaseType;
import cn.org.autumn.table.service.MysqlTableService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Initialize table, scan the pointed destination package, generate the tables.
 */
@Service
public class TableInit {

    private static final Logger log = LoggerFactory.getLogger(TableInit.class);

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

    @PostConstruct
    public void start() {
        if (!envBean.isTableInit())
            return;
        DatabaseType t = databaseHolder.getType();
        if (t.supportsAnnotationTableSync()) {
            mysqlTableService.create();
        } else {
            log.warn("当前库类型={} 未接入注解建表，已跳过 TableInit（请使用 Flyway/手工 DDL 或后续扩展方言）。", t);
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
        if (!databaseHolder.getType().supportsAnnotationTableSync()) {
            log.warn("跳过单表 {} 的注解建表：当前库类型未支持", clazz.getName());
            return;
        }
        mysqlTableService.create(clazz, type);
    }
}

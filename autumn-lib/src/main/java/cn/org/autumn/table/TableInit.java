package cn.org.autumn.table;

import jakarta.annotation.PostConstruct;

import cn.org.autumn.bean.EnvBean;
import cn.org.autumn.table.data.InitType;
import cn.org.autumn.table.service.MysqlTableService;
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

    public static String MYSQL = "mysql";

    @Value("${autumn.database:mysql}")
    private String databaseType;

    @Value("${autumn.table.init:true}")
    private boolean init;

    public String getDatabaseType() {
        return databaseType;
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
        if (MYSQL.equals(databaseType))
            mysqlTableService.create();
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
        mysqlTableService.create(clazz, type);
    }
}

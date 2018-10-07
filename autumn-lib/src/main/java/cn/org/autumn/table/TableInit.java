package cn.org.autumn.table;

import javax.annotation.PostConstruct;

import cn.org.autumn.table.service.MysqlTableService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Initialize table, scan the pointed destination package, generate the tables.
 */
@Service
public class TableInit {

    private static final Logger log = LoggerFactory.getLogger(TableInit.class);

    /**
     * constant value for mysql database.
     */
    public static String MYSQL = "mysql";

    /**
     * Supported database type, value from configuration properties file;
     */
    @Value("${autumn.database}")
    private String databaseType = MYSQL;

    @Value("${autumn.table.init}")
    public boolean init = false;

    /**
     * link to table create service;
     */
    @Autowired
    private MysqlTableService mysqlTableService;

    @PostConstruct
    public void start() {
        if (MYSQL.equals(databaseType))
            mysqlTableService.createMysqlTable();
        else
            throw new NotImplementedException();

    }


}

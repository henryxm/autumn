package cn.org.autumn.config;

import cn.org.autumn.database.AutumnDatabaseHolder;
import cn.org.autumn.database.AutumnDatabaseType;
import cn.org.autumn.handler.EnumTypeHandler;
import cn.org.autumn.model.StubMapper;
import cn.org.autumn.service.DefaultMapper;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusPropertiesCustomizer;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Autowired
    private AutumnDatabaseHolder autumnDatabaseHolder;

    /**
     * 与 {@code application.yml} 中 {@code mybatis-plus.global-config.db-config.column-format} 配合：
     * YAML 为 MySQL 默认；此处按 {@link cn.org.autumn.database.AutumnDatabaseHolder} 覆盖，避免 PG 等收到反引号。
     * 物理列名为保留字（如 {@code order}）时，实体侧用非保留 Java 名 + {@code @TableField("order")}，见 {@code SysConfigEntity}。
     */
    @Bean
    public MybatisPlusPropertiesCustomizer mybatisPlusColumnFormatCustomizer() {
        return props -> {
            GlobalConfig gc = props.getGlobalConfig();
            if (gc == null) {
                gc = new GlobalConfig();
                props.setGlobalConfig(gc);
            }
            GlobalConfig.DbConfig db = gc.getDbConfig();
            if (db == null) {
                db = new GlobalConfig.DbConfig();
                gc.setDbConfig(db);
            }
            AutumnDatabaseType t = autumnDatabaseHolder.getType();
            if (t == AutumnDatabaseType.POSTGRESQL || t == AutumnDatabaseType.ORACLE) {
                db.setColumnFormat("\"%s\"");
            } else if (t == AutumnDatabaseType.SQLSERVER) {
                db.setColumnFormat("[%s]");
            } else {
                db.setColumnFormat("`%s`");
            }
        };
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor();
        AutumnDatabaseType t = autumnDatabaseHolder.getType();
        if (t == AutumnDatabaseType.POSTGRESQL) {
            paginationInnerInterceptor.setDbType(DbType.POSTGRE_SQL);
        } else if (t == AutumnDatabaseType.ORACLE) {
            paginationInnerInterceptor.setDbType(DbType.ORACLE);
        } else if (t == AutumnDatabaseType.SQLSERVER) {
            paginationInnerInterceptor.setDbType(DbType.SQL_SERVER);
        } else if (t == AutumnDatabaseType.MARIADB) {
            paginationInnerInterceptor.setDbType(DbType.MARIADB);
        } else {
            paginationInnerInterceptor.setDbType(DbType.MYSQL);
        }
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(paginationInnerInterceptor);
        return interceptor;
    }

    @Bean
    public DefaultMapper defaultMapper() {
        return new StubMapper();
    }

    @Bean
    public ConfigurationCustomizer mybatisConfigurationCustomizer() {
        return configuration -> configuration.setDefaultEnumTypeHandler(EnumTypeHandler.class);
    }
}

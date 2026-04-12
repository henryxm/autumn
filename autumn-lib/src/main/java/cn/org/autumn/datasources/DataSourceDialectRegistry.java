package cn.org.autumn.datasources;

import cn.org.autumn.database.DatabaseHolder;
import cn.org.autumn.database.DatabaseType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 将 {@link DynamicDataSource} 使用的 lookup key（{@link DataSourceNames#FIRST} / {@link DataSourceNames#SECOND}）
 * 解析为 {@link DatabaseType}，供 {@link DatabaseHolder} 等与当前线程数据源对齐。
 * <p>
 * 解析规则与 {@link DynamicDataSourceConfig} 一致：{@code second.url} 为空时按与 first 相同 URL 处理。
 */
@Component
public class DataSourceDialectRegistry {

    private static final Logger log = LoggerFactory.getLogger(DataSourceDialectRegistry.class);

    private final DatabaseType firstType;

    private final DatabaseType secondType;

    public DataSourceDialectRegistry(Environment environment) {
        String autumnDb = environment.getProperty("autumn.database", "");
        String u1 = environment.getProperty("spring.datasource.druid.first.url");
        if (StringUtils.isBlank(u1)) {
            u1 = environment.getProperty("spring.datasource.url");
        }
        String u2 = environment.getProperty("spring.datasource.druid.second.url");
        if (StringUtils.isBlank(u2)) {
            u2 = u1;
        }
        this.firstType = DatabaseHolder.resolveType(u1, autumnDb);
        this.secondType = DatabaseHolder.resolveType(u2, autumnDb);
    }

    public DatabaseType getFirstType() {
        return firstType;
    }

    public DatabaseType getSecondType() {
        return secondType;
    }

    /**
     * @param lookupKey {@link DynamicDataSource#getDataSource()} 返回值；{@code null} 或空白表示默认目标源（first）
     */
    public DatabaseType resolveForLookupKey(String lookupKey) {
        if (StringUtils.isBlank(lookupKey)) {
            return firstType;
        }
        if (DataSourceNames.FIRST.equals(lookupKey)) {
            return firstType;
        }
        if (DataSourceNames.SECOND.equals(lookupKey)) {
            return secondType;
        }
        log.warn("Unknown datasource lookup key [{}], falling back to first database type [{}]", lookupKey, firstType);
        return firstType;
    }
}

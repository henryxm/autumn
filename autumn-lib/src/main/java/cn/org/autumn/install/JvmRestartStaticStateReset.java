package cn.org.autumn.install;

import cn.org.autumn.config.Config;
import cn.org.autumn.database.runtime.RuntimeSqlDialectRegistry;
import cn.org.autumn.datasources.DynamicDataSource;
import cn.org.autumn.service.BaseCacheService;
import cn.org.autumn.site.ConfigFactory;
import cn.org.autumn.site.DomainFactory;
import cn.org.autumn.site.HostFactory;
import cn.org.autumn.site.LoadFactory;
import cn.org.autumn.site.LoginFactory;
import cn.org.autumn.site.MappingFactory;
import cn.org.autumn.site.PathFactory;
import cn.org.autumn.site.UpgradeFactory;
import cn.org.autumn.table.relational.RelationalSchemaSqlRegistry;
import cn.org.autumn.table.relational.dialect.mysql.MysqlSchemaSql;
import cn.org.autumn.utils.SpringContextUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 安装向导等同 JVM 内二次启动前，清理框架层 static / ThreadLocal，不访问已关闭的 Spring 容器。
 */
@Slf4j
public final class JvmRestartStaticStateReset {

    private JvmRestartStaticStateReset() {
    }

    public static void resetAll() {
        DynamicDataSource.clearDataSource();
        BaseCacheService.clearSharedCacheConfigRegistry();
        Config.getInstance().setApplicationContext(null);
        Config.setInstance(null);
        SpringContextUtils.clearForJvmProcessRestart();
        RuntimeSqlDialectRegistry.resetForJvmRestart();
        RelationalSchemaSqlRegistry.setFallback(MysqlSchemaSql.INSTANCE);
        PathFactory.clearOrderedHandlerCacheForJvmRestart();
        ConfigFactory.clearOrderedHandlerCacheForJvmRestart();
        HostFactory.clearOrderedHandlerCacheForJvmRestart();
        LoginFactory.clearOrderedHandlerCacheForJvmRestart();
        MappingFactory.clearOrderedHandlerCacheForJvmRestart();
        DomainFactory.clearStaticSiteBindCache();
        UpgradeFactory.clearJvmRestartData();
        LoadFactory.resetJvmRestart();
        log.info("Reset environment data.");
    }
}

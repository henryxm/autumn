package cn.org.autumn.table;

import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;

import java.util.Locale;

import cn.org.autumn.bean.EnvBean;
import cn.org.autumn.config.Config;
import cn.org.autumn.table.data.InitType;
import cn.org.autumn.database.DatabaseHolder;
import cn.org.autumn.database.DatabaseType;
import cn.org.autumn.install.InstallMode;
import cn.org.autumn.table.service.MysqlTableService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Initialize table, scan the pointed destination package, generate the tables.
 */
@Slf4j
@Service
public class TableInit {

    @Autowired
    EnvBean envBean;

    @Value("${autumn.database:}")
    private String databaseType;

    @Autowired
    private DatabaseHolder databaseHolder;

    @Value("${autumn.table.init:true}")
    private boolean init;

    @Autowired
    private MysqlTableService mysqlTableService;

    @Autowired
    private Environment springEnvironment;

    @Autowired
    private SysConfigSchemaGate schemaGate;

    private volatile boolean ensured;

    public String getDatabaseType() {
        if (StringUtils.isNotBlank(databaseType)) {
            return databaseType.trim();
        }
        return databaseHolder.getType().name().toLowerCase(Locale.ROOT);
    }

    public boolean isInit() {
        return init;
    }

    @PostConstruct
    public void onConstruct() {
        if (Config.getInstance().getEnvironment() == null && springEnvironment != null) {
            Config.getInstance().setEnv(springEnvironment);
        }
        ensureCreated();
    }

    /**
     * 运维重置入口（如 GeneratorController）：强制再跑注解建表。
     * 启动期请用 {@link #ensureCreated()} 幂等路径。
     */
    public void start() {
        if (Config.getInstance().getEnvironment() == null && springEnvironment != null) {
            Config.getInstance().setEnv(springEnvironment);
        }
        ensured = false;
        ensureCreated();
    }

    /**
     * 幂等建表：安装向导 / {@code autumn.table.init=false} 时不标记 schemaReady；
     * 注解同步成功或不支持注解建表（假定外部 DDL）时 {@link SysConfigSchemaGate#markSchemaReady()}。
     * <p>失败时不置 {@code ensured}，允许后续重试（兼容 InitFactory 二次调用）。
     *
     * @return 当前是否 schemaReady
     */
    public synchronized boolean ensureCreated() {
        if (ensured) {
            return schemaGate.isSchemaReady();
        }
        if (InstallMode.isActive(springEnvironment)) {
            log.debug("TableInit skipped: install mode");
            ensured = true;
            return false;
        }
        // 与 isInit() / EnvBean 对齐：任一为 false 则跳过（二者均绑定 autumn.table.init）
        if (!init || !envBean.isTableInit()) {
            log.debug("TableInit skipped: autumn.table.init=false");
            ensured = true;
            return false;
        }
        DatabaseType t = databaseHolder.getType();
        try {
            if (t.supportsAnnotationTableSync()) {
                long t0 = System.currentTimeMillis();
                mysqlTableService.create();
                schemaGate.markSchemaReady();
                ensured = true;
                log.info("TableInit finished: database={} elapsedMs={}", t, System.currentTimeMillis() - t0);
            } else {
                // 外部 Flyway/手工 DDL；允许 InitFactory 继续，由 SysConfig soft-defer 兜底缺表
                schemaGate.markSchemaReady();
                ensured = true;
                log.warn("当前库类型={} 未接入注解建表，已跳过 TableInit（请使用 Flyway/手工 DDL 或后续扩展方言）。", t);
            }
        } catch (RuntimeException e) {
            log.error("TableInit failed: database={} msg={}", t, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("TableInit failed: database={} msg={}", t, e.getMessage());
            throw new IllegalStateException("TableInit failed: " + e.getMessage(), e);
        }
        return schemaGate.isSchemaReady();
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
            log.warn("安装向导模式下跳过单表注解建表: {}", clazz.getName());
            return;
        }
        if (!databaseHolder.getType().supportsAnnotationTableSync()) {
            log.warn("跳过单表 {} 的注解建表：当前库类型未支持", clazz.getName());
            return;
        }
        mysqlTableService.create(clazz, type);
    }
}

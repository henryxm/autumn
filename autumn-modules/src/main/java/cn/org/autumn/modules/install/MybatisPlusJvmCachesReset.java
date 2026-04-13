package cn.org.autumn.modules.install;

import cn.org.autumn.config.JvmRestartHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * MyBatis-Plus 2.x 在 JVM 内保留多处 static 缓存；安装向导首启使用 H2（{@code MODE=MySQL}）时，
 * {@link com.baomidou.mybatisplus.toolkit.TableInfoHelper#initTableInfo} 会把带反引号的列/表元数据写入
 * {@code tableInfoCache}（按实体类名）。进程内 {@code SpringApplication.exit} 后再 {@code run} 时若不清缓存，
 * 会命中旧 {@link com.baomidou.mybatisplus.entity.TableInfo}，在 PostgreSQL 等非 MySQL 库上仍可能执行带反引号的 SQL。
 * <p>
 * 由 {@link MybatisPlusInstallJvmRestartCleaner} 实现 {@link JvmRestartHandler}，
 * 经 {@link cn.org.autumn.install.InstallJvmRestartCleanupCoordinator} 在重启流程中调用；勿在业务代码中随意调用 {@link #clearForProcessRestart()}。
 */
public final class MybatisPlusJvmCachesReset {

    private static final Logger log = LoggerFactory.getLogger(MybatisPlusJvmCachesReset.class);

    private MybatisPlusJvmCachesReset() {
    }

    /**
     * 在已关闭的 Spring 上下文之后、同一 JVM 内再次启动 Spring Boot 之前调用。
     */
    public static void clearForProcessRestart() {
        clearMapField("com.baomidou.mybatisplus.toolkit.TableInfoHelper", "tableInfoCache");
        clearMapField("com.baomidou.mybatisplus.toolkit.GlobalConfigUtils", "GLOBAL_CONFIG");
        nullStaticField("com.baomidou.mybatisplus.mapper.SqlRunner", "FACTORY");
    }

    private static void clearMapField(String className, String fieldName) {
        try {
            Class<?> c = Class.forName(className);
            Field f = c.getDeclaredField(fieldName);
            f.setAccessible(true);
            Object v = f.get(null);
            if (v instanceof Map) {
                ((Map<?, ?>) v).clear();
            }
        } catch (Throwable t) {
            log.warn("无法清空 MP 缓存 {}#{}（可忽略若未使用 MyBatis-Plus）: {}", className, fieldName, t.toString());
        }
    }

    private static void nullStaticField(String className, String fieldName) {
        try {
            Class<?> c = Class.forName(className);
            Field f = c.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(null, null);
        } catch (Throwable t) {
            log.warn("无法重置 MP 静态字段 {}#{}: {}", className, fieldName, t.toString());
        }
    }
}

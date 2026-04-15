package cn.org.autumn.modules.install;

import org.apache.commons.lang.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 在 JdbcEnvironmentPostProcessor 之前执行：若存在外部数据源配置文件则优先加载；否则在开启 {@code autumn.install.wizard}
 * 时进入占位数据源（H2 内存）安装模式，避免未配置时强绑 application-dev 等业务 profile 中的 JDBC。
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InstallEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String DEFAULT_CONFIG = InstallConstants.DEFAULT_CONFIG_FILE;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String pathProp = environment.getProperty(InstallConstants.CONFIG_PATH, DEFAULT_CONFIG);
        File configFile = new File(pathProp);
        if (!configFile.isAbsolute()) {
            configFile = new File(System.getProperty("user.dir", "."), pathProp).getAbsoluteFile();
        }

        boolean force = Boolean.parseBoolean(environment.getProperty(InstallConstants.FORCE_REINSTALL, "false"))
                || "true".equalsIgnoreCase(System.getenv("AUTUMN_INSTALL_FORCE"));
        boolean wizard = Boolean.parseBoolean(environment.getProperty(InstallConstants.WIZARD_ENABLED, "false"));

        if (configFile.isFile() && !force) {
            loadYamlFile(environment, configFile);
            Map<String, Object> tail = new HashMap<>(2);
            if (StringUtils.isBlank(environment.getProperty(InstallConstants.INSTALL_MODE))) {
                tail.put(InstallConstants.INSTALL_MODE, "false");
            }
            if (!tail.isEmpty()) {
                environment.getPropertySources().addFirst(new MapPropertySource("autumnInstallModeTail", tail));
            }
            return;
        }

        if (wizard && (force || !configFile.isFile())) {
            Map<String, Object> bootstrap = new HashMap<>(24);
            bootstrap.put(InstallConstants.INSTALL_MODE, "true");
            // 安装占位启动阶段不拉起 Quartz，避免 JDBC JobStore / 业务 Job 在安装未完成时访问库或外部资源
            bootstrap.put("spring.quartz.auto-startup", "false");
            bootstrap.put("autumn.table.init", "false");
            bootstrap.put("spring.datasource.driverClassName", "org.h2.Driver");
            bootstrap.put("spring.datasource.druid.first.url",
                    "jdbc:h2:mem:autumn_install_boot;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
            bootstrap.put("spring.datasource.druid.first.username", "sa");
            bootstrap.put("spring.datasource.druid.first.password", "");
            bootstrap.put("spring.datasource.druid.validation-query", "SELECT 1");
            mergeAutoconfigureExclude(environment, bootstrap);
            environment.getPropertySources().addFirst(new MapPropertySource("autumnInstallBootstrap", bootstrap));
        }
    }

    /**
     * 将 {@link InstallConstants#AUTOCONFIGURE_EXCLUDE_EXTRA} 与当前环境中的 {@code spring.autoconfigure.exclude} 合并后写入引导 Map，
     * 便于安装期排除重型自动配置而无需改代码。
     */
    private static void mergeAutoconfigureExclude(ConfigurableEnvironment environment, Map<String, Object> bootstrap) {
        String extra = environment.getProperty(InstallConstants.AUTOCONFIGURE_EXCLUDE_EXTRA);
        if (StringUtils.isBlank(extra)) {
            return;
        }
        String existing = environment.getProperty("spring.autoconfigure.exclude");
        String merged = StringUtils.isBlank(existing) ? extra.trim() : existing.trim() + "," + extra.trim();
        bootstrap.put("spring.autoconfigure.exclude", merged);
    }

    private static void loadYamlFile(ConfigurableEnvironment environment, File configFile) {
        try {
            Resource resource = new FileSystemResource(configFile);
            YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
            List<org.springframework.core.env.PropertySource<?>> loaded = loader.load("autumnDatasourceFile", resource);
            for (int i = loaded.size() - 1; i >= 0; i--) {
                environment.getPropertySources().addFirst(loaded.get(i));
            }
        } catch (Exception e) {
            throw new IllegalStateException("无法加载安装配置文件: " + configFile.getAbsolutePath(), e);
        }
    }
}

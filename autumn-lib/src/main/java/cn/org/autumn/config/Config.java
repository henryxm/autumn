package cn.org.autumn.config;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Config {
    private static final Logger log = LoggerFactory.getLogger(Config.class);

    public static final String DEV = "dev";
    public static final String TEST = "test";
    public static final String PROD = "prod";

    public static final String DERBY = "derby";
    public static final String H2 = "h2";
    public static final String SQLITE = "sqlite";

    public static final String DEV_DOCKER = "dev-docker";
    public static final String TEST_DOCKER = "test-docker";
    public static final String PROD_DOCKER = "prod-docker";

    @Setter
    private static Config instance = null;

    @Getter
    @Setter
    private ApplicationContext applicationContext;

    @Getter
    private Environment environment;

    private Config() {
    }

    public static Config getInstance() {
        if (null == instance) {
            instance = new Config();
            instance.ENVs = new ArrayList<>();
            instance.ENVs.add(DEV);
        }
        return instance;
    }

    public List<String> ENVs;

    public void setEnv(Environment environment) {
        this.environment = environment;
        ENVs.clear();
        ENVs.addAll(Arrays.asList(environment.getActiveProfiles()));
    }

    public static boolean isProd() {
        return getInstance().ENVs.contains(Config.PROD) || getInstance().ENVs.contains(Config.PROD_DOCKER);
    }

    public static boolean isDev() {
        return getInstance().ENVs.contains(Config.DEV) || getInstance().ENVs.contains(Config.DEV_DOCKER) || getInstance().ENVs.contains(Config.DERBY) || getInstance().ENVs.contains(Config.H2) || getInstance().ENVs.contains(Config.SQLITE);
    }

    public static boolean isTest() {
        return getInstance().ENVs.contains(Config.TEST) || getInstance().ENVs.contains(Config.TEST_DOCKER);
    }

    public static boolean linux() {
        String os = System.getProperty("os.name");
        return os.toLowerCase().startsWith("linux");
    }

    public static boolean windows() {
        String os = System.getProperty("os.name");
        return os.toLowerCase().startsWith("win");
    }

    public static boolean mac() {
        String os = System.getProperty("os.name");
        return os.toLowerCase().startsWith("mac");
    }

    public static String home() {
        String home = System.getProperty("user.home");
        if (!home.endsWith("/"))
            home = home + "/";
        return home;
    }

    public static Object getBean(String beanName) {
        return getBean(beanName, false);
    }

    public static Object getBean(String beanName, boolean lowerFirst) {
        try {
            if (null != beanName) {
                if (lowerFirst) {
                    beanName = beanName.trim();
                    if (beanName.length() > 1) {
                        beanName = beanName.substring(0, 1).toLowerCase() + beanName.substring(1);
                    }
                }
                if (null != getInstance().getApplicationContext()) {
                    return getInstance().getApplicationContext().getBean(beanName);
                }
            }
        } catch (Exception e) {
            log.debug("getBean:", e);
        }
        return null;
    }

    /** 读取配置：环境变量 → 系统属性 → Spring {@link Environment}（含 yml/properties）。 */
    public static String getEnv(String key) {
        if (StringUtils.isBlank(key))
            return key;
        Map<String, String> map = System.getenv();
        String value = "";
        if (map.containsKey(key))
            value = map.get(key);
        if (StringUtils.isBlank(value)) {
            value = System.getProperties().getProperty(key);
        }
        if (StringUtils.isBlank(value)) {
            Environment env = getInstance().getEnvironment();
            if (env != null) {
                String p = env.getProperty(key);
                if (StringUtils.isNotBlank(p)) {
                    value = p;
                }
            }
        }
        return value != null ? value : "";
    }

    public static Object getBean(Class<?> clazz) {
        try {
            if (null != getInstance().getApplicationContext()) {
                return getInstance().getApplicationContext().getBean(clazz);
            }
        } catch (Exception e) {
            log.debug("getBean:", e);
        }
        return null;
    }
}

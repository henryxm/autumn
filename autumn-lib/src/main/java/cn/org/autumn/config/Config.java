package cn.org.autumn.config;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Config {
    private static final Logger log = LoggerFactory.getLogger(Config.class);

    public static final String DEV = "dev";
    public static final String TEST = "test";
    public static final String PROD = "prod";

    public static final String DEV_DOCKER = "dev-docker";
    public static final String TEST_DOCKER = "test-docker";
    public static final String PROD_DOCKER = "prod-docker";

    private static Config instance = null;
    private ApplicationContext applicationContext;
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
        for (String e : environment.getActiveProfiles()) {
            ENVs.add(e);
        }
    }

    public Environment getEnvironment() {
        return this.environment;
    }

    public static boolean isProd() {
        if (getInstance().ENVs.contains(Config.PROD) || getInstance().ENVs.contains(Config.PROD_DOCKER))
            return true;
        else
            return false;
    }

    public static boolean isDev() {
        if (getInstance().ENVs.contains(Config.DEV) || getInstance().ENVs.contains(Config.DEV_DOCKER))
            return true;
        else
            return false;
    }

    public static boolean isTest() {
        if (getInstance().ENVs.contains(Config.TEST) || getInstance().ENVs.contains(Config.TEST_DOCKER))
            return true;
        else
            return false;
    }

    public static boolean linux() {
        String os = System.getProperty("os.name");
        if (os.toLowerCase().startsWith("linux")) {
            return true;
        }
        return false;
    }

    public static boolean windows() {
        String os = System.getProperty("os.name");
        if (os.toLowerCase().startsWith("win")) {
            return true;
        }
        return false;
    }

    public static boolean mac() {
        String os = System.getProperty("os.name");
        if (os.toLowerCase().startsWith("mac")) {
            return true;
        }
        return false;
    }

    public static String home() {
        String home = System.getProperty("user.home");
        if (!home.endsWith("/"))
            home = home + "/";
        return home;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
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
        return value;
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

package cn.org.autumn.config;

import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

public class Config {

    public static final String DEV = "dev";
    public static final String TEST = "test";
    public static final String PROD = "prod";

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
        if (getInstance().ENVs.contains(Config.PROD))
            return true;
        else
            return false;
    }

    public static boolean isDev() {
        if (getInstance().ENVs.contains(Config.DEV))
            return true;
        else
            return false;
    }

    public static boolean isTest() {
        if (getInstance().ENVs.contains(Config.TEST))
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
        if (null != getInstance().getApplicationContext()) {
            return getInstance().getApplicationContext().getBean(beanName);
        }
        return null;
    }
}

package cn.org.autumn.config;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

public class Config {

    public static final String ACTIVE = "spring.profiles.active";
    public static final String DEV = "dev";
    public static final String TEST = "test";
    public static final String PROD = "prod";

    private static Config instance = null;

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
        for(String e:environment.getActiveProfiles()){
            ENVs.add(e);
        }
    }
    public Environment getEnvironment(){
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

}

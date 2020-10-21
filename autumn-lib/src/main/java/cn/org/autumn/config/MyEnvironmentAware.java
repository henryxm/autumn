package cn.org.autumn.config;

import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration("env")
public class MyEnvironmentAware implements EnvironmentAware {
    @Override
    public void setEnvironment(Environment environment) {
        Config.getInstance().setEnv(environment);
    }
}

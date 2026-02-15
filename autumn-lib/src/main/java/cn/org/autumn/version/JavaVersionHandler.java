package cn.org.autumn.version;

import cn.org.autumn.config.VersionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Java 运行时版本
 */
@Order(2)
@Component
public class JavaVersionHandler implements VersionHandler {

    @Override
    public String name() {
        return "Java Runtime";
    }

    @Override
    public String version() {
        return System.getProperty("java.version", "unknown");
    }
}

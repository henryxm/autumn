package cn.org.autumn.datasources;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DruidStatViewSecurityEnvironmentPostProcessorTest {

    private final DruidStatViewSecurityEnvironmentPostProcessor processor = new DruidStatViewSecurityEnvironmentPostProcessor();

    @Test
    void blankCredentialsForceDisable() {
        StandardEnvironment env = envWith("true", "", "secret", null);
        processor.postProcessEnvironment(env, new SpringApplication());
        assertFalse(Boolean.parseBoolean(env.getProperty("spring.datasource.druid.stat-view-servlet.enabled")));
    }

    @Test
    void prodWeakPasswordForceDisable() {
        StandardEnvironment env = envWith("true", "admin", "changeme", null);
        env.setActiveProfiles("prod");
        processor.postProcessEnvironment(env, new SpringApplication());
        assertFalse(Boolean.parseBoolean(env.getProperty("spring.datasource.druid.stat-view-servlet.enabled")));
    }

    @Test
    void emptyAllowFallsBackToLocalhost() {
        StandardEnvironment env = envWith("true", "ops", "S3cure!Pass", "");
        processor.postProcessEnvironment(env, new SpringApplication());
        assertTrue(Boolean.parseBoolean(env.getProperty("spring.datasource.druid.stat-view-servlet.enabled")));
        assertEquals("127.0.0.1", env.getProperty("spring.datasource.druid.stat-view-servlet.allow"));
    }

    @Test
    void strongProdCredentialsKeepEnabled() {
        StandardEnvironment env = envWith("true", "ops", "S3cure!Pass", "127.0.0.1");
        env.setActiveProfiles("prod");
        processor.postProcessEnvironment(env, new SpringApplication());
        assertTrue(Boolean.parseBoolean(env.getProperty("spring.datasource.druid.stat-view-servlet.enabled")));
    }

    @Test
    void disabledSkipsPatch() {
        StandardEnvironment env = envWith("false", "", "", "");
        processor.postProcessEnvironment(env, new SpringApplication());
        assertFalse(env.getPropertySources().contains("autumnDruidStatViewSecurity"));
    }

    private static StandardEnvironment envWith(String enabled, String user, String pass, String allow) {
        StandardEnvironment env = new StandardEnvironment();
        Map<String, Object> map = new HashMap<>();
        map.put("spring.datasource.druid.stat-view-servlet.enabled", enabled);
        map.put("spring.datasource.druid.stat-view-servlet.login-username", user);
        map.put("spring.datasource.druid.stat-view-servlet.login-password", pass);
        if (allow != null) {
            map.put("spring.datasource.druid.stat-view-servlet.allow", allow);
        }
        env.getPropertySources().addFirst(new MapPropertySource("test", map));
        return env;
    }
}

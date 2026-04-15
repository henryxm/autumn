package cn.org.autumn.install;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * 当未启用 Redis（{@code autumn.redis.open=false}）或处于安装向导占位启动时，排除 Spring Data Redis 与 Redisson 的自动配置，
 * 避免在未配置 {@code spring.redis.*} 或 Redis 不可达时仍创建客户端、导致启动失败。
 * <p>
 * 顺序晚于 {@link cn.org.autumn.modules.install.InstallEnvironmentPostProcessor}，以便读取到 {@code autumn.install.mode}。
 */
@Order(AutumnRedisStackEnvironmentPostProcessor.ORDER)
public class AutumnRedisStackEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    /**
     * 略高于 {@code Ordered.HIGHEST_PRECEDENCE}，保证在默认安装 EPP 写入属性之后再合并排除项。
     */
    static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 100;

    private static final String EXCLUDE_PROP = "spring.autoconfigure.exclude";

    private static final String[] REDIS_STACK_AUTOCONFIG = new String[]{
            "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
            "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
            // Boot 3：仅可排除 AutoConfiguration.imports 中注册的类；Redisson 3.40+ 使用 V2
            "org.redisson.spring.starter.RedissonAutoConfigurationV2",
    };

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean redisOpen = Boolean.parseBoolean(environment.getProperty("autumn.redis.open", "false"));
        if (redisOpen)
            return;
        Map<String, Object> patch = new HashMap<>(2);
        patch.put("autumn.redis.open", "false");
        patch.put("autumn.shiro.redis", "false");
        String merged = mergeExclude(environment.getProperty(EXCLUDE_PROP));
        patch.put(EXCLUDE_PROP, merged);
        environment.getPropertySources().addFirst(new MapPropertySource("autumnRedisStackDisabled", patch));
    }

    private static String mergeExclude(String existing) {
        StringBuilder sb = new StringBuilder(256);
        if (StringUtils.isNotBlank(existing)) {
            sb.append(existing.trim());
        }
        for (String cls : REDIS_STACK_AUTOCONFIG) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(cls);
        }
        return sb.toString();
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}

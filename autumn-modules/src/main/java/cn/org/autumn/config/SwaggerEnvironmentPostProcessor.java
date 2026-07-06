package cn.org.autumn.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * {@code autumn.swagger.enabled=false} 时在自动配置阶段之前排除 Springfox，
 * 避免 {@link OpenApiAutoConfiguration} 已启动而 {@link SwaggerConfig} 未加载（缺 MVC 兼容补丁）导致慢启动或 NPE。
 * <p>
 * 勿使用 {@code @EnableAutoConfiguration(exclude=...)} 的延迟 {@code @Configuration}，实测会拖慢 Context 刷新。
 */
@Order(SwaggerEnvironmentPostProcessor.ORDER)
public class SwaggerEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 50;

    private static final String EXCLUDE_PROP = "spring.autoconfigure.exclude";

    private static final String SPRINGFOX_OPEN_API = "springfox.boot.starter.autoconfigure.OpenApiAutoConfiguration";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (Boolean.parseBoolean(environment.getProperty("autumn.swagger.enabled", "true"))) {
            return;
        }
        Map<String, Object> patch = new HashMap<>(2);
        patch.put(EXCLUDE_PROP, mergeExclude(environment.getProperty(EXCLUDE_PROP)));
        environment.getPropertySources().addFirst(new MapPropertySource("autumnSwaggerDisabled", patch));
    }

    private static String mergeExclude(String existing) {
        if (StringUtils.isBlank(existing)) {
            return SPRINGFOX_OPEN_API;
        }
        String trimmed = existing.trim();
        if (containsClass(trimmed, SPRINGFOX_OPEN_API)) {
            return trimmed;
        }
        return trimmed + ',' + SPRINGFOX_OPEN_API;
    }

    private static boolean containsClass(String csv, String className) {
        for (String part : csv.split(",")) {
            if (className.equals(StringUtils.trimToEmpty(part))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}

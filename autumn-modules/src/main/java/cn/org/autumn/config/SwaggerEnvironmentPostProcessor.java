package cn.org.autumn.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * {@code autumn.swagger.enabled=false} 时在自动配置阶段之前关闭 springdoc，避免扫描全部 Controller 拖慢 dev 启动。
 */
@Order(SwaggerEnvironmentPostProcessor.ORDER)
public class SwaggerEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 50;

    private static final String EXCLUDE_PROP = "spring.autoconfigure.exclude";

    private static final String[] SPRINGDOC_AUTOCONFIG = new String[]{
            "org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration",
            "org.springdoc.webmvc.ui.SwaggerConfig",
    };

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (Boolean.parseBoolean(environment.getProperty("autumn.swagger.enabled", "true"))) {
            return;
        }
        Map<String, Object> patch = new HashMap<>(4);
        patch.put("springdoc.api-docs.enabled", "false");
        patch.put("springdoc.swagger-ui.enabled", "false");
        patch.put(EXCLUDE_PROP, mergeExclude(environment.getProperty(EXCLUDE_PROP)));
        environment.getPropertySources().addFirst(new MapPropertySource("autumnSwaggerDisabled", patch));
    }

    private static String mergeExclude(String existing) {
        StringBuilder sb = new StringBuilder(256);
        if (StringUtils.isNotBlank(existing)) {
            sb.append(existing.trim());
        }
        for (String cls : SPRINGDOC_AUTOCONFIG) {
            if (containsClass(sb.toString(), cls)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(cls);
        }
        return sb.toString();
    }

    private static boolean containsClass(String csv, String className) {
        if (StringUtils.isBlank(csv)) {
            return false;
        }
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

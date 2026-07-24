package cn.org.autumn.node.role;

import cn.org.autumn.config.InterceptorHandler;
import cn.org.autumn.utils.InterceptorUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 按本机 {@code Profile.roles} 限制 WEB / API / 下载面；空 roles 或 ALL 全放行。
 */
@Slf4j
@Component
@Order(50)
public class ServerRoleInterceptor implements HandlerInterceptor, InterceptorHandler {

    private final ServerRolePathClassifier classifier;

    public ServerRoleInterceptor(ServerRolePathClassifier classifier) {
        this.classifier = classifier;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (InterceptorUtils.skip(handler, this.getClass())) {
            return true;
        }
        if (ServerRoleGate.isUnrestricted()) {
            return true;
        }
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        if (StringUtils.isNotBlank(ctx) && uri.startsWith(ctx)) {
            uri = uri.substring(ctx.length());
        }
        if (StringUtils.isBlank(uri)) {
            uri = "/";
        }
        String need = classifier.requiredCapability(uri);
        if (need == null) {
            return true;
        }
        if (ServerRoleGate.hasCapability(need)) {
            return true;
        }
        if (log.isDebugEnabled()) {
            log.debug("ServerRole deny path={} need={} roles={}", uri, need, ServerRoleGate.currentRoles());
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        String body = "{\"code\":403,\"msg\":\"本节点服务器角色不允许访问该路径\"}";
        response.getWriter().write(body);
        return false;
    }

    @Override
    public HandlerInterceptor getHandlerInterceptor() {
        return this;
    }

    @Override
    public List<String> getPatterns() {
        return List.of("/**");
    }

    @Override
    public List<String> getExcludePatterns() {
        return List.of(
                "/install", "/install/**",
                "/client/**",
                "/actuator/**",
                "/error", "/error/**",
                "/favicon.ico");
    }
}

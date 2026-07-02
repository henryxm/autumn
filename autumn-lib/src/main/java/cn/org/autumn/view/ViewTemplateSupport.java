package cn.org.autumn.view;

import cn.org.autumn.config.ViewHandler;
import cn.org.autumn.install.InstallMode;
import cn.org.autumn.site.ViewFactory;
import cn.org.autumn.utils.SpringContextUtils;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import java.io.IOException;
import java.net.URL;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

/**
 * 视图名解析核心：模板是否存在、路径规范化、缺失时统一回落 {@link #FALLBACK_404_VIEW}。
 * <p>
 * <b>背景</b>：{@code SysPageController}、SPM {@code resourceId} 等常返回以 {@code /} 开头的视图名
 *（如 {@code /modules/foo/pages/index}）。Spring {@code UrlBasedViewResolver} 将其当作 servlet 转发路径；
 * 模板不存在时转发 URL 与当前请求相同，触发 {@code Circular view path}，浏览器 HTTP 500 而非 404。
 * <p>
 * <b>三层防护</b>（共用本类 {@link #resolve(String)}，业务 Controller 无需改动）：
 * <ol>
 *   <li>{@link cn.org.autumn.handler.ViewNameReturnValueHandler} — 主入口，{@code @Controller} 返回 String 时拦截</li>
 *   <li>{@link NameBasedViewResolver} — FreeMarker 视图解析阶段兜底</li>
 *   <li>{@link cn.org.autumn.handler.FreemarkerViewExceptionResolver} — 渲染异常最后兜底</li>
 * </ol>
 * <p>
 * 本类为上述组件的唯一逻辑来源。{@link FreeMarkerConfigurer} 使用 {@code @Lazy}，
 * 且不在构造期注入 {@link TemplateLoader}，避免与
 * {@link cn.org.autumn.site.TemplateFactory#getTemplateLoader()} 形成启动期循环依赖。
 */
@Slf4j
@Component
public class ViewTemplateSupport {

    /** 框架内置 404 页面视图名，对应 {@code templates/404.html}。 */
    public static final String FALLBACK_404_VIEW = "404";

    @Autowired(required = false)
    @Lazy
    private FreeMarkerConfigurer freeMarkerConfigurer;

    @Autowired(required = false)
    private ViewFactory viewFactory;

    @Value("${spring.freemarker.suffix:.html}")
    private String viewSuffix;

    /** 运行时缓存，避免每次 {@link #exists(String)} 重复查 Bean。 */
    private volatile TemplateLoader cachedTemplateLoader;

    /**
     * 统一入口：校验模板并规范化视图名。
     *
     * @return {@link ResolvedView#isNotFound()} 为 {@code true} 时，调用方应设 HTTP 404 并渲染 {@link #FALLBACK_404_VIEW}
     */
    public ResolvedView resolve(String viewName) {
        if (StringUtils.isBlank(viewName)) {
            return ResolvedView.notFound();
        }
        if (isSpecialViewName(viewName)) {
            return ResolvedView.ok(viewName);
        }
        if (!exists(viewName)) {
            if (log.isDebugEnabled()) {
                log.debug("View template missing, fallback to 404: {}", viewName);
            }
            return ResolvedView.notFound();
        }
        return ResolvedView.ok(normalizeViewName(viewName));
    }

    /**
     * 判断 FreeMarker 模板是否存在于 classpath / 动态 TemplateLoader。
     * <p>
     * 安装向导阶段（{@link InstallMode#isActive()}）跳过探测：此时尚无完整业务表与模板索引，强行校验会误拦合法页面。
     */
    public boolean exists(String viewName) {
        if (StringUtils.isBlank(viewName) || isSpecialViewName(viewName)) {
            return true;
        }
        if (InstallMode.isActive()) {
            return true;
        }
        String templatePath = toTemplatePath(viewName);
        if (templatePath == null) {
            return true;
        }
        TemplateLoader loader = resolveTemplateLoader();
        if (loader == null) {
            return classpathTemplateExists(templatePath);
        }
        try {
            return loader.findTemplateSource(templatePath) != null;
        } catch (IOException e) {
            if (log.isDebugEnabled()) {
                log.debug("Template lookup failed for {}: {}", templatePath, e.getMessage());
            }
            return false;
        }
    }

    /**
     * 去掉前导 {@code /}。
     * <p>
     * 必要：Spring MVC 对以 {@code /} 开头的视图名按「绝对转发路径」处理，与当前请求 URI 相同时抛出
     * {@code Circular view path}；去掉前导斜杠后走 FreeMarker 模板名解析（{@code modules/.../index.html}）。
     */
    public String normalizeViewName(String viewName) {
        if (StringUtils.isBlank(viewName) || isSpecialViewName(viewName)) {
            return viewName;
        }
        if (viewName.startsWith("/")) {
            return viewName.substring(1);
        }
        return viewName;
    }

    /**
     * 将控制器视图名映射为 {@link TemplateLoader#findTemplateSource(String)} 使用的路径。
     * <p>
     * 无后缀视图（如 {@code .js}）由 {@link ViewHandler}（如 {@code NoneSuffixViewHandler}）决定，不再追加 {@code .html}。
     */
    public String toTemplatePath(String viewName) {
        if (StringUtils.isBlank(viewName) || isSpecialViewName(viewName)) {
            return null;
        }
        String name = normalizeViewName(viewName);
        if (!InstallMode.isActive() && viewFactory != null) {
            ViewHandler viewHandler = viewFactory.getShould(name);
            if (viewHandler != null) {
                return name;
            }
        }
        String suffix = StringUtils.defaultString(viewSuffix, ".html");
        if (StringUtils.isNotBlank(suffix) && !name.endsWith(suffix)) {
            return name + suffix;
        }
        return name;
    }

    /** {@code redirect:/}、{@code forward:/} 及已是 404 回落页的名称，不参与存在性校验。 */
    public boolean isSpecialViewName(String viewName) {
        if (StringUtils.isBlank(viewName)) {
            return true;
        }
        return viewName.startsWith("redirect:") || viewName.startsWith("forward:") || FALLBACK_404_VIEW.equals(viewName);
    }

    /**
     * 懒加载 TemplateLoader：优先 FreeMarker 配置，其次 Spring 容器。
     * 不在字段上 {@code @Autowired TemplateLoader}，避免拉长 PageFactory → SPM → WebConfig 启动链。
     */
    private TemplateLoader resolveTemplateLoader() {
        if (cachedTemplateLoader != null) {
            return cachedTemplateLoader;
        }
        if (freeMarkerConfigurer != null) {
            Configuration configuration = freeMarkerConfigurer.getConfiguration();
            if (configuration != null && configuration.getTemplateLoader() != null) {
                cachedTemplateLoader = configuration.getTemplateLoader();
                return cachedTemplateLoader;
            }
        }
        ApplicationContext applicationContext = SpringContextUtils.getApplicationContext();
        if (applicationContext != null) {
            try {
                cachedTemplateLoader = applicationContext.getBean(TemplateLoader.class);
                return cachedTemplateLoader;
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /** TemplateLoader 尚未就绪时的 classpath 回退（多 ClassLoader、{@code templates/} 前缀）。 */
    private boolean classpathTemplateExists(String templatePath) {
        if (StringUtils.isBlank(templatePath)) {
            return false;
        }
        String normalized = templatePath.startsWith("/") ? templatePath.substring(1) : templatePath;
        String[] candidates = new String[] {"templates/" + normalized, normalized};
        ClassLoader[] loaders = new ClassLoader[] {Thread.currentThread().getContextClassLoader(), getClass().getClassLoader()};
        for (String candidate : candidates) {
            for (ClassLoader loader : loaders) {
                if (loader == null) {
                    continue;
                }
                URL url = loader.getResource(candidate);
                if (url != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /** {@link #resolve(String)} 的结果：最终视图名 + 是否应返回 HTTP 404。 */
    @Getter
    public static final class ResolvedView {
        private final String viewName;
        private final boolean notFound;

        private ResolvedView(String viewName, boolean notFound) {
            this.viewName = viewName;
            this.notFound = notFound;
        }

        public static ResolvedView ok(String viewName) {
            return new ResolvedView(viewName, false);
        }

        public static ResolvedView notFound() {
            return new ResolvedView(FALLBACK_404_VIEW, true);
        }
    }
}

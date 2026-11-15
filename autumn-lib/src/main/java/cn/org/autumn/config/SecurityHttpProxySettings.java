package cn.org.autumn.config;

import java.util.List;

/**
 * HTTP 代理出站安全设置（由 SysConfig {@code SECURITY_HTTP_PROXY_CONFIG} 提供）。
 * <p>
 * 放在 autumn-lib 供 {@code BaseHttpProxyService} 注入，避免反射跨模块调 SysConfigService。
 */
public interface SecurityHttpProxySettings {

    boolean isEnabled();

    boolean isResolveDns();

    List<String> hostList();
}

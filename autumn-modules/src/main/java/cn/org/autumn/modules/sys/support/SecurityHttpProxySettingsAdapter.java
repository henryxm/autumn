package cn.org.autumn.modules.sys.support;

import cn.org.autumn.config.SecurityHttpProxySettings;
import cn.org.autumn.model.SecurityHttpProxyConfig;
import cn.org.autumn.modules.sys.service.SysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 将 {@link SysConfigService#getSecurityHttpProxyConfig()} 暴露为 lib 侧 {@link SecurityHttpProxySettings}。
 */
@Component
public class SecurityHttpProxySettingsAdapter implements SecurityHttpProxySettings {

    @Autowired
    @Lazy
    private SysConfigService sysConfigService;

    private SecurityHttpProxyConfig cfg() {
        return sysConfigService != null
                ? sysConfigService.getSecurityHttpProxyConfig()
                : new SecurityHttpProxyConfig();
    }

    @Override
    public boolean isEnabled() {
        return cfg().isEnabled();
    }

    @Override
    public boolean isResolveDns() {
        return cfg().isResolveDns();
    }

    @Override
    public List<String> hostList() {
        List<String> list = cfg().hostList();
        return list != null ? list : Collections.emptyList();
    }
}

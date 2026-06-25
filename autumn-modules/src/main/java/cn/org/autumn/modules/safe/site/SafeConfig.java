package cn.org.autumn.modules.safe.site;

import cn.org.autumn.config.FilterChainHandler;
import cn.org.autumn.model.PayCredentialConfig;
import cn.org.autumn.modules.sys.service.SysConfigService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class SafeConfig implements FilterChainHandler {

    @Autowired
    @Lazy
    private SysConfigService sysConfigService;

    public PayCredentialConfig get() {
        PayCredentialConfig config = sysConfigService.getConfigObjectValidate(PayCredentialConfig.CONFIG_KEY, PayCredentialConfig.class);
        return config == null ? new PayCredentialConfig() : config;
    }

    @Override
    public void definition(Map<String, String> map) {
        map.put("/safe/api/v1/**", "anon");
    }
}

package cn.org.autumn.modules.safe.service;

import cn.org.autumn.model.PayCredentialConfig;
import cn.org.autumn.modules.sys.service.SysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class PayCredentialConfigService {

    @Autowired
    @Lazy
    private SysConfigService sysConfigService;

    public PayCredentialConfig get() {
        PayCredentialConfig config = sysConfigService.getConfigObjectValidate(PayCredentialConfig.CONFIG_KEY, PayCredentialConfig.class);
        return config == null ? new PayCredentialConfig() : config;
    }
}

package cn.org.autumn.modules.sys.redis;

import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.utils.RedisKeys;
import cn.org.autumn.modules.sys.entity.SysConfigEntity;
import cn.org.autumn.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SysConfigRedis {
    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    SysConfigService sysConfigService;

    public void saveOrUpdate(SysConfigEntity config) {
        if (config == null) {
            return;
        }
        String namespace = sysConfigService.getNameSpace();
        String key = RedisKeys.getSysConfigKey(namespace, config.getParamKey());
        redisUtils.set(key, config, 120);
    }

    public void delete(String configKey) {
        String namespace = sysConfigService.getNameSpace();
        String key = RedisKeys.getSysConfigKey(namespace, configKey);
        redisUtils.delete(key);
    }

    public SysConfigEntity get(String configKey) {
        String namespace = sysConfigService.getNameSpace();
        String key = RedisKeys.getSysConfigKey(namespace, configKey);
        Object o = redisUtils.get(key);
        if (o instanceof SysConfigEntity) {
            return (SysConfigEntity) o;
        }
        return null;
    }
}

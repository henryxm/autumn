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
        String key = RedisKeys.getSysConfigKey(sysConfigService.getNameSpace(), config.getParamKey());
        redisUtils.set(key, config, 120);
    }

    public void delete(String configKey) {
        redisUtils.delete(RedisKeys.getSysConfigKey(sysConfigService.getNameSpace(), configKey));
    }

    public SysConfigEntity get(String configKey) {
        Object o = redisUtils.get(RedisKeys.getSysConfigKey(sysConfigService.getNameSpace(), configKey));
        if (o instanceof SysConfigEntity) {
            return (SysConfigEntity) o;
        }
        return null;
    }
}

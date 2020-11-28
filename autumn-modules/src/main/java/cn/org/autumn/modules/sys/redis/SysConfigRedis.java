package cn.org.autumn.modules.sys.redis;

import cn.org.autumn.utils.RedisKeys;
import cn.org.autumn.modules.sys.entity.SysConfigEntity;
import cn.org.autumn.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SysConfigRedis {
    @Autowired
    private RedisUtils redisUtils;

    public void saveOrUpdate(SysConfigEntity config) {
        if (config == null) {
            return;
        }
        String key = RedisKeys.getSysConfigKey(config.getParamKey());
        redisUtils.set(key, config);
    }

    public void delete(String configKey) {
        String key = RedisKeys.getSysConfigKey(configKey);
        redisUtils.delete(key);
    }

    public SysConfigEntity get(String configKey) {
        String key = RedisKeys.getSysConfigKey(configKey);
        Object o = redisUtils.get(key);
        if (o instanceof SysConfigEntity) {
            return (SysConfigEntity) o;
        }
        return null;
    }
}

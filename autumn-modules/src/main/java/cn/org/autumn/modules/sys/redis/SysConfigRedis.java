package cn.org.autumn.modules.sys.redis;

import cn.org.autumn.modules.sys.entity.SysConfigEntity;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.utils.RedisKeys;
import cn.org.autumn.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SysConfigRedis {
    @Autowired
    @Lazy
    private RedisUtils redisUtils;

    @Autowired
    @Lazy
    SysConfigService sysConfigService;

    public void saveOrUpdate(SysConfigEntity config) {
        if (config == null) {
            return;
        }
        String key = RedisKeys.getSysConfigKey(sysConfigService.getNameSpace(), config.getParamKey());
        String value = config.getParamValue() == null ? "" : config.getParamValue();
        redisUtils.set(key, value, 600);
    }

    public void delete(String configKey) {
        redisUtils.delete(RedisKeys.getSysConfigKey(sysConfigService.getNameSpace(), configKey));
    }

    /**
     * 仅缓存 paramValue String；遗留非 String 类型删键回空，由调用方回源 DB。
     */
    public SysConfigEntity get(String configKey) {
        String redisKey = RedisKeys.getSysConfigKey(sysConfigService.getNameSpace(), configKey);
        Object o;
        try {
            o = redisUtils.get(redisKey);
        } catch (Exception e) {
            log.warn("SysConfigRedis get fail key={} msg={}", configKey, e.getMessage());
            return null;
        }
        if (o == null) {
            return null;
        }
        if (!(o instanceof String)) {
            log.warn("SysConfigRedis unexpected type key={} type={} purge", configKey, o.getClass().getName());
            redisUtils.delete(redisKey);
            return null;
        }
        SysConfigEntity entity = new SysConfigEntity();
        entity.setParamKey(configKey);
        entity.setParamValue((String) o);
        return entity;
    }
}

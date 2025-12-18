package cn.org.autumn.service;

import cn.org.autumn.config.CacheConfig;
import cn.org.autumn.model.KeyPair;
import cn.org.autumn.utils.RsaUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RsaService {

    @Autowired
    CacheService cacheService;

    private static final CacheConfig config = CacheConfig.builder().cacheName("RsaServiceCache").valueType(KeyPair.class).build();

    public KeyPair getKeyPair() {
        KeyPair pair = RsaUtil.generate();

        KeyPair result = cacheService.compute("user", RsaUtil::generate, config);

        return pair;
    }
}

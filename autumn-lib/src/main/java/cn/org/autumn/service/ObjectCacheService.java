package cn.org.autumn.service;

import cn.org.autumn.model.DefaultEntity;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ObjectCacheService extends ShareCacheService<DefaultMapper, DefaultEntity> {
    public Class<?> getModelClass() {
        return DefaultEntity.class;
    }
}

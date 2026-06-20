package cn.org.autumn.service;

import cn.org.autumn.entity.SnowBased;
import cn.org.autumn.entity.UuidBased;
import cn.org.autumn.utils.Snow;
import cn.org.autumn.utils.Uuid;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * 第二主键（{@code uuid}）自动填充基类：在 CRUD 写路径上，若实体实现了标记接口且 {@code uuid} 尚未赋值，则自动生成。
 * <p>
 * <strong>适用实体</strong>：
 * <ul>
 *   <li>{@link UuidBased} → {@link Uuid#uuid()}（32 位小写十六进制字符串）</li>
 *   <li>{@link SnowBased} → {@link Snow#uuid()}（64 位雪花 Long）</li>
 * </ul>
 * <strong>不覆盖</strong>业务侧已显式写入的非空 {@code uuid}。
 * <p>
 * <strong>继承链</strong>（业务 Service 默认已具备本能力，无需重复实现）：
 * <pre>
 * ModuleService → BaseService → DistributedService → ShareCacheService
 *     → BaseCacheService → BaseQueueService → AutoIdService → DialectService
 * </pre>
 * 仅需让实体 {@code implements UuidBased} 或 {@code implements SnowBased}，经 {@code insert*} /
 * {@code insertOrUpdate*} / {@code update*} 写库时即自动补全。
 * <p>
 * <strong>非继承链场景</strong>：在持久化前手动调用 {@link #autoId(Object)} 或 {@link #autoId(List)}。
 * <p>
 * <strong>纪律</strong>：自增 {@code id} 仍仅服务后台生成 CRUD；关联列、API、缓存须存对方实体的 {@code uuid}，
 * 禁止以 {@code id} 作外键或对外资源标识。详见 {@code docs/AI_DUAL_KEY.md}、{@code docs/AI_STANDARDS.md} §10.4。
 *
 * @see UuidBased
 * @see SnowBased
 * @see Uuid
 * @see Snow
 */
public class AutoIdService<M extends BaseMapper<T>, T> extends DialectService<M, T> {

    /**
     * 为单条实体补全第二主键（仅当 {@code uuid} 为空 / 0 时写入）。
     */
    public static <T> void autoId(T entity) {
        if (entity instanceof UuidBased) {
            UuidBased based = (UuidBased) entity;
            if (StringUtils.isBlank(based.getUuid()))
                based.setUuid(Uuid.uuid());
        }
        if (entity instanceof SnowBased) {
            SnowBased based = (SnowBased) entity;
            if (null == based.getUuid() || 0L == based.getUuid())
                based.setUuid(Snow.uuid());
        }
    }

    /**
     * 批量为实体列表补全第二主键。
     */
    public static <T> void autoId(List<T> entities) {
        for (T entity : entities) {
            autoId(entity);
        }
    }

    @Override
    public boolean insert(T entity) {
        autoId(entity);
        return super.insert(entity);
    }

    @Override
    public boolean insertAllColumn(T entity) {
        autoId(entity);
        return super.insertAllColumn(entity);
    }

    @Override
    public boolean insertBatch(List<T> entities) {
        autoId(entities);
        return super.insertBatch(entities);
    }

    @Override
    public boolean insertBatch(List<T> entities, int batchSize) {
        autoId(entities);
        return super.insertBatch(entities, batchSize);
    }

    @Override
    public boolean insertOrUpdate(T entity) {
        autoId(entity);
        return super.insertOrUpdate(entity);
    }

    @Override
    public boolean insertOrUpdateAllColumn(T entity) {
        autoId(entity);
        return super.insertOrUpdateAllColumn(entity);
    }

    @Override
    public boolean insertOrUpdateBatch(List<T> entities) {
        autoId(entities);
        return super.insertOrUpdateBatch(entities);
    }

    @Override
    public boolean insertOrUpdateBatch(List<T> entities, int batchSize) {
        autoId(entities);
        return super.insertOrUpdateBatch(entities, batchSize);
    }

    @Override
    public boolean insertOrUpdateAllColumnBatch(List<T> entities) {
        autoId(entities);
        return super.insertOrUpdateAllColumnBatch(entities);
    }

    @Override
    public boolean insertOrUpdateAllColumnBatch(List<T> entities, int batchSize) {
        autoId(entities);
        return super.insertOrUpdateAllColumnBatch(entities, batchSize);
    }

    @Override
    public boolean updateById(T entity) {
        autoId(entity);
        return super.updateById(entity);
    }

    @Override
    public boolean updateAllColumnById(T entity) {
        autoId(entity);
        return super.updateAllColumnById(entity);
    }

    @Override
    public boolean update(T entity, Wrapper<T> wrapper) {
        autoId(entity);
        return super.update(entity, wrapper);
    }

    @Override
    public boolean updateBatchById(List<T> entities) {
        autoId(entities);
        return super.updateBatchById(entities);
    }

    @Override
    public boolean updateBatchById(List<T> entities, int batchSize) {
        autoId(entities);
        return super.updateBatchById(entities, batchSize);
    }

    @Override
    public boolean updateAllColumnBatchById(List<T> entities) {
        autoId(entities);
        return super.updateAllColumnBatchById(entities);
    }

    @Override
    public boolean updateAllColumnBatchById(List<T> entities, int batchSize) {
        autoId(entities);
        return super.updateAllColumnBatchById(entities, batchSize);
    }
}

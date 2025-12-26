package cn.org.autumn.modules.oauth.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.config.EncryptionLoader;
import cn.org.autumn.config.EncryptConfigHandler;
import cn.org.autumn.model.AesKey;
import cn.org.autumn.model.RsaKey;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.oauth.dao.EncryptKeyDao;
import cn.org.autumn.modules.oauth.entity.EncryptKeyEntity;
import cn.org.autumn.site.EncryptConfigFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 加密密钥服务
 * 用于保存和管理RSA和AES密钥信息
 *
 * @author Autumn
 */
@Slf4j
@Service
public class EncryptKeyService extends ModuleService<EncryptKeyDao, EncryptKeyEntity> implements EncryptionLoader, LoopJob.OneDay {

    @Autowired
    private EncryptConfigFactory encryptConfigFactory;

    /**
     * 保存或更新加密密钥信息
     * 根据session查询现有记录，如果存在则更新，不存在则插入
     *
     * @param session         客户端会话ID
     * @param serverRsaKey    服务端RSA密钥对
     * @param clientPublicKey 客户端公钥
     * @param aesKey          AES密钥信息
     * @return 保存后的实体
     */
    @Transactional(rollbackFor = Exception.class)
    public EncryptKeyEntity saveOrUpdate(String session, RsaKey serverRsaKey, String clientPublicKey, AesKey aesKey) {
        try {
            if (StringUtils.isBlank(session)) {
                throw new IllegalArgumentException("session不能为空");
            }
            EncryptKeyEntity entity = getBySession(session);
            if (entity == null) {
                entity = new EncryptKeyEntity();
                entity.setSession(session);
                entity.setCreate(new Date());
            }
            // RSA密钥对
            if (serverRsaKey != null) {
                entity.setPublicKey(serverRsaKey.getPublicKey());
                entity.setPrivateKey(serverRsaKey.getPrivateKey());
                // 使用RSA密钥的过期时间，如果AES也有过期时间，使用较早的那个
                if (serverRsaKey.getExpireTime() != null) {
                    Date rsaExpire = new Date(serverRsaKey.getExpireTime());
                    if (entity.getExpire() == null || rsaExpire.before(entity.getExpire())) {
                        entity.setExpire(rsaExpire);
                    }
                }
            }
            // 客户端公钥
            if (StringUtils.isNotBlank(clientPublicKey)) {
                entity.setClientKey(clientPublicKey);
            }
            // AES密钥
            if (aesKey != null) {
                entity.setAesKey(aesKey.getKey());
                entity.setAesIv(aesKey.getVector());
                // 使用AES密钥的过期时间，如果RSA也有过期时间，使用较早的那个
                if (aesKey.getExpireTime() != null) {
                    Date aesExpire = new Date(aesKey.getExpireTime());
                    if (entity.getExpire() == null || aesExpire.before(entity.getExpire())) {
                        entity.setExpire(aesExpire);
                    }
                }
            }
            insertOrUpdate(entity);
            return entity;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 根据session查询加密密钥信息
     *
     * @param session 客户端会话ID
     * @return 加密密钥实体，如果不存在则返回null
     */
    public EncryptKeyEntity getBySession(String session) {
        if (StringUtils.isBlank(session)) {
            return null;
        }
        return baseMapper.getBySession(session);
    }

    /**
     * 从数据库加载AES密钥
     * 实现EncryptKeyLoader接口
     *
     * @param session 客户端会话ID
     * @return AES密钥对象，如果不存在则返回null
     */
    @Override
    public AesKey loadAesKey(String session) {
        try {
            if (StringUtils.isBlank(session)) {
                return null;
            }
            EncryptKeyEntity entity = getBySession(session);
            if (entity == null || StringUtils.isBlank(entity.getAesKey()) || StringUtils.isBlank(entity.getAesIv())) {
                return null;
            }
            // 构建AesKey对象
            AesKey aesKey = new AesKey();
            aesKey.setSession(session);
            aesKey.setKey(entity.getAesKey());
            aesKey.setVector(entity.getAesIv());
            // 设置过期时间
            if (entity.getExpire() != null) {
                aesKey.setExpireTime(entity.getExpire().getTime());
            }
            if (log.isDebugEnabled()) {
                log.debug("从数据库加载AES密钥成功，session: {}", session);
            }
            return aesKey;
        } catch (Exception e) {
            log.error("从数据库加载AES密钥失败，session: {}, 错误: {}", session, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从数据库加载RSA密钥对
     * 实现EncryptKeyLoader接口
     *
     * @param session 客户端会话ID
     * @return RSA密钥对对象，如果不存在则返回null
     */
    @Override
    public RsaKey loadRsaKey(String session) {
        try {
            if (StringUtils.isBlank(session)) {
                return null;
            }
            EncryptKeyEntity entity = getBySession(session);
            if (entity == null || StringUtils.isBlank(entity.getPublicKey()) || StringUtils.isBlank(entity.getPrivateKey())) {
                return null;
            }
            // 构建RsaKey对象
            RsaKey rsaKey = new RsaKey();
            rsaKey.setSession(session);
            rsaKey.setPublicKey(entity.getPublicKey());
            rsaKey.setPrivateKey(entity.getPrivateKey());
            // 设置过期时间
            if (entity.getExpire() != null) {
                rsaKey.setExpireTime(entity.getExpire().getTime());
            }
            if (log.isDebugEnabled()) {
                log.debug("从数据库加载RSA密钥对成功，session: {}", session);
            }
            return rsaKey;
        } catch (Exception e) {
            log.error("从数据库加载RSA密钥对失败，session: {}, 错误: {}", session, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从数据库加载客户端公钥
     * 实现EncryptKeyLoader接口
     *
     * @param session 客户端会话ID
     * @return 客户端公钥字符串，如果不存在则返回null
     */
    @Override
    public String loadClientPublicKey(String session) {
        if (StringUtils.isBlank(session)) {
            return null;
        }
        try {
            EncryptKeyEntity entity = getBySession(session);
            if (entity == null || StringUtils.isBlank(entity.getClientKey())) {
                return null;
            }
            if (log.isDebugEnabled()) {
                log.debug("从数据库加载客户端公钥成功，session: {}", session);
            }
            return entity.getClientKey();
        } catch (Exception e) {
            log.error("从数据库加载客户端公钥失败，session: {}, 错误: {}", session, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void onOneDay() {
        cleanExpiredKeys();
    }

    /**
     * 清理过期的加密密钥记录
     * 清理条件：expire（过期时间）+ AES Config中的serverBufferMinutes（服务端冗余保留时间）
     * 如果当前时间超过了这两个时间之和，则删除该记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void cleanExpiredKeys() {
        try {
            // 获取AES配置
            EncryptConfigHandler.AesConfig aesConfig = encryptConfigFactory.getAesConfig();
            // 如果无法获取配置，使用默认值60分钟
            int serverBufferMinutes = aesConfig != null ? aesConfig.getServerBufferMinutes() : 60;
            // 计算清理时间点：当前时间 - serverBufferMinutes
            // 如果记录的expire时间 + serverBufferMinutes < 当前时间，则清理
            // 即：expire < 当前时间 - serverBufferMinutes
            // 数据库清理记录再额外多增加一天缓冲时间
            Date cleanBeforeTime = new Date(System.currentTimeMillis() - (serverBufferMinutes * 60 * 1000L) - (24 * 60 * 1000L));
            // 直接使用SQL删除过期记录
            int deletedCount = baseMapper.deleteExpiredKeys(cleanBeforeTime);
            if (deletedCount > 0) {
                log.info("清理过期加密密钥记录完成，共清理 {} 条记录，清理时间点: {}", deletedCount, cleanBeforeTime);
            }
        } catch (Exception e) {
            log.error("清理过期加密密钥记录失败: {}", e.getMessage(), e);
        }
    }
}
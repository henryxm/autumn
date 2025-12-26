package cn.org.autumn.modules.oauth.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.model.AesKey;
import cn.org.autumn.model.RsaKey;
import cn.org.autumn.modules.oauth.dao.EncryptKeyDao;
import cn.org.autumn.modules.oauth.entity.EncryptKeyEntity;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 加密密钥服务
 * 用于保存和管理RSA和AES密钥信息
 *
 * @author Autumn
 */
@Slf4j
@Service
public class EncryptKeyService extends ModuleService<EncryptKeyDao, EncryptKeyEntity> {

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
}
package cn.org.autumn.modules.usr.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.config.AccountHandler;
import cn.org.autumn.modules.usr.dao.UserOpenDao;
import cn.org.autumn.modules.usr.entity.UserOpenEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 系统对接服务
 * 用于管理微信和支付宝等第三方平台的用户绑定关系
 * <p>
 * 平台特性说明：
 * - openid: 同一用户在不同应用下有不同的openid
 * - unionid: 同一用户在同一平台下有相同的unionid
 * - platform: 平台类型，alipay(支付宝) 或 wechat(微信)
 * - appid: 应用ID，用于区分不同的应用
 *
 * @author User
 * @date 2025-12
 */
@Service
public class UserOpenService extends ModuleService<UserOpenDao, UserOpenEntity> implements AccountHandler {

    /**
     * 平台类型常量
     */
    public static final String PLATFORM_ALIPAY = "alipay";
    public static final String PLATFORM_WECHAT = "wechat";

    /**
     * 注销账号，进行逻辑删除
     * 当用户注销账号时，将该用户的所有第三方平台绑定关系标记为已删除（逻辑删除）
     * 保留数据记录，但标记为已删除状态，后续查询时可以通过deleted字段过滤
     *
     * @param obj 用户对象，包含用户UUID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void canceled(User obj) {
        if (obj == null || StringUtils.isBlank(obj.getUuid())) {
            return;
        }
        // 查询该用户的所有绑定关系
        List<UserOpenEntity> list = getByUuid(obj.getUuid());
        if (list == null || list.isEmpty()) {
            return;
        }
        // 批量进行逻辑删除
        Date now = new Date();
        for (UserOpenEntity entity : list) {
            // 只处理未删除的记录
            if (!entity.isDeleted()) {
                entity.setDeleted(true);
                entity.setUpdate(now);
                this.updateById(entity);
            }
        }
    }

    /**
     * 移除账号，进行物理删除
     * 当用户被彻底删除时，物理删除该用户的所有第三方平台绑定关系
     * 数据将被永久删除，无法恢复
     *
     * @param obj 用户对象，包含用户UUID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removed(User obj) {
        if (obj == null || StringUtils.isBlank(obj.getUuid())) {
            return;
        }
        // 查询该用户的所有绑定关系（包括已逻辑删除的）
        List<UserOpenEntity> list = baseMapper.getAllByUuid(obj.getUuid());
        if (list == null || list.isEmpty()) {
            return;
        }
        // 批量物理删除
        for (UserOpenEntity entity : list) {
            this.removeById(entity.getId());
        }
    }

    /**
     * 根据openid、platform和appid精确查询
     * 这是最精确的查询方式，用于确定某个应用下的特定用户
     *
     * @param openid   开放ID
     * @param platform 平台类型（alipay或wechat）
     * @param appid    应用ID
     * @return 绑定关系实体，如果不存在返回null
     */
    public UserOpenEntity getByOpenidAndPlatformAndAppid(String openid, String platform, String appid) {
        if (StringUtils.isBlank(openid) || StringUtils.isBlank(platform) || StringUtils.isBlank(appid)) {
            return null;
        }
        return baseMapper.getByOpenidAndPlatformAndAppid(openid, platform, appid);
    }

    /**
     * 根据openid和platform查询（不指定appid）
     * 可能返回多个结果，因为同一用户在不同应用下有不同的openid
     *
     * @param openid   开放ID
     * @param platform 平台类型
     * @return 绑定关系列表
     */
    public List<UserOpenEntity> getByOpenidAndPlatform(String openid, String platform) {
        if (StringUtils.isBlank(openid) || StringUtils.isBlank(platform)) {
            return null;
        }
        return baseMapper.getByOpenidAndPlatform(openid, platform);
    }

    /**
     * 根据unionid和platform查询
     * 用于查询同一平台下的所有应用绑定关系
     * 因为unionid在同一平台下是唯一的，可以跨应用识别同一用户
     *
     * @param unionid  联合ID
     * @param platform 平台类型
     * @return 绑定关系列表
     */
    public List<UserOpenEntity> getByUnionidAndPlatform(String unionid, String platform) {
        if (StringUtils.isBlank(unionid) || StringUtils.isBlank(platform)) {
            return null;
        }
        return baseMapper.getByUnionidAndPlatform(unionid, platform);
    }

    /**
     * 根据unionid、platform和appid查询
     * 用于查询特定应用下的unionid绑定关系
     *
     * @param unionid  联合ID
     * @param platform 平台类型
     * @param appid    应用ID
     * @return 绑定关系实体，如果不存在返回null
     */
    public UserOpenEntity getByUnionidAndPlatformAndAppid(String unionid, String platform, String appid) {
        if (StringUtils.isBlank(unionid) || StringUtils.isBlank(platform) || StringUtils.isBlank(appid)) {
            return null;
        }
        return baseMapper.getByUnionidAndPlatformAndAppid(unionid, platform, appid);
    }

    /**
     * 根据uuid查询用户的所有平台绑定关系
     * 用于查询某个系统用户绑定了哪些第三方平台
     *
     * @param uuid 系统用户UUID
     * @return 绑定关系列表
     */
    public List<UserOpenEntity> getByUuid(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return null;
        }
        return baseMapper.getByUuid(uuid);
    }

    /**
     * 根据uuid和platform查询
     * 用于查询用户在某个平台下的所有应用绑定
     *
     * @param uuid     系统用户UUID
     * @param platform 平台类型
     * @return 绑定关系列表
     */
    public List<UserOpenEntity> getByUuidAndPlatform(String uuid, String platform) {
        if (StringUtils.isBlank(uuid) || StringUtils.isBlank(platform)) {
            return null;
        }
        return baseMapper.getByUuidAndPlatform(uuid, platform);
    }

    /**
     * 根据openid查询（跨平台查询）
     * 用于查询某个openid在所有平台下的绑定关系
     *
     * @param openid 开放ID
     * @return 绑定关系列表
     */
    public List<UserOpenEntity> getByOpenid(String openid) {
        if (StringUtils.isBlank(openid)) {
            return null;
        }
        return baseMapper.getByOpenid(openid);
    }

    /**
     * 根据unionid查询（跨平台查询）
     * 用于查询某个unionid在所有平台下的绑定关系
     *
     * @param unionid 联合ID
     * @return 绑定关系列表
     */
    public List<UserOpenEntity> getByUnionid(String unionid) {
        if (StringUtils.isBlank(unionid)) {
            return null;
        }
        return baseMapper.getByUnionid(unionid);
    }

    /**
     * 保存或更新绑定关系
     * 如果已存在相同的openid+platform+appid组合，则更新；否则创建新记录
     *
     * @param entity 绑定关系实体
     * @return 保存后的实体
     */
    @Transactional(rollbackFor = Exception.class)
    public UserOpenEntity saveOrUpdateEntity(UserOpenEntity entity) {
        if (entity == null) {
            return null;
        }
        // 检查必填字段
        if (StringUtils.isBlank(entity.getOpenid()) ||
                StringUtils.isBlank(entity.getPlatform()) ||
                StringUtils.isBlank(entity.getAppid())) {
            throw new IllegalArgumentException("openid、platform和appid不能为空");
        }
        // 查询是否已存在
        UserOpenEntity existing = getByOpenidAndPlatformAndAppid(entity.getOpenid(), entity.getPlatform(), entity.getAppid());
        Date now = new Date();
        if (existing != null) {
            // 更新现有记录
            entity.setId(existing.getId());
            entity.setUpdate(now);
            // 保留创建时间
            if (entity.getCreate() == null) {
                entity.setCreate(existing.getCreate());
            }
            this.updateById(entity);
            return entity;
        } else {
            // 创建新记录
            entity.setCreate(now);
            entity.setUpdate(now);
            this.save(entity);
            return entity;
        }
    }

    /**
     * 根据openid、platform和appid保存或更新绑定关系
     * 便捷方法，用于快速创建或更新绑定
     *
     * @param uuid     系统用户UUID
     * @param platform 平台类型
     * @param appid    应用ID
     * @param openid   开放ID
     * @param unionid  联合ID（可选）
     * @return 保存后的实体
     */
    @Transactional(rollbackFor = Exception.class)
    public UserOpenEntity saveOrUpdateEntity(String uuid, String platform, String appid, String openid, String unionid) {
        UserOpenEntity entity = new UserOpenEntity();
        entity.setUuid(uuid);
        entity.setPlatform(platform);
        entity.setAppid(appid);
        entity.setOpenid(openid);
        entity.setUnionid(unionid);
        return saveOrUpdateEntity(entity);
    }

    /**
     * 根据openid、platform和appid删除绑定关系
     *
     * @param openid   开放ID
     * @param platform 平台类型
     * @param appid    应用ID
     * @return 是否删除成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByOpenidAndPlatformAndAppid(String openid, String platform, String appid) {
        if (StringUtils.isBlank(openid) || StringUtils.isBlank(platform) || StringUtils.isBlank(appid)) {
            return false;
        }
        return baseMapper.deleteByOpenidAndPlatformAndAppid(openid, platform, appid) > 0;
    }

    /**
     * 根据uuid删除用户的所有绑定关系
     *
     * @param uuid 系统用户UUID
     * @return 是否删除成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByUuid(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return false;
        }
        return baseMapper.deleteByUuid(uuid) > 0;
    }

    /**
     * 根据uuid和platform删除用户在某个平台的所有绑定关系
     *
     * @param uuid     系统用户UUID
     * @param platform 平台类型
     * @return 是否删除成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByUuidAndPlatform(String uuid, String platform) {
        if (StringUtils.isBlank(uuid) || StringUtils.isBlank(platform)) {
            return false;
        }
        return baseMapper.deleteByUuidAndPlatform(uuid, platform) > 0;
    }

    /**
     * 根据unionid和platform删除同一平台下的所有绑定关系
     *
     * @param unionid  联合ID
     * @param platform 平台类型
     * @return 是否删除成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean removeByUnionidAndPlatform(String unionid, String platform) {
        if (StringUtils.isBlank(unionid) || StringUtils.isBlank(platform)) {
            return false;
        }
        return baseMapper.deleteByUnionidAndPlatform(unionid, platform) > 0;
    }

    /**
     * 检查绑定关系是否存在
     *
     * @param openid   开放ID
     * @param platform 平台类型
     * @param appid    应用ID
     * @return 是否存在
     */
    public boolean exists(String openid, String platform, String appid) {
        return getByOpenidAndPlatformAndAppid(openid, platform, appid) != null;
    }

    /**
     * 根据unionid和platform检查是否存在绑定关系
     *
     * @param unionid  联合ID
     * @param platform 平台类型
     * @return 是否存在
     */
    public boolean existsByUnionid(String unionid, String platform) {
        List<UserOpenEntity> list = getByUnionidAndPlatform(unionid, platform);
        return list != null && !list.isEmpty();
    }

    /**
     * 根据uuid检查是否存在绑定关系
     *
     * @param uuid 系统用户UUID
     * @return 是否存在
     */
    public boolean existsByUuid(String uuid) {
        List<UserOpenEntity> list = getByUuid(uuid);
        return list != null && !list.isEmpty();
    }

    /**
     * 根据openid、platform和appid获取系统用户UUID
     * 用于第三方登录时，通过openid查找对应的系统用户
     *
     * @param openid   开放ID
     * @param platform 平台类型
     * @param appid    应用ID
     * @return 系统用户UUID，如果不存在返回null
     */
    public String getUuidByOpenid(String openid, String platform, String appid) {
        UserOpenEntity entity = getByOpenidAndPlatformAndAppid(openid, platform, appid);
        return entity != null ? entity.getUuid() : null;
    }

    /**
     * 根据unionid和platform获取系统用户UUID
     * 优先使用unionid查找，因为unionid在同一平台下是唯一的
     * 如果找到多个绑定关系，返回第一个的UUID
     *
     * @param unionid  联合ID
     * @param platform 平台类型
     * @return 系统用户UUID，如果不存在返回null
     */
    public String getUuidByUnionid(String unionid, String platform) {
        List<UserOpenEntity> list = getByUnionidAndPlatform(unionid, platform);
        if (list != null && !list.isEmpty()) {
            return list.get(0).getUuid();
        }
        return null;
    }

    /**
     * 批量更新unionid
     * 当获取到unionid时，可以批量更新同一平台下所有应用的unionid
     *
     * @param openid   开放ID
     * @param platform 平台类型
     * @param unionid  联合ID
     * @return 更新的记录数
     */
    @Transactional(rollbackFor = Exception.class)
    public int updateUnionidByOpenidAndPlatform(String openid, String platform, String unionid) {
        if (StringUtils.isBlank(openid) || StringUtils.isBlank(platform)) {
            return 0;
        }
        List<UserOpenEntity> list = getByOpenidAndPlatform(openid, platform);
        if (list == null || list.isEmpty()) {
            return 0;
        }
        int count = 0;
        Date now = new Date();
        for (UserOpenEntity entity : list) {
            entity.setUnionid(unionid);
            entity.setUpdate(now);
            this.updateById(entity);
            count++;
        }
        return count;
    }
}

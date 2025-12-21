package cn.org.autumn.modules.usr.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.config.AccountHandler;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.usr.dao.UserTokenDao;
import cn.org.autumn.modules.usr.entity.UserTokenEntity;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import org.apache.commons.lang.StringUtils;
import org.apache.oltu.oauth2.common.OAuth;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UserTokenService extends ModuleService<UserTokenDao, UserTokenEntity> implements AccountHandler {

    private final static int EXPIRE = 3600 * 12;

    @Override
    public String ico() {
        return "fa-eye-slash";
    }

    public UserTokenEntity queryByToken(String token) {
        return this.selectOne(new EntityWrapper<UserTokenEntity>().eq("token", token));
    }

    public UserTokenEntity getUuid(String uuid) {
        if (StringUtils.isBlank(uuid))
            return null;
        return baseMapper.getUuid(uuid);
    }

    public UserTokenEntity getToken(String token) {
        if (StringUtils.isBlank(token))
            return null;
        return baseMapper.getToken(token);
    }

    public List<UserTokenEntity> getUser(String user) {
        if (StringUtils.isBlank(user))
            return null;
        return baseMapper.getUser(user);
    }

    public void saveToken(String token) {
        Map map = (Map) JSON.parse(token);
        UserTokenEntity userTokenEntity = null;
        if (map.containsKey(OAuth.OAUTH_ACCESS_TOKEN)) {
            String tk = (String) map.get(OAuth.OAUTH_ACCESS_TOKEN);
            userTokenEntity = queryByToken(tk);
        }
        if (null == userTokenEntity) {
            String tk = (String) map.get(OAuth.OAUTH_ACCESS_TOKEN);
            userTokenEntity = new UserTokenEntity();
            if (ShiroUtils.isLogin()) {
                String userUuid = ShiroUtils.getUserUuid();
                userTokenEntity.setUserUuid(userUuid);
            }
            userTokenEntity.setToken(tk);
        }
        if (map.containsKey(OAuth.OAUTH_REFRESH_TOKEN)) {
            userTokenEntity.setRefreshToken((String) map.get(OAuth.OAUTH_REFRESH_TOKEN));
        }
        if (map.containsKey(OAuth.OAUTH_EXPIRES_IN)) {
            Integer expire = (Integer) map.get(OAuth.OAUTH_EXPIRES_IN);
            Date date = new Date();
            date.setTime(date.getTime() + expire * 1000);
            userTokenEntity.setExpireTime(date);
            userTokenEntity.setUpdateTime(date);
        }
        insertOrUpdate(userTokenEntity);
    }

    public UserTokenEntity createToken(String userUuid) {
        //当前时间
        Date now = new Date();
        //过期时间
        Date expireTime = new Date(now.getTime() + EXPIRE * 1000);
        //生成token
        String token = generateToken();
        //保存或更新用户token
        UserTokenEntity tokenEntity = new UserTokenEntity();
        tokenEntity.setUserUuid(userUuid);
        tokenEntity.setToken(token);
        tokenEntity.setUpdateTime(now);
        tokenEntity.setExpireTime(expireTime);
        this.insertOrUpdate(tokenEntity);
        return tokenEntity;
    }

    public void expireToken(String userUuid) {
        Date now = new Date();
        UserTokenEntity tokenEntity = new UserTokenEntity();
        tokenEntity.setUserUuid(userUuid);
        tokenEntity.setUpdateTime(now);
        tokenEntity.setExpireTime(now);
        this.insertOrUpdate(tokenEntity);
    }

    public void deleteUser(String user) {
        baseMapper.deleteUser(user);
    }

    public void deleteUuid(String user) {
        baseMapper.deleteUuid(user);
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public void canceled(User obj) {
        if (null != obj)
            baseMapper.deleteUser(obj.getUuid());
    }
}

package cn.org.autumn.modules.api.service;

import cn.org.autumn.modules.sys.dto.SysUser;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.usr.dao.UserTokenDao;
import cn.org.autumn.modules.usr.entity.UserTokenEntity;
import cn.org.autumn.modules.usr.service.UserTokenService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class AppApiUserService {

    @Autowired
    @Lazy
    SysUserService sysUserService;

    @Autowired
    @Lazy
    UserTokenService userTokenService;

    @Autowired
    UserTokenDao userTokenDao;

    public SysUser checkUserPassword(String username, String password){

        //JSONObject jsonObject = new JSONObject();
        SysUser sysUser = new SysUser();

        //通过用户名获取用户实体
        SysUserEntity sysUserEntity = sysUserService.getUsername(username);

        if(null != sysUserEntity){
            //把用户输入的密码加密
            String passwordInput = ShiroUtils.sha256(password, sysUserEntity.getSalt());
            //System.out.println(passwordNew);

            //密码匹配
            if(StringUtils.equals(sysUserEntity.getPassword(), passwordInput)){

                //获取用户token实体
                UserTokenEntity userTokenEntity = userTokenDao.getByUuid(sysUserEntity.getUuid());
                if(null != userTokenEntity){

                    //二次登录，更新token
                    userTokenService.createOrUpdateToken(userTokenEntity);

                }else{
                    //首次登录
                    userTokenEntity = new UserTokenEntity();
                    userTokenEntity.setUserUuid(sysUserEntity.getUuid());
                    userTokenService.createOrUpdateToken(userTokenEntity);
                }

                //密码匹配成功，生成新的登录token
                //UserTokenEntity userTokenEntity = userTokenService.createToken(sysUserEntity.getUuid());

                sysUser.setUsername(sysUserEntity.getUsername());
                sysUser.setToken(userTokenEntity.getToken());

                return sysUser;
            }else{
                return sysUser;
            }
        }else{
            return sysUser;
        }

    }


}

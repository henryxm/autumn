package cn.org.autumn.modules.sys.shiro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.org.autumn.modules.oauth.service.ClientDetailsService;
import cn.org.autumn.modules.oauth.store.ValueType;
import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.dao.SysMenuDao;
import cn.org.autumn.modules.sys.dao.SysUserDao;
import cn.org.autumn.modules.sys.entity.SysMenuEntity;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserRealm extends AuthorizingRealm {
    @Autowired
    private SysUserDao sysUserDao;
    @Autowired
    private SysMenuDao sysMenuDao;

    @Autowired
    ClientDetailsService clientDetailsService;

    @Autowired
    UserProfileService userProfileService;

    @Autowired
    SysUserService sysUserService;

    @Autowired
    SysUserRoleService sysUserRoleService;

    /**
     * 授权(验证权限时调用)
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        SysUserEntity user = (SysUserEntity) principals.getPrimaryPrincipal();
        String uuid = user.getUuid();

        List<String> permsList;

        //系统管理员，拥有最高权限
        if (sysUserRoleService.isSystemAdministrator(user)) {
            List<SysMenuEntity> menuList = sysMenuDao.selectList(null);
            permsList = new ArrayList<>(menuList.size());
            for (SysMenuEntity menu : menuList) {
                permsList.add(menu.getPerms());
            }
        } else {
            permsList = sysUserDao.getPermsByUserUuid(uuid);
        }

        //用户权限列表
        Set<String> permsSet = new HashSet<>();
        for (String perms : permsList) {
            if (StringUtils.isBlank(perms)) {
                continue;
            }
            permsSet.addAll(Arrays.asList(perms.trim().split(",")));
        }

        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        info.setStringPermissions(permsSet);
        return info;
    }

    /**
     * 认证(登录时调用)
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authcToken) throws AuthenticationException {
        UsernamePasswordToken token = (UsernamePasswordToken) authcToken;
        String username = token.getUsername();
        SysUserEntity user = new SysUserEntity();
        if (token instanceof OauthAccessTokenToken) {
            user = (SysUserEntity) clientDetailsService.get(ValueType.accessToken, username).getValue();
        } else {
            //查询用户信息
            if (token instanceof OauthUsernameToken)
                user.setUuid(username);
            else
                user.setUsername(username);
            user = sysUserDao.selectOne(user);
        }

        //账号不存在
        if (user == null) {
            throw new UnknownAccountException("账号或密码不正确");
        }

        //账号锁定
        if (user.getStatus() == 0) {
            throw new LockedAccountException("账号已被锁定,请联系管理员");
        }
        user = userProfileService.setProfile(user);
        if (StringUtils.isNotEmpty(user.getParentUuid())) {
            SysUserEntity parent = sysUserDao.getByUuid(user.getParentUuid());
            if (null != parent) {
                parent = userProfileService.setProfile(parent);
                user.setParent(parent);
            }
        }
        SimpleAuthenticationInfo info = new SimpleAuthenticationInfo(user, user.getPassword(), ByteSource.Util.bytes(user.getSalt()), getName());
        return info;
    }

    @Override
    public void setCredentialsMatcher(CredentialsMatcher credentialsMatcher) {
        NamedHashedCredentialsMatcher shaCredentialsMatcher = new NamedHashedCredentialsMatcher();
        shaCredentialsMatcher.setHashAlgorithmName(ShiroUtils.hashAlgorithmName);
        shaCredentialsMatcher.setHashIterations(ShiroUtils.hashIterations);
        super.setCredentialsMatcher(shaCredentialsMatcher);
    }
}

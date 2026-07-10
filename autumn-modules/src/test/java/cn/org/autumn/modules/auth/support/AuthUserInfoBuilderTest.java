package cn.org.autumn.modules.auth.support;

import cn.org.autumn.auth.scope.AuthScopeCatalog;
import cn.org.autumn.auth.scope.AuthScopeSet;
import cn.org.autumn.auth.scope.AuthTrack;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.usr.entity.UserProfileEntity;
import cn.org.autumn.opl.model.OpenUserInfoSnapshot;
import org.junit.Assert;
import org.junit.Test;

public class AuthUserInfoBuilderTest {

    @Test
    public void basicOAuthScopeReturnsLegacyFields() {
        AuthScopeCatalog catalog = new AuthScopeCatalog();
        SysUserEntity user = new SysUserEntity();
        user.setUuid("u1");
        user.setVerify(1);
        user.setStatus(1);
        user.setMobile("13800000000");
        UserProfileEntity profile = new UserProfileEntity();
        profile.setUuid("u1");
        profile.setNickname("nick");
        profile.setIcon("icon.png");
        profile.setUsername("user1");
        AuthScopeSet granted = AuthScopeSet.of("basic").expand(catalog, AuthTrack.OAUTH);
        cn.org.autumn.auth.model.AuthUserInfo info = AuthUserInfoBuilder.build(catalog, AuthTrack.OAUTH, granted, user, profile, null, null);
        Assert.assertEquals("u1", info.getUuid());
        Assert.assertEquals("nick", info.getNickname());
        Assert.assertEquals("icon.png", info.getIcon());
        Assert.assertEquals("user1", info.getUsername());
        Assert.assertNull(info.getMobile());
    }

    @Test
    public void identityOnlyOmitsProfileFields() {
        AuthScopeCatalog catalog = new AuthScopeCatalog();
        UserProfileEntity profile = new UserProfileEntity();
        profile.setUuid("u1");
        profile.setNickname("nick");
        AuthScopeSet granted = AuthScopeSet.of("identity");
        cn.org.autumn.auth.model.AuthUserInfo info = AuthUserInfoBuilder.build(catalog, AuthTrack.OAUTH, granted, null, profile, null, null);
        Assert.assertEquals("u1", info.getUuid());
        Assert.assertNull(info.getNickname());
    }

    @Test
    public void oplEmailScopeReturnsEmailInOpenUserInfo() {
        AuthScopeCatalog catalog = new AuthScopeCatalog();
        SysUserEntity user = new SysUserEntity();
        user.setUuid("u1");
        user.setEmail("user@example.com");
        AuthScopeSet granted = AuthScopeSet.of("openid", "email");
        cn.org.autumn.auth.model.AuthUserInfo info = AuthUserInfoBuilder.build(catalog, AuthTrack.OPL, granted, user, null, "oid1", null);
        OpenUserInfoSnapshot snapshot = AuthUserInfoBuilder.toOpenUserInfo(info);
        Assert.assertEquals("oid1", snapshot.getOpenId());
        Assert.assertEquals("user@example.com", snapshot.getEmail());
    }

    @Test
    public void oplEmailOmittedWhenScopeNotGranted() {
        AuthScopeCatalog catalog = new AuthScopeCatalog();
        SysUserEntity user = new SysUserEntity();
        user.setEmail("user@example.com");
        AuthScopeSet granted = AuthScopeSet.of("openid");
        cn.org.autumn.auth.model.AuthUserInfo info = AuthUserInfoBuilder.build(catalog, AuthTrack.OPL, granted, user, null, "oid1", null);
        OpenUserInfoSnapshot snapshot = AuthUserInfoBuilder.toOpenUserInfo(info);
        Assert.assertNull(snapshot.getEmail());
    }
}

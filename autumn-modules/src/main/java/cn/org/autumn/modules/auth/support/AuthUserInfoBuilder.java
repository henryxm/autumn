package cn.org.autumn.modules.auth.support;

import cn.org.autumn.auth.model.AuthUserInfo;
import cn.org.autumn.auth.scope.AuthField;
import cn.org.autumn.auth.scope.AuthScopeCatalog;
import cn.org.autumn.auth.scope.AuthScopeSet;
import cn.org.autumn.auth.scope.AuthTrack;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.usr.entity.UserProfileEntity;
import cn.org.autumn.modules.usr.dto.UserProfile;
import java.util.Set;
import cn.org.autumn.opl.model.OpenUserInfoSnapshot;
import org.apache.commons.lang.StringUtils;

public final class AuthUserInfoBuilder {

    private AuthUserInfoBuilder() {
    }

    public static AuthUserInfo build(AuthScopeCatalog catalog, AuthTrack track, AuthScopeSet grantedScope, SysUserEntity user, UserProfileEntity profile, String openId, String unionId) {
        AuthUserInfo info = new AuthUserInfo();
        if (catalog == null || track == null || grantedScope == null || grantedScope.isEmpty()) {
            return info;
        }
        Set<AuthField> fields = catalog.fieldsFor(track, grantedScope);
        if (fields.contains(AuthField.uuid)) {
            if (profile != null && StringUtils.isNotBlank(profile.getUuid())) {
                info.setUuid(profile.getUuid());
            } else if (user != null) {
                info.setUuid(user.getUuid());
            }
        }
        if (fields.contains(AuthField.openId) && StringUtils.isNotBlank(openId)) {
            info.setOpenId(openId);
        }
        if (fields.contains(AuthField.unionId) && StringUtils.isNotBlank(unionId)) {
            info.setUnionId(unionId);
        }
        if (fields.contains(AuthField.nickname) && profile != null) {
            info.setNickname(profile.getNickname());
        }
        if (fields.contains(AuthField.icon) && profile != null) {
            info.setIcon(StringUtils.defaultString(profile.getIcon()));
        }
        if (fields.contains(AuthField.username) && profile != null) {
            info.setUsername(profile.getUsername());
        }
        if (fields.contains(AuthField.mobile)) {
            String mobile = profile != null ? profile.getMobile() : null;
            if (StringUtils.isBlank(mobile) && user != null) {
                mobile = user.getMobile();
            }
            if (StringUtils.isNotBlank(mobile)) {
                info.setMobile(mobile);
            }
        }
        if (fields.contains(AuthField.email) && user != null && StringUtils.isNotBlank(user.getEmail())) {
            info.setEmail(user.getEmail());
        }
        if (fields.contains(AuthField.verified) && user != null) {
            info.setVerified(user.getVerify());
        }
        if (fields.contains(AuthField.status) && user != null) {
            info.setStatus(user.getStatus());
        }
        return info;
    }

    public static UserProfile toUserProfile(AuthUserInfo info) {
        UserProfile profile = new UserProfile();
        if (info == null) {
            return profile;
        }
        if (StringUtils.isNotBlank(info.getUuid())) {
            profile.setUuid(info.getUuid());
        }
        if (StringUtils.isNotBlank(info.getIcon())) {
            profile.setIcon(info.getIcon());
        }
        if (StringUtils.isNotBlank(info.getUsername())) {
            profile.setUsername(info.getUsername());
        }
        if (StringUtils.isNotBlank(info.getNickname())) {
            profile.setNickname(info.getNickname());
        }
        return profile;
    }

    public static OpenUserInfoSnapshot toOpenUserInfo(AuthUserInfo info) {
        OpenUserInfoSnapshot snapshot = new OpenUserInfoSnapshot();
        if (info == null) {
            return snapshot;
        }
        snapshot.setOpenId(info.getOpenId());
        snapshot.setUnionId(info.getUnionId());
        snapshot.setNickname(info.getNickname());
        snapshot.setIcon(info.getIcon());
        snapshot.setMobile(info.getMobile());
        snapshot.setEmail(info.getEmail());
        snapshot.setVerified(info.getVerified());
        snapshot.setStatus(info.getStatus());
        return snapshot;
    }
}

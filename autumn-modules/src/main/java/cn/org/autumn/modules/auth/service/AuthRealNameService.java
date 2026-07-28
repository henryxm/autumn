package cn.org.autumn.modules.auth.service;

import cn.org.autumn.auth.model.AuthRealNameInfo;
import cn.org.autumn.auth.scope.AuthScopeSet;
import cn.org.autumn.auth.spi.AuthRealNameProvider;
import cn.org.autumn.site.Factory;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 实名详情解析与按档位裁剪。
 */
@Service
public class AuthRealNameService extends Factory {

    public boolean hasProvider() {
        return !getOrderList(AuthRealNameProvider.class).isEmpty();
    }

    /** granted 已 expand 后是否含任一实名三档。 */
    public boolean hasAnyRealNameScope(AuthScopeSet grantedScope) {
        if (grantedScope == null || grantedScope.isEmpty()) {
            return false;
        }
        for (String tier : AuthScopeSet.REALNAME_TIERS) {
            if (grantedScope.contains(tier)) {
                return true;
            }
        }
        return false;
    }

    public AuthRealNameInfo resolve(String userUuid) {
        if (StringUtils.isBlank(userUuid)) {
            return null;
        }
        List<AuthRealNameProvider> providers = getOrderList(AuthRealNameProvider.class);
        for (AuthRealNameProvider provider : providers) {
            if (provider == null) {
                continue;
            }
            AuthRealNameInfo info = provider.getRealName(userUuid.trim());
            if (info != null) {
                return info;
            }
        }
        return null;
    }

    /**
     * 按 granted 实名档位投影字段；未授权字段不拷贝。
     */
    public AuthRealNameInfo project(AuthRealNameInfo full, AuthScopeSet grantedScope) {
        if (full == null || !hasAnyRealNameScope(grantedScope)) {
            return null;
        }
        AuthRealNameInfo out = new AuthRealNameInfo();
        if (grantedScope.contains(AuthScopeSet.REALNAME_ATTR)) {
            out.setAge(full.getAge());
            if (StringUtils.isNotBlank(full.getGender())) {
                out.setGender(full.getGender());
            }
            if (StringUtils.isNotBlank(full.getEthnicity())) {
                out.setEthnicity(full.getEthnicity());
            }
        }
        if (grantedScope.contains(AuthScopeSet.REALNAME_PERSON)) {
            if (StringUtils.isNotBlank(full.getName())) {
                out.setName(full.getName());
            }
            if (StringUtils.isNotBlank(full.getBirthday())) {
                out.setBirthday(full.getBirthday());
            }
        }
        if (grantedScope.contains(AuthScopeSet.REALNAME_ID)) {
            if (StringUtils.isNotBlank(full.getIdNumber())) {
                out.setIdNumber(full.getIdNumber());
            }
            if (StringUtils.isNotBlank(full.getAddress())) {
                out.setAddress(full.getAddress());
            }
        }
        return out;
    }
}

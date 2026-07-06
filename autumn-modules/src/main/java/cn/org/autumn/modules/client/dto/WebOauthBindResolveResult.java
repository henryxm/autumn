package cn.org.autumn.modules.client.dto;

import cn.org.autumn.modules.usr.dto.UserProfile;
import lombok.Getter;
import lombok.Setter;

/** {@link cn.org.autumn.modules.client.service.WebOauthBindService#resolveAndBind} 结果。 */
@Getter
@Setter
public class WebOauthBindResolveResult {

    private UserProfile profile;
    private boolean idempotent;

    public static WebOauthBindResolveResult of(UserProfile profile, boolean idempotent) {
        WebOauthBindResolveResult result = new WebOauthBindResolveResult();
        result.setProfile(profile);
        result.setIdempotent(idempotent);
        return result;
    }
}

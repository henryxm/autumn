package cn.org.autumn.modules.opc.dto;

import cn.org.autumn.modules.usr.dto.UserProfile;
import lombok.Getter;
import lombok.Setter;

/** {@link cn.org.autumn.modules.opc.service.ConnectBindService#resolveAndBind} 结果。 */
@Getter
@Setter
public class ConnectBindResolveResult {

    private UserProfile profile;
    private boolean idempotent;

    public static ConnectBindResolveResult of(UserProfile profile, boolean idempotent) {
        ConnectBindResolveResult result = new ConnectBindResolveResult();
        result.setProfile(profile);
        result.setIdempotent(idempotent);
        return result;
    }
}

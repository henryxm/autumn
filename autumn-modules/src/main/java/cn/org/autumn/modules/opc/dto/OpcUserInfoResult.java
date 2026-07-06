package cn.org.autumn.modules.opc.dto;

import cn.org.autumn.opl.model.OpenUserInfoSnapshot;
import lombok.Getter;
import lombok.Setter;

/** OPC 拉取 userInfo 结果；{@code platformUser} 仅同实例本地 OPL 直调时有值，不经 HTTP 暴露。 */
@Getter
@Setter
public class OpcUserInfoResult {

    private OpenUserInfoSnapshot snapshot;
    /** 本地 OPL token 对应的 sys_user.uuid；跨实例 HTTP userInfo 时为 null。 */
    private String platformUser;

    public static OpcUserInfoResult of(OpenUserInfoSnapshot snapshot, String platformUser) {
        OpcUserInfoResult result = new OpcUserInfoResult();
        result.setSnapshot(snapshot);
        result.setPlatformUser(platformUser);
        return result;
    }
}

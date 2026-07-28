package cn.org.autumn.modules.usr.dto;

import cn.org.autumn.auth.model.AuthRealNameInfo;
import cn.org.autumn.modules.usr.entity.UserProfileEntity;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
public class UserProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private String uuid = "";

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private String icon = "";

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private String username = "";

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private String nickname = "";

    /** OAuth userInfo scope=phone；可为空。 */
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private String mobile;

    /** OAuth userInfo scope=verified：0 未实名 / 1 已实名；可为空表示未下发该字段。 */
    private Integer verified;

    /** 授权含任一 realname_* 时由 RP 预取的实名详情；未授权或拉取失败时为 null。 */
    private AuthRealNameInfo realName;

    public static UserProfile from(UserProfileEntity userProfileEntity) {
        UserProfile userProfile = new UserProfile();
        if (StringUtils.isNotEmpty(userProfileEntity.getIcon()))
            userProfile.setIcon(userProfileEntity.getIcon());
        else
            userProfile.setIcon("");
        userProfile.setUsername(userProfileEntity.getUsername());
        userProfile.setNickname(userProfileEntity.getNickname());
        userProfile.setUuid(userProfileEntity.getUuid());
        if (StringUtils.isNotBlank(userProfileEntity.getMobile())) {
            userProfile.setMobile(userProfileEntity.getMobile());
        }
        return userProfile;
    }

    @Override
    public String toString() {
        return "UserProfile{" +
                "uuid='" + uuid + '\'' +
                ", icon='" + icon + '\'' +
                ", username='" + username + '\'' +
                ", nickname='" + nickname + '\'' +
                ", mobile='" + mobile + '\'' +
                ", verified=" + verified +
                '}';
    }
}

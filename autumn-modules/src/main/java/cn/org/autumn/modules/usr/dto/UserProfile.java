package cn.org.autumn.modules.usr.dto;

import cn.org.autumn.modules.usr.entity.UserProfileEntity;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;

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

    public static UserProfile from(UserProfileEntity userProfileEntity) {
        UserProfile userProfile = new UserProfile();
        if (StringUtils.isNotEmpty(userProfileEntity.getIcon()))
            userProfile.setIcon(userProfileEntity.getIcon());
        else
            userProfile.setIcon("");
        userProfile.setUsername(userProfileEntity.getUsername());
        userProfile.setNickname(userProfileEntity.getNickname());
        userProfile.setUuid(userProfileEntity.getUuid());
        return userProfile;
    }

    @Override
    public String toString() {
        return "UserProfile{" +
                "uuid='" + uuid + '\'' +
                ", icon='" + icon + '\'' +
                ", username='" + username + '\'' +
                ", nickname='" + nickname + '\'' +
                '}';
    }
}

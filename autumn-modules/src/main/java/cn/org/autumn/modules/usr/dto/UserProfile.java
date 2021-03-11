package cn.org.autumn.modules.usr.dto;

import cn.org.autumn.modules.usr.entity.UserProfileEntity;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;

public class UserProfile implements Serializable {
    private static final long serialVersionUID = 1L;
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private String uuid = "";
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private String openId = "";
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
    private String unionId = "";
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
        userProfile.setOpenId(userProfileEntity.getOpenId());
        userProfile.setUnionId(userProfileEntity.getUnionId());
        userProfile.setUuid(userProfileEntity.getUuid());
        return userProfile;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getOpenId() {
        return openId;
    }

    public void setOpenId(String openId) {
        this.openId = openId;
    }

    public String getUnionId() {
        return unionId;
    }

    public void setUnionId(String unionId) {
        this.unionId = unionId;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    @Override
    public String toString() {
        return "UserProfile{" +
                "uuid='" + uuid + '\'' +
                ", openId='" + openId + '\'' +
                ", unionId='" + unionId + '\'' +
                ", icon='" + icon + '\'' +
                ", username='" + username + '\'' +
                ", nickname='" + nickname + '\'' +
                '}';
    }
}

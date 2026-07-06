package cn.org.autumn.opl.model;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/**
 * 应用注册或重置密钥结果（含一次性明文 secret）。
 */
@Getter
@Setter
public class OpenAppRegisterOutcome implements Serializable {
    private static final long serialVersionUID = 1L;

    private String appId;
    private String appSecret;
    private String name;
    private String redirectUri;
    private String scope;
    private OpenAppType appType;

    public static OpenAppRegisterOutcome of(String appId, String appSecret, String name, String redirectUri, String scope, OpenAppType appType) {
        OpenAppRegisterOutcome outcome = new OpenAppRegisterOutcome();
        outcome.setAppId(appId);
        outcome.setAppSecret(appSecret);
        outcome.setName(name);
        outcome.setRedirectUri(redirectUri);
        outcome.setScope(scope);
        outcome.setAppType(appType);
        return outcome;
    }
}

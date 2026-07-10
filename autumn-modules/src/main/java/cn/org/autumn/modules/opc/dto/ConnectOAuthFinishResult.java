package cn.org.autumn.modules.opc.dto;

import lombok.Getter;

/** 开放 OAuth 完成结果：成功跳转或绑定选择页。 */
@Getter
public class ConnectOAuthFinishResult {

    private final String redirectUrl;
    private final boolean bindChoice;

    private ConnectOAuthFinishResult(String redirectUrl, boolean bindChoice) {
        this.redirectUrl = redirectUrl;
        this.bindChoice = bindChoice;
    }

    public static ConnectOAuthFinishResult success(String redirectUrl) {
        return new ConnectOAuthFinishResult(redirectUrl, false);
    }

    public static ConnectOAuthFinishResult bindChoice(String redirectUrl) {
        return new ConnectOAuthFinishResult(redirectUrl, true);
    }
}

package cn.org.autumn.modules.qrc.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketCreateResult {
    private String uuid;
    private String qrUrl;
    private long expireIn;
    private String intent;
    private String status;
    /**
     * 桌面快捷登录开关（建票下发给前端）。
     * <ul>
     *   <li>{@code true}/{@code false}：票据带 OAuth {@code clientId} 时，取自
     *       {@code qrc_client_grant.quick}——是否允许该客户端登录页经 AS Bridge
     *       探测本机超然信并展示快捷账号（默认 false，需在扫码授权客户端配置中显式开启）</li>
     *   <li>{@code null}：无 client 上下文（如 AS 本站 B2 自有登录），前端回落
     *       {@code LoginPageConfig.chaoranWakeEnabled}</li>
     * </ul>
     * 不表示 TLS 证书是否启用；证书下发由 Account 桌面证书管理单独控制。
     */
    private Boolean quick;

    public static TicketCreateResult of(String uuid, String qrUrl, long expireIn, String intent, String status) {
        TicketCreateResult result = new TicketCreateResult();
        result.setUuid(uuid);
        result.setQrUrl(qrUrl);
        result.setExpireIn(expireIn);
        result.setIntent(intent);
        result.setStatus(status);
        return result;
    }
}

package cn.org.autumn.modules.qrc.service;

import cn.org.autumn.modules.qrc.model.Intent;
import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class IntentDisplaySupport {

    public String intentTitle(TicketSnapshot ticket, String clientName) {
        if (ticket == null || StringUtils.isBlank(ticket.getIntent())) {
            return "登录确认";
        }
        switch (ticket.getIntent()) {
            case Intent.SELF_WEB_LOGIN:
                return "网页版登录确认";
            case Intent.OAUTH_AUTHORIZE:
                return "授权「" + safeClientName(clientName) + "」登录";
            case Intent.OAUTH_CONSENT:
                return "授权「" + safeClientName(clientName) + "」";
            case Intent.OAUTH_DEVICE:
                return "授权「" + safeClientName(clientName) + "」";
            default:
                return "登录确认";
        }
    }

    public String intentHint(TicketSnapshot ticket) {
        if (ticket == null || StringUtils.isBlank(ticket.getIntent())) {
            return "请确认是否本人操作";
        }
        switch (ticket.getIntent()) {
            case Intent.SELF_WEB_LOGIN:
                return "你的账号正在请求登录网页版，请确认是否本人操作";
            case Intent.OAUTH_AUTHORIZE:
                return "该应用请求使用你的 Autumn 账号登录，请确认是否授权";
            case Intent.OAUTH_CONSENT:
                return "该应用请求访问你的账号信息，请确认是否授权";
            case Intent.OAUTH_DEVICE:
                return "该应用请求访问你的账号，请确认是否授权";
            default:
                return "请确认是否本人操作";
        }
    }

    public String deviceHint(TicketSnapshot ticket) {
        if (ticket == null || StringUtils.isBlank(ticket.getAgent())) {
            return "网页端";
        }
        String agent = ticket.getAgent().toLowerCase();
        if (agent.contains("windows")) {
            return "Windows 电脑";
        }
        if (agent.contains("macintosh") || agent.contains("mac os")) {
            return "Mac 电脑";
        }
        if (agent.contains("linux")) {
            return "Linux 电脑";
        }
        if (agent.contains("iphone") || agent.contains("ipad")) {
            return "iOS 设备";
        }
        if (agent.contains("android")) {
            return "Android 设备";
        }
        return "网页端";
    }

    private String safeClientName(String clientName) {
        return StringUtils.isNotBlank(clientName) ? clientName : "第三方应用";
    }
}

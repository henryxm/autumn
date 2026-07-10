package cn.org.autumn.modules.client.service;

import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.oauth2.WebOauthEndpointResolver;
import cn.org.autumn.modules.qrc.dto.OpenTicketStatusRequest;
import cn.org.autumn.utils.HttpClientUtils;
import com.alibaba.fastjson.JSON;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** RP 联邦 QRC：代理远程 AS Open API 取消票据。 */
@Service
public class OauthRpQrcService {

    @Autowired
    AuthSiteRoleService authSiteRoleService;

    @Autowired
    WebOauthEndpointResolver webOauthEndpointResolver;

    public String cancelTicket(HttpServletRequest request, String uuid) {
        WebAuthenticationEntity rpClient = requireRpClient(request);
        OpenTicketStatusRequest body = new OpenTicketStatusRequest();
        body.setUuid(uuid);
        body.setClientId(rpClient.getClientId());
        body.setClientSecret(rpClient.getClientSecret());
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("data", body);
        String url = webOauthEndpointResolver.resolveQrcOpenCancelUri(rpClient);
        HttpClientUtils.doPostJson(url, JSON.toJSONString(envelope));
        return uuid;
    }

    private WebAuthenticationEntity requireRpClient(HttpServletRequest request) {
        if (!authSiteRoleService.isRpEnabled()) {
            throw new IllegalStateException("当前站点未启用 RP 角色");
        }
        WebAuthenticationEntity rpClient = authSiteRoleService.resolveRpClient(request);
        if (rpClient == null) {
            throw new IllegalStateException("未配置 RP OAuth 客户端");
        }
        return rpClient;
    }
}

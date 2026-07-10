package cn.org.autumn.modules.client.service;

import cn.org.autumn.model.Response;
import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.oauth2.WebOauthEndpointResolver;
import cn.org.autumn.modules.qrc.dto.OpenTicketCreateRequest;
import cn.org.autumn.modules.qrc.dto.OpenTicketStatusRequest;
import cn.org.autumn.modules.qrc.dto.TicketCreateResult;
import cn.org.autumn.utils.HttpClientUtils;
import cn.org.autumn.utils.WebPathUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** RP 联邦 QRC：代理远程 AS Open API 取消票据。 */
@Service
public class OauthRpQrcService {

    @Autowired
    AuthSiteRoleService authSiteRoleService;

    @Autowired
    OauthRpStateService oauthRpStateService;

    @Autowired
    WebOauthEndpointResolver webOauthEndpointResolver;

    public TicketCreateResult createTicket(HttpServletRequest request, String callback) {
        WebAuthenticationEntity rpClient = requireRpClient(request);
        OpenTicketCreateRequest body = new OpenTicketCreateRequest();
        body.setClientId(rpClient.getClientId());
        body.setClientSecret(rpClient.getClientSecret());
        body.setRedirectUri(rpClient.getRedirectUri());
        body.setScope(StringUtils.defaultIfBlank(rpClient.getScope(), "basic"));
        body.setState(issueCallbackState(request, callback));
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("data", body);
        String url = webOauthEndpointResolver.resolveQrcOpenCreateUri(rpClient);
        String raw = HttpClientUtils.doPostJson(url, JSON.toJSONString(envelope));
        Response<TicketCreateResult> response = JSON.parseObject(raw, new TypeReference<Response<TicketCreateResult>>() {
        });
        if (response == null || !response.success() || response.getData() == null) {
            throw new IllegalStateException(response == null ? "建票失败" : StringUtils.defaultIfBlank(response.getMsg(), "建票失败"));
        }
        return response.getData();
    }

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

    private String issueCallbackState(HttpServletRequest request, String callback) {
        return oauthRpStateService.issueState(WebPathUtils.safeOauthCallbackForClient(request, callback));
    }
}

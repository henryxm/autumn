package cn.org.autumn.modules.client.service;

import cn.org.autumn.model.AuthLoginProviderType;
import cn.org.autumn.modules.client.dto.ScanLoginCredentialView;
import cn.org.autumn.modules.client.model.ScanLoginCredentialContext;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.opc.service.ConnectLoginService;
import cn.org.autumn.modules.qrc.dto.OpenTicketCreateRequest;
import cn.org.autumn.modules.qrc.dto.OpenTicketStatusRequest;
import cn.org.autumn.modules.qrc.dto.SessionExchangeRequest;
import cn.org.autumn.modules.qrc.dto.TicketCreateRequest;
import cn.org.autumn.modules.qrc.dto.TicketCreateResult;
import cn.org.autumn.modules.qrc.dto.TicketStatusResult;
import cn.org.autumn.modules.qrc.model.DeliveryMode;
import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import cn.org.autumn.modules.qrc.service.ScanTicketService;
import cn.org.autumn.modules.qrc.support.QrcApiSupport;
import cn.org.autumn.modules.qrc.support.ScanWebSupport;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 扫码登录统一门面：B2 同源 Web、D RP 联邦、B3 Open API 服务端建票。
 * 业务模块应注入本类而非重复实现 HTTP 协议；控制器层为薄委托。
 */
@Service
public class ScanLoginFacade {

    @Autowired
    private ScanTicketService scanTicketService;

    @Autowired
    private ScanWebSupport scanWebSupport;

    @Autowired
    private QrcApiSupport qrcApiSupport;

    @Autowired
    private RpQrcCallbackService rpQrcCallbackService;

    @Autowired
    private OauthRpQrcService oauthRpQrcService;

    @Autowired
    private ScanLoginCredentialService scanLoginCredentialService;

    @Autowired
    private RpQrcInboundService rpQrcInboundService;

    @Autowired
    private RpQrcPendingStore rpQrcPendingStore;

    @Autowired
    private RpQrcEventStreamService rpQrcEventStreamService;

    @Autowired
    private ConnectLoginService connectLoginService;

    /** B2：同源 Web 建票。 */
    public TicketCreateResult createAsWebTicket(HttpServletRequest request, TicketCreateRequest data) throws Exception {
        TicketSnapshot ticket = scanTicketService.create(scanWebSupport.buildWebCreateContext(data, request));
        return scanTicketService.toCreateResult(ticket);
    }

    /** B2：同源 Web 轮询状态。 */
    public TicketStatusResult pollAsWebStatus(String uuid) throws Exception {
        TicketSnapshot ticket = scanTicketService.getRequired(uuid);
        return scanTicketService.toStatusResult(ticket);
    }

    /** B2：同源 Web exchange 换 Session，返回跳转 URL。 */
    public String exchangeAsWebSession(SessionExchangeRequest data, HttpServletRequest request) {
        return scanWebSupport.exchangeSession(data, request);
    }

    /** D：RP 联邦建票（WEBHOOK 回调模式，代理远程 AS Open API）。 */
    public TicketCreateResult createRpTicket(HttpServletRequest request, String callback) {
        return rpQrcCallbackService.createTicket(request, callback, null, null);
    }

    /** D：RP 联邦建票（指定凭证 type/id）。 */
    public TicketCreateResult createRpTicket(HttpServletRequest request, String callback, String type, String id) {
        return rpQrcCallbackService.createTicket(request, callback, type, id);
    }

    /** 按凭证类型建票：经典同源 B2；开放同源 OPC OAuth；跨站走 D。 */
    public TicketCreateResult createWebTicketByCredential(HttpServletRequest request, String type, String id, String callback) throws Exception {
        ScanLoginCredentialContext credential = scanLoginCredentialService.require(type, id);
        if ("as".equalsIgnoreCase(credential.getQrcMode())) {
            if (AuthLoginProviderType.OAUTH2_OPEN.equalsIgnoreCase(type)) {
                return createAsOpenWebTicket(request, credential);
            }
            TicketCreateRequest body = new TicketCreateRequest();
            body.setIntent("SELF_WEB_LOGIN");
            return createAsWebTicket(request, body);
        }
        return createRpTicket(request, callback, type, id);
    }

    /** B2：同源开放 OPC 扫码建票（OAUTH_DEVICE + POLL_CODE）。 */
    public TicketCreateResult createAsOpenWebTicket(HttpServletRequest request, ScanLoginCredentialContext credential) throws Exception {
        OpenTicketCreateRequest body = new OpenTicketCreateRequest();
        body.setClientId(credential.getClientId());
        body.setClientSecret(credential.getClientSecret());
        body.setRedirectUri(credential.getRedirectUri());
        body.setScope(credential.getScope());
        Map<String, String> payload = new HashMap<>();
        payload.put("delivery", DeliveryMode.POLL_CODE);
        body.setPayload(payload);
        return createOpenTicket(body, request);
    }

    /** B2：同源开放 OPC 扫码完成（轮询拿到 code 后换票绑定）。 */
    public String completeOpenWebLogin(HttpServletRequest request, String type, String id, String code, String callback) {
        if (StringUtils.isBlank(code)) {
            throw new IllegalArgumentException("授权码不能为空");
        }
        ScanLoginCredentialContext credential = scanLoginCredentialService.require(type, id);
        if (!AuthLoginProviderType.OAUTH2_OPEN.equalsIgnoreCase(credential.getType())) {
            throw new IllegalArgumentException("仅支持 oauth2_open 凭证");
        }
        ConnectAppEntity app = credential.getConnectApp();
        if (app == null) {
            throw new IllegalStateException("未配置开放平台应用");
        }
        return connectLoginService.finishOAuthLogin(request, app, code, callback).getRedirectUrl();
    }

    /** D：RP 联邦 SSE 订阅（建票后浏览器唯一推送通道）。 */
    public SseEmitter streamRpTicket(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            throw new IllegalArgumentException("uuid 不能为空");
        }
        cn.org.autumn.modules.client.model.RpQrcPendingSession pending = rpQrcPendingStore.get(uuid);
        if (pending == null) {
            throw new IllegalStateException("扫码会话不存在或已过期");
        }
        if (pending.getExpiredAt() > 0 && pending.getExpiredAt() < System.currentTimeMillis() && !"COMPLETED".equals(pending.getStatus())) {
            pending.setStatus("EXPIRED");
            rpQrcPendingStore.save(pending);
        }
        return rpQrcEventStreamService.subscribe(uuid, pending);
    }

    /** D：RP 联邦轮询状态（SSE 不可用时的降级通道）。 */
    public TicketStatusResult pollRpTicketStatus(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            throw new IllegalArgumentException("uuid 不能为空");
        }
        cn.org.autumn.modules.client.model.RpQrcPendingSession pending = rpQrcPendingStore.get(uuid);
        if (pending == null) {
            throw new IllegalStateException("扫码会话不存在或已过期");
        }
        if (pending.getExpiredAt() > 0 && pending.getExpiredAt() < System.currentTimeMillis() && !"COMPLETED".equals(pending.getStatus())) {
            pending.setStatus("EXPIRED");
            rpQrcPendingStore.save(pending);
        }
        return rpQrcPendingStore.toStatusResult(pending);
    }

    /** D：RP 联邦取消票据。 */
    public String cancelRpTicket(HttpServletRequest request, String uuid) {
        return oauthRpQrcService.cancelTicket(request, uuid);
    }

    /** A 侧 WEBHOOK 推送到 B 站入站端点。 */
    public void handleRpInbound(String rawBody, Map<String, String> headers) {
        rpQrcInboundService.handleInbound(rawBody, headers);
    }

    /** 凭证解析视图（不含 secret）。 */
    public ScanLoginCredentialView resolveCredentialView(String type, String id) {
        return scanLoginCredentialService.toView(scanLoginCredentialService.require(type, id));
    }

    /** B3：Open API 建票（服务端持 client_secret，无需网页）。 */
    public TicketCreateResult createOpenTicket(OpenTicketCreateRequest data, HttpServletRequest request) throws Exception {
        OpenTicketCreateRequest validated = qrcApiSupport.requireOpenClient(data);
        TicketSnapshot ticket = scanTicketService.create(qrcApiSupport.buildOpenDeviceContext(validated, request));
        return scanTicketService.toCreateResult(ticket);
    }

    /** B3：Open API 轮询。 */
    public TicketStatusResult pollOpenTicket(OpenTicketStatusRequest data) throws Exception {
        OpenTicketStatusRequest validated = qrcApiSupport.requireOpenClient(data);
        TicketSnapshot ticket = scanTicketService.getRequired(validated.getUuid());
        qrcApiSupport.assertTicketClient(ticket, validated.getClientId());
        return scanTicketService.toStatusResult(ticket);
    }

    /** B3：Open API 取消。 */
    public TicketStatusResult cancelOpenTicket(OpenTicketStatusRequest data) throws Exception {
        OpenTicketStatusRequest validated = qrcApiSupport.requireOpenClient(data);
        TicketSnapshot ticket = scanTicketService.cancel(validated.getUuid(), validated.getClientId());
        return scanTicketService.toStatusResult(ticket);
    }
}

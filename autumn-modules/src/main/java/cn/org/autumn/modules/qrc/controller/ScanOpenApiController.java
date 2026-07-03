package cn.org.autumn.modules.qrc.controller;

import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.model.Request;
import cn.org.autumn.model.Response;
import cn.org.autumn.modules.qrc.dto.CreateContext;
import cn.org.autumn.modules.qrc.dto.OpenTicketCreateRequest;
import cn.org.autumn.modules.qrc.dto.TicketActionRequest;
import cn.org.autumn.modules.qrc.dto.TicketCreateResult;
import cn.org.autumn.modules.qrc.dto.TicketStatusResult;
import cn.org.autumn.modules.qrc.model.Intent;
import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import cn.org.autumn.modules.qrc.service.ClientGrantService;
import cn.org.autumn.modules.qrc.service.ScanTicketService;
import cn.org.autumn.modules.usr.interceptor.AuthorizationInterceptor;
import cn.org.autumn.modules.spm.interceptor.SpmInterceptor;
import cn.org.autumn.utils.IPUtils;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/qrc/open/v1")
@SkipInterceptor({AuthorizationInterceptor.class, SpmInterceptor.class})
public class ScanOpenApiController {

    @Autowired
    private ScanTicketService scanTicketService;

    @Autowired
    private ClientGrantService clientGrantService;

    @PostMapping("/ticket/create")
    public Response<TicketCreateResult> create(@Valid @RequestBody Request<OpenTicketCreateRequest> request, HttpServletRequest servlet) {
        try {
            OpenTicketCreateRequest data = request == null ? null : request.getData();
            if (data == null || StringUtils.isBlank(data.getClientId()) || StringUtils.isBlank(data.getClientSecret())) {
                throw new cn.org.autumn.exception.CodeException("client_id与client_secret不能为空", 8609);
            }
            clientGrantService.validateClientSecret(data.getClientId(), data.getClientSecret());
            Map<String, String> payload = new HashMap<>();
            payload.put("clientId", data.getClientId());
            if (StringUtils.isNotBlank(data.getRedirectUri())) {
                payload.put("redirectUri", data.getRedirectUri());
            }
            if (StringUtils.isNotBlank(data.getScope())) {
                payload.put("scope", data.getScope());
            }
            if (StringUtils.isNotBlank(data.getState())) {
                payload.put("state", data.getState());
            }
            if (data.getPayload() != null) {
                payload.putAll(data.getPayload());
            }
            CreateContext ctx = new CreateContext();
            ctx.setIntent(Intent.OAUTH_DEVICE);
            ctx.setClientId(data.getClientId());
            ctx.setClientSecret(data.getClientSecret());
            ctx.setPayload(payload);
            ctx.setIp(IPUtils.getIp(servlet));
            ctx.setAgent(servlet.getHeader("user-agent"));
            TicketSnapshot ticket = scanTicketService.create(ctx);
            return Response.ok(TicketCreateResult.of(ticket.getUuid(), scanTicketService.buildQrUrl(ticket.getUuid()), scanTicketService.getScanLoginConfig().getTicketTtlSeconds(), ticket.getIntent(), ticket.getStatus()));
        } catch (Exception e) {
            return Response.error(e);
        }
    }

    @GetMapping("/ticket/status")
    public Response<TicketStatusResult> status(@RequestParam("uuid") String uuid, @RequestParam("clientId") String clientId, @RequestParam("clientSecret") String clientSecret) {
        try {
            clientGrantService.validateClientSecret(clientId, clientSecret);
            TicketSnapshot ticket = scanTicketService.getRequired(uuid);
            String ticketClient = ticket.getPayload() == null ? null : ticket.getPayload().get("clientId");
            if (StringUtils.isNotBlank(ticketClient) && !clientId.equals(ticketClient)) {
                throw new cn.org.autumn.exception.CodeException("无权查询该票据", 8619);
            }
            return Response.ok(TicketStatusResult.from(ticket));
        } catch (Exception e) {
            return Response.error(e);
        }
    }

    @PostMapping("/ticket/cancel")
    public Response<TicketStatusResult> cancel(@Valid @RequestBody Request<TicketActionRequest> request, @RequestParam("clientId") String clientId, @RequestParam("clientSecret") String clientSecret) {
        try {
            clientGrantService.validateClientSecret(clientId, clientSecret);
            TicketActionRequest data = request == null ? null : request.getData();
            TicketSnapshot ticket = scanTicketService.cancel(data == null ? null : data.getUuid(), clientId);
            return Response.ok(TicketStatusResult.from(ticket));
        } catch (Exception e) {
            return Response.error(e);
        }
    }
}

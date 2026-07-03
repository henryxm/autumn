package cn.org.autumn.modules.qrc.controller;

import cn.org.autumn.annotation.Authenticated;
import cn.org.autumn.model.Request;
import cn.org.autumn.model.Response;
import cn.org.autumn.model.UserContext;
import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import cn.org.autumn.modules.oauth.service.ClientDetailsService;
import cn.org.autumn.modules.qrc.dto.ConfirmResult;
import cn.org.autumn.modules.qrc.dto.TicketActionRequest;
import cn.org.autumn.modules.qrc.dto.TicketDetailResult;
import cn.org.autumn.modules.qrc.dto.TicketStatusResult;
import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import cn.org.autumn.modules.qrc.service.ConsentSupport;
import cn.org.autumn.modules.qrc.service.ScanTicketService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/qrc/api/v1")
public class ScanAppApiController {

    @Autowired
    private ScanTicketService scanTicketService;

    @Autowired
    private ConsentSupport consentSupport;

    @Autowired
    private ClientDetailsService clientDetailsService;

    @GetMapping("/ticket/{uuid}")
    @Authenticated
    public Response<TicketDetailResult> detail(@PathVariable("uuid") String uuid) {
        try {
            TicketSnapshot ticket = scanTicketService.getRequired(uuid);
            String clientId = ticket.getPayload() == null ? null : ticket.getPayload().get("clientId");
            String clientName = "";
            String clientIcon = "";
            if (StringUtils.isNotBlank(clientId)) {
                ClientDetailsEntity client = clientDetailsService.findByClientId(clientId);
                if (client != null) {
                    clientName = client.getClientName();
                    clientIcon = client.getClientIconUri();
                }
            }
            return Response.ok(TicketDetailResult.from(ticket, clientName, clientIcon, consentSupport.describeScopes(ticket)));
        } catch (Exception e) {
            return Response.error(e);
        }
    }

    @PostMapping("/ticket/scan")
    @Authenticated
    public Response<TicketStatusResult> scan(@Valid @RequestBody Request<TicketActionRequest> request, UserContext context) {
        try {
            TicketActionRequest data = request == null ? null : request.getData();
            String uuid = data == null ? null : data.getUuid();
            TicketSnapshot ticket = scanTicketService.scan(uuid, context);
            return Response.ok(TicketStatusResult.from(ticket));
        } catch (Exception e) {
            return Response.error(e);
        }
    }

    @PostMapping("/ticket/confirm")
    @Authenticated
    public Response<TicketStatusResult> confirm(@Valid @RequestBody Request<TicketActionRequest> request, UserContext context, HttpServletRequest servlet) {
        try {
            TicketActionRequest data = request == null ? null : request.getData();
            String uuid = data == null ? null : data.getUuid();
            ConfirmResult confirmResult = scanTicketService.confirm(uuid, context, servlet);
            TicketSnapshot ticket = scanTicketService.getRequired(uuid);
            TicketStatusResult result = TicketStatusResult.from(ticket);
            if (confirmResult != null && StringUtils.isNotBlank(confirmResult.getDeepLink())) {
                result.getResult().put("deepLink", confirmResult.getDeepLink());
            }
            return Response.ok(result);
        } catch (Exception e) {
            return Response.error(e);
        }
    }

    @PostMapping("/ticket/deny")
    @Authenticated
    public Response<TicketStatusResult> deny(@Valid @RequestBody Request<TicketActionRequest> request, UserContext context) {
        try {
            TicketActionRequest data = request == null ? null : request.getData();
            TicketSnapshot ticket = scanTicketService.deny(data == null ? null : data.getUuid(), context);
            return Response.ok(TicketStatusResult.from(ticket));
        } catch (Exception e) {
            return Response.error(e);
        }
    }
}

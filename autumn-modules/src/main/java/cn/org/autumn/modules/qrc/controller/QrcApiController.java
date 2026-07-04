package cn.org.autumn.modules.qrc.controller;

import cn.org.autumn.annotation.Authenticated;
import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.model.Request;
import cn.org.autumn.model.Response;
import cn.org.autumn.model.UserContext;
import cn.org.autumn.modules.qrc.dto.OpenTicketCreateRequest;
import cn.org.autumn.modules.qrc.dto.OpenTicketStatusRequest;
import cn.org.autumn.modules.qrc.dto.TicketActionRequest;
import cn.org.autumn.modules.qrc.dto.TicketCreateResult;
import cn.org.autumn.modules.qrc.dto.TicketDetailRequest;
import cn.org.autumn.modules.qrc.dto.TicketDetailResult;
import cn.org.autumn.modules.qrc.dto.TicketLinkResult;
import cn.org.autumn.modules.qrc.dto.TicketStatusResult;
import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import cn.org.autumn.modules.qrc.service.ScanTicketService;
import cn.org.autumn.modules.qrc.support.QrcApiSupport;
import cn.org.autumn.modules.spm.interceptor.SpmInterceptor;
import cn.org.autumn.modules.usr.interceptor.AuthorizationInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * QRC 开放 API：统一 {@link Request} / {@link Response}，面向 APP 与第三方 Open API。
 */
@Slf4j
@RestController
@RequestMapping("/qrc/api/v1")
public class QrcApiController {

    @Autowired
    private ScanTicketService scanTicketService;

    @Autowired
    private QrcApiSupport qrcApiSupport;

    @GetMapping("/t/{uuid}")
    @SkipInterceptor({AuthorizationInterceptor.class, SpmInterceptor.class})
    public Response<TicketLinkResult> resolve(@PathVariable("uuid") String uuid) {
        try {
            TicketSnapshot ticket = scanTicketService.getRequired(uuid);
            return Response.ok(TicketLinkResult.from(ticket));
        } catch (Exception e) {
            return Response.error(e);
        }
    }

    @PostMapping("/ticket/detail")
    @Authenticated
    public Response<TicketDetailResult> detail(@Valid @RequestBody Request<TicketDetailRequest> request) {
        try {
            TicketDetailRequest data = request == null ? null : request.getData();
            String uuid = data == null ? null : data.getUuid();
            TicketSnapshot ticket = scanTicketService.getRequired(uuid);
            return Response.ok(qrcApiSupport.buildDetail(ticket));
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
            return Response.ok(scanTicketService.toStatusResult(ticket));
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
            scanTicketService.confirm(uuid, context, servlet);
            return Response.ok(scanTicketService.toStatusResult(scanTicketService.getRequired(uuid)));
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
            return Response.ok(scanTicketService.toStatusResult(ticket));
        } catch (Exception e) {
            return Response.error(e);
        }
    }

    @PostMapping("/ticket/open/create")
    @SkipInterceptor({AuthorizationInterceptor.class, SpmInterceptor.class})
    public Response<TicketCreateResult> openCreate(@Valid @RequestBody Request<OpenTicketCreateRequest> request, HttpServletRequest servlet) {
        try {
            OpenTicketCreateRequest data = qrcApiSupport.requireOpenClient(request == null ? null : request.getData());
            TicketSnapshot ticket = scanTicketService.create(qrcApiSupport.buildOpenDeviceContext(data, servlet));
            return Response.ok(scanTicketService.toCreateResult(ticket));
        } catch (Exception e) {
            return Response.error(e);
        }
    }

    @PostMapping("/ticket/open/status")
    @SkipInterceptor({AuthorizationInterceptor.class, SpmInterceptor.class})
    public Response<TicketStatusResult> openStatus(@Valid @RequestBody Request<OpenTicketStatusRequest> request) {
        try {
            OpenTicketStatusRequest data = qrcApiSupport.requireOpenClient(request == null ? null : request.getData());
            TicketSnapshot ticket = scanTicketService.getRequired(data.getUuid());
            qrcApiSupport.assertTicketClient(ticket, data.getClientId());
            return Response.ok(scanTicketService.toStatusResult(ticket));
        } catch (Exception e) {
            return Response.error(e);
        }
    }

    @PostMapping("/ticket/open/cancel")
    @SkipInterceptor({AuthorizationInterceptor.class, SpmInterceptor.class})
    public Response<TicketStatusResult> openCancel(@Valid @RequestBody Request<OpenTicketStatusRequest> request) {
        try {
            OpenTicketStatusRequest data = qrcApiSupport.requireOpenClient(request == null ? null : request.getData());
            TicketSnapshot ticket = scanTicketService.cancel(data.getUuid(), data.getClientId());
            return Response.ok(scanTicketService.toStatusResult(ticket));
        } catch (Exception e) {
            return Response.error(e);
        }
    }
}

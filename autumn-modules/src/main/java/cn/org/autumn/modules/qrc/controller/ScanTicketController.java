package cn.org.autumn.modules.qrc.controller;

import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.model.Request;
import cn.org.autumn.modules.qrc.controller.gen.ScanTicketControllerGen;
import cn.org.autumn.modules.qrc.dto.SessionExchangeRequest;
import cn.org.autumn.modules.qrc.dto.TicketCreateRequest;
import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import cn.org.autumn.modules.qrc.support.ScanWebSupport;
import cn.org.autumn.modules.spm.interceptor.SpmInterceptor;
import cn.org.autumn.modules.usr.interceptor.AuthorizationInterceptor;
import cn.org.autumn.utils.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("qrc/scanticket")
public class ScanTicketController extends ScanTicketControllerGen {

    @Autowired
    private ScanWebSupport scanWebSupport;

    @PostMapping("/web/ticket/create")
    @SkipInterceptor({AuthorizationInterceptor.class, SpmInterceptor.class})
    public R webCreate(@Valid @RequestBody(required = false) Request<TicketCreateRequest> request, HttpServletRequest servlet) {
        try {
            TicketCreateRequest data = request == null ? null : request.getData();
            TicketSnapshot ticket = scanTicketService.create(scanWebSupport.buildWebCreateContext(data, servlet));
            return R.ok().put("data", scanTicketService.toCreateResult(ticket));
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    @GetMapping("/web/ticket/status")
    @SkipInterceptor({AuthorizationInterceptor.class, SpmInterceptor.class})
    public R webStatus(@RequestParam("uuid") String uuid) {
        try {
            TicketSnapshot ticket = scanTicketService.getRequired(uuid);
            return R.ok().put("data", scanTicketService.toStatusResult(ticket));
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    @PostMapping("/web/session/exchange")
    @SkipInterceptor({AuthorizationInterceptor.class, SpmInterceptor.class})
    public R webExchange(@Valid @RequestBody(required = false) Request<SessionExchangeRequest> request, HttpServletRequest servlet) {
        try {
            SessionExchangeRequest data = request == null ? null : request.getData();
            return R.ok().put("data", scanWebSupport.exchangeSession(data, servlet));
        } catch (IllegalArgumentException e) {
            return R.error(e.getMessage());
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }
}

package cn.org.autumn.modules.qrc.controller;

import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.model.Request;
import cn.org.autumn.model.Response;
import cn.org.autumn.modules.qrc.dto.SessionExchangeRequest;
import cn.org.autumn.modules.qrc.dto.TicketCreateRequest;
import cn.org.autumn.modules.qrc.dto.TicketCreateResult;
import cn.org.autumn.modules.qrc.dto.TicketStatusResult;
import cn.org.autumn.modules.qrc.dto.CreateContext;
import cn.org.autumn.modules.qrc.model.Intent;
import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import cn.org.autumn.modules.qrc.service.ScanTicketService;
import cn.org.autumn.modules.qrc.shiro.ScanLoginToken;
import cn.org.autumn.modules.spm.service.SuperPositionModelService;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.shiro.ShiroSessionService;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.modules.usr.interceptor.AuthorizationInterceptor;
import cn.org.autumn.modules.spm.interceptor.SpmInterceptor;
import cn.org.autumn.modules.sys.controller.SysAuthSupport;
import cn.org.autumn.utils.IPUtils;
import cn.org.autumn.utils.R;
import cn.org.autumn.database.CrudGuard;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/qrc/web/v1")
@SkipInterceptor({AuthorizationInterceptor.class, SpmInterceptor.class})
public class ScanWebController {

    @Autowired
    private ScanTicketService scanTicketService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private ShiroSessionService shiroSessionService;

    @Autowired
    private SuperPositionModelService superPositionModelService;

    @Autowired
    private SysConfigService sysConfigService;

    @PostMapping("/ticket/create")
    public Response<TicketCreateResult> create(@Valid @RequestBody(required = false) Request<TicketCreateRequest> request, HttpServletRequest servlet) {
        try {
            TicketCreateRequest data = request == null ? null : request.getData();
            String intent = data == null || StringUtils.isBlank(data.getIntent()) ? Intent.SELF_WEB_LOGIN : data.getIntent();
            CreateContext ctx = new CreateContext();
            ctx.setIntent(intent);
            ctx.setIp(IPUtils.getIp(servlet));
            ctx.setAgent(servlet.getHeader("user-agent"));
            if (data != null && data.getPayload() != null) {
                ctx.setPayload(new HashMap<>(data.getPayload()));
            }
            TicketSnapshot ticket = scanTicketService.create(ctx);
            long expireIn = scanTicketService.getScanLoginConfig().getTicketTtlSeconds();
            return Response.ok(TicketCreateResult.of(ticket.getUuid(), scanTicketService.buildQrUrl(ticket.getUuid()), expireIn, ticket.getIntent(), ticket.getStatus()));
        } catch (Exception e) {
            return Response.error(e);
        }
    }

    @GetMapping("/ticket/status")
    public Response<TicketStatusResult> status(@RequestParam("uuid") String uuid) {
        try {
            TicketSnapshot ticket = scanTicketService.getRequired(uuid);
            return Response.ok(TicketStatusResult.from(ticket));
        } catch (Exception e) {
            return Response.error(e);
        }
    }

    @PostMapping("/session/exchange")
    public R exchange(@Valid @RequestBody(required = false) Request<SessionExchangeRequest> request, HttpServletRequest servlet) {
        try {
            SessionExchangeRequest data = request == null ? null : request.getData();
            String exchange = data == null ? null : data.getExchange();
            if (StringUtils.isBlank(exchange)) {
                return R.error("exchange不能为空");
            }
            boolean rememberMe = data != null && data.isRememberMe();
            CrudGuard.force(() -> {
                sysUserService.login(new ScanLoginToken(exchange));
                try {
                    SysUserEntity current = ShiroUtils.getUserEntity();
                    if (current != null && StringUtils.isNotBlank(current.getUuid())) {
                        shiroSessionService.clearForceLogout(current.getUuid());
                        userProfileService.updateLoginIp(current.getUuid(), IPUtils.getIp(servlet), servlet.getHeader("user-agent"));
                    }
                } catch (Exception ignored) {
                }
            });
            return R.ok().put("data", SysAuthSupport.resolvePostLoginRedirect(servlet, superPositionModelService, sysConfigService.getAccountAuthConfig()));
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }
}

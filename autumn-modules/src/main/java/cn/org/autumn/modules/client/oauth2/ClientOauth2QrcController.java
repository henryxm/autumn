package cn.org.autumn.modules.client.oauth2;

import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.model.Request;
import cn.org.autumn.modules.client.dto.RpQrcCreateRequest;
import cn.org.autumn.modules.client.service.ScanLoginFacade;
import cn.org.autumn.modules.spm.interceptor.SpmInterceptor;
import cn.org.autumn.modules.usr.interceptor.AuthorizationInterceptor;
import cn.org.autumn.utils.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("client/oauth2/qrc/web")
public class ClientOauth2QrcController {

    @Autowired
    ScanLoginFacade scanLoginFacade;

    @PostMapping("/ticket/create")
    @SkipInterceptor({AuthorizationInterceptor.class, SpmInterceptor.class})
    public R create(@Valid @RequestBody(required = false) Request<RpQrcCreateRequest> request, HttpServletRequest servlet) {
        try {
            RpQrcCreateRequest data = request == null ? null : request.getData();
            String callback = data == null ? null : data.getCallback();
            String type = data == null ? null : data.getType();
            String id = data == null ? null : data.getId();
            log.debug("RP QRC create request type={} id={} callback={}", type, id, callback);
            if (StringUtils.isNotBlank(type) && StringUtils.isNotBlank(id)) {
                return R.ok().put("data", scanLoginFacade.createWebTicketByCredential(servlet, type, id, callback));
            }
            return R.ok().put("data", scanLoginFacade.createRpTicket(servlet, callback));
        } catch (Exception e) {
            log.warn("RP QRC create failed: {}", e.getMessage());
            return R.error(e.getMessage());
        }
    }

    @GetMapping(value = "/ticket/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SkipInterceptor({AuthorizationInterceptor.class, SpmInterceptor.class})
    public SseEmitter stream(@RequestParam("uuid") String uuid, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");
        log.debug("RP QRC stream subscribe uuid={}", uuid);
        return scanLoginFacade.streamRpTicket(uuid);
    }

    @GetMapping("/ticket/status")
    @SkipInterceptor({AuthorizationInterceptor.class, SpmInterceptor.class})
    public R status(@RequestParam("uuid") String uuid) {
        try {
            if (StringUtils.isBlank(uuid)) {
                return R.error("uuid 不能为空");
            }
            return R.ok().put("data", scanLoginFacade.pollRpTicketStatus(uuid));
        } catch (Exception e) {
            log.warn("RP QRC status failed uuid={}: {}", uuid, e.getMessage());
            return R.error(e.getMessage());
        }
    }

    @PostMapping("/ticket/cancel")
    @SkipInterceptor({AuthorizationInterceptor.class, SpmInterceptor.class})
    public R cancel(@RequestParam("uuid") String uuid, HttpServletRequest servlet) {
        try {
            if (StringUtils.isBlank(uuid)) {
                return R.error("uuid 不能为空");
            }
            scanLoginFacade.cancelRpTicket(servlet, uuid);
            return R.ok().put("data", uuid);
        } catch (Exception e) {
            log.warn("RP QRC cancel failed uuid={}: {}", uuid, e.getMessage());
            return R.error(e.getMessage());
        }
    }

    @PostMapping("/inbound")
    @SkipInterceptor({AuthorizationInterceptor.class, SpmInterceptor.class})
    public R inbound(@RequestBody(required = false) String rawBody, HttpServletRequest servlet) {
        try {
            if (StringUtils.isBlank(rawBody)) {
                log.warn("RP QRC inbound rejected: empty body");
                return R.error("回调体为空");
            }
            log.debug("RP QRC inbound received bodyLen={} remote={}", rawBody.length(), servlet == null ? null : servlet.getRemoteAddr());
            scanLoginFacade.handleRpInbound(rawBody, readHeaders(servlet));
            return R.ok();
        } catch (Exception e) {
            log.warn("RP QRC inbound failed: {}", e.getMessage());
            return R.error(e.getMessage());
        }
    }

    private static Map<String, String> readHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        if (request == null) {
            return headers;
        }
        Enumeration<String> names = request.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return headers;
    }
}

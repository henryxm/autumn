package cn.org.autumn.modules.client.oauth2;

import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.model.Request;
import cn.org.autumn.modules.client.dto.RpQrcCreateRequest;
import cn.org.autumn.modules.client.service.ScanLoginFacade;
import cn.org.autumn.modules.spm.interceptor.SpmInterceptor;
import cn.org.autumn.modules.usr.interceptor.AuthorizationInterceptor;
import cn.org.autumn.utils.R;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
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
            if (StringUtils.isNotBlank(type) && StringUtils.isNotBlank(id)) {
                return R.ok().put("data", scanLoginFacade.createWebTicketByCredential(servlet, type, id, callback));
            }
            return R.ok().put("data", scanLoginFacade.createRpTicket(servlet, callback));
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    @GetMapping(value = "/ticket/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SkipInterceptor({AuthorizationInterceptor.class, SpmInterceptor.class})
    public SseEmitter stream(@RequestParam("uuid") String uuid, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        return scanLoginFacade.streamRpTicket(uuid);
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
            return R.error(e.getMessage());
        }
    }

    @PostMapping("/inbound")
    @SkipInterceptor({AuthorizationInterceptor.class, SpmInterceptor.class})
    public R inbound(@RequestBody(required = false) String rawBody, HttpServletRequest servlet) {
        try {
            if (StringUtils.isBlank(rawBody)) {
                return R.error("回调体为空");
            }
            scanLoginFacade.handleRpInbound(rawBody, readHeaders(servlet));
            return R.ok();
        } catch (Exception e) {
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

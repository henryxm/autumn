package cn.org.autumn.modules.client.oauth2;

import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.model.Request;
import cn.org.autumn.modules.client.dto.RpQrcCreateRequest;
import cn.org.autumn.modules.client.service.RpQrcPendingStore;
import cn.org.autumn.modules.client.service.ScanLoginFacade;
import cn.org.autumn.modules.qrc.dto.TicketCreateResult;
import cn.org.autumn.modules.qrc.support.ScanBrowserBindSupport;
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

    @Autowired
    ScanBrowserBindSupport scanBrowserBindSupport;

    @Autowired
    RpQrcPendingStore rpQrcPendingStore;

    @PostMapping("/ticket/create")
    @SkipInterceptor({AuthorizationInterceptor.class, SpmInterceptor.class})
    public R create(@Valid @RequestBody(required = false) Request<RpQrcCreateRequest> request,
                    HttpServletRequest servlet, HttpServletResponse response) {
        try {
            RpQrcCreateRequest data = request == null ? null : request.getData();
            String callback = data == null ? null : data.getCallback();
            String type = data == null ? null : data.getType();
            String id = data == null ? null : data.getId();
            log.debug("RP QRC create request type={} id={} callback={}", type, id, callback);
            TicketCreateResult created;
            if (StringUtils.isNotBlank(type) && StringUtils.isNotBlank(id)) {
                created = scanLoginFacade.createWebTicketByCredential(servlet, type, id, callback);
            } else {
                created = scanLoginFacade.createRpTicket(servlet, callback);
            }
            if (created != null && StringUtils.isNotBlank(created.getUuid())) {
                scanBrowserBindSupport.bindOnCreate(created.getUuid(), servlet, response);
                // redisBacked=false 才值得 INFO；正常路径 DEBUG，避免误诊噪音
                if (rpQrcPendingStore.isRedisBacked()) {
                    log.debug("RP QRC create ok uuid={} redisBacked=true", created.getUuid());
                } else {
                    log.warn("RP QRC create ok uuid={} redisBacked=false（多副本 status 会 miss）", created.getUuid());
                }
            }
            return R.ok().put("data", created);
        } catch (Exception e) {
            log.warn("RP QRC create failed redisBacked={}: {}", rpQrcPendingStore.isRedisBacked(), e.getMessage());
            return R.error(e.getMessage());
        }
    }

    @GetMapping(value = "/ticket/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SkipInterceptor({AuthorizationInterceptor.class, SpmInterceptor.class})
    public SseEmitter stream(@RequestParam("uuid") String uuid,
                             HttpServletRequest request,
                             HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-transform");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");
        scanBrowserBindSupport.assertBoundByUuid(uuid, request);
        log.debug("RP QRC stream subscribe uuid={}", uuid);
        return scanLoginFacade.streamRpTicket(uuid);
    }

    @GetMapping("/ticket/status")
    @SkipInterceptor({AuthorizationInterceptor.class, SpmInterceptor.class})
    public R status(@RequestParam("uuid") String uuid, HttpServletRequest request) {
        try {
            if (StringUtils.isBlank(uuid)) {
                return R.error("uuid 不能为空");
            }
            scanBrowserBindSupport.assertBoundByUuid(uuid, request);
            return R.ok().put("data", scanLoginFacade.pollRpTicketStatus(uuid));
        } catch (Exception e) {
            // pending miss 排障：保留 redisBacked，勿与「跳转未登录/SessionFixation」混为一谈
            log.warn("RP QRC status failed uuid={} redisBacked={}: {}", uuid, rpQrcPendingStore.isRedisBacked(), e.getMessage());
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
            scanBrowserBindSupport.assertBoundByUuid(uuid, servlet);
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
        String uuidHint = peekInboundUuid(rawBody);
        String eventHint = peekInboundEvent(rawBody);
        try {
            if (StringUtils.isBlank(rawBody)) {
                log.warn("RP QRC inbound rejected: empty body");
                return R.error("回调体为空");
            }
            log.debug("RP QRC inbound received uuid={} event={} bodyLen={} remote={}",
                    uuidHint, eventHint, rawBody.length(), servlet == null ? null : servlet.getRemoteAddr());
            scanLoginFacade.handleRpInbound(rawBody, readHeaders(servlet));
            log.info("RP QRC inbound ok uuid={} event={}", uuidHint, eventHint);
            return R.ok();
        } catch (Exception e) {
            log.warn("RP QRC inbound fail uuid={} event={} msg={}", uuidHint, eventHint, e.getMessage());
            return R.error(e.getMessage());
        }
    }

    private static String peekInboundUuid(String rawBody) {
        return peekJsonString(rawBody, "uuid");
    }

    private static String peekInboundEvent(String rawBody) {
        return peekJsonString(rawBody, "event");
    }

    /** 轻量截取 JSON 字符串字段，仅用于日志，失败返回 null。 */
    private static String peekJsonString(String rawBody, String field) {
        if (StringUtils.isBlank(rawBody) || StringUtils.isBlank(field)) {
            return null;
        }
        String key = "\"" + field + "\"";
        int i = rawBody.indexOf(key);
        if (i < 0) {
            return null;
        }
        int colon = rawBody.indexOf(':', i + key.length());
        if (colon < 0) {
            return null;
        }
        int q1 = rawBody.indexOf('"', colon + 1);
        int q2 = q1 >= 0 ? rawBody.indexOf('"', q1 + 1) : -1;
        if (q1 < 0 || q2 <= q1) {
            return null;
        }
        return rawBody.substring(q1 + 1, q2);
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

package cn.org.autumn.modules.qrc.support;

import cn.org.autumn.exception.AException;
import cn.org.autumn.modules.qrc.model.TicketPayloads;
import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import cn.org.autumn.modules.qrc.service.ScanTicketService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;

/**
 * Web 扫码票据绑定创建浏览器：status / stream / exchange 须持有创建时下发的 HttpOnly Cookie，
 * 防止仅凭 uuid / exchange 在其它浏览器抢登。
 * <p>
 * Cookie 按 uuid 分名（{@code autumn_qrc_bind_{uuid}}），多标签互不覆盖；
 * <strong>固定 host-only</strong>（不设 Domain）——create/status/stream/exchange 恒同源，
 * 避免集群根 Domain 与 Host 不一致导致 8631。Shiro 会话 Cookie 仍可由 {@code CLUSTER_ROOT_DOMAIN} 跨子域。
 * 同源票写入 ticket payload；RP 远程票写入 Redis/本机旁路（与票同 TTL 量级）。
 * 升级前已发出、payload 无 {@code browserBind} 的短命票据放行至过期。
 */
@Component
public class ScanBrowserBindSupport {

    public static final String COOKIE_NAME = "autumn_qrc_bind";
    public static final String PAYLOAD_KEY = TicketPayloads.BROWSER_BIND;
    private static final long LOCAL_BIND_TTL_MS = 10 * 60 * 1000L;

    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    private ScanTicketService scanTicketService;

    public void bindOnCreate(TicketSnapshot ticket, HttpServletResponse response) {
        bindOnCreate(ticket, null, response);
    }

    public void bindOnCreate(TicketSnapshot ticket, HttpServletRequest request, HttpServletResponse response) {
        if (ticket == null || StringUtils.isBlank(ticket.getUuid()) || response == null) {
            return;
        }
        String secret = newSecret();
        if (ticket.getPayload() == null) {
            ticket.setPayload(new HashMap<>());
        }
        ticket.getPayload().put(PAYLOAD_KEY, secret);
        scanTicketService.persistTicket(ticket);
        rememberBind(ticket.getUuid(), secret);
        writeCookie(request, response, ticket.getUuid(), secret);
    }

    /** RP 等仅有 uuid、无本地 TicketSnapshot 时。 */
    public void bindOnCreate(String uuid, HttpServletResponse response) {
        bindOnCreate(uuid, null, response);
    }

    public void bindOnCreate(String uuid, HttpServletRequest request, HttpServletResponse response) {
        if (StringUtils.isBlank(uuid) || response == null) {
            return;
        }
        TicketSnapshot ticket = scanTicketService.findTicket(uuid);
        if (ticket != null) {
            bindOnCreate(ticket, request, response);
            return;
        }
        String secret = newSecret();
        rememberBind(uuid, secret);
        writeCookie(request, response, uuid, secret);
    }

    public void assertBound(TicketSnapshot ticket, HttpServletRequest request) {
        if (ticket == null) {
            throw new AException("票据不存在或已过期", 8610);
        }
        boolean legacy = ticket.getPayload() == null || !ticket.getPayload().containsKey(PAYLOAD_KEY);
        assertBound(ticket.getUuid(), TicketPayloads.get(ticket, PAYLOAD_KEY), legacy, request);
    }

    public void assertBoundByUuid(String uuid, HttpServletRequest request) {
        if (StringUtils.isBlank(uuid)) {
            throw new AException("uuid 不能为空", 400);
        }
        TicketSnapshot ticket = scanTicketService.findTicket(uuid);
        if (ticket != null) {
            assertBound(ticket, request);
            return;
        }
        assertBound(uuid, null, false, request);
    }

    public void assertBoundForExchange(String exchange, HttpServletRequest request) {
        if (StringUtils.isBlank(exchange)) {
            throw new IllegalArgumentException("exchange不能为空");
        }
        String uuid = scanTicketService.peekExchangeTicketUuid(exchange);
        if (StringUtils.isBlank(uuid)) {
            throw new AException("exchange无效或已过期", 8621);
        }
        assertBoundByUuid(uuid, request);
    }

    private void assertBound(String uuid, String ticketExpected, boolean legacyWithoutBind, HttpServletRequest request) {
        String expected = ticketExpected;
        if (StringUtils.isBlank(expected)) {
            expected = scanTicketService.getBrowserBind(uuid);
        }
        if (StringUtils.isBlank(expected)) {
            if (legacyWithoutBind) {
                // 升级前短命票：无 browserBind 字段则放行至自然过期
                return;
            }
            throw new AException("票据未绑定浏览器，请重新扫码登录", 8630);
        }
        String got = readBindSecret(request, uuid);
        if (!constantTimeEquals(expected, got)) {
            throw new AException("票据与当前浏览器不匹配", 8631);
        }
    }

    private String newSecret() {
        byte[] raw = new byte[24];
        secureRandom.nextBytes(raw);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }

    private void rememberBind(String uuid, String secret) {
        scanTicketService.putBrowserBind(uuid, secret, LOCAL_BIND_TTL_MS / 1000);
    }

    private void writeCookie(HttpServletRequest request, HttpServletResponse response, String uuid, String secret) {
        // 按 uuid 分名，多标签互不覆盖；值仅为 secret；host-only（不设 Domain）
        Cookie cookie = new Cookie(cookieNameForUuid(uuid), secret);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) (LOCAL_BIND_TTL_MS / 1000));
        applySameSite(cookie);
        response.addCookie(cookie);
        // 兼容旧单 Cookie（后开标签仍会覆盖，但旧页可读新格式）
        Cookie legacy = new Cookie(COOKIE_NAME, uuid + "." + secret);
        legacy.setHttpOnly(true);
        legacy.setPath("/");
        legacy.setMaxAge((int) (LOCAL_BIND_TTL_MS / 1000));
        applySameSite(legacy);
        response.addCookie(legacy);
    }

    private static void applySameSite(Cookie cookie) {
        try {
            cookie.setAttribute("SameSite", "Lax");
        } catch (Exception ignored) {
        }
    }

    static String cookieNameForUuid(String uuid) {
        // Cookie 名仅允许 token 字符；uuid 中的 '-' 合法
        return COOKIE_NAME + "_" + uuid;
    }

    private static String readBindSecret(HttpServletRequest request, String uuid) {
        if (request == null || request.getCookies() == null || StringUtils.isBlank(uuid)) {
            return null;
        }
        String perUuidName = cookieNameForUuid(uuid);
        String fromNamed = null;
        String fromLegacy = null;
        for (Cookie cookie : request.getCookies()) {
            if (cookie == null || StringUtils.isBlank(cookie.getName())) {
                continue;
            }
            if (perUuidName.equals(cookie.getName()) && StringUtils.isNotBlank(cookie.getValue())) {
                fromNamed = cookie.getValue();
            } else if (COOKIE_NAME.equals(cookie.getName())) {
                String v = cookie.getValue();
                if (StringUtils.isBlank(v)) {
                    continue;
                }
                int dot = v.indexOf('.');
                if (dot <= 0 || dot >= v.length() - 1) {
                    continue;
                }
                if (uuid.equals(v.substring(0, dot))) {
                    fromLegacy = v.substring(dot + 1);
                }
            }
        }
        return StringUtils.isNotBlank(fromNamed) ? fromNamed : fromLegacy;
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}

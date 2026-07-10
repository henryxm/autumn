package cn.org.autumn.modules.qrc.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.ScanLoginConfig;
import cn.org.autumn.model.UserContext;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.qrc.dao.ScanTicketDao;
import cn.org.autumn.modules.qrc.dto.ConfirmResult;
import cn.org.autumn.modules.qrc.dto.CreateContext;
import cn.org.autumn.modules.qrc.dto.ScannerBrief;
import cn.org.autumn.modules.qrc.dto.TicketCreateResult;
import cn.org.autumn.modules.qrc.dto.TicketStatusResult;
import cn.org.autumn.modules.qrc.entity.ScanTicketEntity;
import cn.org.autumn.modules.qrc.entity.ClientGrantEntity;
import cn.org.autumn.modules.qrc.model.ExchangeSnapshot;
import cn.org.autumn.modules.qrc.model.Intent;
import cn.org.autumn.modules.qrc.model.TicketPayloads;
import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import cn.org.autumn.modules.qrc.model.TicketStatus;
import cn.org.autumn.modules.qrc.service.handler.IntentHandler;
import cn.org.autumn.modules.qrc.service.handler.IntentHandlerRegistry;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.usr.service.UserLoginLogService;
import cn.org.autumn.utils.RedisUtils;
import cn.org.autumn.utils.Uuid;
import cn.org.autumn.utils.WebPathUtils;
import com.alibaba.fastjson2.JSON;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.oltu.oauth2.common.OAuth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

@Slf4j
@Service
public class ScanTicketService extends ModuleService<ScanTicketDao, ScanTicketEntity> implements LoopJob.OneDay {

    public static final String TICKET_KEY_PREFIX = "qrc:ticket:";
    public static final String EXCHANGE_KEY_PREFIX = "qrc:exchange:";

    private final Map<String, TicketSnapshot> localTickets = new ConcurrentHashMap<>();
    private final Map<String, ExchangeSnapshot> localExchanges = new ConcurrentHashMap<>();

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    @Lazy
    private SysConfigService sysConfigService;

    @Autowired
    private IntentHandlerRegistry handlerRegistry;

    @Autowired
    @Lazy
    private SysUserService sysUserService;

    @Autowired
    @Lazy
    private UserLoginLogService userLoginLogService;

    @Autowired
    @Lazy
    private QrcWebhookDeliveryService qrcWebhookDeliveryService;

    @Autowired
    @Lazy
    private ClientGrantService clientGrantService;

    @Override
    public String ico() {
        return "fa-qrcode";
    }

    public ScanLoginConfig getScanLoginConfig() {
        ScanLoginConfig model = sysConfigService.getConfigObjectValidate(ScanLoginConfig.CONFIG_KEY, ScanLoginConfig.class);
        return model == null ? new ScanLoginConfig() : model;
    }

    public TicketSnapshot createAuthorizeTicket(HttpServletRequest request, String clientId, String redirectUri, String scope, String state, String callback) throws Exception {
        return createOAuthTicket(request, Intent.OAUTH_AUTHORIZE, clientId, redirectUri, scope, state, callback);
    }

    public TicketSnapshot createConsentTicket(HttpServletRequest request, String clientId, String redirectUri, String scope, String state, String callback) throws Exception {
        return createOAuthTicket(request, Intent.OAUTH_CONSENT, clientId, redirectUri, scope, state, callback);
    }

    public void fillAuthorizeModel(Model model, TicketSnapshot ticket) {
        fillAuthorizeModel(null, model, ticket);
    }

    public void fillAuthorizeModel(HttpServletRequest request, Model model, TicketSnapshot ticket) {
        model.addAttribute("uuid", ticket.getUuid());
        model.addAttribute("qrUrl", buildQrUrl(request, ticket.getUuid()));
        model.addAttribute("pollIntervalMs", getScanLoginConfig().getPollIntervalMs());
        model.addAttribute("clientId", TicketPayloads.get(ticket, "clientId"));
        model.addAttribute("scope", TicketPayloads.get(ticket, "scope"));
        model.addAttribute("state", TicketPayloads.get(ticket, "state"));
    }

    public boolean shouldUseQrAuthorize() {
        return getScanLoginConfig().isOauthQrFirst();
    }

    @Override
    public void onOneDay() {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, -7);
            baseMapper.deleteExpiredBefore(calendar.getTime());
        } catch (Exception e) {
            log.warn("QRC ticket audit cleanup failed: {}", e.getMessage());
        }
    }

    public TicketSnapshot create(CreateContext ctx) throws Exception {
        if (ctx == null || StringUtils.isBlank(ctx.getIntent())) {
            throw new CodeException("intent不能为空", 8600);
        }
        IntentHandler handler = handlerRegistry.require(ctx.getIntent());
        TicketSnapshot ticket = newSnapshot(ctx);
        handler.onCreate(ticket, ctx);
        saveTicket(ticket);
        persistAudit(ticket, false);
        return ticket;
    }

    public TicketSnapshot getRequired(String uuid) throws CodeException {
        TicketSnapshot ticket = getTicket(uuid);
        if (ticket == null) {
            throw new CodeException("票据不存在或已过期", 8610);
        }
        refreshExpiry(ticket);
        if (TicketStatus.EXPIRED.equals(ticket.getStatus())) {
            throw new CodeException("票据已过期", 8611);
        }
        return ticket;
    }

    public TicketSnapshot scan(String uuid, UserContext scanner) throws Exception {
        return withLock("qrc:ticket:" + uuid, () -> {
            TicketSnapshot ticket = getRequired(uuid);
            if (!TicketStatus.PENDING.equals(ticket.getStatus())) {
                throw new CodeException("票据状态不可扫码", 8612);
            }
            if (scanner == null || StringUtils.isBlank(scanner.getUuid()) || scanner.isRobot()) {
                throw new CodeException("请使用已登录用户扫码", 8613);
            }
            IntentHandler handler = handlerRegistry.require(ticket.getIntent());
            handler.onScan(ticket, scanner);
            ticket.setScanner(scanner.getUuid());
            ticket.setStatus(TicketStatus.SCANNED);
            saveTicket(ticket);
            log.info("QRC ticket scanned uuid={} scanner={} intent={} delivery={}", uuid, scanner.getUuid(), ticket.getIntent(), TicketPayloads.get(ticket, "delivery"));
            deliverScannedWebhook(ticket, scanner.getUuid());
            return ticket;
        });
    }

    public ConfirmResult confirm(String uuid, UserContext scanner, HttpServletRequest request) throws Exception {
        return withLock("qrc:ticket:" + uuid, () -> {
            TicketSnapshot ticket = getRequired(uuid);
            if (TicketStatus.DENIED.equals(ticket.getStatus()) || TicketStatus.CANCELLED.equals(ticket.getStatus())) {
                throw new CodeException("票据已拒绝或取消", 8614);
            }
            if (!TicketStatus.SCANNED.equals(ticket.getStatus()) && !TicketStatus.PENDING.equals(ticket.getStatus())) {
                throw new CodeException("票据状态不可确认", 8615);
            }
            if (scanner == null || StringUtils.isBlank(scanner.getUuid()) || scanner.isRobot()) {
                throw new CodeException("请使用已登录用户确认", 8616);
            }
            if (TicketStatus.PENDING.equals(ticket.getStatus())) {
                ticket.setScanner(scanner.getUuid());
                ticket.setStatus(TicketStatus.SCANNED);
            }
            if (!scanner.getUuid().equals(ticket.getScanner())) {
                throw new CodeException("扫码用户与确认用户不一致", 8617);
            }
            IntentHandler handler = handlerRegistry.require(ticket.getIntent());
            ConfirmResult result = handler.onConfirm(ticket, scanner);
            ticket.setSubject(scanner.getUuid());
            ticket.setStatus(TicketStatus.CONFIRMED);
            if (result != null && result.isCompleted()) {
                ticket.setStatus(TicketStatus.COMPLETED);
                if (result.getResult() != null) {
                    ticket.setResult(result.getResult());
                }
                if (StringUtils.isNotBlank(result.getRedirect())) {
                    ticket.setRedirect(result.getRedirect());
                }
            }
            if (result != null && StringUtils.isNotBlank(result.getExchange())) {
                ticket.setExchange(result.getExchange());
            }
            if (result != null && StringUtils.isNotBlank(result.getDeepLink())) {
                ticket.getResult().put("deepLink", result.getDeepLink());
            }
            saveTicket(ticket);
            persistAudit(ticket, true);
            log.info("QRC ticket confirmed uuid={} scanner={} intent={} status={} delivery={}", uuid, scanner.getUuid(), ticket.getIntent(), ticket.getStatus(), TicketPayloads.get(ticket, "delivery"));
            writeLoginLog(ticket, scanner.getUuid(), request);
            return result;
        });
    }

    public TicketSnapshot deny(String uuid, UserContext scanner) throws Exception {
        return withLock("qrc:ticket:" + uuid, () -> {
            TicketSnapshot ticket = getRequired(uuid);
            ticket.setStatus(TicketStatus.DENIED);
            if (scanner != null) {
                ticket.setScanner(scanner.getUuid());
            }
            saveTicket(ticket);
            deliverDeniedWebhook(ticket, scanner == null ? null : scanner.getUuid());
            persistAudit(ticket, true);
            return ticket;
        });
    }

    public TicketSnapshot cancel(String uuid, String clientId) throws Exception {
        return withLock("qrc:ticket:" + uuid, () -> {
            TicketSnapshot ticket = getRequired(uuid);
            if (StringUtils.isNotBlank(clientId)) {
                String ticketClient = TicketPayloads.get(ticket, "clientId");
                if (StringUtils.isNotBlank(ticketClient) && !clientId.equals(ticketClient)) {
                    throw new CodeException("无权取消该票据", 8618);
                }
            }
            ticket.setStatus(TicketStatus.CANCELLED);
            saveTicket(ticket);
            persistAudit(ticket, true);
            return ticket;
        });
    }

    public String createExchangeToken(String user, String uuid) {
        ExchangeSnapshot snapshot = new ExchangeSnapshot();
        snapshot.setExchange(Uuid.uuid());
        snapshot.setUser(user);
        snapshot.setUuid(uuid);
        snapshot.setExpired(System.currentTimeMillis() + getScanLoginConfig().getExchangeTokenTtlSeconds() * 1000L);
        saveExchange(snapshot);
        return snapshot.getExchange();
    }

    public String peekExchangeTicketIntent(String exchange) {
        if (StringUtils.isBlank(exchange)) {
            return null;
        }
        ExchangeSnapshot snapshot = getExchange(exchange);
        if (snapshot == null || StringUtils.isBlank(snapshot.getUuid())) {
            return null;
        }
        TicketSnapshot ticket = getTicket(snapshot.getUuid());
        return ticket == null ? null : ticket.getIntent();
    }

    public SysUserEntity consumeExchangeToken(String exchange) throws CodeException {
        if (StringUtils.isBlank(exchange)) {
            throw new CodeException("exchange不能为空", 8620);
        }
        ExchangeSnapshot snapshot = getExchange(exchange);
        if (snapshot == null || snapshot.getExpired() < System.currentTimeMillis()) {
            throw new CodeException("exchange无效或已过期", 8621);
        }
        removeExchange(exchange);
        SysUserEntity userEntity = sysUserService.getByUuid(snapshot.getUser());
        if (userEntity == null || userEntity.getStatus() < 1) {
            throw new CodeException("用户不可用", 8622);
        }
        return userEntity;
    }

    public SysUserEntity requireActiveUser(String userUuid) throws CodeException {
        SysUserEntity user = sysUserService.getByUuid(userUuid);
        if (user == null || user.getStatus() < 1) {
            throw new CodeException("用户不可用", 8623);
        }
        return user;
    }

    public TicketCreateResult toCreateResult(TicketSnapshot ticket) {
        return TicketCreateResult.of(ticket.getUuid(), buildQrUrl(ticket.getUuid()), getScanLoginConfig().getTicketTtlSeconds(), ticket.getIntent(), ticket.getStatus());
    }

    public TicketStatusResult toStatusResult(TicketSnapshot ticket) {
        TicketStatusResult result = TicketStatusResult.from(ticket);
        if (ticket != null && StringUtils.isNotBlank(ticket.getScanner()) && shouldExposeScannerBrief(ticket.getStatus())) {
            result.setScannerBrief(buildScannerBrief(ticket.getScanner()));
        }
        return result;
    }

    private void deliverScannedWebhook(TicketSnapshot ticket, String scannerUuid) {
        if (!qrcWebhookDeliveryService.shouldDeliverWebhook(ticket)) {
            log.info("QRC scan webhook skipped uuid={} delivery={} webhook={}", ticket.getUuid(), TicketPayloads.get(ticket, "delivery"), TicketPayloads.get(ticket, "webhook"));
            return;
        }
        String clientId = TicketPayloads.get(ticket, "clientId");
        ClientGrantEntity grant = StringUtils.isBlank(clientId) ? null : clientGrantService.getOrDefault(clientId);
        Map<String, Object> data = new HashMap<>();
        data.put("status", TicketStatus.SCANNED);
        ScannerBrief brief = buildScannerBrief(scannerUuid);
        if (brief != null) {
            data.put("scannerBrief", brief);
        }
        qrcWebhookDeliveryService.deliverScanned(ticket, grant, data);
    }

    private void deliverDeniedWebhook(TicketSnapshot ticket, String scannerUuid) {
        if (!qrcWebhookDeliveryService.shouldDeliverWebhook(ticket)) {
            return;
        }
        String clientId = TicketPayloads.get(ticket, "clientId");
        ClientGrantEntity grant = StringUtils.isBlank(clientId) ? null : clientGrantService.getOrDefault(clientId);
        Map<String, Object> data = new HashMap<>();
        data.put("status", TicketStatus.DENIED);
        ScannerBrief brief = buildScannerBrief(scannerUuid);
        if (brief != null) {
            data.put("scannerBrief", brief);
        }
        qrcWebhookDeliveryService.deliverDenied(ticket, grant, data);
    }

    private boolean shouldExposeScannerBrief(String status) {
        return TicketStatus.SCANNED.equals(status) || TicketStatus.CONFIRMED.equals(status) || TicketStatus.COMPLETED.equals(status);
    }

    private ScannerBrief buildScannerBrief(String scannerUuid) {
        SysUserEntity user = sysUserService.getByUuid(scannerUuid);
        if (user == null) {
            return null;
        }
        ScannerBrief brief = new ScannerBrief();
        brief.setDisplayName(StringUtils.isNotBlank(user.getNickname()) ? user.getNickname() : user.getUsername());
        brief.setIcon(user.getIcon());
        return brief;
    }

    public String buildQrUrl(String uuid) {
        return buildQrUrl(null, uuid);
    }

    public String buildQrUrl(HttpServletRequest request, String uuid) {
        String base = WebPathUtils.absoluteBaseUrl(request, sysConfigService.isSsl());
        if (StringUtils.isBlank(base)) {
            base = sysConfigService.getBaseUrl();
        }
        if (StringUtils.isBlank(base)) {
            base = "";
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/qrc/api/v1/t/" + uuid;
    }

    private TicketSnapshot createOAuthTicket(HttpServletRequest request, String intent, String clientId, String redirectUri, String scope, String state, String callback) throws Exception {
        Map<String, String> payload = new HashMap<>();
        payload.put("clientId", clientId);
        payload.put("redirectUri", redirectUri);
        payload.put("scope", scope == null ? "" : scope);
        payload.put("state", state == null ? "" : state);
        payload.put("callback", callback == null ? "" : callback);
        payload.put(OAuth.OAUTH_RESPONSE_TYPE, "code");
        CreateContext ctx = new CreateContext();
        ctx.setIntent(intent);
        ctx.setClientId(clientId);
        ctx.setPayload(payload);
        ctx.setIp(request.getRemoteAddr());
        ctx.setAgent(request.getHeader("user-agent"));
        return create(ctx);
    }

    private TicketSnapshot newSnapshot(CreateContext ctx) {
        TicketSnapshot ticket = new TicketSnapshot();
        ticket.setUuid(Uuid.uuid());
        ticket.setIntent(ctx.getIntent());
        ticket.setStatus(TicketStatus.PENDING);
        if (ctx.getPayload() != null) {
            ticket.setPayload(new HashMap<>(ctx.getPayload()));
        }
        ticket.setIp(ctx.getIp());
        ticket.setAgent(ctx.getAgent());
        long now = System.currentTimeMillis();
        ticket.setCreated(now);
        ticket.setExpired(now + getScanLoginConfig().getTicketTtlSeconds() * 1000L);
        return ticket;
    }

    private TicketSnapshot getTicket(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return null;
        }
        String key = TICKET_KEY_PREFIX + uuid;
        Object value = redisUtils.isOpen() ? redisUtils.get(key) : localTickets.get(uuid);
        if (value instanceof TicketSnapshot) {
            return (TicketSnapshot) value;
        }
        return localTickets.get(uuid);
    }

    private void saveTicket(TicketSnapshot ticket) {
        if (ticket == null || StringUtils.isBlank(ticket.getUuid())) {
            return;
        }
        long ttl = Math.max(1, (ticket.getExpired() - System.currentTimeMillis()) / 1000);
        String key = TICKET_KEY_PREFIX + ticket.getUuid();
        if (redisUtils.isOpen()) {
            redisUtils.set(key, ticket, ttl);
        }
        localTickets.put(ticket.getUuid(), ticket);
    }

    private void refreshExpiry(TicketSnapshot ticket) {
        if (ticket.getExpired() < System.currentTimeMillis() && !TicketStatus.isTerminal(ticket.getStatus())) {
            ticket.setStatus(TicketStatus.EXPIRED);
            saveTicket(ticket);
        }
    }

    private void saveExchange(ExchangeSnapshot snapshot) {
        long ttl = Math.max(1, getScanLoginConfig().getExchangeTokenTtlSeconds());
        String key = EXCHANGE_KEY_PREFIX + snapshot.getExchange();
        if (redisUtils.isOpen()) {
            redisUtils.set(key, snapshot, ttl);
        }
        localExchanges.put(snapshot.getExchange(), snapshot);
    }

    private ExchangeSnapshot getExchange(String token) {
        String key = EXCHANGE_KEY_PREFIX + token;
        Object value = redisUtils.isOpen() ? redisUtils.get(key) : localExchanges.get(token);
        if (value instanceof ExchangeSnapshot) {
            return (ExchangeSnapshot) value;
        }
        return localExchanges.get(token);
    }

    private void removeExchange(String token) {
        if (redisUtils.isOpen()) {
            redisUtils.delete(EXCHANGE_KEY_PREFIX + token);
        }
        localExchanges.remove(token);
    }

    private void persistAudit(TicketSnapshot ticket, boolean complete) {
        try {
            ScanTicketEntity entity = baseMapper.getByUuid(ticket.getUuid());
            if (entity == null) {
                entity = new ScanTicketEntity();
                entity.setUuid(ticket.getUuid());
                entity.setCreated(new Date(ticket.getCreated()));
            }
            entity.setIntent(ticket.getIntent());
            entity.setStatus(ticket.getStatus());
            entity.setClientId(TicketPayloads.get(ticket, "clientId"));
            entity.setScanner(ticket.getScanner());
            entity.setSubject(ticket.getSubject());
            entity.setIp(ticket.getIp());
            entity.setAgent(ticket.getAgent());
            entity.setPayload(JSON.toJSONString(ticket.getPayload()));
            entity.setResult(JSON.toJSONString(ticket.getResult()));
            entity.setExpired(new Date(ticket.getExpired()));
            if (complete) {
                entity.setCompleted(new Date());
            }
            insertOrUpdate(entity);
        } catch (Exception e) {
            log.debug("QRC audit persist skipped: {}", e.getMessage());
        }
    }

    private void writeLoginLog(TicketSnapshot ticket, String userUuid, HttpServletRequest request) {
        if (request == null || StringUtils.isBlank(userUuid)) {
            return;
        }
        String way = Intent.SELF_WEB_LOGIN.equals(ticket.getIntent()) ? "qrc_self" : "qrc_oauth";
        userLoginLogService.login(userUuid, "", true, way, "扫码登录", request);
    }
}

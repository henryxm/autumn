package cn.org.autumn.modules.sys.shiro;

import cn.org.autumn.bean.EnvBean;
import cn.org.autumn.model.SecurityAuthConfig;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.node.ProfileStore;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 管理员引导口令生命周期：落盘、待办标记、改密/确认清理、会话提示。
 */
@Slf4j
@Service
public class AdminBootstrapService {

    public static final String SESSION_SNOOZE_UNTIL = "autumn.adminBootstrap.snoozeUntil";
    public static final long SNOOZE_TTL_MS = 24L * 60 * 60 * 1000;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private EnvBean envBean;

    @Autowired(required = false)
    private ProfileStore profileStore;

    public Path nodeHome() {
        return profileStore != null ? profileStore.home() : null;
    }

    public boolean hasConfiguredSystemPassword() {
        return StringUtils.isNotBlank(envBean.getSystemPassword());
    }

    /**
     * 首次创建随机引导口令 admin：写恢复文件并置 pending。
     */
    public void onBootstrapAdminCreated(String username, String plainPassword) {
        if (hasConfiguredSystemPassword() || StringUtils.isBlank(plainPassword)) {
            return;
        }
        AdminBootstrapPasswordSupport.writeCredentials(nodeHome(), username, plainPassword);
        markPending(true, null);
        if (log.isDebugEnabled()) {
            log.debug("Admin bootstrap pending set username={} file={}",
                    username, AdminBootstrapPasswordSupport.resolveFile(nodeHome()));
        }
    }

    public boolean isPending() {
        return sysConfigService.getSecurityAuthConfig().isAdminBootstrapPending();
    }

    public void markPending(boolean pending, String acknowledgedAtIso) {
        SecurityAuthConfig cfg = copyAuth(sysConfigService.getSecurityAuthConfig());
        cfg.setAdminBootstrapPending(pending);
        if (acknowledgedAtIso != null) {
            cfg.setBootstrapAcknowledgedAt(acknowledgedAtIso);
        } else if (pending) {
            cfg.setBootstrapAcknowledgedAt("");
        }
        sysConfigService.saveSecurityAuthConfig(cfg);
    }

    /**
     * 改密/重置成功：清 pending、删恢复文件。
     */
    public void clearBootstrapState(String reason) {
        boolean wasPending = isPending();
        boolean hadFile = AdminBootstrapPasswordSupport.exists(nodeHome());
        AdminBootstrapPasswordSupport.deleteQuietly(nodeHome());
        if (wasPending || hadFile) {
            SecurityAuthConfig cfg = copyAuth(sysConfigService.getSecurityAuthConfig());
            cfg.setAdminBootstrapPending(false);
            sysConfigService.saveSecurityAuthConfig(cfg);
            log.info("Cleared admin bootstrap state reason={} wasPending={} hadFile={}", reason, wasPending, hadFile);
        }
    }

    public void acknowledgeRetain() {
        AdminBootstrapPasswordSupport.deleteQuietly(nodeHome());
        markPending(false, Instant.now().toString());
        log.info("Admin password retained by user; bootstrap pending cleared");
    }

    public void snooze(Session session) {
        if (session != null) {
            session.setAttribute(SESSION_SNOOZE_UNTIL, System.currentTimeMillis() + SNOOZE_TTL_MS);
        }
    }

    public Map<String, Object> buildPasswordReminder(Session session) {
        Map<String, Object> reminder = new LinkedHashMap<>();
        if (!isPending()) {
            reminder.put("pending", false);
            reminder.put("level", "none");
            reminder.put("message", "");
            reminder.put("actions", Collections.emptyList());
            return reminder;
        }
        boolean snoozed = isSnoozed(session);
        reminder.put("pending", true);
        reminder.put("level", snoozed ? "weak" : "strong");
        reminder.put("message", snoozed
                ? "仍建议修改系统随机初始管理员口令"
                : "当前为系统随机初始管理员口令，建议立即修改");
        reminder.put("actions", Arrays.asList("change", "snooze", "acknowledge"));
        reminder.put("snoozed", snoozed);
        return reminder;
    }

    public Map<String, Object> opsStatus() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("pending", isPending());
        m.put("credentialsFileExists", AdminBootstrapPasswordSupport.exists(nodeHome()));
        m.put("credentialsFile", AdminBootstrapPasswordSupport.resolveFile(nodeHome()).toString());
        m.put("configuredSystemPassword", hasConfiguredSystemPassword());
        return m;
    }

    /**
     * 仅 pending 且文件仍在时返回明文一次（运维 Token 保护）。
     */
    public Map<String, Object> opsReveal() {
        Map<String, Object> m = opsStatus();
        if (!isPending()) {
            m.put("revealed", false);
            m.put("msg", "引导待办已关闭，不再展示明文");
            return m;
        }
        String pwd = AdminBootstrapPasswordSupport.readPassword(nodeHome());
        if (StringUtils.isBlank(pwd)) {
            m.put("revealed", false);
            m.put("msg", "恢复文件不存在或无法读取");
            return m;
        }
        m.put("revealed", true);
        m.put("password", pwd);
        return m;
    }

    private static boolean isSnoozed(Session session) {
        if (session == null) {
            return false;
        }
        Object v = session.getAttribute(SESSION_SNOOZE_UNTIL);
        if (v instanceof Number) {
            return ((Number) v).longValue() > System.currentTimeMillis();
        }
        if (v instanceof String && StringUtils.isNotBlank((String) v)) {
            try {
                return Long.parseLong((String) v) > System.currentTimeMillis();
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return false;
    }

    private static SecurityAuthConfig copyAuth(SecurityAuthConfig src) {
        SecurityAuthConfig cfg = new SecurityAuthConfig();
        if (src == null) {
            return cfg;
        }
        cfg.setSuperPasswordEnabled(src.isSuperPasswordEnabled());
        cfg.setPasswordUpgradeOnLogin(src.isPasswordUpgradeOnLogin());
        cfg.setAllowUserUuidHint(src.isAllowUserUuidHint());
        cfg.setCsrfEnforce(src.isCsrfEnforce());
        cfg.setCorsAllowedOrigins(src.getCorsAllowedOrigins());
        cfg.setAdminBootstrapPending(src.isAdminBootstrapPending());
        cfg.setBootstrapAcknowledgedAt(src.getBootstrapAcknowledgedAt());
        return cfg;
    }
}

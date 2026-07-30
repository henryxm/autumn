package cn.org.autumn.modules.sys.service;

import static org.apache.shiro.subject.support.DefaultSubjectContext.PRINCIPALS_SESSION_KEY;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.install.InstallMode;
import cn.org.autumn.modules.bot.shiro.RobotPrincipal;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.sys.dao.SysShiroSessionDao;
import cn.org.autumn.modules.sys.entity.SysShiroSessionEntity;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.shiro.ShiroSessionTimeouts;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.stereotype.Service;

/**
 * Shiro Session 数据库持久化：Redis/内存为一级缓存，DB 为权威回源。
 */
@Slf4j
@Service
public class SysShiroSessionService extends ModuleService<SysShiroSessionDao, SysShiroSessionEntity>
        implements LoopJob.OneHour {

    private static final int LIST_LIMIT = 2_000;

    /**
     * 反序列化白名单：仅允许 Shiro Session 图与业务 principal 相关类型，降低 gadget 风险。
     */
    private static final ObjectInputFilter SESSION_PAYLOAD_FILTER = ObjectInputFilter.Config.createFilter(
            "maxdepth=128;maxrefs=10000;"
                    + "org.apache.shiro.**;"
                    + "cn.org.autumn.**;"
                    + "java.lang.**;"
                    + "java.util.**;"
                    + "java.util.concurrent.**;"
                    + "java.math.**;"
                    + "java.time.**;"
                    + "java.sql.Date;"
                    + "java.sql.Timestamp;"
                    + "!*");

    @Override
    public String ico() {
        return "fa-clock-o";
    }

    public void saveOrUpdateBySessionId(Session session) {
        if (InstallMode.isActive() || session == null || session.getId() == null) {
            return;
        }
        String sessionId = String.valueOf(session.getId());
        if (StringUtils.isBlank(sessionId)) {
            return;
        }
        // 匿名 Session（尚无登录身份）不落库：回源价值为零，且会产生 user 为空的垃圾行
        String userUuid = extractUserUuid(session);
        if (StringUtils.isBlank(userUuid)) {
            return;
        }
        try {
            String payload = serializeSession(session);
            if (StringUtils.isBlank(payload)) {
                log.warn("Skip persist shiro session: serialize failed, sessionId={}, sessionClass={}",
                        sessionId, session.getClass().getName());
                return;
            }
            Date now = new Date();
            long timeoutMs = ShiroSessionTimeouts.resolveTimeoutMs(session);
            Date expire = new Date(now.getTime() + timeoutMs);
            Date lastAccess = session.getLastAccessTime() != null ? session.getLastAccessTime() : now;

            SysShiroSessionEntity row = baseMapper.getBySessionId(sessionId);
            if (row == null) {
                row = new SysShiroSessionEntity();
                row.setSessionId(sessionId);
            }
            row.setUser(userUuid);
            row.setPayload(payload);
            row.setExpireTime(expire);
            row.setLastAccessTime(lastAccess);
            row.setUpdateTime(now);
            saveOrUpdate(row);
        } catch (Exception e) {
            log.warn("Persist shiro session to DB failed, sessionId={}, cause={}", sessionId, e.getMessage());
        }
    }

    public Session getValidBySessionId(Serializable sessionId) {
        if (InstallMode.isActive() || sessionId == null) {
            return null;
        }
        String id = String.valueOf(sessionId);
        if (StringUtils.isBlank(id)) {
            return null;
        }
        try {
            SysShiroSessionEntity row = baseMapper.getBySessionId(id);
            if (row == null || StringUtils.isBlank(row.getPayload())) {
                return null;
            }
            if (row.getExpireTime() != null && row.getExpireTime().before(new Date())) {
                baseMapper.deleteBySessionId(id);
                return null;
            }
            Session session = deserializeSession(row.getPayload());
            if (session == null) {
                baseMapper.deleteBySessionId(id);
            }
            return session;
        } catch (Exception e) {
            log.warn("Load shiro session from DB failed, sessionId={}, cause={}", id, e.getMessage());
            try {
                baseMapper.deleteBySessionId(id);
            } catch (Exception ignored) {
            }
            return null;
        }
    }

    public void deleteBySessionId(Serializable sessionId) {
        if (InstallMode.isActive() || sessionId == null) {
            return;
        }
        String id = String.valueOf(sessionId);
        if (StringUtils.isBlank(id)) {
            return;
        }
        try {
            baseMapper.deleteBySessionId(id);
        } catch (Exception e) {
            log.warn("Delete shiro session from DB failed, sessionId={}, cause={}", id, e.getMessage());
        }
    }

    /** @return 删除行数；用于判断是否实际清到 DB。 */
    public int deleteBySessionIdReturning(Serializable sessionId) {
        if (InstallMode.isActive() || sessionId == null) {
            return 0;
        }
        String id = String.valueOf(sessionId);
        if (StringUtils.isBlank(id)) {
            return 0;
        }
        try {
            return Math.max(0, baseMapper.deleteBySessionId(id));
        } catch (Exception e) {
            log.warn("Delete shiro session from DB failed, sessionId={}, cause={}", id, e.getMessage());
            return 0;
        }
    }

    public String findUserBySessionId(Serializable sessionId) {
        if (InstallMode.isActive() || sessionId == null) {
            return null;
        }
        String id = String.valueOf(sessionId);
        if (StringUtils.isBlank(id)) {
            return null;
        }
        try {
            SysShiroSessionEntity row = baseMapper.getBySessionId(id);
            return row == null ? null : row.getUser();
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> listValidSessionIdsByUser(String userUuid, int limit) {
        if (InstallMode.isActive() || StringUtils.isBlank(userUuid)) {
            return Collections.emptyList();
        }
        int cap = limit > 0 ? Math.min(limit, LIST_LIMIT) : LIST_LIMIT;
        try {
            List<String> ids = baseMapper.listValidSessionIdsByUser(userUuid, new Date(), cap, 0);
            return ids == null ? Collections.emptyList() : ids;
        } catch (Exception e) {
            log.warn("List valid shiro sessions by user failed, user={}, cause={}", userUuid, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<String> listValidSessionIds(int limit) {
        if (InstallMode.isActive()) {
            return Collections.emptyList();
        }
        int cap = limit > 0 ? Math.min(limit, LIST_LIMIT) : LIST_LIMIT;
        try {
            List<String> ids = baseMapper.listValidSessionIds(new Date(), cap, 0);
            return ids == null ? Collections.emptyList() : ids;
        } catch (Exception e) {
            log.warn("List valid shiro sessions failed, cause={}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /** 强制下线：按用户删光 DB 会话行。 */
    public int deleteByUserUuid(String userUuid) {
        if (InstallMode.isActive() || StringUtils.isBlank(userUuid)) {
            return 0;
        }
        try {
            return Math.max(0, baseMapper.deleteByUser(userUuid));
        } catch (Exception e) {
            log.warn("Delete shiro sessions by user failed, user={}, cause={}", userUuid, e.getMessage());
            return 0;
        }
    }

    /**
     * 一条 SQL 删除全部 {@code expire_time < now} 的会话行。
     *
     * @return 删除条数
     */
    public int deleteExpiredAll() {
        if (InstallMode.isActive()) {
            return 0;
        }
        try {
            int deleted = baseMapper.deleteExpiredBefore(new Date());
            if (deleted > 0) {
                log.info("Cleaned expired shiro sessions, deleted={}", deleted);
            }
            return Math.max(0, deleted);
        } catch (Exception e) {
            log.warn("Delete expired shiro sessions failed, cause={}", e.getMessage());
            return 0;
        }
    }

    /** 手动清理入口，与定时任务相同：一条 SQL 清全部过期行。 */
    public int cleanupExpired() {
        return deleteExpiredAll();
    }

    @Override
    public void onOneHour() {
        deleteExpiredAll();
    }

    static String serializeSession(Session session) {
        if (session == null) {
            return null;
        }
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(session);
            oos.flush();
            return Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    static Session deserializeSession(String payload) {
        if (StringUtils.isBlank(payload)) {
            return null;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(payload);
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
                ois.setObjectInputFilter(SESSION_PAYLOAD_FILTER);
                Object obj = ois.readObject();
                if (obj instanceof Session) {
                    return (Session) obj;
                }
            }
        } catch (Exception e) {
            log.warn("Deserialize shiro session payload rejected or failed, cause={}", e.getMessage());
        }
        return null;
    }

    public static String extractUserUuid(Session session) {
        if (session == null) {
            return null;
        }
        try {
            PrincipalCollection principals = (PrincipalCollection) session.getAttribute(PRINCIPALS_SESSION_KEY);
            if (principals == null) {
                return null;
            }
            String fromPrimary = uuidFromPrincipal(principals.getPrimaryPrincipal());
            if (StringUtils.isNotBlank(fromPrimary)) {
                return fromPrimary;
            }
            for (Object p : principals) {
                String uuid = uuidFromPrincipal(p);
                if (StringUtils.isNotBlank(uuid)) {
                    return uuid;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String uuidFromPrincipal(Object principal) {
        if (principal instanceof SysUserEntity) {
            return ((SysUserEntity) principal).getUuid();
        }
        if (principal instanceof RobotPrincipal) {
            RobotPrincipal robot = (RobotPrincipal) principal;
            return StringUtils.isNotBlank(robot.getOwner()) ? robot.getOwner() : robot.getUuid();
        }
        return null;
    }
}

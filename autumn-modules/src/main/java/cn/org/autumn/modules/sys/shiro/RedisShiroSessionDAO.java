package cn.org.autumn.modules.sys.shiro;

import static org.apache.shiro.subject.support.DefaultSubjectContext.PRINCIPALS_SESSION_KEY;

import cn.org.autumn.cluster.UserHandler;
import cn.org.autumn.install.InstallMode;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.service.SysShiroSessionService;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.utils.RedisExpireUtil;
import cn.org.autumn.utils.RedisKeys;
import com.google.gson.Gson;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Shiro Session：本地 cache → Redis（可选）→ DB 回源。
 * <p>
 * DB 为权威持久化；Redis 清空后可由 {@link SysShiroSessionService} 回源并回填。
 */
@Slf4j
@Component
public class RedisShiroSessionDAO extends EnterpriseCacheSessionDAO implements LoopJob.TenMinute, DisposableBean {

    @Autowired(required = false)
    private RedisTemplate redisTemplate;

    @Autowired(required = false)
    private SysShiroSessionService sysShiroSessionService;

    @Autowired
    SysUserService sysUserService;

    @Autowired
    UserProfileService userProfileService;

    @Autowired
    SysConfigService sysConfigService;

    @Autowired
    Gson gson;

    @Autowired(required = false)
    List<UserHandler> userHandlers;

    static final Map<Serializable, Session> cache = new ConcurrentHashMap<>();
    static final Map<Serializable, Session> update = new ConcurrentHashMap<>();
    /** 已将 user 落库的 sessionId，避免登录后每次 touch 都写库。 */
    static final Set<Serializable> userPersisted = ConcurrentHashMap.newKeySet();

    @Override
    protected Serializable doCreate(Session session) {
        Serializable sessionId = super.doCreate(session);
        if (null != session && null != sessionId) {
            cache.put(sessionId, session);
            if (InstallMode.isActive()) {
                return sessionId;
            }
            persistSession(session);
        }
        return sessionId;
    }

    @Override
    protected Session doReadSession(Serializable sessionId) {
        if (InstallMode.isActive()) {
            Session session = super.doReadSession(sessionId);
            if (session == null) {
                session = cache.get(sessionId);
            }
            return session;
        }
        Session session = super.doReadSession(sessionId);
        if (null == session) {
            session = cache.get(sessionId);
        }
        if (session == null && redisTemplate != null) {
            String key = RedisKeys.getShiroSessionKey(sysConfigService.getNameSpace(), sessionId.toString());
            session = getShiroSessionFromRedis(key, sessionId);
            if (null != session) {
                cache.put(sessionId, session);
            }
        }
        if (session == null && sysShiroSessionService != null) {
            session = sysShiroSessionService.getValidBySessionId(sessionId);
            if (session != null) {
                cache.put(sessionId, session);
                writeRedisOnly(session);
            }
        }
        return session;
    }

    @Override
    protected void doUpdate(Session session) {
        super.doUpdate(session);
        if (null == session || session.getId() == null) {
            return;
        }
        cache.put(session.getId(), session);
        update.put(session.getId(), session);
        if (InstallMode.isActive() || sysShiroSessionService == null) {
            return;
        }
        // 登录写入 principals 后立刻补写 DB user，不等十分钟刷盘；每会话仅一次
        if (!userPersisted.contains(session.getId())
                && StringUtils.isNotBlank(SysShiroSessionService.extractUserUuid(session))) {
            sysShiroSessionService.saveOrUpdateBySessionId(session);
            userPersisted.add(session.getId());
        }
    }

    @Override
    protected void doDelete(Session session) {
        super.doDelete(session);
        if (null == session || session.getId() == null) {
            return;
        }
        cache.remove(session.getId());
        update.remove(session.getId());
        userPersisted.remove(session.getId());
        if (InstallMode.isActive()) {
            return;
        }
        if (redisTemplate != null) {
            try {
                String key = RedisKeys.getShiroSessionKey(sysConfigService.getNameSpace(), session.getId().toString());
                redisTemplate.delete(key);
            } catch (Exception e) {
                log.warn("Delete redis shiro session failed, cause={}", e.getMessage());
            }
        }
        if (sysShiroSessionService != null) {
            sysShiroSessionService.deleteBySessionId(session.getId());
        }
    }

    @Override
    public void onTenMinute() {
        if (InstallMode.isActive()) {
            return;
        }
        cache.clear();
        try {
            Iterator<Map.Entry<Serializable, Session>> iterator = update.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Serializable, Session> entry = iterator.next();
                Session session = entry.getValue();
                persistSession(session);
                iterator.remove();
            }
        } catch (Exception e) {
            log.error("Execution error:{}", e.getMessage());
        }
    }

    private void persistSession(Session session) {
        if (session == null) {
            return;
        }
        // create 早于 applyGlobalSessionTimeout 时仍是 30 分钟；记住我则为 7 天，勿强行改回 1 天
        ShiroSessionTimeouts.normalizeTimeout(session);
        writeRedisOnly(session);
        // DB 仅持久化已登录会话；匿名会话只走 cache/Redis
        if (sysShiroSessionService != null
                && StringUtils.isNotBlank(SysShiroSessionService.extractUserUuid(session))) {
            sysShiroSessionService.saveOrUpdateBySessionId(session);
            if (session.getId() != null) {
                userPersisted.add(session.getId());
            }
        }
        syncUserHandlers(session);
    }

    private void writeRedisOnly(Session session) {
        if (redisTemplate == null || session == null || session.getId() == null) {
            return;
        }
        try {
            String key = RedisKeys.getShiroSessionKey(sysConfigService.getNameSpace(), session.getId().toString());
            redisTemplate.opsForValue().set(key, session);
            RedisExpireUtil.expire(redisTemplate, key, ShiroSessionTimeouts.resolveRedisTtlDays(session), TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("Write redis shiro session failed, sessionId={}, sessionClass={}, cause={}",
                    session.getId(), session.getClass().getName(), e.getMessage());
        }
    }

    /**
     * 登录成功后按是否记住我拉长 Session/Redis/DB 过期（普通 1 天，记住我 7 天）。
     * <p>
     * 注意：{@code Subject#getSession()} 常为不可序列化的代理，必须落到 DAO 内的原生 Session 再写 Redis/DB。
     */
    public void refreshAfterLogin(Session session, boolean rememberMe) {
        if (session == null || InstallMode.isActive()) {
            return;
        }
        Serializable id = session.getId();
        // 先通过代理写入 timeout/属性（会下沉到 Native Session）
        ShiroSessionTimeouts.applyRememberMe(session, rememberMe);
        Session nativeSession = resolveNativeSession(id);
        if (nativeSession == null) {
            nativeSession = session;
        }
        // 再对原生 Session 写一遍，确保序列化落库读到的是 SimpleSession
        ShiroSessionTimeouts.applyRememberMe(nativeSession, rememberMe);
        if (id != null) {
            userPersisted.remove(id);
            cache.put(id, nativeSession);
            update.put(id, nativeSession);
        }
        persistSession(nativeSession);
    }

    /** 取 DAO 缓存 / Redis / DB 中的原生 Session，避免序列化 Subject 代理。 */
    private Session resolveNativeSession(Serializable sessionId) {
        if (sessionId == null) {
            return null;
        }
        Session cached = cache.get(sessionId);
        if (cached != null) {
            return cached;
        }
        try {
            return doReadSession(sessionId);
        } catch (Exception e) {
            log.warn("Resolve native shiro session failed, sessionId={}, cause={}", sessionId, e.getMessage());
            return null;
        }
    }

    private Session getShiroSessionFromRedis(String key, Serializable sessionId) {
        if (redisTemplate == null) {
            return null;
        }
        try {
            return (Session) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Shiro session deserialize failed, key={}, sessionId={}, cause={}", key, sessionId, e.getMessage());
            if (sessionId != null) {
                cache.remove(sessionId);
            }
            try {
                redisTemplate.delete(key);
            } catch (Exception ex) {
                log.warn("Failed to delete corrupt Shiro session, key={}, cause={}", key, ex.getMessage());
            }
            return null;
        }
    }

    private void syncUserHandlers(Session session) {
        if (null == userHandlers || userHandlers.isEmpty() || session == null) {
            return;
        }
        boolean same = true;
        for (UserHandler userHandler : userHandlers) {
            same = sysConfigService.isSame(userHandler);
            if (!same) {
                break;
            }
        }
        if (same) {
            return;
        }
        PrincipalCollection principals = (PrincipalCollection) session.getAttribute(PRINCIPALS_SESSION_KEY);
        if (null != principals) {
            Object o = principals.getPrimaryPrincipal();
            if (o instanceof SysUserEntity) {
                SysUserEntity sysUserEntity = (SysUserEntity) o;
                if (null != sysUserEntity.getParent()) {
                    SysUserEntity parent = sysUserEntity.getParent();
                    sysUserService.copy(parent);
                    if (null != parent.getProfile()) {
                        userProfileService.copy(parent.getProfile());
                    }
                }
                sysUserService.copy(sysUserEntity);
                if (null != sysUserEntity.getProfile()) {
                    userProfileService.copy(sysUserEntity.getProfile());
                }
            }
        }
    }

    @Override
    public void destroy() throws Exception {
        onTenMinute();
    }
}

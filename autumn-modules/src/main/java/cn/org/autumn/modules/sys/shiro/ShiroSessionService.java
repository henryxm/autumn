package cn.org.autumn.modules.sys.shiro;

import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.utils.RedisKeys;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.apache.shiro.subject.support.DefaultSubjectContext.PRINCIPALS_SESSION_KEY;

/**
 * Shiro会话管理服务
 * 用于管理用户会话，支持强制下线等功能
 */
@SuppressWarnings({"unchecked", "rawtypes"})
@Slf4j
@Service
public class ShiroSessionService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private SessionManager sessionManager;

    private SessionDAO getSessionDAO() {
        if (sessionManager instanceof DefaultWebSessionManager) {
            return ((DefaultWebSessionManager) sessionManager).getSessionDAO();
        }
        return null;
    }

    /**
     * 根据用户UUID获取该用户的所有活动会话ID
     * 支持Redis和内存两种模式
     */
    public Collection<Serializable> getActiveSessionsByUserUuid(String userUuid) {
        Set<Serializable> sessionIds = new HashSet<>();
        try {
            // 从SessionDAO获取活动会话
            SessionDAO sessionDAO = getSessionDAO();
            if (sessionDAO != null) {
                Collection<Session> activeSessions = sessionDAO.getActiveSessions();
                for (Session session : activeSessions) {
                    PrincipalCollection principals = (PrincipalCollection) session.getAttribute(PRINCIPALS_SESSION_KEY);
                    if (principals != null) {
                        Object primaryPrincipal = principals.getPrimaryPrincipal();
                        if (primaryPrincipal instanceof SysUserEntity) {
                            SysUserEntity user = (SysUserEntity) primaryPrincipal;
                            if (userUuid.equals(user.getUuid())) {
                                sessionIds.add(session.getId());
                            }
                        }
                    }
                }
            }

            // 如果是Redis模式，还需要从Redis中查找
            if (isRedisEnabled()) {
                addSessionsFromRedis(userUuid, sessionIds);
            }
        } catch (Exception e) {
            log.error("获取用户活动会话失败，用户UUID: {}", userUuid, e);
        }
        return sessionIds;
    }

    /**
     * 从Redis中获取用户的会话
     */
    private void addSessionsFromRedis(String userUuid, Set<Serializable> sessionIds) {
        try {
            String sessionPrefix = RedisKeys.getSessionPrefix(sysConfigService.getNameSpace());
            Set<String> keys = redisTemplate.keys(sessionPrefix + "*");
            if (!keys.isEmpty()) {
                for (String key : keys) {
                    try {
                        Session session = (Session) redisTemplate.opsForValue().get(key);
                        if (session != null) {
                            PrincipalCollection principals = (PrincipalCollection) session.getAttribute(PRINCIPALS_SESSION_KEY);
                            if (principals != null) {
                                Object primaryPrincipal = principals.getPrimaryPrincipal();
                                if (primaryPrincipal instanceof SysUserEntity) {
                                    SysUserEntity user = (SysUserEntity) primaryPrincipal;
                                    if (userUuid.equals(user.getUuid())) {
                                        sessionIds.add(session.getId());
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("解析Redis会话失败，key: {}", key, e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("从Redis获取用户会话失败", e);
        }
    }

    /**
     * 检查是否启用Redis
     */
    private boolean isRedisEnabled() {
        try {
            // 尝试获取Redis中是否有数据，如果有则说明启用了Redis
            String sessionPrefix = RedisKeys.getSessionPrefix(sysConfigService.getNameSpace());
            Set<String> keys = (Set<String>) redisTemplate.keys(sessionPrefix + "*");
            return !keys.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 强制用户下线（使该用户的所有会话失效）
     *
     * @param userUuid 用户UUID
     * @return 下线的会话数量
     */
    public int forceLogoutByUserUuid(String userUuid) {
        int count = 0;
        Collection<Serializable> sessionIds = getActiveSessionsByUserUuid(userUuid);
        SessionDAO sessionDAO = getSessionDAO();
        if (sessionDAO == null) {
            log.warn("SessionDAO不可用，无法强制用户下线");
            return 0;
        }
        for (Serializable sessionId : sessionIds) {
            try {
                Session session = sessionDAO.readSession(sessionId);
                if (session != null) {
                    // 设置会话为过期状态
                    session.setTimeout(0);
                    // 删除会话
                    sessionDAO.delete(session);
                    count++;
                    if (log.isDebugEnabled())
                        log.debug("强制下线，用户UUID: {}, 会话ID: {}", userUuid, sessionId);
                }
            } catch (Exception e) {
                log.error("强制用户下线失败，会话ID: {}", sessionId, e);
            }
        }
        return count;
    }
}


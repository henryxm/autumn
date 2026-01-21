package cn.org.autumn.modules.sys.shiro;

import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.utils.RedisKeys;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;

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

    /**
     * 从 SessionManager 获取 SessionDAO（含 RedisShiroSessionDAO）。
     * 若 SessionManager 被 Spring AOP 代理（如 JDK 动态代理），instanceof DefaultWebSessionManager 会为 false，
     * 故先通过 Advised 取出目标对象再判断。
     */
    private SessionDAO getSessionDAO() {
        Object target = sessionManager;
        if (sessionManager instanceof Advised) {
            try {
                target = ((Advised) sessionManager).getTargetSource().getTarget();
            } catch (Exception e) {
                log.debug("获取 SessionManager 目标失败: {}", e.getMessage());
                return null;
            }
        }
        if (target instanceof DefaultWebSessionManager) {
            return ((DefaultWebSessionManager) target).getSessionDAO();
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
            log.error("获取失败:{}, 错误:{}", userUuid, e.getMessage());
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
            if (null != keys && !keys.isEmpty()) {
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
                        log.warn("解析失败:{}, 异常:{}", key, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("查询失败:{}, 错误:{}", userUuid, e.getMessage());
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
     * 收集所有活动会话（SessionDAO + Redis 去重）
     */
    private Map<Serializable, Session> getAllSessionsMap() {
        Map<Serializable, Session> map = new LinkedHashMap<>();
        try {
            SessionDAO sessionDAO = getSessionDAO();
            if (sessionDAO != null) {
                Collection<Session> active = sessionDAO.getActiveSessions();
                if (active != null) {
                    for (Session s : active) {
                        if (s != null && s.getId() != null) map.put(s.getId(), s);
                    }
                }
            }
            if (isRedisEnabled()) {
                String sessionPrefix = RedisKeys.getSessionPrefix(sysConfigService.getNameSpace());
                Set<String> keys = redisTemplate.keys(sessionPrefix + "*");
                if (keys != null && !keys.isEmpty()) {
                    for (String key : keys) {
                        try {
                            Session s = (Session) redisTemplate.opsForValue().get(key);
                            if (s != null && s.getId() != null && !map.containsKey(s.getId()))
                                map.put(s.getId(), s);
                        } catch (Exception e) {
                            log.warn("解析失败:{}, 错误:{}", key, e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("收集失败:{}", e.getMessage());
        }
        return map;
    }

    /**
     * 将会话转为前端展示的 Map
     *
     * @param forceLogoutUuids 被标记为「需强制重新登录」的 userUuid 集合，可为 null
     */
    private Map<String, Object> sessionToMap(Session s, String currentSessionId, Set<String> forceLogoutUuids) {
        Map<String, Object> m = new LinkedHashMap<>();
        String sid = s.getId() != null ? s.getId().toString() : "";
        m.put("sessionId", sid);
        m.put("current", sid.equals(currentSessionId));
        m.put("host", s.getHost() != null ? s.getHost() : "");
        m.put("startTime", s.getStartTimestamp() != null ? s.getStartTimestamp().getTime() : null);
        m.put("lastAccessTime", s.getLastAccessTime() != null ? s.getLastAccessTime().getTime() : null);
        m.put("timeout", s.getTimeout());
        String userUuid = "";
        String username = "";
        PrincipalCollection principals = (PrincipalCollection) s.getAttribute(PRINCIPALS_SESSION_KEY);
        if (principals != null) {
            Object p = principals.getPrimaryPrincipal();
            if (p instanceof SysUserEntity) {
                SysUserEntity u = (SysUserEntity) p;
                userUuid = u.getUuid() != null ? u.getUuid() : "";
                username = u.getUsername() != null ? u.getUsername() : "";
            }
        }
        m.put("userUuid", userUuid);
        m.put("username", username);
        m.put("forceLogout", forceLogoutUuids != null && !userUuid.isEmpty() && forceLogoutUuids.contains(userUuid));
        return m;
    }

    /**
     * 从 Redis 中获取当前被标记为「需强制重新登录」的 userUuid 集合
     */
    private Set<String> getForceLogoutUserUuids() {
        Set<String> set = new HashSet<>();
        try {
            String prefix = RedisKeys.getForceLogoutPrefix(sysConfigService.getNameSpace());
            Set<String> keys = redisTemplate.keys(prefix + "*");
            if (keys != null) {
                for (String k : keys) {
                    String suf = k.length() > prefix.length() ? k.substring(prefix.length()) : "";
                    if (!suf.isEmpty()) set.add(suf);
                }
            }
        } catch (Exception e) {
            log.warn("getForceLogoutUserUuids: {}", e.getMessage());
        }
        return set;
    }

    /**
     * 获取活动会话列表（分页），支持按用户 UUID、用户名、会话 ID 筛选
     *
     * @param userUuidFilter   用户 UUID，模糊匹配，空则不过滤
     * @param usernameFilter   用户名，模糊匹配，空则不过滤
     * @param currentSessionId 当前请求的 sessionId，用于标记「当前会话」
     * @param sessionIdFilter  会话 ID，精确匹配，空则不过滤
     * @param page             页码，从 1 开始
     * @param limit            每页条数
     * @return Map：list=当前页列表，totalCount=总数，currPage=当前页，totalPage=总页数；每项含 sessionId、userUuid、username、host、startTime、lastAccessTime、timeout、current
     */
    public Map<String, Object> getActiveSessionList(String userUuidFilter, String usernameFilter, String currentSessionId, String sessionIdFilter, int page, int limit) {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<Serializable, Session> all = getAllSessionsMap();
        Set<String> forceLogoutUuids = getForceLogoutUserUuids();
        for (Session s : all.values()) {
            String sid = s.getId() != null ? s.getId().toString() : "";
            if (StringUtils.isNotBlank(sessionIdFilter) && !sessionIdFilter.trim().equals(sid)) continue;
            Map<String, Object> m = sessionToMap(s, currentSessionId != null ? currentSessionId : "", forceLogoutUuids);
            String uuid = (String) m.get("userUuid");
            String uname = (String) m.get("username");
            if (StringUtils.isNotBlank(userUuidFilter) && (uuid == null || !uuid.contains(userUuidFilter))) continue;
            if (StringUtils.isNotBlank(usernameFilter) && (uname == null || !uname.contains(usernameFilter))) continue;
            list.add(m);
        }
        list.sort((a, b) -> {
            Long t1 = (Long) a.get("lastAccessTime");
            Long t2 = (Long) b.get("lastAccessTime");
            if (t1 == null && t2 == null) return 0;
            if (t1 == null) return 1;
            if (t2 == null) return -1;
            return Long.compare(t2, t1);
        });
        int totalCount = list.size();
        int totalPage = totalCount <= 0 ? 1 : (totalCount + limit - 1) / limit;
        int currPage = Math.max(1, Math.min(page, totalPage));
        int from = (currPage - 1) * limit;
        int to = Math.min(from + limit, totalCount);
        List<Map<String, Object>> sub = (from < to) ? new ArrayList<>(list.subList(from, to)) : new ArrayList<>();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("list", sub);
        out.put("totalCount", totalCount);
        out.put("currPage", currPage);
        out.put("totalPage", totalPage);
        return out;
    }

    /**
     * 删除指定会话
     *
     * @param sessionId 会话 ID
     * @return 是否删除成功
     */
    public boolean deleteSession(Serializable sessionId) {
        if (sessionId == null)
            return false;
        SessionDAO dao = getSessionDAO();
        try {
            Session session = null;
            if (dao != null)
                session = dao.readSession(sessionId);
            if (session == null && isRedisEnabled()) {
                String key = RedisKeys.getShiroSessionKey(sysConfigService.getNameSpace(), sessionId.toString());
                session = (Session) redisTemplate.opsForValue().get(key);
                if (session != null)
                    redisTemplate.delete(key);
                if (session != null && dao != null) {
                    try {
                        dao.delete(session);
                    } catch (Exception ignored) {
                    }
                }
                return session != null;
            }
            if (session != null) {
                session.setTimeout(0);
                dao.delete(session);
                return true;
            }
        } catch (Exception e) {
            log.error("删除会话:{}, 错误:{}", sessionId, e.getMessage());
        }
        return false;
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
                    boolean del = deleteSession(sessionId);
                    if (del)
                        count++;
                }
            } catch (Exception e) {
                log.error("下线失败:{}, 错误:{}", sessionId, e.getMessage());
            }
        }
        markForceLogout(userUuid);
        return count;
    }

    /**
     * 标记用户为「强制下线」：在 TTL 内禁止其通过 RememberMe 自动登录，必须重新输入密码。
     * 依赖 Redis；若 Redis 不可用则仅记录日志。
     */
    private void markForceLogout(String userUuid) {
        if (userUuid == null || userUuid.isEmpty())
            return;
        try {
            String key = RedisKeys.getForceLogoutKey(sysConfigService.getNameSpace(), userUuid);
            redisTemplate.opsForValue().set(key, "1", 7, TimeUnit.DAYS);
            if (log.isDebugEnabled()) log.debug("已标记强制下线: userUuid={}", userUuid);
        } catch (Exception e) {
            log.warn("标记强制下线失败, userUuid={}: {}", userUuid, e.getMessage());
        }
    }

    /**
     * 取消某用户的「强制重新登录」标记，删除 Redis 中的标记后，该用户可再次通过 RememberMe 自动登录。
     */
    public void clearForceLogout(String userUuid) {
        if (userUuid == null || userUuid.isEmpty()) return;
        try {
            redisTemplate.delete(RedisKeys.getForceLogoutKey(sysConfigService.getNameSpace(), userUuid));
            if (log.isDebugEnabled()) log.debug("已取消强制重登: userUuid={}", userUuid);
        } catch (Exception e) {
            log.warn("取消强制重登失败, userUuid={}: {}", userUuid, e.getMessage());
        }
    }
}


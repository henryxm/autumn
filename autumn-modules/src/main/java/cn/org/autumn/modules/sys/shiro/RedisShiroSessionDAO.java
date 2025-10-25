package cn.org.autumn.modules.sys.shiro;

import cn.org.autumn.cluster.UserHandler;
import cn.org.autumn.modules.job.task.LoopJob;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.utils.RedisKeys;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.apache.shiro.subject.support.DefaultSubjectContext.PRINCIPALS_SESSION_KEY;

@Slf4j
@Component
public class RedisShiroSessionDAO extends EnterpriseCacheSessionDAO implements LoopJob.TenMinute, DisposableBean {

    @Autowired
    private RedisTemplate redisTemplate;

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

    //创建session
    @Override
    protected Serializable doCreate(Session session) {
        Serializable sessionId = super.doCreate(session);
        if (null != session && null != sessionId) {
            String key = RedisKeys.getShiroSessionKey(sysConfigService.getNameSpace(), sessionId.toString());
            setShiroSession(key, session);
            cache.put(sessionId, session);
        }
        return sessionId;
    }

    //获取session
    @Override
    protected Session doReadSession(Serializable sessionId) {
        Session session = super.doReadSession(sessionId);
        if (null == session)
            session = cache.get(sessionId);
        if (session == null) {
            String key = RedisKeys.getShiroSessionKey(sysConfigService.getNameSpace(), sessionId.toString());
            session = getShiroSession(key);
            if (null != session)
                cache.put(sessionId, session);
        }
        return session;
    }

    //更新session
    @Override
    protected void doUpdate(Session session) {
        super.doUpdate(session);
        if (null != session) {
            cache.put(session.getId(), session);
            update.put(session.getId(), session);
        }
    }

    //删除session
    @Override
    protected void doDelete(Session session) {
        super.doDelete(session);
        if (null != session) {
            cache.remove(session.getId());
            String key = RedisKeys.getShiroSessionKey(sysConfigService.getNameSpace(), session.getId().toString());
            redisTemplate.delete(key);
        }
    }

    @Override
    public void onTenMinute() {
        cache.clear();
        try {
            Iterator<Map.Entry<Serializable, Session>> iterator = update.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Serializable, Session> entry = iterator.next();
                Session session = entry.getValue();
                String key = RedisKeys.getShiroSessionKey(sysConfigService.getNameSpace(), session.getId().toString());
                setShiroSession(key, session);
                iterator.remove();
            }
        } catch (Exception e) {
            log.error("执行异常:{}", e.getMessage());
        }
    }

    private Session getShiroSession(String key) {
        return (Session) redisTemplate.opsForValue().get(key);
    }

    private void setShiroSession(String key, Session session) {
        redisTemplate.opsForValue().set(key, session);
        //60分钟过期
        redisTemplate.expire(key, 60, TimeUnit.MINUTES);

        //如果没找到userHandler 则不需要同步用户
        if (null == userHandlers || userHandlers.size() == 0)
            return;
        //如果有 userHandler, 但是如果是同一台服务器，则不需要同步
        boolean same = true;
        for (UserHandler userHandler : userHandlers) {
            same = sysConfigService.isSame(userHandler);
            if (!same)
                break;
        }
        if (same)
            return;

        PrincipalCollection principals = (PrincipalCollection) session.getAttribute(PRINCIPALS_SESSION_KEY);
        if (null != principals) {
            Object o = principals.getPrimaryPrincipal();
            if (o instanceof SysUserEntity) {
                SysUserEntity sysUserEntity = (SysUserEntity) o;
                // 增加子账户后，需优先同步主账户
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
package cn.org.autumn.modules.sys.shiro;

import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.utils.RedisKeys;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.concurrent.TimeUnit;

import static org.apache.shiro.subject.support.DefaultSubjectContext.PRINCIPALS_SESSION_KEY;

@Component
public class RedisShiroSessionDAO extends EnterpriseCacheSessionDAO {
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    SysUserService sysUserService;

    //创建session
    @Override
    protected Serializable doCreate(Session session) {
        Serializable sessionId = super.doCreate(session);
        final String key = RedisKeys.getShiroSessionKey(sessionId.toString());
        setShiroSession(key, session);
        return sessionId;
    }

    @Override
    public Session readSession(Serializable sessionId) throws UnknownSessionException {
        return doReadSession(sessionId);
    }

    //获取session
    @Override
    protected Session doReadSession(Serializable sessionId) {
        Session session = super.doReadSession(sessionId);
        if (session == null) {
            final String key = RedisKeys.getShiroSessionKey(sessionId.toString());
            session = getShiroSession(key);
        }
        return session;
    }

    //更新session
    @Override
    protected void doUpdate(Session session) {
        super.doUpdate(session);
        final String key = RedisKeys.getShiroSessionKey(session.getId().toString());
        setShiroSession(key, session);
    }

    //删除session
    @Override
    protected void doDelete(Session session) {
        super.doDelete(session);
        final String key = RedisKeys.getShiroSessionKey(session.getId().toString());
        redisTemplate.delete(key);
    }

    private Session getShiroSession(String key) {
        return (Session) redisTemplate.opsForValue().get(key);
    }

    private void setShiroSession(String key, Session session) {
        redisTemplate.opsForValue().set(key, session);
        //60分钟过期
        redisTemplate.expire(key, 60, TimeUnit.MINUTES);
        PrincipalCollection principals = (PrincipalCollection) session.getAttribute(PRINCIPALS_SESSION_KEY);
        if (null != principals) {
            Object o = principals.getPrimaryPrincipal();
            if (null != o && o instanceof SysUserEntity) {
                sysUserService.merge((SysUserEntity) o);
            }
        }
    }
}
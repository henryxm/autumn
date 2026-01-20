package cn.org.autumn.modules.sys.shiro;

import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.utils.RedisKeys;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SubjectContext;
import org.apache.shiro.web.mgt.CookieRememberMeManager;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 扩展 RememberMe：当用户被强制下线后，在 TTL 内禁止通过 RememberMe 自动登录，
 * 并清除其 RememberMe Cookie，必须重新输入密码。
 */
@Slf4j
public class ForceLogoutRememberMeManager extends CookieRememberMeManager {

    private RedisTemplate redisTemplate;
    private SysConfigService sysConfigService;

    public void setRedisTemplate(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void setSysConfigService(SysConfigService sysConfigService) {
        this.sysConfigService = sysConfigService;
    }

    @Override
    public PrincipalCollection getRememberedPrincipals(SubjectContext subjectContext) {
        PrincipalCollection principals = super.getRememberedPrincipals(subjectContext);
        if (principals == null || redisTemplate == null || sysConfigService == null)
            return principals;
        Object p = principals.getPrimaryPrincipal();
        String userUuid = null;
        if (p instanceof SysUserEntity)
            userUuid = ((SysUserEntity) p).getUuid();
        if (userUuid == null || userUuid.isEmpty())
            return principals;
        try {
            String key = RedisKeys.getForceLogoutKey(sysConfigService.getNameSpace(), userUuid);
            if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                forgetIdentity(subjectContext);
                if (log.isDebugEnabled())
                    log.debug("已标记强制下线，拒绝 RememberMe: userUuid={}", userUuid);
                return null;
            }
        } catch (Exception e) {
            log.warn("检查强制下线标记失败: {}", e.getMessage());
        }
        return principals;
    }
}

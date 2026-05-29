package cn.org.autumn.modules.safe.spi;

import cn.org.autumn.exception.CodeException;
import cn.org.autumn.model.Error;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 默认使用登录密码校验重置身份。
 */
@Component
@Order(1000)
public class LoginPasswordPayResetVerifier implements PayCredentialResetVerifier {

    @Autowired
    @Lazy
    private SysUserService sysUserService;

    @Override
    public boolean supports(PayResetContext ctx) {
        return ctx != null && StringUtils.isNotBlank(ctx.getLoginPassword());
    }

    @Override
    public void verifyReset(PayResetContext ctx) throws CodeException {
        if (ctx == null || StringUtils.isBlank(ctx.getUserUuid()))
            throw new CodeException(Error.USER_NOT_LOGIN);
        if (StringUtils.isBlank(ctx.getLoginPassword()))
            throw new CodeException(Error.LOGIN_FAILED);
        try {
            SysUserEntity user = sysUserService.getByUuid(ctx.getUserUuid());
            if (user == null)
                throw new CodeException(Error.DATA_NOT_FOUND);
            String hashed = ShiroUtils.sha256(ctx.getLoginPassword(), user.getSalt());
            if (!StringUtils.equals(hashed, user.getPassword()))
                throw new CodeException(Error.LOGIN_FAILED);
        } catch (CodeException e) {
            throw e;
        } catch (Exception e) {
            throw new CodeException(Error.LOGIN_FAILED);
        }
    }
}

package cn.org.autumn.modules.usr.interceptor;

import cn.org.autumn.annotation.Login;
import cn.org.autumn.config.InterceptorHandler;
import cn.org.autumn.exception.AException;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.usr.entity.UserTokenEntity;
import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.modules.usr.service.UserTokenService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.ui.ModelMap;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限(Token)验证
 */
@Component
public class AuthorizationInterceptor extends HandlerInterceptorAdapter implements InterceptorHandler {
    @Autowired
    private UserTokenService userTokenService;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private SysUserService sysUserService;

    public static final String USER_KEY = "userUuid";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Login annotation;
        if (handler instanceof HandlerMethod) {
            annotation = ((HandlerMethod) handler).getMethodAnnotation(Login.class);
        } else {
            return true;
        }

        if (annotation == null) {
            return true;
        }

        //从header中获取token
        String token = request.getHeader("token");
        //如果header中不存在token，则从参数中获取token
        if (StringUtils.isBlank(token)) {
            token = request.getParameter("token");
        }

        //token为空
        if (StringUtils.isBlank(token)) {
            throw new AException("token不能为空");
        }

        //查询token信息
        UserTokenEntity tokenEntity = userTokenService.queryByToken(token);
        if (tokenEntity == null || tokenEntity.getExpireTime().getTime() < System.currentTimeMillis()) {
            throw new AException("token失效，请重新登录");
        }

        //设置userUuid到request里，后续根据userUuid，获取用户信息
        request.setAttribute(USER_KEY, tokenEntity.getUserUuid());
        return true;
    }

    private SysUserEntity append(SysUserEntity sysUserEntity) {
        if (null != sysUserEntity) {
            if (null == sysUserEntity.getProfile())
                sysUserEntity = userProfileService.setProfile(sysUserEntity);
            if (null == sysUserEntity.getProfile()) {
                sysUserEntity.setProfile(userProfileService.from(sysUserEntity, sysUserService.getPassword(), null));
            }
            if (null == sysUserEntity.getProfile().getIcon()) {
                sysUserEntity.getProfile().setIcon("");
            }
        }
        return sysUserEntity;
    }

    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable ModelAndView modelAndView) throws Exception {
        if (null != modelAndView) {
            ModelMap modelMap = modelAndView.getModelMap();
            if (ShiroUtils.isLogin()) {
                SysUserEntity sysUserEntity = sysUserService.getCache(ShiroUtils.getUserUuid());
                sysUserEntity = append(sysUserEntity);
                if (StringUtils.isNotEmpty(sysUserEntity.getParentUuid()) && null == sysUserEntity.getParent()) {
                    SysUserEntity parent = sysUserService.getByUuid(sysUserEntity.getParentUuid());
                    parent = append(parent);
                    sysUserEntity.setParent(parent);
                }
                modelMap.put("user", sysUserEntity);
            }
        }
    }

    @Override
    public HandlerInterceptor getHandlerInterceptor() {
        return this;
    }

    @Override
    public List<String> getPatterns() {
        List<String> list = new ArrayList<>();
        list.add("/**");
        return list;
    }
}

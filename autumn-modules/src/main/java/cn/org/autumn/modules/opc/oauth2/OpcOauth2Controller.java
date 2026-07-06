package cn.org.autumn.modules.opc.oauth2;

import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.opc.service.ConnectAppService;
import cn.org.autumn.modules.opc.service.ConnectBindService;
import cn.org.autumn.modules.opc.service.ConnectLoginService;
import cn.org.autumn.modules.opc.service.ConnectOauthService;
import cn.org.autumn.utils.R;
import cn.org.autumn.modules.opc.support.ConnectBindException;
import cn.org.autumn.modules.opc.support.ConnectBindException.ConflictType;
import cn.org.autumn.modules.opc.support.OpenOAuthStateSupport;
import cn.org.autumn.opc.OpcConstants;
import cn.org.autumn.site.PageFactory;
import cn.org.autumn.utils.WebPathUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.view.RedirectView;

/** OPC OAuth2 客户端入口：跳转远程 OPL 授权、处理回调。 */
@Slf4j
@Controller
@RequestMapping(OpcConstants.OAUTH2_BASE)
public class OpcOauth2Controller {

    @Autowired
    private ConnectAppService connectAppService;

    @Autowired
    private ConnectOauthService connectOauthService;

    @Autowired
    private ConnectLoginService connectLoginService;

    @Autowired
    private ConnectBindService connectBindService;

    @Autowired
    private PageFactory pageFactory;

    @ResponseBody
    @PostMapping("bind/unbind")
    public R unbind(@RequestParam(required = false) String appId) {
        if (StringUtils.isBlank(appId)) {
            return R.error("缺少 appId 参数");
        }
        ConnectAppEntity app = connectAppService.getByAppId(appId.trim());
        if (app == null) {
            return R.error("未找到接入应用配置");
        }
        try {
            connectBindService.unbindForSessionUser(app);
            return R.ok();
        } catch (IllegalStateException e) {
            return R.error(e.getMessage());
        }
    }

    @RequestMapping(OpcConstants.NS + "/authorize")
    public Object authorize(HttpServletRequest request, HttpServletResponse response, Model model,
                            @RequestParam(value = "appId", required = false) String appId,
                            @RequestParam(required = false) String state) {
        if (StringUtils.isBlank(appId)) {
            return authCallbackErrorPage(request, response, model, OAuthError.OAUTH_ERROR, "缺少 appId 参数，请从应用内重新打开登录入口。");
        }
        ConnectAppEntity app = connectAppService.getByAppId(appId);
        if (app == null) {
            log.warn("open oauth authorize rejected, app not found: appId={}", appId);
            return authCallbackErrorPage(request, response, model, OAuthError.OAUTH_ERROR, "接入应用不存在，请检查 appId 是否已在「第三方登录接入管理」中完成接入。");
        }
        if (app.getStatus() != ConnectAppEntity.STATUS_ACTIVE) {
            log.warn("open oauth authorize rejected, app disabled: appId={}", appId);
            return authCallbackErrorPage(request, response, model, OAuthError.OAUTH_ERROR, "接入应用已禁用，请联系管理员启用后再试。");
        }
        if (StringUtils.isBlank(state)) {
            state = UUID.randomUUID().toString().replace("-", "");
        }
        OpenOAuthStateSupport.bindState(request, appId, state);
        return new RedirectView(connectOauthService.buildAuthorizeUrl(app, state), false);
    }

    @RequestMapping("callback")
    public Object callback(HttpServletRequest request, HttpServletResponse response, Model model,
                           @RequestParam(required = false) String appId,
                           @RequestParam(required = false, name = OAuth.OAUTH_CODE) String code,
                           @RequestParam(required = false) String state,
                           @RequestParam(required = false) String error,
                           @RequestParam(required = false) String error_description,
                           @RequestParam(required = false) String callback) {
        if (StringUtils.isNotBlank(error)) {
            return authCallbackErrorPage(request, response, model, error, error_description);
        }
        if (StringUtils.isBlank(code)) {
            return authCallbackErrorPage(request, response, model, OAuthError.OAUTH_ERROR, "未收到授权码");
        }
        if (StringUtils.isBlank(appId)) {
            appId = request.getParameter("app_id");
        }
        ConnectAppEntity app = connectAppService.getByAppId(appId);
        if (app == null) {
            return authCallbackErrorPage(request, response, model, OAuthError.OAUTH_ERROR, "接入应用不存在，请检查 appId 是否已在「第三方登录接入管理」中完成接入。");
        }
        if (app.getStatus() != ConnectAppEntity.STATUS_ACTIVE) {
            return authCallbackErrorPage(request, response, model, OAuthError.OAUTH_ERROR, "接入应用已禁用，请联系管理员启用后再试。");
        }
        if (!OpenOAuthStateSupport.consumeState(request, appId, state)) {
            log.warn("opc oauth callback state invalid: appId={}", appId);
            return authCallbackErrorPage(request, response, model, OAuthError.OAUTH_ERROR, "授权状态无效或已过期，请重新发起登录。");
        }
        try {
            connectLoginService.completeOAuthCallback(app, code);
        } catch (ConnectBindException e) {
            log.warn("opc oauth bind conflict: type={}, appId={}", e.getConflictType(), e.getAppId());
            if (e.getConflictType() == ConflictType.USERINFO_INVALID) {
                return authCallbackErrorPage(request, response, model, OAuthError.OAUTH_ERROR, "授权用户信息无效，请重新发起授权。");
            }
            return authCallbackConflictPage(request, response, model, e);
        } catch (Exception e) {
            log.error("opc oauth callback failed: {}", e.getMessage());
            return authCallbackErrorPage(request, response, model, OAuthError.OAUTH_ERROR, "授权登录失败，请稍后重试。");
        }
        String redirect = WebPathUtils.safeOauthCallbackForClient(request, callback);
        if (StringUtils.isBlank(redirect)) {
            redirect = WebPathUtils.forBrowser(request, OpcConstants.OAUTH2_BASE + "/success");
            if (StringUtils.isNotBlank(appId)) {
                redirect = redirect + (redirect.contains("?") ? "&" : "?") + "appId=" + appId;
            }
        }
        return pageFactory.direct(request, response, model, redirect);
    }

    private Object authCallbackErrorPage(HttpServletRequest request, HttpServletResponse response, Model model, String error, String description) {
        model.addAttribute("oauthError", error);
        model.addAttribute("loginUrl", WebPathUtils.forBrowser(request, "/login"));
        if ("access_denied".equalsIgnoreCase(error)) {
            model.addAttribute("title", "授权已取消");
            model.addAttribute("message", "您已取消授权，应用将无法关联您的账号。如需继续使用，请重新发起授权并确认。");
        } else if (StringUtils.isNotBlank(description)) {
            model.addAttribute("title", "授权失败");
            model.addAttribute("message", description);
        } else {
            model.addAttribute("title", "授权未完成");
            model.addAttribute("message", "授权未能完成，请稍后重试。");
        }
        return pageFactory.authCallbackError(request, response, model);
    }

    private Object authCallbackConflictPage(HttpServletRequest request, HttpServletResponse response, Model model, ConnectBindException e) {
        model.addAttribute("oauthError", "bind_conflict");
        model.addAttribute("conflictType", e.getConflictType().name());
        model.addAttribute("loginUrl", WebPathUtils.forBrowser(request, "/login"));
        model.addAttribute("logoutUrl", WebPathUtils.forBrowser(request, "/logout"));
        model.addAttribute("manageUrl", WebPathUtils.forBrowser(request, "/modules/opc/connectbind"));
        model.addAttribute("title", "账号绑定冲突");
        model.addAttribute("message", buildConflictMessage(e));
        model.addAttribute("conflictSolutions", buildConflictSolutions(e));
        return pageFactory.authCallbackError(request, response, model);
    }

    private String buildConflictMessage(ConnectBindException e) {
        if (e.getConflictType() == ConflictType.UPSTREAM_BOUND_TO_OTHER) {
            return "该开放平台账号已与其他本地用户绑定，无法关联到当前授权用户。";
        }
        if (e.getConflictType() == ConflictType.LOCAL_ALREADY_BOUND) {
            return "该本地用户已绑定其他开放平台账号，无法重复绑定。";
        }
        return "授权用户信息无效，请重新发起授权。";
    }

    private String buildConflictSolutions(ConnectBindException e) {
        StringBuilder sb = new StringBuilder();
        if (e.getConflictType() == ConflictType.UPSTREAM_BOUND_TO_OTHER) {
            sb.append("① 使用已绑定的本地账号登录后重试；\n");
            sb.append("② 在「接入绑定管理」或后台解除原绑定关系；\n");
            sb.append("③ 换用未冲突的开放平台账号重新授权。");
        } else if (e.getConflictType() == ConflictType.LOCAL_ALREADY_BOUND) {
            sb.append("① 先解除当前本地账号的开放平台绑定（登录态下 POST open/oauth2/bind/unbind?appId=...），再重新授权；\n");
            sb.append("② 换用未绑定的本地账号登录后再授权；\n");
            sb.append("③ 联系管理员在后台调整绑定关系。");
        } else {
            sb.append("请重新发起授权；若问题持续，请联系管理员。");
        }
        return sb.toString();
    }
}

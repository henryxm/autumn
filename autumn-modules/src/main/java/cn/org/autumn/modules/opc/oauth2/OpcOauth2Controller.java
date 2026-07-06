package cn.org.autumn.modules.opc.oauth2;

import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.opc.service.ConnectAppService;
import cn.org.autumn.modules.opc.service.ConnectLoginService;
import cn.org.autumn.modules.opc.service.ConnectOauthService;
import cn.org.autumn.modules.opc.support.OpenOAuthStateSupport;
import cn.org.autumn.opc.OpcConstants;
import cn.org.autumn.site.PageFactory;
import cn.org.autumn.utils.WebPathUtils;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import java.util.UUID;
import org.apache.commons.lang.StringUtils;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private PageFactory pageFactory;

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
}

package cn.org.autumn.modules.opc.oauth2;

import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.opc.service.ConnectAppService;
import cn.org.autumn.modules.opc.service.ConnectLoginService;
import cn.org.autumn.modules.opc.service.ConnectOauthService;
import cn.org.autumn.opc.OpcConstants;
import cn.org.autumn.site.PageFactory;
import cn.org.autumn.utils.WebPathUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

    @RequestMapping("authorize")
    public RedirectView authorize(@RequestParam("appId") String appId, @RequestParam(required = false) String state) {
        ConnectAppEntity app = connectAppService.getByAppId(appId);
        if (app == null || app.getStatus() != ConnectAppEntity.STATUS_ACTIVE) {
            throw new IllegalArgumentException("无效的接入应用");
        }
        return new RedirectView(connectOauthService.buildAuthorizeUrl(app, state), false);
    }

    @RequestMapping("callback")
    public Object callback(HttpServletRequest request, HttpServletResponse response, Model model,
                           @RequestParam(required = false) String appId,
                           @RequestParam(required = false, name = OAuth.OAUTH_CODE) String code,
                           @RequestParam(required = false) String error,
                           @RequestParam(required = false) String error_description,
                           @RequestParam(required = false) String callback) {
        if (StringUtils.isNotBlank(error)) {
            return oauthCallbackErrorPage(request, model, error, error_description);
        }
        if (StringUtils.isBlank(code)) {
            return oauthCallbackErrorPage(request, model, OAuthError.OAUTH_ERROR, "未收到授权码");
        }
        if (StringUtils.isBlank(appId)) {
            appId = request.getParameter("app_id");
        }
        ConnectAppEntity app = connectAppService.getByAppId(appId);
        if (app == null) {
            return oauthCallbackErrorPage(request, model, OAuthError.OAUTH_ERROR, "无效的appId");
        }
        try {
            connectLoginService.completeOAuthCallback(app, code);
        } catch (Exception e) {
            log.error("opc oauth callback failed: {}", e.getMessage());
            return oauthCallbackErrorPage(request, model, OAuthError.OAUTH_ERROR, e.getMessage());
        }
        String redirect = WebPathUtils.safeOauthCallbackForClient(request, callback);
        if (StringUtils.isBlank(redirect)) {
            redirect = WebPathUtils.forBrowser(request, "/");
        }
        return pageFactory.direct(request, response, model, redirect);
    }

    private String oauthCallbackErrorPage(HttpServletRequest request, Model model, String error, String description) {
        model.addAttribute("oauthError", error);
        model.addAttribute("loginUrl", WebPathUtils.forBrowser(request, "/login.html"));
        if ("access_denied".equalsIgnoreCase(error)) {
            model.addAttribute("title", "授权已取消");
            model.addAttribute("message", "您已取消授权，应用无法获取您的 openId 与 unionId。");
        } else if (StringUtils.isNotBlank(description)) {
            model.addAttribute("title", "授权失败");
            model.addAttribute("message", description);
        } else {
            model.addAttribute("title", "授权未完成");
            model.addAttribute("message", "授权未能完成，请稍后重试。");
        }
        return "oauth2/callback-error";
    }
}

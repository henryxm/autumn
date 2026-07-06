package cn.org.autumn.modules.oauth.oauth2.support;

import cn.org.autumn.view.ViewTemplateSupport;
import javax.servlet.http.HttpServletResponse;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;

/**
 * OAuth2 AS 错误响应 JSON 与 HTTP 写出（OAuth / OPL 共用）。
 */
public final class OAuthResponseSupport {

    private OAuthResponseSupport() {
    }

    public static String oauthErrorBody(String description, String error, int status) throws OAuthSystemException {
        OAuthResponse response = OAuthASResponse.errorResponse(status).setError(error).setErrorDescription(description).buildJSONMessage();
        return response.getBody();
    }

    public static String writeOAuthError(HttpServletResponse response, String description, String error, int status) throws OAuthSystemException {
        OAuthResponse oAuthResponse = OAuthASResponse.errorResponse(status).setError(error).setErrorDescription(description).buildJSONMessage();
        if (response != null && !response.isCommitted()) {
            response.setStatus(oAuthResponse.getResponseStatus());
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");
            try {
                response.getWriter().write(oAuthResponse.getBody());
                response.getWriter().flush();
            } catch (Exception e) {
                throw new OAuthSystemException(e);
            }
            return ViewTemplateSupport.REQUEST_HANDLED;
        }
        return oAuthResponse.getBody();
    }
}

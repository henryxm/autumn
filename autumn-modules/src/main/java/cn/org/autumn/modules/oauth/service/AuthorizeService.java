package cn.org.autumn.modules.oauth.service;

import cn.org.autumn.modules.oauth.store.TokenStore;
import cn.org.autumn.modules.oauth.store.ValueType;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import com.alibaba.fastjson.JSON;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.message.types.ParameterStyle;
import org.apache.oltu.oauth2.rs.request.OAuthAccessResourceRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthorizeService {

    @Autowired
    ClientDetailsService clientDetailsService;

    public SysUserEntity getAuthorized(HttpServletRequest request) {
        try {
            OAuthAccessResourceRequest oauthRequest = new OAuthAccessResourceRequest(request, ParameterStyle.QUERY, ParameterStyle.HEADER);
            String accessToken = oauthRequest.getAccessToken();
            Object resp = JSON.parse(accessToken);
            Map map = (Map) resp;
            String accessTokenKey = "";
            if (map.containsKey(OAuth.OAUTH_ACCESS_TOKEN)) {
                accessTokenKey = (String) map.get(OAuth.OAUTH_ACCESS_TOKEN);
            }
            // 验证访问令牌
            if (clientDetailsService.isValidAccessToken(accessTokenKey)) {
                TokenStore tokenStore = clientDetailsService.get(ValueType.accessToken, accessTokenKey);
                Object user = tokenStore.getValue();
                if (user instanceof SysUserEntity) {
                    return (SysUserEntity) user;
                }
            }
        } catch (Exception e) {
            log.error("Get Authorized", e);
        }
        return null;
    }
}

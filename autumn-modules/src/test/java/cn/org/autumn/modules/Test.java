package cn.org.autumn.modules;

import cn.org.autumn.utils.HttpClientUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;

import java.util.HashMap;
import java.util.Map;

public class Test {

    public static void main(String[] args) throws OAuthSystemException, OAuthProblemException {

//        String url = "http://localhost/oauth2/userInfo";
        String url = "http://localhost/oauth/clientdetails/info/1";
        OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
        String accessToken = "{\"access_token\":\"8aefd30777ab305db60264b57a558d8d\",\"refresh_token\":\"e0efc004d325e0322cbb4fce5f83c43b\",\"expires_in\":3600}";

        OAuthClientRequest authUserRequest = new OAuthBearerClientRequest(url).setAccessToken(accessToken).buildQueryMessage();
        OAuthResourceResponse resourceResponse = oAuthClient.resource(authUserRequest, OAuth.HttpMethod.GET, OAuthResourceResponse.class);


        String userinfo = resourceResponse.getBody();

        System.out.println(userinfo);

    }

    @org.junit.Test
    public void using() {
        Map<String, String> paramMap = new HashMap<>();
        String state = "state";
        String scope = "all";
        paramMap.put(OAuth.OAUTH_STATE, state);
        paramMap.put(OAuth.OAUTH_SCOPE, scope);
        paramMap.put(OAuth.OAUTH_REDIRECT_URI, "http://localhost/client/oauth2/callback");
        paramMap.put(OAuth.OAUTH_GRANT_TYPE, String.valueOf(GrantType.REFRESH_TOKEN));
        paramMap.put(OAuth.OAUTH_CLIENT_ID, "default_client_id");
//        paramMap.put(OAuth.OAUTH_CODE, "880a57fb007f9a5bc0ea6fe3fcc376c0");
        paramMap.put(OAuth.OAUTH_REFRESH_TOKEN, "880a57fb007f9a5bc0ea6fe3fcc376c0");
        paramMap.put(OAuth.OAUTH_CLIENT_SECRET, "c8b6cb557bd145ee90708114c9bc66c9");

        String accessToken = HttpClientUtils.doPost("http://localhost/oauth2/token", paramMap);
        System.out.println(accessToken);

    }

}

package cn.org.autumn.modules.opc.site;

import cn.org.autumn.config.FilterChainHandler;
import cn.org.autumn.opc.OpcConstants;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OpcConfig implements FilterChainHandler {

    @Override
    public void definition(Map<String, String> map) {
        map.put(OpcConstants.OAUTH2_BASE + "/**", "anon");
        map.put(OpcConstants.OAUTH2_LOGIN_PAGE, "anon");
        map.put(OpcConstants.OAUTH2_LOGIN_PAGE + ".html", "anon");
        map.put(OpcConstants.OAUTH2_SUCCESS_PAGE, "anon");
        map.put(OpcConstants.OAUTH2_SUCCESS_PAGE + ".html", "anon");
        map.put(OpcConstants.API_PLATFORM + "/**", "anon");
    }
}

package cn.org.autumn.modules.opl.site;

import cn.org.autumn.config.FilterChainHandler;
import cn.org.autumn.opl.OplConstants;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OplConfig implements FilterChainHandler {

    @Override
    public void definition(Map<String, String> map) {
        map.put(OplConstants.OAUTH2_BASE + "/**", "anon");
        map.put(OplConstants.API_V1_BASE + "/**", "anon");
    }
}

package cn.org.autumn.modules.qrc.site;

import cn.org.autumn.config.FilterChainHandler;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class QrcConfig implements FilterChainHandler {

    @Override
    public void definition(Map<String, String> map) {
        map.put("/qrc/api/v1/**", "anon");
        map.put("/qrc/scanticket/web/**", "anon");
    }
}

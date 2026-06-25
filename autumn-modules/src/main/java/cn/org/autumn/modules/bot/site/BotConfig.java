package cn.org.autumn.modules.bot.site;

import cn.org.autumn.config.FilterChainHandler;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class BotConfig implements FilterChainHandler {

    @Override
    public void definition(Map<String, String> map) {
        map.put("/bot/api/v1/**", "anon");
    }
}

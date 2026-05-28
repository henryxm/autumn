package cn.org.autumn.modules.bot.site;

import cn.org.autumn.config.FilterChainHandler;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class BotConfig implements FilterChainHandler {

    @Override
    public void definition(Map<String, String> map) {
        map.put("/robot/api/v1/**", "anon");
    }
}
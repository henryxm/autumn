package cn.org.autumn.modules.qrc.service.handler;

import cn.org.autumn.exception.CodeException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IntentHandlerRegistry {
    private final Map<String, IntentHandler> handlers = new HashMap<>();

    @Autowired
    public IntentHandlerRegistry(List<IntentHandler> list) {
        if (list != null) {
            for (IntentHandler handler : list) {
                if (handler != null && StringUtils.isNotBlank(handler.intent())) {
                    handlers.put(handler.intent(), handler);
                }
            }
        }
    }

    public IntentHandler require(String intent) throws CodeException {
        IntentHandler handler = handlers.get(intent);
        if (handler == null) {
            throw new CodeException("不支持的扫码意图:" + intent, 8601);
        }
        return handler;
    }
}

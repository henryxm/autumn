package cn.org.autumn.modules.qrc.spi;

import cn.org.autumn.modules.qrc.model.TicketSnapshot;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface ConsentProvider {
    default boolean supports(String clientId, String intent) {
        return false;
    }

    default List<String> describeScopes(TicketSnapshot ticket, Map<String, String> payload) {
        return Collections.emptyList();
    }
}

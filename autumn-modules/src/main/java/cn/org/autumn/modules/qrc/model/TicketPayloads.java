package cn.org.autumn.modules.qrc.model;

import java.util.Collections;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public final class TicketPayloads {

    private TicketPayloads() {
    }

    public static Map<String, String> map(TicketSnapshot ticket) {
        if (ticket == null || ticket.getPayload() == null) {
            return Collections.emptyMap();
        }
        return ticket.getPayload();
    }

    public static String get(TicketSnapshot ticket, String key) {
        if (StringUtils.isBlank(key)) {
            return null;
        }
        return map(ticket).get(key);
    }
}

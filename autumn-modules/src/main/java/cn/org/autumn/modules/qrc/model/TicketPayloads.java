package cn.org.autumn.modules.qrc.model;

import java.util.Collections;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public final class TicketPayloads {

    /** Bridge probe-grant 绑定材料；仅建票/授权页下发，勿写入 status/QR。 */
    public static final String PROBE_NONCE = "probeNonce";

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

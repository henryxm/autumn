package cn.org.autumn.modules.qrc.model;

import cn.org.autumn.modules.qrc.dto.ScannerBrief;
import cn.org.autumn.modules.qrc.dto.TicketStatusResult;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/** B2 同源扫码 SSE 事件（与 ticket/status 字段对齐，供浏览器 EventSource 消费）。 */
@Getter
@Setter
public class AsQrcStreamEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String uuid;
    private String status;
    private ScannerBrief scannerBrief;
    private String exchange;
    private String redirectUrl;
    private Map<String, String> result = new HashMap<>();

    public static AsQrcStreamEvent from(TicketStatusResult status) {
        if (status == null) {
            return null;
        }
        AsQrcStreamEvent event = new AsQrcStreamEvent();
        event.setUuid(status.getUuid());
        event.setStatus(status.getStatus());
        event.setScannerBrief(status.getScannerBrief());
        event.setExchange(status.getExchange());
        event.setRedirectUrl(status.getRedirect());
        if (status.getResult() != null) {
            event.setResult(new HashMap<>(status.getResult()));
        }
        return event;
    }
}

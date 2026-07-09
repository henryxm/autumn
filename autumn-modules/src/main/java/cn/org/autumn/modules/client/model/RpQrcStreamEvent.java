package cn.org.autumn.modules.client.model;

import cn.org.autumn.modules.qrc.dto.ScannerBrief;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RpQrcStreamEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String uuid;
    private String status;
    private ScannerBrief scannerBrief;
    private String redirectUrl;
    private Map<String, String> result = new HashMap<>();

    public static RpQrcStreamEvent from(RpQrcPendingSession pending) {
        if (pending == null) {
            return null;
        }
        RpQrcStreamEvent event = new RpQrcStreamEvent();
        event.setUuid(pending.getUuid());
        event.setStatus(pending.getStatus());
        event.setScannerBrief(pending.getScannerBrief());
        event.setRedirectUrl(pending.getRedirectUrl());
        if (pending.getResult() != null) {
            event.setResult(new HashMap<>(pending.getResult()));
        }
        return event;
    }
}

package cn.org.autumn.modules.client.model;

import cn.org.autumn.modules.qrc.dto.ScannerBrief;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RpQrcPendingSession implements Serializable {
    private static final long serialVersionUID = 1L;

    private String uuid;
    private String status = "PENDING";
    private String clientId;
    private String credentialType;
    private String credentialId;
    private String browserSessionId;
    private String callback;
    private String redirectUrl;
    private String code;
    private String state;
    private Map<String, String> result = new HashMap<>();
    private ScannerBrief scannerBrief;
    private long expiredAt;
}

package cn.org.autumn.opl.model;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpenPlatformEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String event;
    private String appId;
    private String account;
    private String user;
    private String openId;
    private String unionId;
    private Map<String, Object> payload = new LinkedHashMap<>();

    public static OpenPlatformEvent of(String event) {
        OpenPlatformEvent e = new OpenPlatformEvent();
        e.setEvent(event);
        return e;
    }
}

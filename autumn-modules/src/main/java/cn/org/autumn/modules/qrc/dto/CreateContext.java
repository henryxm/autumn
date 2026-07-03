package cn.org.autumn.modules.qrc.dto;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateContext {
    private String intent;
    private Map<String, String> payload = new HashMap<>();
    private String ip;
    private String agent;
    private String clientId;
    private String clientSecret;
}

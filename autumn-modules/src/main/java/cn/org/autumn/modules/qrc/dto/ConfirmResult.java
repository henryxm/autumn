package cn.org.autumn.modules.qrc.dto;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfirmResult {
    private boolean completed;
    private String redirect;
    private Map<String, String> result = new HashMap<>();
    private String exchange;
    private String deepLink;

    public static ConfirmResult ofExchange(String exchange) {
        ConfirmResult r = new ConfirmResult();
        r.setCompleted(false);
        r.setExchange(exchange);
        return r;
    }

    public static ConfirmResult completed(String redirect, Map<String, String> result) {
        ConfirmResult r = new ConfirmResult();
        r.setCompleted(true);
        r.setRedirect(redirect);
        if (result != null) {
            r.setResult(result);
        }
        return r;
    }
}

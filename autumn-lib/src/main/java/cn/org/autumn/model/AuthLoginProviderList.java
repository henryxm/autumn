package cn.org.autumn.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** {@code GET /auth/login/providers} 响应体。 */
@Getter
@Setter
public class AuthLoginProviderList implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_ICON_PATH = "/statics/img/auth-login-default.svg";

    private List<AuthLoginProviderView> providers = new ArrayList<>();
    private String defaultIconUrl = DEFAULT_ICON_PATH;
    private boolean visible;

    public static AuthLoginProviderList of(List<AuthLoginProviderView> providers, String defaultIconUrl) {
        AuthLoginProviderList list = new AuthLoginProviderList();
        if (providers != null) {
            list.providers = new ArrayList<>(providers);
        }
        if (defaultIconUrl != null) {
            list.defaultIconUrl = defaultIconUrl;
        }
        list.visible = list.providers != null && !list.providers.isEmpty();
        return list;
    }
}

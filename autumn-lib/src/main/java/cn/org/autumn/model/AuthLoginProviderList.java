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

    /** 兼容旧版：等同 tabProviders。 */
    private List<AuthLoginProviderView> providers = new ArrayList<>();
    private List<AuthLoginProviderView> tabProviders = new ArrayList<>();
    private List<AuthLoginProviderView> qrProviders = new ArrayList<>();
    private String defaultIconUrl = DEFAULT_ICON_PATH;
    private boolean visible;
    private boolean tabVisible;
    private boolean qrVisible;

    public static AuthLoginProviderList of(List<AuthLoginProviderView> tabProviders, List<AuthLoginProviderView> qrProviders, String defaultIconUrl) {
        AuthLoginProviderList list = new AuthLoginProviderList();
        if (tabProviders != null) {
            list.tabProviders = new ArrayList<>(tabProviders);
            list.providers = new ArrayList<>(tabProviders);
        }
        if (qrProviders != null) {
            list.qrProviders = new ArrayList<>(qrProviders);
        }
        if (defaultIconUrl != null) {
            list.defaultIconUrl = defaultIconUrl;
        }
        list.tabVisible = list.tabProviders != null && !list.tabProviders.isEmpty();
        list.qrVisible = list.qrProviders != null && !list.qrProviders.isEmpty();
        list.visible = list.tabVisible || list.qrVisible;
        return list;
    }
}

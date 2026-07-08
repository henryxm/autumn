package cn.org.autumn.modules.sys.service;

import cn.org.autumn.database.runtime.WrapperColumns;
import cn.org.autumn.model.AuthLoginProviderList;
import cn.org.autumn.model.AuthLoginProviderType;
import cn.org.autumn.model.AuthLoginProviderView;
import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.oauth2.WebOauthEndpointResolver;
import cn.org.autumn.modules.client.service.AuthSiteRoleService;
import cn.org.autumn.modules.client.service.WebAuthenticationService;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.opc.service.ConnectAppService;
import cn.org.autumn.modules.opc.support.ConnectBindSupport;
import cn.org.autumn.opc.OpcConstants;
import cn.org.autumn.utils.WebPathUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** 登录页授权 Provider 聚合（经典 OAuth RP + 开放平台 OPC）。 */
@Service
public class AuthLoginProviderService {

    @Autowired
    private WebAuthenticationService webAuthenticationService;

    @Autowired
    private ConnectAppService connectAppService;

    @Autowired
    private AuthSiteRoleService authSiteRoleService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private WebOauthEndpointResolver webOauthEndpointResolver;

    @Autowired
    private ConnectBindSupport connectBindSupport;

    public AuthLoginProviderList listPageProviders() {
        String baseUrl = sysConfigService.getBaseUrl();
        List<RankedProvider> ranked = new ArrayList<>();
        if (authSiteRoleService.isRpEnabled()) {
            ranked.addAll(listClassicProviders(baseUrl));
        }
        ranked.addAll(listOpenProviders(baseUrl));
        ranked.sort(Comparator.comparingLong(RankedProvider::getCreatedAt).reversed());
        List<AuthLoginProviderView> views = new ArrayList<>(ranked.size());
        for (int i = 0; i < ranked.size(); i++) {
            AuthLoginProviderView view = ranked.get(i).getView();
            view.setSortOrder(i);
            views.add(view);
        }
        return AuthLoginProviderList.of(views, AuthLoginProviderList.DEFAULT_ICON_PATH);
    }

    private List<RankedProvider> listClassicProviders(String baseUrl) {
        QueryWrapper<WebAuthenticationEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("page_login", 1);
        wrapper.orderByDesc(WrapperColumns.columnInWrapper("create_time"));
        List<WebAuthenticationEntity> rows = webAuthenticationService.selectList(wrapper);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<RankedProvider> views = new ArrayList<>();
        for (WebAuthenticationEntity entity : rows) {
            if (!isClassicEligible(entity, baseUrl)) {
                continue;
            }
            AuthLoginProviderView view = new AuthLoginProviderView();
            view.setType(AuthLoginProviderType.OAUTH2_CLASSIC);
            view.setId(entity.getClientId());
            view.setClientId(entity.getClientId());
            view.setName(StringUtils.defaultIfBlank(entity.getName(), entity.getClientId()));
            view.setIconUrl(StringUtils.trimToEmpty(entity.getIcon()));
            view.setSameInstance(WebPathUtils.isSameSiteUrl(entity.getOriginUri(), baseUrl));
            view.setLoginUrl("/oauth2/login?client_id=" + urlEncode(entity.getClientId()));
            views.add(new RankedProvider(view, createdAtMillis(entity.getCreateTime())));
        }
        return views;
    }

    private List<RankedProvider> listOpenProviders(String baseUrl) {
        List<ConnectAppEntity> rows = connectAppService.listPageLoginActive();
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<RankedProvider> views = new ArrayList<>();
        for (ConnectAppEntity entity : rows) {
            if (!isOpenEligible(entity, baseUrl)) {
                continue;
            }
            AuthLoginProviderView view = new AuthLoginProviderView();
            view.setType(AuthLoginProviderType.OAUTH2_OPEN);
            view.setId(entity.getAppId());
            view.setAppId(entity.getAppId());
            view.setName(StringUtils.defaultIfBlank(entity.getName(), entity.getAppId()));
            view.setIconUrl(StringUtils.trimToEmpty(entity.getIcon()));
            view.setPlatformBaseUrl(StringUtils.trimToEmpty(entity.getPlatformBaseUrl()));
            view.setSameInstance(resolveOpenSameInstance(entity, baseUrl));
            view.setLoginUrl(OpcConstants.OAUTH2_LOGIN_PATH + "?appId=" + urlEncode(entity.getAppId()));
            views.add(new RankedProvider(view, createdAtMillis(entity.getCreate())));
        }
        return views;
    }

    private boolean isClassicEligible(WebAuthenticationEntity entity, String baseUrl) {
        if (entity == null || entity.getPageLogin() != 1) {
            return false;
        }
        if (StringUtils.isBlank(entity.getClientId()) || StringUtils.isBlank(entity.getClientSecret())) {
            return false;
        }
        if (StringUtils.isBlank(entity.getRedirectUri())) {
            return false;
        }
        return StringUtils.isNotBlank(webOauthEndpointResolver.resolveAuthorizeUri(entity));
    }

    private boolean isOpenEligible(ConnectAppEntity entity, String baseUrl) {
        if (entity == null || entity.getPageLogin() != 1 || entity.getStatus() != ConnectAppEntity.STATUS_ACTIVE) {
            return false;
        }
        if (StringUtils.isBlank(entity.getAppId()) || StringUtils.isBlank(entity.getRedirectUri())) {
            return false;
        }
        if (!connectAppService.hasConfiguredSecret(entity)) {
            return false;
        }
        String fallbackBase = resolveOpenFallbackBase(entity, baseUrl);
        return connectAppService.tryFillDefaultUris(entity, fallbackBase);
    }

    private String resolveOpenFallbackBase(ConnectAppEntity entity, String siteBaseUrl) {
        if (StringUtils.isNotBlank(entity.getPlatformBaseUrl())) {
            return null;
        }
        if (connectBindSupport.isSamePlatform(entity)) {
            return siteBaseUrl;
        }
        return null;
    }

    private boolean resolveOpenSameInstance(ConnectAppEntity entity, String baseUrl) {
        if (connectBindSupport.isSamePlatform(entity)) {
            return true;
        }
        return WebPathUtils.isSameSiteUrl(entity.getPlatformBaseUrl(), baseUrl);
    }

    private static long createdAtMillis(Date created) {
        return created == null ? 0L : created.getTime();
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(StringUtils.defaultString(value), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    private static final class RankedProvider {
        private final AuthLoginProviderView view;
        private final long createdAt;

        private RankedProvider(AuthLoginProviderView view, long createdAt) {
            this.view = view;
            this.createdAt = createdAt;
        }

        private AuthLoginProviderView getView() {
            return view;
        }

        private long getCreatedAt() {
            return createdAt;
        }
    }
}

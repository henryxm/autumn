package cn.org.autumn.modules.sys.service;

import cn.org.autumn.database.runtime.WrapperColumns;
import cn.org.autumn.model.AuthLoginProviderList;
import cn.org.autumn.model.AuthLoginProviderType;
import cn.org.autumn.model.AuthLoginProviderView;
import cn.org.autumn.model.PageLoginSupport;
import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.oauth2.WebOauthEndpointResolver;
import cn.org.autumn.modules.client.service.AuthSiteRoleService;
import cn.org.autumn.modules.client.service.ScanLoginCredentialService;
import cn.org.autumn.modules.client.service.WebAuthenticationService;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.opc.service.ConnectAppService;
import cn.org.autumn.modules.opc.support.ConnectBindSupport;
import cn.org.autumn.opc.OpcConstants;
import cn.org.autumn.utils.WebPathUtils;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang.StringUtils;
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

    @Autowired
    private ScanLoginCredentialService scanLoginCredentialService;

    public AuthLoginProviderList listPageProviders() {
        String baseUrl = sysConfigService.getBaseUrl();
        List<RankedProvider> tabRanked = new ArrayList<>();
        List<RankedProvider> qrRanked = new ArrayList<>();
        if (authSiteRoleService.isRpEnabled()) {
            tabRanked.addAll(listClassicTabProviders(baseUrl));
            qrRanked.addAll(listClassicQrProviders(baseUrl));
        }
        tabRanked.addAll(listOpenTabProviders(baseUrl));
        qrRanked.addAll(listOpenQrProviders(baseUrl));
        tabRanked.sort(Comparator.comparingLong(RankedProvider::getCreatedAt).reversed());
        qrRanked.sort(Comparator.comparingLong(RankedProvider::getCreatedAt).reversed());
        return AuthLoginProviderList.of(toViews(tabRanked), toViews(qrRanked), AuthLoginProviderList.DEFAULT_ICON_PATH);
    }

    private List<AuthLoginProviderView> toViews(List<RankedProvider> ranked) {
        List<AuthLoginProviderView> views = new ArrayList<>(ranked.size());
        for (int i = 0; i < ranked.size(); i++) {
            AuthLoginProviderView view = ranked.get(i).getView();
            view.setSortOrder(i);
            views.add(view);
        }
        return views;
    }

    private List<RankedProvider> listClassicTabProviders(String baseUrl) {
        EntityWrapper<WebAuthenticationEntity> wrapper = new EntityWrapper<>();
        wrapper.in("page_login", new Object[]{PageLoginSupport.TAB, PageLoginSupport.TAB_AND_QR});
        wrapper.orderBy(WrapperColumns.columnInWrapper("create_time"), false);
        List<WebAuthenticationEntity> rows = webAuthenticationService.selectList(wrapper);
        return buildClassicProviders(rows, baseUrl, true);
    }

    private List<RankedProvider> listClassicQrProviders(String baseUrl) {
        EntityWrapper<WebAuthenticationEntity> wrapper = new EntityWrapper<>();
        wrapper.in("page_login", new Object[]{PageLoginSupport.QR, PageLoginSupport.TAB_AND_QR});
        wrapper.orderBy(WrapperColumns.columnInWrapper("create_time"), false);
        List<WebAuthenticationEntity> rows = webAuthenticationService.selectList(wrapper);
        return buildClassicProviders(rows, baseUrl, false);
    }

    private List<RankedProvider> buildClassicProviders(List<WebAuthenticationEntity> rows, String baseUrl, boolean tabMode) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<RankedProvider> views = new ArrayList<>();
        for (WebAuthenticationEntity entity : rows) {
            if (tabMode && !isClassicTabEligible(entity, baseUrl)) {
                continue;
            }
            if (!tabMode && !isClassicQrEligible(entity, baseUrl)) {
                continue;
            }
            AuthLoginProviderView view = new AuthLoginProviderView();
            view.setType(AuthLoginProviderType.OAUTH2_CLASSIC);
            view.setCredentialType(AuthLoginProviderType.OAUTH2_CLASSIC);
            view.setId(entity.getClientId());
            view.setClientId(entity.getClientId());
            view.setName(StringUtils.defaultIfBlank(entity.getName(), entity.getClientId()));
            view.setIconUrl(StringUtils.trimToEmpty(entity.getIcon()));
            view.setSameInstance(WebPathUtils.isSameSiteUrl(entity.getOriginUri(), baseUrl));
            view.setQrMode(webOauthEndpointResolver.hasRemoteOrigin(entity) ? "rp" : "as");
            if (tabMode) {
                view.setLoginUrl("/oauth2/login?client_id=" + urlEncode(entity.getClientId()));
            }
            views.add(new RankedProvider(view, createdAtMillis(entity.getCreateTime())));
        }
        return views;
    }

    private List<RankedProvider> listOpenTabProviders(String baseUrl) {
        List<ConnectAppEntity> rows = connectAppService.listPageLoginActive();
        return buildOpenProviders(rows, baseUrl, true);
    }

    private List<RankedProvider> listOpenQrProviders(String baseUrl) {
        List<ConnectAppEntity> rows = connectAppService.listPageQrActive();
        return buildOpenProviders(rows, baseUrl, false);
    }

    private List<RankedProvider> buildOpenProviders(List<ConnectAppEntity> rows, String baseUrl, boolean tabMode) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<RankedProvider> views = new ArrayList<>();
        for (ConnectAppEntity entity : rows) {
            if (tabMode && !isOpenTabEligible(entity, baseUrl)) {
                continue;
            }
            if (!tabMode && !isOpenQrEligible(entity, baseUrl)) {
                continue;
            }
            AuthLoginProviderView view = new AuthLoginProviderView();
            view.setType(AuthLoginProviderType.OAUTH2_OPEN);
            view.setCredentialType(AuthLoginProviderType.OAUTH2_OPEN);
            view.setId(entity.getAppId());
            view.setAppId(entity.getAppId());
            view.setName(StringUtils.defaultIfBlank(entity.getName(), entity.getAppId()));
            view.setIconUrl(StringUtils.trimToEmpty(entity.getIcon()));
            view.setPlatformBaseUrl(StringUtils.trimToEmpty(entity.getPlatformBaseUrl()));
            view.setSameInstance(resolveOpenSameInstance(entity, baseUrl));
            view.setQrMode(resolveOpenQrMode(entity, baseUrl));
            if (tabMode) {
                view.setLoginUrl(OpcConstants.OAUTH2_LOGIN_PATH + "?appId=" + urlEncode(entity.getAppId()));
            }
            views.add(new RankedProvider(view, createdAtMillis(entity.getCreate())));
        }
        return views;
    }

    private boolean isClassicTabEligible(WebAuthenticationEntity entity, String baseUrl) {
        if (entity == null || !PageLoginSupport.showTab(entity.getPageLogin())) {
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

    private boolean isClassicQrEligible(WebAuthenticationEntity entity, String baseUrl) {
        if (entity == null || !PageLoginSupport.showQr(entity.getPageLogin())) {
            return false;
        }
        if (StringUtils.isBlank(entity.getClientId()) || StringUtils.isBlank(entity.getClientSecret())) {
            return false;
        }
        if (StringUtils.isBlank(entity.getRedirectUri())) {
            return false;
        }
        try {
            scanLoginCredentialService.require(AuthLoginProviderType.OAUTH2_CLASSIC, entity.getClientId());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isOpenTabEligible(ConnectAppEntity entity, String baseUrl) {
        if (entity == null || !PageLoginSupport.showTab(entity.getPageLogin()) || entity.getStatus() != ConnectAppEntity.STATUS_ACTIVE) {
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

    private boolean isOpenQrEligible(ConnectAppEntity entity, String baseUrl) {
        if (entity == null || !PageLoginSupport.showQr(entity.getPageLogin()) || entity.getStatus() != ConnectAppEntity.STATUS_ACTIVE) {
            return false;
        }
        if (StringUtils.isBlank(entity.getAppId()) || StringUtils.isBlank(entity.getRedirectUri())) {
            return false;
        }
        if (!connectAppService.hasConfiguredSecret(entity)) {
            return false;
        }
        try {
            scanLoginCredentialService.require(AuthLoginProviderType.OAUTH2_OPEN, entity.getAppId());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String resolveOpenQrMode(ConnectAppEntity entity, String baseUrl) {
        if (connectBindSupport.isSamePlatform(entity) || WebPathUtils.isSameSiteUrl(entity.getPlatformBaseUrl(), baseUrl)) {
            return "as";
        }
        return "rp";
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

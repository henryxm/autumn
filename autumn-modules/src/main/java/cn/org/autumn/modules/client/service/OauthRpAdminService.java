package cn.org.autumn.modules.client.service;

import cn.org.autumn.config.ClientType;
import cn.org.autumn.database.runtime.WrapperColumns;
import cn.org.autumn.modules.client.dao.WebOauthBindDao;
import cn.org.autumn.modules.client.dto.OauthRpBindAdminView;
import cn.org.autumn.modules.client.dto.OauthRpClientView;
import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.client.entity.WebOauthBindEntity;
import cn.org.autumn.modules.auth.support.AuthScopeSupport;
import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import cn.org.autumn.modules.oauth.service.ClientDetailsService;
import cn.org.autumn.modules.support.AdminPageQueries;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.utils.WebPathUtils;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Uuid;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 经典 OAuth2 下游 RP 综合管理（面向 oauthrpmanage 管理页）。 */
@Service
public class OauthRpAdminService {

    @Autowired
    private WebAuthenticationService webAuthenticationService;

    @Autowired
    private WebOauthBindService webOauthBindService;

    @Autowired
    private WebOauthBindDao webOauthBindDao;

    @Autowired
    private ClientDetailsService clientDetailsService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private AuthScopeSupport authScopeSupport;

    public Map<String, Object> overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("clientCount", webAuthenticationService.selectCount(new QueryWrapper<>()));
        data.put("bindCount", webOauthBindService.selectCount(new QueryWrapper<>()));
        return data;
    }

    public PageUtils pageClients(Map<String, Object> params) {
        QueryWrapper<WebAuthenticationEntity> wrapper = new QueryWrapper<>();
        AdminPageQueries.applyKeyword(wrapper, params, "client_id", "name", "uuid", "origin_uri");
        wrapper.orderByDesc(WrapperColumns.columnInWrapper("create_time"));
        PageUtils page = webAuthenticationService.queryPage(webAuthenticationService.getPage(params), wrapper);
        enrichClientRows(page);
        return page;
    }

    public OauthRpClientView getClientDetail(String clientId) {
        WebAuthenticationEntity entity = requireClient(clientId);
        return toClientView(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public OauthRpClientView quickCreate(String clientId) {
        if (StringUtils.isBlank(clientId)) {
            throw new IllegalArgumentException("clientId不能为空");
        }
        clientId = clientId.trim();
        if (webAuthenticationService.getByClientId(clientId) != null) {
            throw new IllegalStateException("接入应用已存在: " + clientId);
        }
        String baseUrl = sysConfigService.getBaseUrl();
        ClientDetailsEntity asClient = clientDetailsService.findByClientId(clientId);
        String secret;
        String redirectUri;
        if (asClient != null) {
            secret = asClient.getClientSecret();
            redirectUri = StringUtils.defaultIfBlank(asClient.getRedirectUri(), baseUrl + "/client/oauth2/callback");
        } else {
            secret = Uuid.uuid();
            redirectUri = baseUrl + "/client/oauth2/callback";
        }
        WebAuthenticationEntity created = webAuthenticationService.create(baseUrl, clientId, secret, ClientType.ManualCreate, clientId, "basic", "normal");
        if (created == null) {
            throw new IllegalStateException("创建接入应用失败");
        }
        created.setRedirectUri(redirectUri);
        created.setOriginUri("");
        webAuthenticationService.updateAllColumnById(created);
        return toClientView(created);
    }

    @Transactional(rollbackFor = Exception.class)
    public OauthRpClientView saveClient(String clientId, String name, String clientSecret, String originUri, String redirectUri, String scope, String userInfoDelivery, String icon, String hash, Integer pageLogin) {
        if (StringUtils.isBlank(clientId)) {
            throw new IllegalArgumentException("clientId不能为空");
        }
        clientId = clientId.trim();
        String baseUrl = sysConfigService.getBaseUrl();
        WebAuthenticationEntity existing = webAuthenticationService.getByClientId(clientId);
        boolean sameInstance = WebPathUtils.isSameSiteUrl(originUri, baseUrl);
        if (existing == null) {
            if (StringUtils.isBlank(clientSecret)) {
                if (sameInstance) {
                    ClientDetailsEntity asClient = clientDetailsService.findByClientId(clientId);
                    clientSecret = asClient == null ? Uuid.uuid() : asClient.getClientSecret();
                } else {
                    throw new IllegalArgumentException("跨实例接入时 clientSecret 不能为空");
                }
            }
            existing = webAuthenticationService.create(baseUrl, clientId, clientSecret, ClientType.ManualCreate, StringUtils.defaultIfBlank(name, clientId), StringUtils.defaultIfBlank(scope, "basic"), "normal");
            if (existing == null) {
                throw new IllegalStateException("创建接入应用失败");
            }
        }
        applyClientFields(existing, name, clientSecret, originUri, redirectUri, scope, userInfoDelivery, baseUrl, sameInstance, icon, hash, pageLogin);
        validateOAuthScope(existing.getScope());
        webAuthenticationService.updateAllColumnById(existing);
        return toClientView(existing);
    }

    @Transactional(rollbackFor = Exception.class)
    public OauthRpClientView updateClient(String clientId, String name, String clientSecret, String originUri, String redirectUri, String scope, String userInfoDelivery, String icon, String hash, Integer pageLogin) {
        WebAuthenticationEntity existing = requireClient(clientId);
        String baseUrl = sysConfigService.getBaseUrl();
        applyClientFields(existing, name, clientSecret, originUri, redirectUri, scope, userInfoDelivery, baseUrl, WebPathUtils.isSameSiteUrl(originUri, baseUrl), icon, hash, pageLogin);
        validateOAuthScope(existing.getScope());
        webAuthenticationService.updateAllColumnById(existing);
        return toClientView(existing);
    }

    public PageUtils pageBinds(Map<String, Object> params) {
        QueryWrapper<WebOauthBindEntity> wrapper = new QueryWrapper<>();
        String clientId = AdminPageQueries.stringParam(params, "clientId");
        if (StringUtils.isNotBlank(clientId)) {
            WebAuthenticationEntity app = webAuthenticationService.getByClientId(clientId.trim());
            if (app != null) {
                wrapper.eq("authentication", app.getUuid());
            } else {
                wrapper.eq("authentication", "__none__");
            }
        }
        AdminPageQueries.applyKeyword(wrapper, params, "upper", "user");
        wrapper.orderByDesc(WrapperColumns.columnInWrapper("create"));
        PageUtils page = webOauthBindService.queryPage(webOauthBindService.getPage(params), wrapper);
        enrichBindRows(page);
        return page;
    }

    @Transactional(rollbackFor = Exception.class)
    public WebOauthBindEntity createBind(String clientId, String localUserUuid, String upperUuid) {
        if (StringUtils.isBlank(clientId) || StringUtils.isBlank(localUserUuid) || StringUtils.isBlank(upperUuid)) {
            throw new IllegalArgumentException("clientId、本地用户与上游 uuid 不能为空");
        }
        WebAuthenticationEntity app = requireClient(clientId);
        if (sysUserService.getByUuid(localUserUuid.trim()) == null) {
            throw new IllegalArgumentException("本地用户不存在");
        }
        WebOauthBindEntity existingUpper = webOauthBindDao.getByAuthenticationAndUpper(app.getUuid(), upperUuid.trim());
        if (existingUpper != null) {
            throw new IllegalArgumentException("上游 uuid 已绑定");
        }
        WebOauthBindEntity existingUser = webOauthBindDao.getByAuthenticationAndUser(app.getUuid(), localUserUuid.trim());
        if (existingUser != null) {
            throw new IllegalArgumentException("本地用户已绑定");
        }
        WebOauthBindEntity bind = new WebOauthBindEntity();
        bind.setAuthentication(app.getUuid());
        bind.setUser(localUserUuid.trim());
        bind.setUpper(upperUuid.trim());
        Date now = new Date();
        bind.setCreate(now);
        bind.setUpdate(now);
        webOauthBindService.insert(bind);
        return bind;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteBind(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id不能为空");
        }
        WebOauthBindEntity bind = webOauthBindService.selectById(id);
        if (bind == null) {
            throw new IllegalArgumentException("绑定不存在");
        }
        webOauthBindService.deleteById(id);
    }

    public List<WebAuthenticationEntity> listAllClientsBrief() {
        QueryWrapper<WebAuthenticationEntity> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc(WrapperColumns.columnInWrapper("create_time"));
        List<WebAuthenticationEntity> list = webAuthenticationService.selectList(wrapper);
        if (list == null) {
            return new ArrayList<>();
        }
        for (WebAuthenticationEntity entity : list) {
            stripSecret(entity);
        }
        return list;
    }

    private WebAuthenticationEntity requireClient(String clientId) {
        if (StringUtils.isBlank(clientId)) {
            throw new IllegalArgumentException("clientId不能为空");
        }
        WebAuthenticationEntity entity = webAuthenticationService.getByClientId(clientId.trim());
        if (entity == null) {
            throw new IllegalArgumentException("接入应用不存在");
        }
        return entity;
    }

    private void applyClientFields(WebAuthenticationEntity entity, String name, String clientSecret, String originUri, String redirectUri, String scope, String userInfoDelivery, String baseUrl, boolean sameInstance, String icon, String hash, Integer pageLogin) {
        if (StringUtils.isNotBlank(name)) {
            entity.setName(name.trim());
            entity.setDescription(name.trim());
        }
        if (StringUtils.isNotBlank(clientSecret)) {
            entity.setClientSecret(clientSecret.trim());
        }
        entity.setOriginUri(StringUtils.trimToEmpty(originUri));
        if (StringUtils.isNotBlank(redirectUri)) {
            entity.setRedirectUri(redirectUri.trim());
        } else if (StringUtils.isBlank(entity.getRedirectUri())) {
            entity.setRedirectUri(baseUrl + "/client/oauth2/callback");
        }
        if (StringUtils.isNotBlank(scope)) {
            entity.setScope(scope.trim());
        }
        entity.setUserInfoDelivery(StringUtils.trimToNull(userInfoDelivery));
        webAuthenticationService.applyIcon(entity, icon, hash);
        if (pageLogin != null) {
            entity.setPageLogin(pageLogin);
        }
        String authBase = sameInstance ? baseUrl : StringUtils.removeEnd(StringUtils.trimToEmpty(originUri), "/");
        if (StringUtils.isBlank(authBase)) {
            authBase = baseUrl;
        }
        entity.setAuthorizeUri(authBase + "/oauth2/authorize");
        entity.setAccessTokenUri(authBase + "/oauth2/token");
        entity.setUserInfoUri(authBase + "/oauth2/userInfo");
    }

    private void enrichClientRows(PageUtils page) {
        if (page == null || page.getList() == null) {
            return;
        }
        List<OauthRpClientView> rows = new ArrayList<>();
        for (Object item : page.getList()) {
            if (item instanceof WebAuthenticationEntity) {
                rows.add(toClientView((WebAuthenticationEntity) item));
            }
        }
        page.setList(rows);
    }

    private OauthRpClientView toClientView(WebAuthenticationEntity entity) {
        String baseUrl = sysConfigService.getBaseUrl();
        boolean sameInstance = WebPathUtils.isSameSiteUrl(entity.getOriginUri(), baseUrl);
        OauthRpClientView view = new OauthRpClientView();
        view.setId(entity.getId());
        view.setUuid(entity.getUuid());
        view.setClientId(entity.getClientId());
        view.setName(entity.getName());
        view.setOriginUri(entity.getOriginUri());
        view.setRedirectUri(entity.getRedirectUri());
        view.setAuthorizeUri(entity.getAuthorizeUri());
        view.setAccessTokenUri(entity.getAccessTokenUri());
        view.setUserInfoUri(entity.getUserInfoUri());
        view.setScope(entity.getScope());
        view.setUserInfoDelivery(entity.getUserInfoDelivery());
        view.setCreateTime(entity.getCreateTime());
        view.setBindCount(webOauthBindService.selectCount(new QueryWrapper<WebOauthBindEntity>().eq("authentication", entity.getUuid())));
        view.setLoginUrl(baseUrl + "/oauth2/login?client_id=" + entity.getClientId());
        view.setLoginAuthentication("oauth2:" + entity.getClientId());
        view.setSecretConfigured(StringUtils.isNotBlank(entity.getClientSecret()));
        view.setSameInstance(sameInstance);
        view.setIcon(entity.getIcon());
        view.setHash(entity.getHash());
        view.setPageLogin(entity.getPageLogin());
        return view;
    }

    private void enrichBindRows(PageUtils page) {
        if (page == null || page.getList() == null) {
            return;
        }
        Map<String, WebAuthenticationEntity> cache = new HashMap<>();
        List<OauthRpBindAdminView> rows = new ArrayList<>();
        for (Object item : page.getList()) {
            if (!(item instanceof WebOauthBindEntity)) {
                continue;
            }
            WebOauthBindEntity bind = (WebOauthBindEntity) item;
            OauthRpBindAdminView view = new OauthRpBindAdminView();
            view.setId(bind.getId());
            view.setAuthentication(bind.getAuthentication());
            view.setUser(bind.getUser());
            view.setUsername(resolveUsername(bind.getUser()));
            view.setUpper(bind.getUpper());
            view.setCreate(bind.getCreate());
            view.setUpdate(bind.getUpdate());
            WebAuthenticationEntity app = cache.get(bind.getAuthentication());
            if (app == null && StringUtils.isNotBlank(bind.getAuthentication())) {
                app = webAuthenticationService.getByUuid(bind.getAuthentication());
                if (app != null) {
                    cache.put(bind.getAuthentication(), app);
                }
            }
            if (app != null) {
                view.setClientId(app.getClientId());
                view.setClientName(app.getName());
            }
            rows.add(view);
        }
        page.setList(rows);
    }

    private void stripSecret(WebAuthenticationEntity entity) {
        if (entity != null) {
            entity.setClientSecret(null);
        }
    }

    private String resolveUsername(String userUuid) {
        if (StringUtils.isBlank(userUuid)) {
            return "";
        }
        SysUserEntity user = sysUserService.getByUuid(userUuid);
        if (user == null) {
            return userUuid;
        }
        return StringUtils.defaultIfBlank(user.getUsername(), userUuid);
    }

    private void validateOAuthScope(String scope) {
        if (StringUtils.isBlank(scope)) {
            return;
        }
        ClientDetailsEntity helper = new ClientDetailsEntity();
        helper.setScope(scope.trim());
        try {
            authScopeSupport.validateOAuthScope(authScopeSupport.toSnapshot(helper), scope.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("无效的授权范围: " + scope.trim(), e);
        }
    }
}

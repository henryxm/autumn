package cn.org.autumn.modules.oauth.service;

import cn.org.autumn.config.ClientType;
import cn.org.autumn.database.runtime.WrapperColumns;
import cn.org.autumn.modules.oauth.dto.OauthAsClientView;
import cn.org.autumn.modules.oauth.dto.OauthAsCreateOutcome;
import cn.org.autumn.modules.auth.support.AuthScopeSupport;
import cn.org.autumn.modules.oauth.entity.ClientDetailsEntity;
import cn.org.autumn.modules.oauth.entity.TokenStoreEntity;
import cn.org.autumn.modules.support.AdminPageQueries;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Uuid;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 经典 OAuth2 上游 AS 综合管理（面向 oauthasmanage 管理页）。 */
@Service
public class OauthAsAdminService {

    @Autowired
    private ClientDetailsService clientDetailsService;

    @Autowired
    private TokenStoreService tokenStoreService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private AuthScopeSupport authScopeSupport;

    public Map<String, Object> overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("clientCount", clientDetailsService.selectCount(new QueryWrapper<>()));
        data.put("trustedCount", clientDetailsService.selectCount(new QueryWrapper<ClientDetailsEntity>().eq("trusted", 1)));
        data.put("activeCount", clientDetailsService.selectCount(new QueryWrapper<ClientDetailsEntity>().eq("archived", 0)));
        data.put("tokenCount", tokenStoreService.selectCount(new QueryWrapper<>()));
        return data;
    }

    public PageUtils pageClients(Map<String, Object> params) {
        QueryWrapper<ClientDetailsEntity> wrapper = new QueryWrapper<>();
        AdminPageQueries.applyKeyword(wrapper, params, "client_id", "client_name", "uuid");
        wrapper.orderByDesc(WrapperColumns.columnInWrapper("create_time"));
        PageUtils page = clientDetailsService.queryPage(clientDetailsService.getPage(params), wrapper);
        enrichClientRows(page);
        return page;
    }

    public OauthAsClientView getClientDetail(String clientId) {
        return toClientView(requireClient(clientId));
    }

    @Transactional(rollbackFor = Exception.class)
    public OauthAsCreateOutcome createClient(String clientId, String name, String redirectUri, String scope) {
        if (StringUtils.isBlank(clientId)) {
            throw new IllegalArgumentException("clientId不能为空");
        }
        clientId = clientId.trim();
        if (clientDetailsService.findByClientId(clientId) != null) {
            throw new IllegalStateException("客户端已存在: " + clientId);
        }
        String baseUrl = sysConfigService.getBaseUrl();
        String secret = Uuid.uuid();
        if (StringUtils.isBlank(name)) {
            name = clientId;
        }
        if (StringUtils.isBlank(redirectUri)) {
            redirectUri = baseUrl + "/client/oauth2/callback";
        }
        if (StringUtils.isBlank(scope)) {
            scope = "basic";
        }
        validateOAuthScope(scope);
        ClientDetailsEntity entity = clientDetailsService.create(baseUrl, clientId, secret, ClientType.ManualCreate, name, name);
        if (entity == null) {
            throw new IllegalStateException("创建客户端失败");
        }
        entity.setRedirectUri(redirectUri.trim());
        entity.setScope(scope.trim());
        entity.setTrusted(1);
        entity.setArchived(0);
        clientDetailsService.updateAllColumnById(entity);
        return toCreateOutcome(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public OauthAsCreateOutcome resetSecret(String clientId) {
        ClientDetailsEntity entity = requireClient(clientId);
        String secret = Uuid.uuid();
        entity.setClientSecret(secret);
        clientDetailsService.updateAllColumnById(entity);
        return toCreateOutcome(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public ClientDetailsEntity updateClient(String clientId, String name, String redirectUri, String scope, Integer trusted, Integer archived) {
        ClientDetailsEntity entity = requireClient(clientId);
        if (StringUtils.isNotBlank(name)) {
            entity.setClientName(name.trim());
        }
        if (StringUtils.isNotBlank(redirectUri)) {
            entity.setRedirectUri(redirectUri.trim());
        }
        if (StringUtils.isNotBlank(scope)) {
            validateOAuthScope(scope.trim());
            entity.setScope(scope.trim());
        }
        if (trusted != null) {
            entity.setTrusted(trusted);
        }
        if (archived != null) {
            entity.setArchived(archived);
        }
        clientDetailsService.updateAllColumnById(entity);
        return entity;
    }

    public PageUtils pageTokens(Map<String, Object> params) {
        QueryWrapper<TokenStoreEntity> wrapper = new QueryWrapper<>();
        AdminPageQueries.applyKeyword(wrapper, params, "user_uuid", "access_token", "refresh_token", "auth_code");
        wrapper.orderByDesc(WrapperColumns.columnInWrapper("update_time"));
        PageUtils page = tokenStoreService.queryPage(tokenStoreService.getPage(params), wrapper);
        enrichTokenRows(page);
        return page;
    }

    private ClientDetailsEntity requireClient(String clientId) {
        if (StringUtils.isBlank(clientId)) {
            throw new IllegalArgumentException("clientId不能为空");
        }
        ClientDetailsEntity entity = clientDetailsService.findByClientId(clientId.trim());
        if (entity == null) {
            throw new IllegalArgumentException("客户端不存在");
        }
        return entity;
    }

    private void enrichClientRows(PageUtils page) {
        if (page == null || page.getList() == null) {
            return;
        }
        List<OauthAsClientView> rows = new ArrayList<>();
        for (Object item : page.getList()) {
            if (item instanceof ClientDetailsEntity) {
                rows.add(toClientView((ClientDetailsEntity) item));
            }
        }
        page.setList(rows);
    }

    private OauthAsClientView toClientView(ClientDetailsEntity entity) {
        String baseUrl = sysConfigService.getBaseUrl();
        OauthAsClientView view = new OauthAsClientView();
        view.setId(entity.getId());
        view.setUuid(entity.getUuid());
        view.setClientId(entity.getClientId());
        view.setClientName(entity.getClientName());
        view.setRedirectUri(entity.getRedirectUri());
        view.setScope(entity.getScope());
        view.setGrantTypes(entity.getGrantTypes());
        view.setTrusted(entity.getTrusted());
        view.setArchived(entity.getArchived());
        view.setCreateTime(entity.getCreateTime());
        view.setAuthorizeUrl(baseUrl + "/oauth2/authorize");
        view.setLoginUrl(baseUrl + "/oauth2/login?client_id=" + entity.getClientId());
        return view;
    }

    private OauthAsCreateOutcome toCreateOutcome(ClientDetailsEntity entity) {
        String baseUrl = sysConfigService.getBaseUrl();
        OauthAsCreateOutcome outcome = new OauthAsCreateOutcome();
        outcome.setClientId(entity.getClientId());
        outcome.setClientSecret(entity.getClientSecret());
        outcome.setClientName(entity.getClientName());
        outcome.setRedirectUri(entity.getRedirectUri());
        outcome.setScope(entity.getScope());
        outcome.setAuthorizeUrl(baseUrl + "/oauth2/authorize");
        outcome.setTokenUrl(baseUrl + "/oauth2/token");
        outcome.setUserInfoUrl(baseUrl + "/oauth2/userInfo");
        return outcome;
    }

    private void enrichTokenRows(PageUtils page) {
        if (page == null || page.getList() == null) {
            return;
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object item : page.getList()) {
            if (!(item instanceof TokenStoreEntity)) {
                continue;
            }
            TokenStoreEntity token = (TokenStoreEntity) item;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", token.getId());
            row.put("userUuid", token.getUserUuid());
            row.put("username", resolveUsername(token.getUserUuid()));
            row.put("authCode", maskToken(token.getAuthCode()));
            row.put("accessToken", maskToken(token.getAccessToken()));
            row.put("refreshToken", maskToken(token.getRefreshToken()));
            row.put("accessTokenExpiredIn", token.getAccessTokenExpiredIn());
            row.put("refreshTokenExpiredIn", token.getRefreshTokenExpiredIn());
            row.put("createTime", token.getCreateTime());
            row.put("updateTime", token.getUpdateTime());
            rows.add(row);
        }
        page.setList(rows);
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

    private String maskToken(String token) {
        if (StringUtils.isBlank(token)) {
            return "";
        }
        if (token.length() <= 12) {
            return token;
        }
        return token.substring(0, 8) + "..." + token.substring(token.length() - 4);
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

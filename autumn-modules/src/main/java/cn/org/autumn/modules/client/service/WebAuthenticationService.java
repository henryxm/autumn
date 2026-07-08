package cn.org.autumn.modules.client.service;

import cn.org.autumn.base.ModuleService;
import cn.org.autumn.config.ClientType;
import cn.org.autumn.config.DomainHandler;
import cn.org.autumn.config.UsingHandler;
import cn.org.autumn.modules.client.dao.WebAuthenticationDao;
import cn.org.autumn.modules.client.entity.WebAuthenticationEntity;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.site.UpgradeFactory;
import cn.org.autumn.utils.Utils;
import cn.org.autumn.utils.Uuid;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class WebAuthenticationService extends ModuleService<WebAuthenticationDao, WebAuthenticationEntity> implements UpgradeFactory.Domain, DomainHandler, UsingHandler {

    @Autowired
    SysConfigService sysConfigService;

    @Override
    public String ico() {
        return "fa-mobile";
    }

    @Override
    public boolean using(Object value) {
        return isIconHashInUse(value);
    }

    /**
     * 登录页图标文件 hash 是否仍被 RP 接入应用引用。
     */
    public boolean isIconHashInUse(Object hash) {
        if (hash == null) {
            return false;
        }
        String normalized = normalizeIconHash(String.valueOf(hash));
        if (StringUtils.isBlank(normalized)) {
            return false;
        }
        return countByIconHash(normalized) > 0;
    }

    public int countByIconHash(String hash) {
        String normalized = normalizeIconHash(hash);
        if (StringUtils.isBlank(normalized)) {
            return 0;
        }
        return baseMapper.countByHashInUse(normalized);
    }

    public static String normalizeIconHash(String hash) {
        if (StringUtils.isBlank(hash)) {
            return null;
        }
        return hash.trim();
    }

    /**
     * 更新 RP 接入应用的登录页图标与文件 hash（扩展项目可直接调用）。
     */
    @Transactional(rollbackFor = Exception.class)
    public WebAuthenticationEntity updateIcon(String clientId, String icon, String hash) {
        if (StringUtils.isBlank(clientId)) {
            throw new IllegalArgumentException("clientId不能为空");
        }
        WebAuthenticationEntity entity = getByClientId(clientId.trim());
        if (entity == null) {
            throw new IllegalArgumentException("接入应用不存在");
        }
        applyIcon(entity, icon, hash);
        updateById(entity);
        return entity;
    }

    @Transactional(rollbackFor = Exception.class)
    public WebAuthenticationEntity updateIconByUuid(String uuid, String icon, String hash) {
        if (StringUtils.isBlank(uuid)) {
            throw new IllegalArgumentException("uuid不能为空");
        }
        WebAuthenticationEntity entity = getByUuid(uuid.trim());
        if (entity == null) {
            throw new IllegalArgumentException("接入应用不存在");
        }
        applyIcon(entity, icon, hash);
        updateById(entity);
        return entity;
    }

    public void applyIcon(WebAuthenticationEntity entity, String icon, String hash) {
        if (entity == null) {
            return;
        }
        if (icon != null) {
            entity.setIcon(StringUtils.trimToEmpty(icon));
        }
        if (hash != null) {
            entity.setHash(normalizeIconHash(hash));
        } else if (icon != null && StringUtils.isBlank(entity.getIcon())) {
            entity.setHash(null);
        }
    }

    public WebAuthenticationEntity getByClientId(String clientId) {
        if (StringUtils.isBlank(clientId))
            return null;
        return baseMapper.getByClientId(clientId);
    }

    public WebAuthenticationEntity getByUuid(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return null;
        }
        return baseMapper.getByUuid(uuid);
    }

    public boolean hasClientId(String clientId) {
        if (StringUtils.isBlank(clientId))
            return false;
        Integer has = baseMapper.hasClientId(clientId);
        return null != has && has > 0;
    }

    public WebAuthenticationEntity create(String baseUrl, String clientId, String clientSecret, ClientType clientType, String name, String scope, String state) {
        try {
            if (StringUtils.isBlank(clientId))
                return null;
            WebAuthenticationEntity webAuthClientEntity = new WebAuthenticationEntity();
            webAuthClientEntity.setUuid(Uuid.uuid());
            webAuthClientEntity.setClientId(clientId);
            if (StringUtils.isBlank(name))
                name = clientId;
            webAuthClientEntity.setName(name);
            if (StringUtils.isBlank(clientSecret))
                clientSecret = Uuid.uuid();
            webAuthClientEntity.setClientSecret(clientSecret);
            if (StringUtils.isBlank(baseUrl))
                baseUrl = sysConfigService.getBaseUrl();
            webAuthClientEntity.setRedirectUri(baseUrl + "/client/oauth2/callback");
            webAuthClientEntity.setDescription(name);
            webAuthClientEntity.setAuthorizeUri(baseUrl + "/oauth2/authorize");
            webAuthClientEntity.setAccessTokenUri(baseUrl + "/oauth2/token");
            webAuthClientEntity.setUserInfoUri(baseUrl + "/oauth2/userInfo");
            if (StringUtils.isBlank(scope))
                scope = "basic";
            webAuthClientEntity.setScope(scope);
            if (StringUtils.isBlank(state))
                state = "normal";
            webAuthClientEntity.setState(state);
            webAuthClientEntity.setClientType(clientType);
            webAuthClientEntity.setCreateTime(new Date());
            insert(webAuthClientEntity);
            return webAuthClientEntity;
        } catch (Exception ignored) {
        }
        return null;
    }

    @Order(2000)
    public void init() {
        super.init();
        if (!hasClientId(sysConfigService.getClientId())) {
            ClientType type = baseMapper.countClientType(ClientType.SiteDefault) > 0 ? null : ClientType.SiteDefault;
            create(sysConfigService.getBaseUrl(), sysConfigService.getClientId(), sysConfigService.getClientSecret(), type, "默认的客户端", "basic", "normal");
        }
        updateClientType(sysConfigService.getClientId(), ClientType.SiteDefault);
    }

    public void updateClientType(String clientId, ClientType clientType) {
        baseMapper.updateClientType(clientId, clientType);
    }

    @Override
    public void onDomainChanged() {
        String host = sysConfigService.getSiteDomain();
        String scheme = sysConfigService.getScheme();
        List<WebAuthenticationEntity> entities = selectByMap(null);
        for (WebAuthenticationEntity entity : entities) {
            try {
                update(entity, scheme, host, false);
            } catch (Exception e) {
                log.debug("Update failed:{}", e.getMessage());
            }
        }
    }

    public void update(WebAuthenticationEntity entity, String scheme, String host, boolean force) {
        if (StringUtils.isBlank(entity.getUuid())) {
            entity.setUuid(Uuid.uuid());
            updateById(entity);
        }
        if (!force && !Objects.equals(entity.getClientType(), ClientType.SiteDefault))
            return;
        if (StringUtils.isNotBlank(entity.getAccessTokenUri()))
            entity.setAccessTokenUri(Utils.replaceSchemeHost(scheme, host, entity.getAccessTokenUri()));
        if (StringUtils.isNotBlank(entity.getAuthorizeUri()))
            entity.setAuthorizeUri(Utils.replaceSchemeHost(scheme, host, entity.getAuthorizeUri()));
        if (StringUtils.isNotBlank(entity.getRedirectUri()))
            entity.setRedirectUri(Utils.replaceSchemeHost(scheme, host, entity.getRedirectUri()));
        if (StringUtils.isNotBlank(entity.getUserInfoUri()))
            entity.setUserInfoUri(Utils.replaceSchemeHost(scheme, host, entity.getUserInfoUri()));
        entity.setName(host);
        entity.setDescription(host);
        entity.setClientId(host);
        updateById(entity);
    }

    public void update(String uuid, String domain) {
        WebAuthenticationEntity entity = baseMapper.getByUuid(uuid);
        if (null != entity) {
            String scheme = sysConfigService.getScheme();
            update(entity, scheme, domain, true);
        }
    }

    @Override
    public boolean isSiteDomain(String domain) {
        return baseMapper.count(domain) > 0;
    }
}

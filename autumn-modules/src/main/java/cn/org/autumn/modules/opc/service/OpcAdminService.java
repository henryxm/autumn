package cn.org.autumn.modules.opc.service;

import cn.org.autumn.modules.open.support.AdminPageQueries;
import cn.org.autumn.database.runtime.WrapperColumns;
import cn.org.autumn.modules.opc.dto.OpcAppAdminView;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Uuid;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 开放接入应用综合管理：面向 {@code opcmanage.html}（应用配置与状态）。绑定维护见 {@link ConnectBindManageService}。 */
@Service
public class OpcAdminService {

    @Autowired
    private ConnectAppService connectAppService;

    @Autowired
    private ConnectBindManageService connectBindManageService;

    @Autowired
    private SysUserService sysUserService;

    public Map<String, Object> overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("appCount", connectAppService.selectCount(new EntityWrapper<>()));
        data.put("activeAppCount", connectAppService.selectCount(new EntityWrapper<ConnectAppEntity>().eq("status", ConnectAppEntity.STATUS_ACTIVE)));
        data.put("bindCount", connectBindManageService.countAllBinds());
        return data;
    }

    public PageUtils pageApps(Map<String, Object> params) {
        EntityWrapper<ConnectAppEntity> wrapper = new EntityWrapper<>();
        AdminPageQueries.applyKeyword(wrapper, params, "name", "app_id", "user", "platform_base_url");
        wrapper.orderBy(WrapperColumns.columnInWrapper("create"), false);
        PageUtils page = connectAppService.queryPage(connectAppService.getPage(params), wrapper);
        enrichAppRows(page);
        return page;
    }

    public OpcAppAdminView getAppDetail(String appId) {
        ConnectAppEntity app = connectAppService.getByAppId(appId);
        if (app == null) {
            throw new IllegalArgumentException("接入应用不存在");
        }
        return toAppView(app);
    }

    @Transactional(rollbackFor = Exception.class)
    public ConnectAppEntity saveApp(String ownerUser, String appId, String appSecret, String platformBaseUrl, String redirectUri, String name, String scope, String icon, String hash, Integer pageLogin) {
        Uuid.requireValid(ownerUser);
        return connectAppService.saveConfig(ownerUser, appId, appSecret, platformBaseUrl, redirectUri, name, scope, icon, hash, pageLogin);
    }

    @Transactional(rollbackFor = Exception.class)
    public ConnectAppEntity applyApp(String ownerUser, String platformBaseUrl, String name, String redirectUri, String scope, String accessToken, Integer pageLogin) {
        Uuid.requireValid(ownerUser);
        ConnectAppEntity app = connectAppService.applyToPlatform(ownerUser, platformBaseUrl, name, redirectUri, scope, accessToken);
        return connectAppService.applyPageLogin(app, pageLogin);
    }

    @Transactional(rollbackFor = Exception.class)
    public ConnectAppEntity updateApp(String ownerUser, String appId, String appSecret, String platformBaseUrl, String redirectUri, String name, String scope, String icon, String hash, Integer pageLogin) {
        ConnectAppEntity existing = connectAppService.getByAppId(appId);
        if (existing == null) {
            throw new IllegalArgumentException("接入应用不存在");
        }
        String user = StringUtils.isBlank(ownerUser) ? existing.getUser() : ownerUser;
        if (StringUtils.isNotBlank(ownerUser)) {
            Uuid.requireValid(ownerUser);
        }
        return connectAppService.updateConfig(user, appId, appSecret, platformBaseUrl, redirectUri, name, scope, icon, hash, pageLogin);
    }

    @Transactional(rollbackFor = Exception.class)
    public ConnectAppEntity updateAppStatus(String appId, int status) {
        return connectAppService.updateStatus(appId, status);
    }

    public List<ConnectAppEntity> listAllAppsBrief() {
        EntityWrapper<ConnectAppEntity> wrapper = new EntityWrapper<>();
        wrapper.orderBy(WrapperColumns.columnInWrapper("create"), false);
        List<ConnectAppEntity> list = connectAppService.selectList(wrapper);
        if (list == null) {
            return new ArrayList<>();
        }
        for (ConnectAppEntity app : list) {
            connectAppService.stripSecret(app);
        }
        return list;
    }

    private void enrichAppRows(PageUtils page) {
        if (page == null || page.getList() == null) {
            return;
        }
        List<OpcAppAdminView> rows = new ArrayList<>();
        for (Object item : page.getList()) {
            if (item instanceof ConnectAppEntity) {
                rows.add(toAppView((ConnectAppEntity) item));
            }
        }
        page.setList(rows);
    }

    private OpcAppAdminView toAppView(ConnectAppEntity app) {
        OpcAppAdminView view = new OpcAppAdminView();
        view.setId(app.getId());
        view.setUuid(app.getUuid());
        view.setUser(app.getUser());
        view.setUsername(resolveUsername(app.getUser()));
        view.setAppId(app.getAppId());
        view.setName(app.getName());
        view.setPlatformBaseUrl(app.getPlatformBaseUrl());
        view.setRedirectUri(app.getRedirectUri());
        view.setAuthorizeUri(app.getAuthorizeUri());
        view.setTokenUri(app.getTokenUri());
        view.setUserInfoUri(app.getUserInfoUri());
        view.setScope(app.getScope());
        view.setStatus(app.getStatus());
        view.setCreate(app.getCreate());
        view.setUpdate(app.getUpdate());
        view.setBindCount(connectBindManageService.countBindsForApp(app.getUuid()));
        view.setAuthorizeUrl(connectAppService.buildAuthorizeEntryUrl(app.getAppId()));
        view.setSecretConfigured(connectAppService.hasConfiguredSecret(app));
        view.setIcon(app.getIcon());
        view.setHash(app.getHash());
        view.setPageLogin(app.getPageLogin());
        return view;
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
}

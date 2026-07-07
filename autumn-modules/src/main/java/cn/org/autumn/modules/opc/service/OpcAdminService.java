package cn.org.autumn.modules.opc.service;

import cn.org.autumn.modules.open.support.AdminPageQueries;
import cn.org.autumn.database.runtime.WrapperColumns;
import cn.org.autumn.modules.opc.dto.OpcAppAdminView;
import cn.org.autumn.modules.opc.dto.OpcBindAdminView;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.opc.entity.ConnectBindEntity;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Uuid;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 开放接入综合管理：面向 opcmanage 管理页。 */
@Service
public class OpcAdminService {

    @Autowired
    private ConnectAppService connectAppService;

    @Autowired
    private ConnectBindService connectBindService;

    @Autowired
    private SysUserService sysUserService;

    public Map<String, Object> overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("appCount", connectAppService.selectCount(new EntityWrapper<>()));
        data.put("activeAppCount", connectAppService.selectCount(new EntityWrapper<ConnectAppEntity>().eq("status", ConnectAppEntity.STATUS_ACTIVE)));
        data.put("bindCount", connectBindService.selectCount(new EntityWrapper<>()));
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
    public ConnectAppEntity saveApp(String ownerUser, String appId, String appSecret, String platformBaseUrl, String redirectUri, String name, String scope) {
        Uuid.requireValid(ownerUser);
        ConnectAppEntity app = connectAppService.saveConfig(ownerUser, appId, appSecret, platformBaseUrl, redirectUri, name, scope);
        return app;
    }

    @Transactional(rollbackFor = Exception.class)
    public ConnectAppEntity applyApp(String ownerUser, String platformBaseUrl, String name, String redirectUri, String scope, String accessToken) {
        Uuid.requireValid(ownerUser);
        return connectAppService.applyToPlatform(ownerUser, platformBaseUrl, name, redirectUri, scope, accessToken);
    }

    @Transactional(rollbackFor = Exception.class)
    public ConnectAppEntity updateApp(String ownerUser, String appId, String appSecret, String platformBaseUrl, String redirectUri, String name, String scope) {
        ConnectAppEntity existing = connectAppService.getByAppId(appId);
        if (existing == null) {
            throw new IllegalArgumentException("接入应用不存在");
        }
        String user = StringUtils.isBlank(ownerUser) ? existing.getUser() : ownerUser;
        if (StringUtils.isNotBlank(ownerUser)) {
            Uuid.requireValid(ownerUser);
        }
        return connectAppService.updateConfig(user, appId, appSecret, platformBaseUrl, redirectUri, name, scope);
    }

    @Transactional(rollbackFor = Exception.class)
    public ConnectAppEntity updateAppStatus(String appId, int status) {
        return connectAppService.updateStatus(appId, status);
    }

    public PageUtils pageBinds(Map<String, Object> params) {
        EntityWrapper<ConnectBindEntity> wrapper = new EntityWrapper<>();
        String appId = AdminPageQueries.stringParam(params, "appId");
        if (StringUtils.isNotBlank(appId)) {
            ConnectAppEntity app = connectAppService.getByAppId(appId.trim());
            if (app != null) {
                wrapper.eq("connect_app", app.getUuid());
            } else {
                wrapper.eq("connect_app", "__none__");
            }
        }
        String connectApp = AdminPageQueries.stringParam(params, "connectApp");
        if (StringUtils.isNotBlank(connectApp)) {
            wrapper.eq("connect_app", connectApp.trim());
        }
        AdminPageQueries.applyKeyword(wrapper, params, "open_id", "union_id", "user");
        wrapper.orderBy(WrapperColumns.columnInWrapper("create"), false);
        PageUtils page = connectBindService.queryPage(connectBindService.getPage(params), wrapper);
        enrichBindRows(page);
        return page;
    }

    @Transactional(rollbackFor = Exception.class)
    public ConnectBindEntity createBind(String connectAppUuid, String localUserUuid, String openId, String unionId) {
        if (StringUtils.isBlank(connectAppUuid) || StringUtils.isBlank(localUserUuid) || StringUtils.isBlank(openId)) {
            throw new IllegalArgumentException("connectApp、本地用户与openId不能为空");
        }
        Uuid.requireValid(localUserUuid);
        ConnectAppEntity app = findAppByUuid(connectAppUuid);
        if (app == null) {
            throw new IllegalArgumentException("接入应用不存在");
        }
        if (sysUserService.getByUuid(localUserUuid) == null) {
            throw new IllegalArgumentException("本地用户不存在");
        }
        ConnectBindEntity existingOpen = connectBindService.getByOpenId(connectAppUuid, openId.trim());
        if (existingOpen != null) {
            throw new IllegalArgumentException("openId已绑定");
        }
        ConnectBindEntity existingUser = connectBindService.getByConnectAppAndUser(connectAppUuid, localUserUuid);
        if (existingUser != null) {
            throw new IllegalArgumentException("本地用户已绑定");
        }
        ConnectBindEntity bind = new ConnectBindEntity();
        bind.setConnectApp(connectAppUuid.trim());
        bind.setUser(localUserUuid.trim());
        bind.setOpenId(openId.trim());
        bind.setUnionId(StringUtils.isBlank(unionId) ? null : unionId.trim());
        Date now = new Date();
        bind.setCreate(now);
        bind.setUpdate(now);
        connectBindService.insert(bind);
        return bind;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteBind(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id不能为空");
        }
        ConnectBindEntity bind = connectBindService.selectById(id);
        if (bind == null) {
            throw new IllegalArgumentException("绑定不存在");
        }
        connectBindService.deleteById(id);
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

    private ConnectAppEntity findAppByUuid(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return null;
        }
        ConnectAppEntity app = connectAppService.selectOne(new EntityWrapper<ConnectAppEntity>().eq("uuid", uuid));
        connectAppService.stripSecret(app);
        return app;
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
        view.setBindCount(connectBindService.selectCount(new EntityWrapper<ConnectBindEntity>().eq("connect_app", app.getUuid())));
        view.setAuthorizeUrl(connectAppService.buildAuthorizeEntryUrl(app.getAppId()));
        view.setSecretConfigured(true);
        return view;
    }

    private void enrichBindRows(PageUtils page) {
        if (page == null || page.getList() == null) {
            return;
        }
        Map<String, ConnectAppEntity> appCache = new HashMap<>();
        List<OpcBindAdminView> rows = new ArrayList<>();
        for (Object item : page.getList()) {
            if (!(item instanceof ConnectBindEntity)) {
                continue;
            }
            ConnectBindEntity bind = (ConnectBindEntity) item;
            OpcBindAdminView view = new OpcBindAdminView();
            view.setId(bind.getId());
            view.setUuid(bind.getUuid());
            view.setConnectApp(bind.getConnectApp());
            view.setUser(bind.getUser());
            view.setUsername(resolveUsername(bind.getUser()));
            view.setOpenId(bind.getOpenId());
            view.setUnionId(bind.getUnionId());
            view.setCreate(bind.getCreate());
            view.setUpdate(bind.getUpdate());
            ConnectAppEntity app = appCache.get(bind.getConnectApp());
            if (app == null && StringUtils.isNotBlank(bind.getConnectApp())) {
                app = findAppByUuid(bind.getConnectApp());
                if (app != null) {
                    appCache.put(bind.getConnectApp(), app);
                }
            }
            if (app != null) {
                view.setAppId(app.getAppId());
                view.setAppName(app.getName());
            }
            rows.add(view);
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
}

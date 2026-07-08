package cn.org.autumn.modules.opc.service;

import cn.org.autumn.database.runtime.WrapperColumns;
import cn.org.autumn.modules.support.AdminPageQueries;
import cn.org.autumn.modules.opc.dto.OpcAppBriefView;
import cn.org.autumn.modules.opc.dto.OpcBindAdminView;
import cn.org.autumn.modules.opc.dto.OpcBindManageView;
import cn.org.autumn.modules.opc.dto.OpcBindTechnicalView;
import cn.org.autumn.modules.opc.entity.ConnectAppEntity;
import cn.org.autumn.modules.opc.entity.ConnectBindEntity;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.modules.usr.entity.UserProfileEntity;
import cn.org.autumn.modules.usr.service.UserProfileService;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.Uuid;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 接入绑定查询与维护：供 {@code /modules/opc/connectbind} 友好管理页与 {@code opcmanage} 绑定 Tab。
 * <p>
 * 对用户 API 返回 {@link OpcBindManageView}（脱敏）；对 {@code opcmanage} 返回 {@link OpcBindAdminView}（运维字段）。
 */
@Service
public class ConnectBindManageService {

    @Autowired
    private ConnectAppService connectAppService;

    @Autowired
    private ConnectBindService connectBindService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private UserProfileService userProfileService;

    public int countAllBinds() {
        return connectBindService.selectCount(new EntityWrapper<>());
    }

    public int countBindsForApp(String connectAppUuid) {
        if (StringUtils.isBlank(connectAppUuid)) {
            return 0;
        }
        return connectBindService.selectCount(new EntityWrapper<ConnectBindEntity>().eq("connect_app", connectAppUuid));
    }

    /** 绑定管理页概览：管理员看全站，普通用户仅看自己的绑定数。 */
    public Map<String, Object> manageOverview(String viewerUserUuid, boolean admin) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("admin", admin);
        if (admin) {
            data.put("bindCount", countAllBinds());
            data.put("appCount", connectAppService.selectCount(new EntityWrapper<>()));
            data.put("activeAppCount", connectAppService.selectCount(
                    new EntityWrapper<ConnectAppEntity>().eq("status", ConnectAppEntity.STATUS_ACTIVE)));
            return data;
        }
        EntityWrapper<ConnectBindEntity> wrapper = new EntityWrapper<>();
        wrapper.eq("user", viewerUserUuid);
        data.put("bindCount", connectBindService.selectCount(wrapper));
        data.put("appCount", countDistinctAppsForUser(viewerUserUuid));
        return data;
    }

    public PageUtils pageBindsManageViews(String viewerUserUuid, boolean admin, Map<String, Object> params) {
        PageUtils page = pageBindsForViewer(viewerUserUuid, admin, params);
        if (page == null || page.getList() == null) {
            return page;
        }
        List<OpcBindManageView> views = new ArrayList<>();
        for (Object item : page.getList()) {
            if (item instanceof OpcBindAdminView) {
                views.add(toManageView((OpcBindAdminView) item, admin));
            }
        }
        page.setList(views);
        return page;
    }

    public List<OpcAppBriefView> listAppBriefsForManagePage(String viewerUserUuid, boolean admin) {
        List<ConnectAppEntity> apps = listAppsForBindFilter(viewerUserUuid, admin);
        List<OpcAppBriefView> views = new ArrayList<>();
        if (apps == null) {
            return views;
        }
        for (ConnectAppEntity app : apps) {
            OpcAppBriefView view = new OpcAppBriefView();
            view.setAppId(app.getAppId());
            view.setName(StringUtils.defaultIfBlank(app.getName(), app.getAppId()));
            if (admin) {
                view.setConnectApp(app.getUuid());
            }
            views.add(view);
        }
        return views;
    }

    /** {@code opcmanage} 绑定 Tab：返回含运维字段的 {@link OpcBindAdminView} 分页。 */
    public PageUtils pageBinds(Map<String, Object> params) {
        return pageBindsInternal(params);
    }

    public PageUtils pageBindsForViewer(String viewerUserUuid, boolean admin, Map<String, Object> params) {
        Map<String, Object> scoped = params == null ? new LinkedHashMap<>() : new LinkedHashMap<>(params);
        if (!admin) {
            scoped.put("user", viewerUserUuid);
        }
        return pageBindsInternal(scoped);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteBindForViewer(Long id, String viewerUserUuid, boolean admin) {
        if (id == null) {
            throw new IllegalArgumentException("id不能为空");
        }
        ConnectBindEntity bind = connectBindService.selectById(id);
        if (bind == null) {
            throw new IllegalArgumentException("绑定不存在");
        }
        if (!admin && !StringUtils.equalsIgnoreCase(bind.getUser(), viewerUserUuid)) {
            throw new IllegalArgumentException("无权限解除该绑定");
        }
        connectBindService.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void createBindForViewer(String viewerUserUuid, boolean admin, String connectAppUuid, String localUserUuid, String openId, String unionId) {
        if (!admin) {
            throw new IllegalArgumentException("仅系统管理员可手动添加绑定");
        }
        createBind(connectAppUuid, localUserUuid, openId, unionId);
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

    private PageUtils pageBindsInternal(Map<String, Object> params) {
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
        String userUuid = AdminPageQueries.stringParam(params, "user");
        if (StringUtils.isNotBlank(userUuid)) {
            wrapper.eq("user", userUuid.trim());
        }
        AdminPageQueries.applyKeyword(wrapper, params, "open_id", "union_id", "user");
        wrapper.orderBy(WrapperColumns.columnInWrapper("create"), false);
        PageUtils page = connectBindService.queryPage(connectBindService.getPage(params), wrapper);
        enrichBindRows(page);
        return page;
    }

    private List<ConnectAppEntity> listAppsForBindFilter(String viewerUserUuid, boolean admin) {
        if (admin) {
            return listAllAppsBrief();
        }
        EntityWrapper<ConnectBindEntity> wrapper = new EntityWrapper<>();
        wrapper.eq("user", viewerUserUuid);
        List<ConnectBindEntity> binds = connectBindService.selectList(wrapper);
        Set<String> appUuids = new HashSet<>();
        if (binds != null) {
            for (ConnectBindEntity bind : binds) {
                if (StringUtils.isNotBlank(bind.getConnectApp())) {
                    appUuids.add(bind.getConnectApp());
                }
            }
        }
        List<ConnectAppEntity> apps = new ArrayList<>();
        for (String appUuid : appUuids) {
            ConnectAppEntity app = findAppByUuid(appUuid);
            if (app != null) {
                apps.add(app);
            }
        }
        return apps;
    }

    private List<ConnectAppEntity> listAllAppsBrief() {
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
            rows.add(toAdminView((ConnectBindEntity) item, appCache));
        }
        page.setList(rows);
    }

    private OpcBindAdminView toAdminView(ConnectBindEntity bind, Map<String, ConnectAppEntity> appCache) {
        OpcBindAdminView view = new OpcBindAdminView();
        view.setId(bind.getId());
        view.setUuid(bind.getUuid());
        view.setConnectApp(bind.getConnectApp());
        view.setUser(bind.getUser());
        view.setUsername(resolveUsername(bind.getUser()));
        view.setNickname(resolveNickname(bind.getUser()));
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
            view.setAppIcon(app.getIcon());
        }
        return view;
    }

    private OpcBindManageView toManageView(OpcBindAdminView src, boolean admin) {
        OpcBindManageView view = new OpcBindManageView();
        view.setId(src.getId());
        view.setAppName(StringUtils.defaultIfBlank(src.getAppName(), "第三方应用"));
        view.setAppIcon(StringUtils.trimToEmpty(src.getAppIcon()));
        view.setAccountLabel(resolveAccountLabel(src));
        view.setBoundAt(src.getCreate());
        if (admin) {
            OpcBindTechnicalView technical = new OpcBindTechnicalView();
            technical.setAppId(src.getAppId());
            technical.setConnectApp(src.getConnectApp());
            technical.setUsername(src.getUsername());
            technical.setNickname(src.getNickname());
            technical.setUserUuid(src.getUser());
            technical.setOpenId(src.getOpenId());
            technical.setUnionId(src.getUnionId());
            technical.setUpdatedAt(src.getUpdate());
            view.setTechnical(technical);
        }
        return view;
    }

    private String resolveAccountLabel(OpcBindAdminView src) {
        if (StringUtils.isNotBlank(src.getNickname())) {
            return src.getNickname();
        }
        if (StringUtils.isNotBlank(src.getUsername())) {
            return src.getUsername();
        }
        return "当前账号";
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

    private String resolveNickname(String userUuid) {
        if (StringUtils.isBlank(userUuid)) {
            return "";
        }
        UserProfileEntity profile = userProfileService.getByUuid(userUuid);
        return profile == null ? "" : StringUtils.defaultString(profile.getNickname());
    }

    private int countDistinctAppsForUser(String userUuid) {
        EntityWrapper<ConnectBindEntity> wrapper = new EntityWrapper<>();
        wrapper.eq("user", userUuid);
        List<ConnectBindEntity> binds = connectBindService.selectList(wrapper);
        if (binds == null || binds.isEmpty()) {
            return 0;
        }
        Set<String> appUuids = new HashSet<>();
        for (ConnectBindEntity bind : binds) {
            if (StringUtils.isNotBlank(bind.getConnectApp())) {
                appUuids.add(bind.getConnectApp());
            }
        }
        return appUuids.size();
    }
}

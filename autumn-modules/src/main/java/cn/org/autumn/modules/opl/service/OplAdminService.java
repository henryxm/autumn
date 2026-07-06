package cn.org.autumn.modules.opl.service;

import cn.org.autumn.modules.open.support.AdminPageQueries;
import cn.org.autumn.database.runtime.WrapperColumns;
import cn.org.autumn.modules.opl.dto.OplAppAdminView;
import cn.org.autumn.modules.opl.dto.OplAppUserView;
import cn.org.autumn.modules.opl.entity.OpenAccountEntity;
import cn.org.autumn.modules.opl.entity.OpenAppEntity;
import cn.org.autumn.modules.opl.entity.OpenCodeEntity;
import cn.org.autumn.modules.opl.entity.OpenIdentityEntity;
import cn.org.autumn.modules.opl.entity.OpenTokenEntity;
import cn.org.autumn.modules.opl.entity.OpenUnionEntity;
import cn.org.autumn.opl.model.OpenAppRegisterOutcome;
import cn.org.autumn.opl.model.OpenAppType;
import cn.org.autumn.modules.sys.entity.SysUserEntity;
import cn.org.autumn.modules.sys.service.SysUserService;
import cn.org.autumn.utils.PageUtils;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 开放平台综合管理：统计、分页与行 enrichment（面向 oplmanage 管理页）。
 */
@Service
public class OplAdminService {

    @Autowired
    private OpenAccountService openAccountService;

    @Autowired
    private OpenAppService openAppService;

    @Autowired
    private OpenIdentityService openIdentityService;

    @Autowired
    private OpenUnionService openUnionService;

    @Autowired
    private OpenCodeService openCodeService;

    @Autowired
    private OpenTokenService openTokenService;

    @Autowired
    private SysUserService sysUserService;

    public Map<String, Object> overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("accountCount", openAccountService.selectCount(new EntityWrapper<>()));
        data.put("appCount", openAppService.selectCount(new EntityWrapper<>()));
        data.put("identityCount", openIdentityService.selectCount(new EntityWrapper<>()));
        data.put("unionCount", openUnionService.selectCount(new EntityWrapper<>()));
        data.put("codeCount", openCodeService.selectCount(new EntityWrapper<>()));
        data.put("tokenCount", openTokenService.selectCount(new EntityWrapper<>()));
        data.put("activeAppCount", openAppService.selectCount(new EntityWrapper<OpenAppEntity>().eq("status", OpenAppEntity.STATUS_ACTIVE)));
        return data;
    }

    public List<Map<String, String>> listAppTypes() {
        List<Map<String, String>> types = new ArrayList<>();
        for (OpenAppType type : OpenAppType.values()) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("value", type.name());
            item.put("label", type.getLabel());
            types.add(item);
        }
        return types;
    }

    public PageUtils pageAccounts(Map<String, Object> params) {
        EntityWrapper<OpenAccountEntity> wrapper = new EntityWrapper<>();
        AdminPageQueries.applyKeyword(wrapper, params, "name", "uuid", "user");
        wrapper.orderBy(WrapperColumns.columnInWrapper("create"), false);
        PageUtils page = openAccountService.queryPage(openAccountService.getPage(params), wrapper);
        enrichAccountRows(page);
        return page;
    }

    public PageUtils pageApps(Map<String, Object> params) {
        EntityWrapper<OpenAppEntity> wrapper = new EntityWrapper<>();
        String account = AdminPageQueries.stringParam(params, "account");
        if (StringUtils.isNotBlank(account)) {
            wrapper.eq("account", account.trim());
        }
        AdminPageQueries.applyKeyword(wrapper, params, "name", "app_id", "uuid");
        wrapper.orderBy(WrapperColumns.columnInWrapper("create"), false);
        PageUtils page = openAppService.queryPage(openAppService.getPage(params), wrapper);
        enrichAppRows(page);
        return page;
    }

    public OplAppAdminView getAppDetail(String appId) {
        OpenAppEntity app = requireApp(appId);
        return toAppView(app);
    }

    @Transactional(rollbackFor = Exception.class)
    public OpenAppRegisterOutcome createApp(String accountUuid, String name, String redirectUri, String scope, OpenAppType appType) {
        openAccountService.requireActiveAccount(accountUuid);
        return openAppService.register(accountUuid, name, redirectUri, scope, appType);
    }

    @Transactional(rollbackFor = Exception.class)
    public OpenAppRegisterOutcome resetAppSecret(String appId) {
        OpenAppEntity app = requireApp(appId);
        return openAppService.resetSecret(app.getAccount(), appId);
    }

    @Transactional(rollbackFor = Exception.class)
    public OpenAppEntity updateAppStatus(String appId, int status) {
        return openAppService.updateStatus(appId, status);
    }

    @Transactional(rollbackFor = Exception.class)
    public OpenAppEntity updateAppInfo(String appId, String name, String redirectUri, String scope, OpenAppType appType) {
        OpenAppEntity app = requireApp(appId);
        return openAppService.updateApp(app.getAccount(), appId, name, redirectUri, scope, appType);
    }

    @Transactional(rollbackFor = Exception.class)
    public OpenAccountEntity createAccount(String userUuid, String name) {
        if (StringUtils.isBlank(userUuid)) {
            throw new IllegalArgumentException("用户 uuid 不能为空");
        }
        return openAccountService.getOrCreateByUser(userUuid.trim(), name);
    }

    public PageUtils pageAppUsers(Map<String, Object> params) {
        String appId = AdminPageQueries.stringParam(params, "appId");
        if (StringUtils.isBlank(appId)) {
            throw new IllegalArgumentException("appId不能为空");
        }
        OpenAppEntity app = requireApp(appId);
        EntityWrapper<OpenIdentityEntity> wrapper = new EntityWrapper<>();
        wrapper.eq("app_id", appId.trim());
        AdminPageQueries.applyKeyword(wrapper, params, "open_id", "user");
        wrapper.orderBy(WrapperColumns.columnInWrapper("create"), false);
        PageUtils page = openIdentityService.queryPage(openIdentityService.getPage(params), wrapper);
        enrichIdentityRows(page, app.getAccount());
        return page;
    }

    public PageUtils pageUnions(Map<String, Object> params) {
        EntityWrapper<OpenUnionEntity> wrapper = new EntityWrapper<>();
        String account = AdminPageQueries.stringParam(params, "account");
        if (StringUtils.isNotBlank(account)) {
            wrapper.eq("account", account.trim());
        }
        AdminPageQueries.applyKeyword(wrapper, params, "union_id", "user");
        wrapper.orderBy(WrapperColumns.columnInWrapper("create"), false);
        PageUtils page = openUnionService.queryPage(openUnionService.getPage(params), wrapper);
        enrichUnionRows(page);
        return page;
    }

    public PageUtils pageCodes(Map<String, Object> params) {
        EntityWrapper<OpenCodeEntity> wrapper = new EntityWrapper<>();
        String appId = AdminPageQueries.stringParam(params, "appId");
        if (StringUtils.isNotBlank(appId)) {
            wrapper.eq("app_id", appId.trim());
        }
        AdminPageQueries.applyKeyword(wrapper, params, "code", "user");
        wrapper.orderBy(WrapperColumns.columnInWrapper("create"), false);
        PageUtils page = openCodeService.queryPage(openCodeService.getPage(params), wrapper);
        enrichSimpleUserRows(page);
        return page;
    }

    public PageUtils pageTokens(Map<String, Object> params) {
        EntityWrapper<OpenTokenEntity> wrapper = new EntityWrapper<>();
        String appId = AdminPageQueries.stringParam(params, "appId");
        if (StringUtils.isNotBlank(appId)) {
            wrapper.eq("app_id", appId.trim());
        }
        AdminPageQueries.applyKeyword(wrapper, params, "access_token", "refresh_token", "open_id", "union_id", "user");
        wrapper.orderBy("update_time", false);
        PageUtils page = openTokenService.queryPage(openTokenService.getPage(params), wrapper);
        enrichSimpleUserRows(page);
        return page;
    }

    public List<OpenAccountEntity> listAllAccounts() {
        EntityWrapper<OpenAccountEntity> wrapper = new EntityWrapper<>();
        wrapper.orderBy(WrapperColumns.columnInWrapper("create"), false);
        List<OpenAccountEntity> list = openAccountService.selectList(wrapper);
        return list == null ? new ArrayList<>() : list;
    }

    private OpenAppEntity requireApp(String appId) {
        OpenAppEntity app = openAppService.getByAppId(appId);
        if (app == null) {
            throw new IllegalArgumentException("应用不存在");
        }
        return app;
    }

    private void enrichAccountRows(PageUtils page) {
        if (page == null || page.getList() == null) {
            return;
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object item : page.getList()) {
            if (!(item instanceof OpenAccountEntity)) {
                continue;
            }
            OpenAccountEntity account = (OpenAccountEntity) item;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", account.getId());
            row.put("uuid", account.getUuid());
            row.put("user", account.getUser());
            row.put("username", resolveUsername(account.getUser()));
            row.put("name", account.getName());
            row.put("status", account.getStatus());
            row.put("create", account.getCreate());
            row.put("update", account.getUpdate());
            row.put("appCount", openAppService.selectCount(new EntityWrapper<OpenAppEntity>().eq("account", account.getUuid())));
            row.put("unionCount", openUnionService.selectCount(new EntityWrapper<OpenUnionEntity>().eq("account", account.getUuid())));
            rows.add(row);
        }
        page.setList(rows);
    }

    private void enrichAppRows(PageUtils page) {
        if (page == null || page.getList() == null) {
            return;
        }
        List<OplAppAdminView> rows = new ArrayList<>();
        for (Object item : page.getList()) {
            if (item instanceof OpenAppEntity) {
                rows.add(toAppView((OpenAppEntity) item));
            }
        }
        page.setList(rows);
    }

    private OplAppAdminView toAppView(OpenAppEntity app) {
        OplAppAdminView view = new OplAppAdminView();
        view.setId(app.getId());
        view.setUuid(app.getUuid());
        view.setAccount(app.getAccount());
        view.setAppId(app.getAppId());
        view.setName(app.getName());
        view.setAppType(app.getAppType());
        view.setRedirectUri(app.getRedirectUri());
        view.setScope(app.getScope());
        view.setStatus(app.getStatus());
        view.setCreate(app.getCreate());
        view.setUpdate(app.getUpdate());
        OpenAccountEntity account = openAccountService.getByUuid(app.getAccount());
        view.setAccountName(account == null ? app.getAccount() : account.getName());
        view.setUserCount(openIdentityService.selectCount(new EntityWrapper<OpenIdentityEntity>().eq("app_id", app.getAppId())));
        view.setCodeCount(openCodeService.selectCount(new EntityWrapper<OpenCodeEntity>().eq("app_id", app.getAppId())));
        view.setTokenCount(openTokenService.selectCount(new EntityWrapper<OpenTokenEntity>().eq("app_id", app.getAppId())));
        return view;
    }

    private void enrichIdentityRows(PageUtils page, String accountUuid) {
        if (page == null || page.getList() == null) {
            return;
        }
        List<OplAppUserView> rows = new ArrayList<>();
        for (Object item : page.getList()) {
            if (!(item instanceof OpenIdentityEntity)) {
                continue;
            }
            OpenIdentityEntity identity = (OpenIdentityEntity) item;
            OplAppUserView row = new OplAppUserView();
            row.setId(identity.getId());
            row.setUuid(identity.getUuid());
            row.setAppId(identity.getAppId());
            row.setUser(identity.getUser());
            row.setUsername(resolveUsername(identity.getUser()));
            row.setOpenId(identity.getOpenId());
            row.setCreate(identity.getCreate());
            OpenUnionEntity union = openUnionService.getByAccountAndUser(accountUuid, identity.getUser());
            if (union != null) {
                row.setUnionId(union.getUnionId());
            }
            rows.add(row);
        }
        page.setList(rows);
    }

    private void enrichUnionRows(PageUtils page) {
        if (page == null || page.getList() == null) {
            return;
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object item : page.getList()) {
            if (!(item instanceof OpenUnionEntity)) {
                continue;
            }
            OpenUnionEntity union = (OpenUnionEntity) item;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", union.getId());
            row.put("uuid", union.getUuid());
            row.put("account", union.getAccount());
            row.put("user", union.getUser());
            row.put("username", resolveUsername(union.getUser()));
            row.put("unionId", union.getUnionId());
            row.put("create", union.getCreate());
            row.put("update", union.getUpdate());
            rows.add(row);
        }
        page.setList(rows);
    }

    private void enrichSimpleUserRows(PageUtils page) {
        if (page == null || page.getList() == null) {
            return;
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object item : page.getList()) {
            Map<String, Object> row = new LinkedHashMap<>();
            if (item instanceof OpenCodeEntity) {
                OpenCodeEntity entity = (OpenCodeEntity) item;
                row.put("id", entity.getId());
                row.put("code", entity.getCode());
                row.put("appId", entity.getAppId());
                row.put("user", entity.getUser());
                row.put("username", resolveUsername(entity.getUser()));
                row.put("redirectUri", entity.getRedirectUri());
                row.put("expire", entity.getExpire());
                row.put("create", entity.getCreate());
            } else if (item instanceof OpenTokenEntity) {
                OpenTokenEntity entity = (OpenTokenEntity) item;
                row.put("id", entity.getId());
                row.put("appId", entity.getAppId());
                row.put("user", entity.getUser());
                row.put("username", resolveUsername(entity.getUser()));
                row.put("openId", entity.getOpenId());
                row.put("unionId", entity.getUnionId());
                row.put("accessToken", maskToken(entity.getAccessToken()));
                row.put("refreshToken", maskToken(entity.getRefreshToken()));
                row.put("accessExpireIn", entity.getAccessExpireIn());
                row.put("refreshExpireIn", entity.getRefreshExpireIn());
                row.put("updateTime", entity.getUpdateTime());
            } else {
                continue;
            }
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
}

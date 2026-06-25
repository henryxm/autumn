package cn.org.autumn.modules.sys.service;

import static cn.org.autumn.modules.sys.service.SysConfigService.SYSTEM_UPGRADE;

import cn.org.autumn.database.CrudGuard;
import cn.org.autumn.modules.sys.entity.SystemUpgrade;
import cn.org.autumn.site.InitFactory;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 全局 CRUD 开关运行时协调：从 {@link SystemUpgrade} 配置加载并支持管理端热更新。
 */
@Slf4j
@Order(-2000)
@Service
public class CrudGuardService implements InitFactory.Before, InitFactory.Init {

    @Autowired
    private CrudGuard crudGuard;

    @Autowired
    private SysConfigService sysConfigService;

    @Override
    public void before() {
        reloadFromConfig();
    }

    @Override
    public void init() {
        reloadFromConfig();
    }

    public void reloadFromConfig() {
        SystemUpgrade upgrade = sysConfigService.getSystemUpgrade();
        if (upgrade == null) {
            upgrade = new SystemUpgrade();
        }
        crudGuard.apply(upgrade.isDatabaseWrite(), upgrade.isLocalWrite(), upgrade.getDescription());
    }

    public SystemUpgrade snapshot() {
        SystemUpgrade upgrade = new SystemUpgrade();
        upgrade.setDatabaseWrite(crudGuard.global());
        upgrade.setLocalWrite(crudGuard.local());
        upgrade.setDescription(crudGuard.hint());
        return upgrade;
    }

    @Transactional(rollbackFor = Exception.class)
    public SystemUpgrade update(boolean databaseWrite, boolean localWrite, String description) {
        SystemUpgrade upgrade = sysConfigService.getSystemUpgrade();
        if (upgrade == null) {
            upgrade = new SystemUpgrade();
        }
        upgrade.setDatabaseWrite(databaseWrite);
        upgrade.setLocalWrite(localWrite);
        if (description != null) {
            upgrade.setDescription(description);
        }
        String json = new Gson().toJson(upgrade);
        CrudGuard.force(() -> sysConfigService.updateValueByKey(SYSTEM_UPGRADE, json));
        crudGuard.apply(upgrade.isDatabaseWrite(), upgrade.isLocalWrite(), upgrade.getDescription());
        if (log.isDebugEnabled()) {
            log.debug("Database CRUD switches updated: databaseWrite={}, localWrite={}", databaseWrite, localWrite);
        }
        return snapshot();
    }

    public SystemUpgrade resetToConfig() {
        reloadFromConfig();
        return snapshot();
    }

    public boolean isWritableForUser() {
        return crudGuard.global() && crudGuard.local();
    }

    public String statusLabel() {
        if (!crudGuard.global()) {
            return "系统只读";
        }
        if (!crudGuard.local()) {
            return "用户只读";
        }
        return "读写正常";
    }

    public String hintMessage() {
        String description = crudGuard.hint();
        return StringUtils.isNotBlank(description) ? description.trim() : "";
    }
}

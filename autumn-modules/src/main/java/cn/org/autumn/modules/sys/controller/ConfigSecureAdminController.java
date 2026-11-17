package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.crypto.FieldEncryptContext;
import cn.org.autumn.crypto.FieldEncryptMigrationService;
import cn.org.autumn.crypto.FieldEncryptService;
import cn.org.autumn.exception.AException;
import cn.org.autumn.modules.sys.entity.SysConfigEntity;
import cn.org.autumn.modules.sys.service.SysConfigService;
import cn.org.autumn.modules.sys.support.AdminAccessGuard;
import cn.org.autumn.utils.PageUtils;
import cn.org.autumn.utils.R;
import cn.org.autumn.validator.ValidatorUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * SysConfig paramValue 字段加密运维页 API：明文编辑、密文落库、存量迁移（系统管理员 + IP 白名单）。
 */
@Slf4j
@RestController
@RequestMapping("sys/configsecure")
@SkipInterceptor
public class ConfigSecureAdminController {

    private static final String ENTITY = SysConfigEntity.class.getName();
    private static final int MAX_BATCH_SIZE = 2000;

    @Autowired
    @Lazy
    private SysConfigService sysConfigService;

    @Autowired
    private FieldEncryptService fieldEncryptService;

    @Autowired
    private FieldEncryptMigrationService migrationService;

    @Autowired
    private AdminAccessGuard adminAccessGuard;

    @GetMapping("/status")
    public R status(HttpServletRequest request) {
        return admin(request, "status", () -> R.ok().put("data", fieldEncryptService.statusSnapshot())
                .put("entity", ENTITY)
                .put("field", "paramValue"));
    }

    @GetMapping("/list")
    public R list(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        return admin(request, "list", () -> {
            PageUtils page = sysConfigService.queryPage(params, 1);
            @SuppressWarnings("unchecked")
            List<Object> rawList = page.getList() == null ? List.of() : (List<Object>) page.getList();
            List<Map<String, Object>> rows = new ArrayList<>(rawList.size());
            for (Object item : rawList) {
                if (!(item instanceof SysConfigEntity entity)) {
                    continue;
                }
                rows.add(toListRow(entity));
            }
            page.setList(rows);
            return R.ok().put("page", page);
        });
    }

    @GetMapping("/info/{id}")
    public R info(@PathVariable("id") Long id, HttpServletRequest request) {
        return admin(request, "info", () -> {
            SysConfigEntity config = sysConfigService.getById(id);
            if (config == null) {
                return R.error("配置不存在");
            }
            plainParamValueForDisplay(config);
            boolean atRestEncrypted = isAtRestEncrypted(id);
            return R.ok().put("config", config).put("atRestEncrypted", atRestEncrypted);
        });
    }

    @PostMapping("/save")
    public R save(@RequestBody SysConfigEntity config, HttpServletRequest request) {
        return admin(request, "save", () -> {
            persistAutoEncrypt(config, true);
            adminAccessGuard.auditWithIp(request, "config_secure_save",
                    "key=" + StringUtils.defaultString(config.getParamKey()));
            return R.ok().put("atRestEncrypted", fieldEncryptService.isKeyConfigured());
        });
    }

    @PostMapping("/update")
    public R update(@RequestBody SysConfigEntity config, HttpServletRequest request) {
        return admin(request, "update", () -> {
            if (config == null || config.getId() == null) {
                return R.error("id 不能为空");
            }
            persistAutoEncrypt(config, false);
            adminAccessGuard.auditWithIp(request, "config_secure_update",
                    "id=" + config.getId() + " key=" + StringUtils.defaultString(config.getParamKey()));
            return R.ok().put("atRestEncrypted", isAtRestEncrypted(config.getId()));
        });
    }

    /**
     * 单条加密（明文 → ENC$ 落库）；不依赖全局写开关，仅需密钥就绪。
     */
    @PostMapping("/migrateOne")
    public R migrateOne(@RequestParam("id") String id,
                        @RequestParam(value = "dryRun", defaultValue = "false") boolean dryRun,
                        @RequestParam(value = "confirm", defaultValue = "false") boolean confirm,
                        HttpServletRequest request) {
        if (!dryRun && !confirm) {
            return admin(request, "migrateOne", () -> R.error("非预检写入须传 confirm=true"));
        }
        return admin(request, "migrateOne", () -> runMigrateOne(request, id, dryRun, true));
    }

    /**
     * 单条还原（ENC$ → 明文落库）。
     */
    @PostMapping("/restoreOne")
    public R restoreOne(@RequestParam("id") String id,
                        @RequestParam(value = "dryRun", defaultValue = "false") boolean dryRun,
                        @RequestParam(value = "confirm", defaultValue = "false") boolean confirm,
                        HttpServletRequest request) {
        if (!dryRun && !confirm) {
            return admin(request, "restoreOne", () -> R.error("非预检写入须传 confirm=true"));
        }
        return admin(request, "restoreOne", () -> runMigrateOne(request, id, dryRun, false));
    }

    @PostMapping("/migrate")
    public R migrate(@RequestParam(value = "dryRun", defaultValue = "true") boolean dryRun,
                     @RequestParam(value = "batchSize", defaultValue = "200") int batchSize,
                     @RequestParam(value = "confirm", defaultValue = "false") boolean confirm,
                     HttpServletRequest request) {
        if (!dryRun && !confirm) {
            return admin(request, "migrate", () -> R.error("非预检写入须传 confirm=true"));
        }
        return admin(request, "migrate", () -> runMigrate(request, dryRun, batchSize, true));
    }

    @PostMapping("/restore")
    public R restore(@RequestParam(value = "dryRun", defaultValue = "true") boolean dryRun,
                     @RequestParam(value = "batchSize", defaultValue = "200") int batchSize,
                     @RequestParam(value = "confirm", defaultValue = "false") boolean confirm,
                     HttpServletRequest request) {
        if (!dryRun && !confirm) {
            return admin(request, "restore", () -> R.error("非预检写入须传 confirm=true"));
        }
        return admin(request, "restore", () -> runMigrate(request, dryRun, batchSize, false));
    }

    /**
     * 清理本机 map / JSON 解析缓存，并从数据库重新加载。
     */
    @PostMapping("/clearLocalCache")
    public R clearLocalCache(HttpServletRequest request) {
        return admin(request, "clearLocalCache", () -> {
            int loaded = sysConfigService.reloadLocalFromDatabase();
            adminAccessGuard.auditWithIp(request, "config_secure_clear_local_cache", "loaded=" + loaded);
            return R.ok().put("msg", "本机缓存已清理并从数据库重新加载").put("loaded", loaded);
        });
    }

    /**
     * 清理共享 Redis 中的 SysConfig 值缓存（下次读回源 DB）。
     */
    @PostMapping("/clearRedis")
    public R clearRedis(HttpServletRequest request) {
        return admin(request, "clearRedis", () -> {
            sysConfigService.clearRedisConfigCache();
            adminAccessGuard.auditWithIp(request, "config_secure_clear_redis", "");
            return R.ok().put("msg", "Redis 配置缓存已清理");
        });
    }

    /**
     * 清 Redis + 本机从 DB 回源 + 通知集群各节点丢弃脏缓存。
     */
    @PostMapping("/reload")
    public R reload(HttpServletRequest request) {
        return admin(request, "reload", () -> {
            sysConfigService.reloadAfterFieldEncryptMigration();
            int loaded = sysConfigService.getLocalCacheSize();
            adminAccessGuard.auditWithIp(request, "config_secure_reload", "loaded=" + loaded);
            return R.ok().put("msg", "已清理 Redis 与本机缓存并从数据库重新加载，已通知集群").put("loaded", loaded);
        });
    }

    private R runMigrate(HttpServletRequest request, boolean dryRun, int batchSize, boolean encrypt) {
        int size = Math.min(Math.max(batchSize, 1), MAX_BATCH_SIZE);
        try {
            FieldEncryptMigrationService.MigrationResult result = encrypt
                    ? migrationService.migrate(ENTITY, dryRun, size)
                    : migrationService.restore(ENTITY, dryRun, size);
            if (!dryRun) {
                refreshConfigCaches();
            }
            String action = encrypt ? "migrate" : "restore";
            adminAccessGuard.auditWithIp(request, "config_secure_" + action,
                    "dryRun=" + dryRun + " batchSize=" + size + " processed=" + result.getProcessed());
            return toMigrationR(result);
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    private R runMigrateOne(HttpServletRequest request, String id, boolean dryRun, boolean encrypt) {
        try {
            FieldEncryptMigrationService.MigrationResult result = encrypt
                    ? migrationService.migrateOne(ENTITY, id, dryRun)
                    : migrationService.restoreOne(ENTITY, id, dryRun);
            if (!dryRun && result.getProcessed() > 0) {
                refreshConfigCaches();
            }
            String action = encrypt ? "migrateOne" : "restoreOne";
            adminAccessGuard.auditWithIp(request, "config_secure_" + action,
                    "id=" + id + " dryRun=" + dryRun + " processed=" + result.getProcessed());
            return toMigrationR(result);
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    private static R toMigrationR(FieldEncryptMigrationService.MigrationResult result) {
        R r = R.ok().put("entity", result.getEntity()).put("dryRun", result.isDryRun())
                .put("action", result.getAction()).put("scanned", result.getScanned())
                .put("pending", result.getPending()).put("processed", result.getProcessed())
                .put("migrated", result.getMigrated());
        if (StringUtils.isNotBlank(result.getMessage())) {
            r.put("msg", result.getMessage());
        }
        return r;
    }

    /**
     * 运维页保存：有密钥时强制 ENC$ 落库（不依赖全局写开关），API 侧实体保持明文态。
     */
    private void persistAutoEncrypt(SysConfigEntity config, boolean create) {
        ValidatorUtils.validateEntity(config);
        if (!fieldEncryptService.isKeyConfigured()) {
            if (create) {
                sysConfigService.save(config);
            } else {
                sysConfigService.update(config);
            }
            return;
        }
        FieldEncryptContext.runSkip(() -> {
            fieldEncryptService.applyEncryptBeforePersist(config);
            if (create) {
                sysConfigService.save(config);
            } else {
                sysConfigService.update(config);
            }
        });
    }

    private Map<String, Object> toListRow(SysConfigEntity entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", entity.getId());
        row.put("paramKey", entity.getParamKey());
        String plain = plainParamValueForDisplay(entity);
        row.put("paramValue", plain);
        row.put("remark", entity.getRemark());
        row.put("status", entity.getStatus());
        row.put("type", entity.getType());
        row.put("category", entity.getCategory());
        boolean atRestEncrypted = isAtRestEncrypted(entity.getId());
        row.put("atRestEncrypted", atRestEncrypted);
        // canEncrypt：落库明文且有可写内容（用展示明文判断即可）
        row.put("canEncrypt", !atRestEncrypted && fieldEncryptService.isKeyConfigured()
                && StringUtils.isNotBlank(plain));
        row.put("canDecrypt", atRestEncrypted && fieldEncryptService.isReadDecryptEnabled());
        return row;
    }

    /** 列表/编辑展示用明文；若实体仍带 ENC$ 则再解密一次。 */
    private String plainParamValueForDisplay(SysConfigEntity entity) {
        if (entity == null) {
            return null;
        }
        String value = entity.getParamValue();
        if (fieldEncryptService.needsRestore(value)) {
            try {
                value = fieldEncryptService.decryptValue(value);
                entity.setParamValue(value);
            } catch (Exception e) {
                log.warn("configsecure 列表解密失败 id={} key={} msg={}",
                        entity.getId(), entity.getParamKey(), e.getMessage());
            }
        }
        return value;
    }

    private boolean isAtRestEncrypted(Long id) {
        return id != null && fieldEncryptService.isKeyConfigured()
                && sysConfigService.isParamValueEncryptedAtRest(id);
    }

    private void refreshConfigCaches() {
        try {
            sysConfigService.reloadAfterFieldEncryptMigration();
        } catch (Exception e) {
            log.warn("configsecure 迁移后刷新配置缓存失败 msg={}", e.getMessage());
        }
    }

    private R admin(HttpServletRequest request, String method, Supplier<R> action) {
        try {
            adminAccessGuard.requireOpsAdmin(request, getClass(), method);
            return action.get();
        } catch (AException e) {
            return R.error(e.getCode() > 0 ? e.getCode() : 403, e.getMessage());
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }
}

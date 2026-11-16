package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.config.Config;
import cn.org.autumn.crypto.FieldEncryptMigrationService;
import cn.org.autumn.crypto.FieldEncryptService;
import cn.org.autumn.exception.AException;
import cn.org.autumn.modules.sys.service.FieldEncryptRuntimeService;
import cn.org.autumn.modules.sys.support.AdminAccessGuard;
import cn.org.autumn.utils.R;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 字段存储加密运维 API（系统管理员 + IP 白名单）。
 */
@RestController
@RequestMapping("sys/crypto/field")
@SkipInterceptor
public class FieldEncryptAdminController {

    private static final int MAX_BATCH_SIZE = 2000;

    @Autowired
    private FieldEncryptMigrationService migrationService;

    @Autowired
    private FieldEncryptService fieldEncryptService;

    @Autowired
    private FieldEncryptRuntimeService runtimeService;

    @Autowired
    private AdminAccessGuard adminAccessGuard;

    @GetMapping("/status")
    public R status(@RequestParam(value = "reload", defaultValue = "false") boolean reload,
                    HttpServletRequest request) {
        return admin(request, "status", () -> {
            if (reload) {
                runtimeService.reloadFromRedis();
            }
            return R.ok().put("data", fieldEncryptService.statusSnapshot());
        });
    }

    @GetMapping("/list")
    public R list(HttpServletRequest request) {
        return admin(request, "list", () -> R.ok().put("list", migrationService.listEncryptEntities()));
    }

    @PostMapping("/writeEnabled")
    public R setWriteEnabled(@RequestParam("enabled") boolean enabled, HttpServletRequest request) {
        return admin(request, "writeEnabled", () -> {
            try {
                runtimeService.setWriteEncryptEnabled(enabled);
                adminAccessGuard.auditWithIp(request, "field_encrypt_write_enabled", "enabled=" + enabled);
                return R.ok().put("data", fieldEncryptService.statusSnapshot());
            } catch (Exception e) {
                return R.error(e.getMessage());
            }
        });
    }

    @PostMapping("/resetWriteEnabled")
    public R resetWriteEnabled(HttpServletRequest request) {
        return admin(request, "resetWriteEnabled", () -> {
            runtimeService.resetWriteEncryptOverride();
            adminAccessGuard.auditWithIp(request, "field_encrypt_reset_write_enabled", "");
            return R.ok().put("data", fieldEncryptService.statusSnapshot());
        });
    }

    @PostMapping("/reloadCluster")
    public R reloadCluster(HttpServletRequest request) {
        return admin(request, "reloadCluster", () -> {
            if (!runtimeService.isClusterMode()) {
                return R.error("当前非 Redis 集群模式，无需集群刷新");
            }
            runtimeService.reloadFromRedis();
            adminAccessGuard.auditWithIp(request, "field_encrypt_reload_cluster", "");
            return R.ok().put("data", fieldEncryptService.statusSnapshot());
        });
    }

    @PostMapping("/generateKey")
    public R generateKey(HttpServletRequest request) {
        return admin(request, "generateKey", () -> {
            if (Config.isProd()) {
                return R.error(403, "生产环境禁止在线生成密钥");
            }
            adminAccessGuard.auditWithIp(request, "field_encrypt_generate_key", "dev");
            return R.ok().put("data", fieldEncryptService.generateDevKeyMaterial());
        });
    }

    @PostMapping("/generateVector")
    public R generateVector(HttpServletRequest request) {
        return admin(request, "generateVector", () -> {
            if (Config.isProd()) {
                return R.error(403, "生产环境禁止在线生成向量");
            }
            adminAccessGuard.auditWithIp(request, "field_encrypt_generate_vector", "dev");
            return R.ok().put("data", fieldEncryptService.generateDevVector());
        });
    }

    @PostMapping("/test")
    public R test(@RequestParam(value = "plain", defaultValue = "") String plain,
                  @RequestParam(value = "vector", required = false) String vector,
                  HttpServletRequest request) {
        return admin(request, "test", () -> {
            if (!fieldEncryptService.isKeyConfigured()) {
                return R.error("未配置有效的 autumn.crypto.field.key，无法执行加解密测试");
            }
            Map<String, Object> data = fieldEncryptService.testRoundTrip(plain, vector);
            if (data.containsKey("error")) {
                return R.error(String.valueOf(data.get("error")));
            }
            adminAccessGuard.auditWithIp(request, "field_encrypt_test", "plainLen=" + StringUtils.length(plain));
            return R.ok().put("data", data);
        });
    }

    @PostMapping("/decrypt")
    public R decrypt(@RequestParam(value = "cipher", defaultValue = "") String cipher, HttpServletRequest request) {
        return admin(request, "decrypt", () -> {
            Map<String, Object> data = fieldEncryptService.testDecrypt(cipher);
            if (data.containsKey("error")) {
                return R.error(String.valueOf(data.get("error")));
            }
            adminAccessGuard.auditWithIp(request, "field_encrypt_decrypt", "cipherLen=" + StringUtils.length(cipher));
            return R.ok().put("data", data);
        });
    }

    @PostMapping("/migrate")
    public R migrate(@RequestParam("entity") String entity,
                     @RequestParam(value = "dryRun", defaultValue = "true") boolean dryRun,
                     @RequestParam(value = "batchSize", defaultValue = "200") int batchSize,
                     HttpServletRequest request) {
        return runMigration(request, entity, dryRun, batchSize, true);
    }

    @PostMapping("/restore")
    public R restore(@RequestParam("entity") String entity,
                     @RequestParam(value = "dryRun", defaultValue = "true") boolean dryRun,
                     @RequestParam(value = "batchSize", defaultValue = "200") int batchSize,
                     HttpServletRequest request) {
        return runMigration(request, entity, dryRun, batchSize, false);
    }

    @PostMapping("/migrateOne")
    public R migrateOne(@RequestParam("entity") String entity,
                        @RequestParam("id") String id,
                        @RequestParam(value = "dryRun", defaultValue = "true") boolean dryRun,
                        @RequestParam(value = "confirm", defaultValue = "false") boolean confirm,
                        HttpServletRequest request) {
        if (!dryRun && !confirm) {
            return admin(request, "migrateOne", () -> R.error("非预检写入须传 confirm=true"));
        }
        return runMigrationOne(request, entity, id, dryRun, true);
    }

    @PostMapping("/restoreOne")
    public R restoreOne(@RequestParam("entity") String entity,
                        @RequestParam("id") String id,
                        @RequestParam(value = "dryRun", defaultValue = "true") boolean dryRun,
                        @RequestParam(value = "confirm", defaultValue = "false") boolean confirm,
                        HttpServletRequest request) {
        if (!dryRun && !confirm) {
            return admin(request, "restoreOne", () -> R.error("非预检写入须传 confirm=true"));
        }
        return runMigrationOne(request, entity, id, dryRun, false);
    }

    private R runMigration(HttpServletRequest request, String entity, boolean dryRun, int batchSize, boolean encrypt) {
        String action = encrypt ? "migrate" : "restore";
        return admin(request, action, () -> {
            if (StringUtils.isBlank(entity)) {
                return R.error("entity 不能为空");
            }
            int size = Math.min(Math.max(batchSize, 1), MAX_BATCH_SIZE);
            try {
                FieldEncryptMigrationService.MigrationResult result = encrypt
                        ? migrationService.migrate(entity, dryRun, size)
                        : migrationService.restore(entity, dryRun, size);
                adminAccessGuard.auditWithIp(request, "field_encrypt_" + action,
                        "entity=" + entity + " dryRun=" + dryRun + " batchSize=" + size
                                + " processed=" + result.getProcessed());
                return toMigrationResult(result);
            } catch (Exception e) {
                return R.error(e.getMessage());
            }
        });
    }

    private R runMigrationOne(HttpServletRequest request, String entity, String id, boolean dryRun, boolean encrypt) {
        String action = encrypt ? "migrateOne" : "restoreOne";
        return admin(request, action, () -> {
            if (StringUtils.isBlank(entity)) {
                return R.error("entity 不能为空");
            }
            try {
                FieldEncryptMigrationService.MigrationResult result = encrypt
                        ? migrationService.migrateOne(entity, id, dryRun)
                        : migrationService.restoreOne(entity, id, dryRun);
                adminAccessGuard.auditWithIp(request, "field_encrypt_" + action,
                        "entity=" + entity + " id=" + id + " dryRun=" + dryRun);
                return toMigrationResult(result);
            } catch (Exception e) {
                return R.error(e.getMessage());
            }
        });
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

    private R toMigrationResult(FieldEncryptMigrationService.MigrationResult result) {
        R r = R.ok().put("entity", result.getEntity()).put("dryRun", result.isDryRun())
                .put("action", result.getAction()).put("scanned", result.getScanned())
                .put("pending", result.getPending()).put("processed", result.getProcessed())
                .put("migrated", result.getMigrated());
        if (StringUtils.isNotBlank(result.getMessage())) {
            r.put("msg", result.getMessage());
        }
        return r;
    }
}

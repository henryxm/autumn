package cn.org.autumn.modules.sys.controller;

import cn.org.autumn.annotation.SkipInterceptor;
import cn.org.autumn.crypto.FieldEncryptMigrationService;
import cn.org.autumn.crypto.FieldEncryptService;
import cn.org.autumn.modules.sys.service.FieldEncryptRuntimeService;
import cn.org.autumn.modules.sys.service.SysUserRoleService;
import cn.org.autumn.modules.sys.shiro.ShiroUtils;
import cn.org.autumn.modules.wall.service.IpWhiteService;
import cn.org.autumn.utils.R;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 字段存储加密运维 API（系统管理员）。
 */
@RestController
@RequestMapping("sys/crypto/field")
@SkipInterceptor
public class FieldEncryptAdminController {

    @Autowired
    private FieldEncryptMigrationService migrationService;

    @Autowired
    private FieldEncryptService fieldEncryptService;

    @Autowired
    private FieldEncryptRuntimeService runtimeService;

    @Autowired
    private SysUserRoleService sysUserRoleService;

    @Autowired
    private IpWhiteService ipWhiteService;

    @GetMapping("/status")
    public R status(HttpServletRequest request) {
        return admin(request, () -> {
            runtimeService.reloadFromRedis();
            return R.ok().put("data", fieldEncryptService.statusSnapshot());
        });
    }

    @GetMapping("/list")
    public R list(HttpServletRequest request) {
        return admin(request, () -> R.ok().put("list", migrationService.listEncryptEntities()));
    }

    @PostMapping("/writeEnabled")
    public R setWriteEnabled(@RequestParam("enabled") boolean enabled, HttpServletRequest request) {
        return admin(request, () -> {
            try {
                runtimeService.setWriteEncryptEnabled(enabled);
                return R.ok().put("data", fieldEncryptService.statusSnapshot());
            } catch (Exception e) {
                return R.error(e.getMessage());
            }
        });
    }

    @PostMapping("/resetWriteEnabled")
    public R resetWriteEnabled(HttpServletRequest request) {
        return admin(request, () -> {
            runtimeService.resetWriteEncryptOverride();
            return R.ok().put("data", fieldEncryptService.statusSnapshot());
        });
    }

    @PostMapping("/reloadCluster")
    public R reloadCluster(HttpServletRequest request) {
        return admin(request, () -> {
            if (!runtimeService.isClusterMode()) {
                return R.error("当前非 Redis 集群模式，无需集群刷新");
            }
            runtimeService.reloadFromRedis();
            return R.ok().put("data", fieldEncryptService.statusSnapshot());
        });
    }

    @PostMapping("/generateKey")
    public R generateKey(HttpServletRequest request) {
        return admin(request, () -> R.ok().put("data", fieldEncryptService.generateDevKeyMaterial()));
    }

    @PostMapping("/generateVector")
    public R generateVector(HttpServletRequest request) {
        return admin(request, () -> R.ok().put("data", fieldEncryptService.generateDevVector()));
    }

    @PostMapping("/test")
    public R test(@RequestParam(value = "plain", defaultValue = "") String plain,
                  @RequestParam(value = "vector", required = false) String vector,
                  HttpServletRequest request) {
        return admin(request, () -> {
            if (!fieldEncryptService.isKeyConfigured()) {
                return R.error("未配置有效的 autumn.crypto.field.key，无法执行加解密测试");
            }
            Map<String, Object> data = fieldEncryptService.testRoundTrip(plain, vector);
            if (data.containsKey("error")) {
                return R.error(String.valueOf(data.get("error")));
            }
            return R.ok().put("data", data);
        });
    }

    @PostMapping("/decrypt")
    public R decrypt(@RequestParam(value = "cipher", defaultValue = "") String cipher, HttpServletRequest request) {
        return admin(request, () -> {
            Map<String, Object> data = fieldEncryptService.testDecrypt(cipher);
            if (data.containsKey("error")) {
                return R.error(String.valueOf(data.get("error")));
            }
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
                        @RequestParam(value = "dryRun", defaultValue = "false") boolean dryRun,
                        HttpServletRequest request) {
        return runMigrationOne(request, entity, id, dryRun, true);
    }

    @PostMapping("/restoreOne")
    public R restoreOne(@RequestParam("entity") String entity,
                        @RequestParam("id") String id,
                        @RequestParam(value = "dryRun", defaultValue = "false") boolean dryRun,
                        HttpServletRequest request) {
        return runMigrationOne(request, entity, id, dryRun, false);
    }

    private R runMigration(HttpServletRequest request, String entity, boolean dryRun, int batchSize, boolean encrypt) {
        return admin(request, () -> {
            if (StringUtils.isBlank(entity)) {
                return R.error("entity 不能为空");
            }
            try {
                FieldEncryptMigrationService.MigrationResult result = encrypt
                        ? migrationService.migrate(entity, dryRun, batchSize)
                        : migrationService.restore(entity, dryRun, batchSize);
                return toMigrationResult(result);
            } catch (Exception e) {
                return R.error(e.getMessage());
            }
        });
    }

    private R runMigrationOne(HttpServletRequest request, String entity, String id, boolean dryRun, boolean encrypt) {
        return admin(request, () -> {
            if (StringUtils.isBlank(entity)) {
                return R.error("entity 不能为空");
            }
            try {
                FieldEncryptMigrationService.MigrationResult result = encrypt
                        ? migrationService.migrateOne(entity, id, dryRun)
                        : migrationService.restoreOne(entity, id, dryRun);
                return toMigrationResult(result);
            } catch (Exception e) {
                return R.error(e.getMessage());
            }
        });
    }

    private R admin(HttpServletRequest request, Supplier<R> action) {
        if (!authorize(request)) {
            return R.error(403, "无权限");
        }
        return action.get();
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

    private boolean authorize(HttpServletRequest request) {
        ipWhiteService.check(request, getClass(), "fieldEncrypt");
        return ShiroUtils.isLogin() && sysUserRoleService.isSystemAdministrator(ShiroUtils.getUserUuid());
    }
}

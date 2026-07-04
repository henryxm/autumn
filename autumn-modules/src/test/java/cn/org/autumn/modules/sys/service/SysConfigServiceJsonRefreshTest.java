package cn.org.autumn.modules.sys.service;

import cn.org.autumn.config.JsonTypeConfigRefresher;
import cn.org.autumn.model.AccountAuthConfig;
import cn.org.autumn.model.AesConfig;
import cn.org.autumn.model.DistributedLockConfig;
import cn.org.autumn.model.PayCredentialConfig;
import cn.org.autumn.model.RobotQuotaConfig;
import cn.org.autumn.model.RsaConfig;
import cn.org.autumn.model.ScanLoginConfig;
import cn.org.autumn.modules.oss.cloud.CloudStorageConfig;
import cn.org.autumn.modules.sys.entity.LoadingTheme;
import cn.org.autumn.modules.sys.entity.SysConfigEntity;
import cn.org.autumn.modules.sys.entity.SystemUpgrade;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SysConfigServiceJsonRefreshTest {

    static Stream<String> builtinJsonConfigClasses() {
        return Stream.of(
                SystemUpgrade.class.getName(),
                LoadingTheme.class.getName(),
                DistributedLockConfig.class.getName(),
                CloudStorageConfig.class.getName(),
                RsaConfig.class.getName(),
                AesConfig.class.getName(),
                RobotQuotaConfig.class.getName(),
                PayCredentialConfig.class.getName(),
                ScanLoginConfig.class.getName(),
                AccountAuthConfig.class.getName()
        );
    }

    @Test
    void isJsonTypeConfig_delegatesToRefresher() {
        SysConfigEntity entity = new SysConfigEntity();
        entity.setType(SysConfigService.json_type);
        entity.setOptions(AccountAuthConfig.class.getName());
        assertTrue(SysConfigService.isJsonTypeConfig(entity));

        entity.setType("text");
        assertFalse(SysConfigService.isJsonTypeConfig(entity));

        entity.setType(SysConfigService.json_type);
        entity.setOptions("");
        assertFalse(SysConfigService.isJsonTypeConfig(entity));
    }

    @ParameterizedTest
    @MethodSource("builtinJsonConfigClasses")
    void mergeMissingFields_builtinJsonTypes(String className) throws Exception {
        Class<?> clazz = Class.forName(className);
        JsonObject defaults = JsonTypeConfigRefresher.buildDefaultConfigJson(clazz, clazz.newInstance(), JsonTypeConfigRefresher.refreshGson());
        JsonTypeConfigRefresher.MergeResult result = JsonTypeConfigRefresher.mergeMissingFields(className, "{}");
        JsonObject merged = JsonParser.parseString(result.getJson()).getAsJsonObject();
        for (Map.Entry<String, com.google.gson.JsonElement> entry : defaults.entrySet()) {
            assertTrue(merged.has(entry.getKey()), () -> className + " missing key " + entry.getKey());
        }
        assertTrue(result.isChanged());
    }

    @Test
    void mergeMissingFields_preservesExistingAccountAuthValues() throws Exception {
        String stored = "{\"registerEnabled\":true,\"forgotPasswordEnabled\":false}";
        JsonTypeConfigRefresher.MergeResult outcome = JsonTypeConfigRefresher.mergeMissingFields(AccountAuthConfig.class.getName(), stored);
        assertTrue(outcome.getAddedFields().contains("postLoginRedirect"));
        AccountAuthConfig parsed = JsonTypeConfigRefresher.refreshGson().fromJson(outcome.getJson(), AccountAuthConfig.class);
        assertTrue(parsed.isRegisterEnabled());
        assertFalse(parsed.isForgotPasswordEnabled());
    }
}

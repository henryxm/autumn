package cn.org.autumn.config;

import cn.org.autumn.model.AccountAuthConfig;
import cn.org.autumn.model.AesConfig;
import cn.org.autumn.model.DistributedLockConfig;
import cn.org.autumn.model.PayCredentialConfig;
import cn.org.autumn.model.RobotQuotaConfig;
import cn.org.autumn.model.RsaConfig;
import cn.org.autumn.model.ScanLoginConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonTypeConfigRefresherTest {

    static Stream<String> libJsonConfigClasses() {
        return Stream.of(
                AccountAuthConfig.class.getName(),
                AesConfig.class.getName(),
                RsaConfig.class.getName(),
                RobotQuotaConfig.class.getName(),
                DistributedLockConfig.class.getName(),
                PayCredentialConfig.class.getName(),
                ScanLoginConfig.class.getName()
        );
    }

    @Test
    void mergeMissingFields_addsNullStringFieldsFromConfigModel() throws Exception {
        String stored = "{\"registerEnabled\":true,\"forgotPasswordEnabled\":false}";
        JsonTypeConfigRefresher.MergeResult outcome = JsonTypeConfigRefresher.mergeMissingFields(AccountAuthConfig.class.getName(), stored);

        assertTrue(outcome.getAddedFields().contains("postLoginRedirect"));
        assertTrue(JsonParser.parseString(outcome.getJson()).getAsJsonObject().has("postLoginRedirect"));
        AccountAuthConfig parsed = JsonTypeConfigRefresher.refreshGson().fromJson(outcome.getJson(), AccountAuthConfig.class);
        assertTrue(parsed.isRegisterEnabled());
        assertFalse(parsed.isForgotPasswordEnabled());
    }

    @Test
    void buildDefaultConfigJson_includesNullFieldsWithSerializeNulls() throws Exception {
        JsonObject json = JsonTypeConfigRefresher.buildDefaultConfigJson(AccountAuthConfig.class, new AccountAuthConfig(), JsonTypeConfigRefresher.refreshGson());
        assertTrue(json.has("postLoginRedirect"));
    }

    @Test
    void refreshGson_serializesNulls() {
        String json = JsonTypeConfigRefresher.refreshGson().toJson(new AccountAuthConfig());
        assertTrue(json.contains("postLoginRedirect"));
    }

    @Test
    void isJsonTypeConfig() {
        assertTrue(JsonTypeConfigRefresher.isJsonTypeConfig("json", AccountAuthConfig.class.getName(), "json"));
        assertFalse(JsonTypeConfigRefresher.isJsonTypeConfig("text", AccountAuthConfig.class.getName(), "json"));
        assertFalse(JsonTypeConfigRefresher.isJsonTypeConfig("json", "", "json"));
        assertFalse(JsonTypeConfigRefresher.isJsonTypeConfig("json", null, "json"));
    }

    @ParameterizedTest
    @MethodSource("libJsonConfigClasses")
    void mergeMissingFields_fromEmptyObject(String className) throws Exception {
        Class<?> clazz = Class.forName(className);
        JsonObject defaults = JsonTypeConfigRefresher.buildDefaultConfigJson(clazz, clazz.newInstance(), JsonTypeConfigRefresher.refreshGson());
        JsonTypeConfigRefresher.MergeResult result = JsonTypeConfigRefresher.mergeMissingFields(className, "{}");
        JsonObject merged = JsonParser.parseString(result.getJson()).getAsJsonObject();
        for (Map.Entry<String, com.google.gson.JsonElement> entry : defaults.entrySet()) {
            assertTrue(merged.has(entry.getKey()), () -> className + " missing key " + entry.getKey());
        }
        assertTrue(result.isChanged());
    }
}

package cn.org.autumn.config;

import cn.org.autumn.annotation.ConfigField;
import cn.org.autumn.annotation.ConfigParam;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang.StringUtils;

/**
 * json_type 配置刷新：按 {@link ConfigParam}/{@link ConfigField} 模型补全缺失 JSON 字段，保留已有值。
 */
public final class JsonTypeConfigRefresher {

    private JsonTypeConfigRefresher() {
    }

    public static boolean isJsonTypeConfig(String type, String options, String jsonTypeValue) {
        return jsonTypeValue != null && jsonTypeValue.equals(type) && StringUtils.isNotBlank(options);
    }

    public static MergeResult mergeMissingFields(String className, String storedValue) throws Exception {
        Class<?> clazz = Class.forName(className.trim());
        Object defaultInstance = clazz.newInstance();
        Gson gson = refreshGson();
        JsonObject defaultJson = buildDefaultConfigJson(clazz, defaultInstance, gson);
        JsonObject storedJson = StringUtils.isBlank(storedValue) ? new JsonObject() : JsonParser.parseString(storedValue.trim()).getAsJsonObject();
        if (!storedJson.isJsonObject()) {
            throw new IllegalArgumentException("配置值不是 JSON 对象");
        }
        List<String> addedFields = new ArrayList<>();
        JsonObject merged = storedJson.deepCopy();
        mergeMissingJsonFields(defaultJson, merged, "", addedFields);
        Object mergedObject = gson.fromJson(merged, clazz);
        List<String> fixes = applyConfigPostProcess(mergedObject);
        String mergedJson = gson.toJson(mergedObject);
        String before = StringUtils.defaultString(storedValue, "").trim();
        MergeResult outcome = new MergeResult();
        outcome.json = mergedJson;
        outcome.addedFields = addedFields;
        outcome.fixes = fixes;
        outcome.changed = !addedFields.isEmpty() || !fixes.isEmpty() || !Objects.equals(before, mergedJson);
        return outcome;
    }

    public static Gson refreshGson() {
        return new GsonBuilder().serializeNulls().create();
    }

    public static JsonObject buildDefaultConfigJson(Class<?> clazz, Object instance, Gson gson) throws Exception {
        JsonObject json = gson.toJsonTree(instance).getAsJsonObject();
        ensureConfigFieldKeys(clazz, instance, json, gson, "");
        return json;
    }

    public static List<String> listConfigFieldNames(Class<?> clazz) {
        List<String> names = new ArrayList<>();
        collectConfigFieldNames(clazz, names);
        return names;
    }

    private static void collectConfigFieldNames(Class<?> clazz, List<String> names) {
        if (clazz == null) {
            return;
        }
        for (Field field : clazz.getDeclaredFields()) {
            if (!isConfigModelField(clazz, field)) {
                continue;
            }
            if (isNestedConfigType(field.getType())) {
                collectConfigFieldNames(field.getType(), names);
                continue;
            }
            names.add(field.getName());
        }
    }

    static List<String> applyConfigPostProcess(Object config) {
        if (config == null) {
            return Collections.emptyList();
        }
        List<String> notes = new ArrayList<>();
        notes.addAll(invokeValidateAndFix(config));
        invokeNormalize(config);
        return notes;
    }

    private static List<String> invokeValidateAndFix(Object config) {
        try {
            Method method = config.getClass().getMethod("validateAndFix");
            Object fixes = method.invoke(config);
            if (fixes instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) fixes;
                return list == null ? Collections.emptyList() : new ArrayList<>(list);
            }
        } catch (NoSuchMethodException ignored) {
        } catch (Exception ignored) {
        }
        return Collections.emptyList();
    }

    private static void invokeNormalize(Object config) {
        try {
            Method method = config.getClass().getMethod("normalize");
            method.invoke(config);
        } catch (NoSuchMethodException ignored) {
        } catch (Exception ignored) {
        }
    }

    private static void ensureConfigFieldKeys(Class<?> clazz, Object instance, JsonObject json, Gson gson, String prefix) throws Exception {
        for (Field field : clazz.getDeclaredFields()) {
            if (!isConfigModelField(clazz, field)) {
                continue;
            }
            field.setAccessible(true);
            String key = field.getName();
            if (isNestedConfigType(field.getType())) {
                Object nested = field.get(instance);
                if (nested == null) {
                    nested = field.getType().newInstance();
                }
                JsonObject nestedJson;
                if (json.has(key) && json.get(key).isJsonObject()) {
                    nestedJson = json.getAsJsonObject(key);
                } else {
                    nestedJson = gson.toJsonTree(nested).getAsJsonObject();
                    json.add(key, nestedJson);
                }
                ensureConfigFieldKeys(field.getType(), nested, nestedJson, gson, StringUtils.isBlank(prefix) ? key : prefix + "." + key);
                continue;
            }
            if (!json.has(key)) {
                json.add(key, gson.toJsonTree(field.get(instance)));
            }
        }
    }

    private static boolean isConfigModelField(Class<?> clazz, Field field) {
        if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
            return false;
        }
        if ("serialVersionUID".equals(field.getName())) {
            return false;
        }
        if (field.getAnnotation(ConfigField.class) != null || field.getAnnotation(ConfigParam.class) != null) {
            return true;
        }
        return clazz.getAnnotation(ConfigParam.class) != null;
    }

    private static boolean isNestedConfigType(Class<?> type) {
        return type != null && type.getAnnotation(ConfigParam.class) != null && !type.isPrimitive() && !type.equals(String.class);
    }

    private static void mergeMissingJsonFields(JsonObject defaults, JsonObject target, String prefix, List<String> addedFields) {
        for (Map.Entry<String, JsonElement> entry : defaults.entrySet()) {
            String key = entry.getKey();
            String path = StringUtils.isBlank(prefix) ? key : prefix + "." + key;
            if (!target.has(key)) {
                target.add(key, entry.getValue());
                addedFields.add(path);
                continue;
            }
            JsonElement defaultElement = entry.getValue();
            JsonElement targetElement = target.get(key);
            if (defaultElement != null && defaultElement.isJsonObject() && targetElement != null && targetElement.isJsonObject()) {
                mergeMissingJsonFields(defaultElement.getAsJsonObject(), targetElement.getAsJsonObject(), path, addedFields);
            }
        }
    }

    public static final class MergeResult {
        private String json;
        private List<String> addedFields = new ArrayList<>();
        private List<String> fixes = new ArrayList<>();
        private boolean changed;

        public String getJson() {
            return json;
        }

        public List<String> getAddedFields() {
            return addedFields;
        }

        public List<String> getFixes() {
            return fixes;
        }

        public boolean isChanged() {
            return changed;
        }
    }
}

package cn.org.autumn.model;

import cn.org.autumn.config.GsonConfig;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 兼容请求包装：
 * 1) 新协议：Request<T>（data + 可选加密字段）
 * 2) 旧协议：直接提交业务字段（自动汇总到 legacyBody）
 *
 * @param <T> 业务数据类型
 */
@Getter
@Setter
public class CompatibleRequest<T> extends Request<T> {

    /**
     * 旧协议明文请求体字段（非 Request 标准字段）
     */
    private Map<String, Object> legacy = new LinkedHashMap<>();

    @JsonAnySetter
    public void putLegacy(String key, Object value) {
        legacy.put(key, value);
    }

    @JsonIgnore
    public boolean hasLegacy() {
        return legacy != null && !legacy.isEmpty();
    }

    /**
     * 统一解析业务对象：
     * - 优先取 Request.data（新协议）
     * - 否则回退 legacyBody（旧协议）
     *
     * @param clazz 目标类型
     * @param gson  Gson 实例
     * @param <X>   目标类型泛型
     * @return 业务对象；无法解析时返回 null
     */
    @JsonIgnore
    public <X> X resolve(Class<X> clazz, Gson gson) {
        if (clazz == null || gson == null) {
            return null;
        }
        Object data = getData();
        if (data != null) {
            if (clazz.isInstance(data)) {
                return clazz.cast(data);
            }
            return gson.fromJson(gson.toJson(data), clazz);
        }
        if (hasLegacy()) {
            return gson.fromJson(gson.toJson(legacy), clazz);
        }
        return null;
    }

    public static <X> X resolve(CompatibleRequest<X> request, Class<X> clazz) {
        return request != null ? request.resolve(clazz, GsonConfig.getGson()) : null;
    }
}

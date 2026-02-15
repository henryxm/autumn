package cn.org.autumn.site;

import cn.org.autumn.config.VersionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 依赖库版本打印工厂。
 * <p>
 * 自动收集所有 {@link VersionHandler} 实现，在启动时以统一格式打印各组件版本号。
 */
@Slf4j
@Component
public class VersionFactory extends Factory {

    private List<VersionHandler> handlers;

    /**
     * 获取所有已注册的版本处理器（按 @Order 排序）
     */
    private List<VersionHandler> getHandlers() {
        if (handlers == null) {
            handlers = getOrderList(VersionHandler.class);
        }
        return handlers;
    }

    /**
     * 收集所有版本信息为 Map（名称 → 版本号）
     */
    public Map<String, String> getVersions() {
        Map<String, String> versions = new LinkedHashMap<>();
        List<VersionHandler> list = getHandlers();
        if (list != null && !list.isEmpty()) {
            for (VersionHandler handler : list) {
                try {
                    String name = handler.name();
                    String version = handler.version();
                    if (name != null && version != null) {
                        versions.putIfAbsent(name, version);
                    }
                } catch (Exception e) {
                    log.debug("Failed to get version from {}: {}", handler.getClass().getSimpleName(), e.getMessage());
                }
            }
        }
        return versions;
    }

    /**
     * 以日志方式打印所有版本号。
     * 自动对齐名称列宽。
     */
    public void printVersions() {
        Map<String, String> versions = getVersions();
        if (versions.isEmpty()) {
            return;
        }
        // 计算最大名称长度用于对齐
        int maxLen = versions.keySet().stream().mapToInt(String::length).max().orElse(12);
        String format = "%-" + maxLen + "s : %s";
        for (Map.Entry<String, String> entry : versions.entrySet()) {
            log.info(String.format(format, entry.getKey(), entry.getValue()));
        }
    }
}

package cn.org.autumn.version;

import cn.org.autumn.site.VersionFactory;
import cn.org.autumn.utils.SpringContextUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Autumn 框架版本信息中心。
 * <p>
 * 通过 {@link VersionFactory} 自动收集所有 {@link cn.org.autumn.config.VersionHandler}
 * 实现，以日志方式统一打印各组件版本号。
 * <p>
 * 扩展方式：只需在任意模块中实现 {@link cn.org.autumn.config.VersionHandler} 接口并注册为
 * Spring Bean，即可在启动时自动输出该组件版本。
 */
@Slf4j
public class Autumn {

    /**
     * 从 META-INF/MANIFEST.MF 中读取当前项目版本
     */
    public static String getVersion() {
        String version = Autumn.class.getPackage().getImplementationVersion();
        return version != null ? version : "dev";
    }

    /**
     * 以日志方式打印所有已注册组件的版本号。
     * <p>
     * 委托给 {@link VersionFactory} 自动收集所有 VersionHandler 实现并输出。
     */
    public static void versions() {
        try {
            VersionFactory factory = SpringContextUtils.getApplicationContext().getBean(VersionFactory.class);
            factory.printVersions();
        } catch (Exception e) {
            // 降级：若工厂不可用，打印基本版本信息
            log.info("Autumn : {}", getVersion());
        }
    }
}
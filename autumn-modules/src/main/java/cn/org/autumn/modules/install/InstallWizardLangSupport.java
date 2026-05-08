package cn.org.autumn.modules.install;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 安装阶段 {@code lang} 词条来源：<b>classpath 种子文件</b>，不依赖业务库已初始化。
 * <p>
 * 与正常运行期 {@link cn.org.autumn.modules.lan.service.LanguageService} 内存缓存的关系：
 * 安装模式下 {@link cn.org.autumn.modules.lan.interceptor.LanguageInterceptor} 先装入本类种子，
 * 再合并 {@code languageService.getLanguage(...)}（仅读内存 Map，默认不访问 DB）；若引导库或提前加载过词条，则以内存为准覆盖同名字段。
 * 这样既不是「二选一」，也避免安装前强行查业务表。
 */
public final class InstallWizardLangSupport {

    private static final String DEFAULT_TAG = "zh_cn";

    private static final Map<String, Properties> BUNDLES = new ConcurrentHashMap<>();

    static {
        BUNDLES.put("zh_cn", loadProps("install/lang/install-lang_zh_cn.properties"));
        BUNDLES.put("en_us", loadProps("install/lang/install-lang_en_us.properties"));
    }

    private InstallWizardLangSupport() {
    }

    private static Properties loadProps(String classpath) {
        Properties p = new Properties();
        try (InputStream in = InstallWizardLangSupport.class.getClassLoader().getResourceAsStream(classpath)) {
            if (in == null) {
                return p;
            }
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                p.load(reader);
            }
        } catch (IOException ignored) {
        }
        return p;
    }

    /**
     * 与 {@link cn.org.autumn.modules.lan.service.LanguageService#toLang(Locale)} 对齐的 tag，用于选 bundle 与合并内存词条。
     */
    public static String toLangTag(Locale locale) {
        if (locale == null) {
            return DEFAULT_TAG;
        }
        String tag = locale.toLanguageTag().replace("-", "_").toLowerCase(Locale.ROOT);
        if ("en".equals(tag)) {
            return "en_us";
        }
        if ("zh".equals(tag)) {
            return "zh_cn";
        }
        return tag;
    }

    /**
     * 当前语言下的安装向导种子词条（仅 classpath，不访问数据库）。
     */
    public static Map<String, String> seedMap(Locale locale) {
        String tag = toLangTag(locale);
        Properties primary = BUNDLES.get(tag);
        if (primary == null || primary.isEmpty()) {
            primary = BUNDLES.get(DEFAULT_TAG);
        }
        if (primary == null || primary.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> out = new HashMap<>();
        for (String name : primary.stringPropertyNames()) {
            out.put(name, primary.getProperty(name));
        }
        return out;
    }
}

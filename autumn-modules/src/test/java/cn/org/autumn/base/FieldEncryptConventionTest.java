package cn.org.autumn.base;

import cn.org.autumn.annotation.FieldEncrypt;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 约束：含 {@code @FieldEncrypt} 的实体，其模块 Service 须继承 {@link EncryptModuleService}。
 * <p>
 * 业务仓库扩展扫描：{@code -Dautumn.fieldEncrypt.convention.packages=cn.example.modules,cn.other.app}
 */
public class FieldEncryptConventionTest {

    private static final String MODULE_BASE = "cn.org.autumn.modules";
    private static final String EXTRA_PACKAGES_PROPERTY = "autumn.fieldEncrypt.convention.packages";

    @Test
    public void encryptEntityServiceMustExtendEncryptModuleService() throws Exception {
        Set<Class<?>> encryptEntities = findAllEncryptEntities();
        if (encryptEntities.isEmpty()) {
            return;
        }
        for (Class<?> entityClass : encryptEntities) {
            Class<?> serviceClass = resolveServiceClass(entityClass);
            Assert.assertNotNull("未找到 " + entityClass.getName() + " 对应 Service", serviceClass);
            Assert.assertTrue(serviceClass.getName() + " 须 extends EncryptModuleService",
                    EncryptModuleService.class.isAssignableFrom(serviceClass));
        }
    }

    private static Set<Class<?>> findAllEncryptEntities() throws Exception {
        Set<Class<?>> out = new HashSet<>();
        for (String basePackage : scanPackages()) {
            out.addAll(findEncryptEntities(basePackage));
        }
        return out;
    }

    private static Set<String> scanPackages() {
        Set<String> packages = new HashSet<>();
        packages.add(MODULE_BASE);
        String extra = System.getProperty(EXTRA_PACKAGES_PROPERTY);
        if (StringUtils.hasText(extra)) {
            packages.addAll(Arrays.stream(extra.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toSet()));
        }
        return packages;
    }

    private static Set<Class<?>> findEncryptEntities(String basePackage) throws Exception {
        Set<Class<?>> out = new HashSet<>();
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter((metadataReader, factory) -> true);
        for (BeanDefinition bd : scanner.findCandidateComponents(basePackage)) {
            Class<?> clazz = Class.forName(bd.getBeanClassName());
            if (hasFieldEncrypt(clazz)) {
                out.add(clazz);
            }
        }
        return out;
    }

    private static boolean hasFieldEncrypt(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getAnnotation(FieldEncrypt.class) != null) {
                return true;
            }
        }
        return false;
    }

    private static Class<?> resolveServiceClass(Class<?> entityClass) throws ClassNotFoundException {
        String pkg = entityClass.getPackage().getName();
        if (!pkg.endsWith(".entity")) {
            return null;
        }
        String servicePkg = pkg.substring(0, pkg.length() - ".entity".length()) + ".service";
        String serviceName = entityClass.getSimpleName().replace("Entity", "Service");
        return Class.forName(servicePkg + "." + serviceName);
    }
}

package cn.org.autumn.base;

import cn.org.autumn.annotation.FieldEncrypt;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * 约束：含 {@code @FieldEncrypt} 的实体，其模块 Service 须继承 {@link EncryptModuleService}。
 */
public class FieldEncryptConventionTest {

    private static final String MODULE_BASE = "cn.org.autumn.modules";

    @Test
    public void encryptEntityServiceMustExtendEncryptModuleService() throws Exception {
        Set<Class<?>> encryptEntities = findEncryptEntities();
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

    private static Set<Class<?>> findEncryptEntities() throws Exception {
        Set<Class<?>> out = new HashSet<>();
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter((metadataReader, factory) -> true);
        for (BeanDefinition bd : scanner.findCandidateComponents(MODULE_BASE)) {
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

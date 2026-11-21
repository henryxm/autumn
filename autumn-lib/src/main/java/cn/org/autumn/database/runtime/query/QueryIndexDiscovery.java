package cn.org.autumn.database.runtime.query;

import cn.org.autumn.database.runtime.RuntimeSql;
import cn.org.autumn.table.annotation.Index;
import cn.org.autumn.table.annotation.Indexes;
import cn.org.autumn.table.utils.ClassTools;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 启动发现：扫描 {@link RuntimeSql} 子类中带空 {@code @Index}/{@code @Indexes} 的<strong>无参 public String</strong>方法并 invoke，
 * 由 {@link IndexedSelect#sql()} 登记查询即索引。
 * <p>
 * 方法注解<strong>仅白名单</strong>，不解析 fields/name 等；无标记方法的类不实例化。
 */
@Slf4j
public final class QueryIndexDiscovery {

    private static final AtomicBoolean DISCOVERED = new AtomicBoolean(false);

    private QueryIndexDiscovery() {
    }

    public static void discover(Set<String> packs) {
        if (!DISCOVERED.compareAndSet(false, true)) {
            return;
        }
        int classesHit = 0;
        int methodsInvoked = 0;

        if (packs != null) {
            for (String pkg : packs) {
                if (pkg == null || pkg.trim().isEmpty()) {
                    continue;
                }
                Set<Class<?>> classes;
                try {
                    classes = ClassTools.getClasses(pkg.trim());
                } catch (Exception e) {
                    log.warn("QueryIndexDiscovery skip pack={}: {}", pkg, e.toString());
                    continue;
                }
                for (Class<?> clazz : classes) {
                    if (clazz == null || clazz.isInterface() || clazz.isAnnotation()
                            || Modifier.isAbstract(clazz.getModifiers())) {
                        continue;
                    }
                    if (!RuntimeSql.class.isAssignableFrom(clazz)) {
                        continue;
                    }
                    List<Method> marked = findMarkedMethods(clazz);
                    if (marked.isEmpty()) {
                        continue;
                    }
                    int n = invokeMarked(clazz, marked);
                    if (n > 0) {
                        classesHit++;
                        methodsInvoked += n;
                    }
                }
            }
        }

        if (classesHit > 0) {
            if (QueryIndexRegistry.isEmpty()) {
                log.warn("QueryIndexDiscovery: 已调用带 @Index 的方法但未登记索引；"
                        + "请确认方法内使用 IndexedSelect.sql()，且 select(table) 与实体表名一致");
            } else {
                log.info("QueryIndexDiscovery done: daoSqlClasses={} methods={} tables={}",
                        classesHit, methodsInvoked, QueryIndexRegistry.tableCount());
            }
        } else {
            log.debug("QueryIndexDiscovery done: no @Index/@Indexes on RuntimeSql provider methods");
        }
        QueryIndexRegistry.freezeAfterBootstrap();
    }

    /** 测试用：允许再次 discover。 */
    public static void resetForTests() {
        DISCOVERED.set(false);
        QueryIndexRegistry.clear();
        IndexedSelect.clearSqlMemoForTests();
    }

    private static List<Method> findMarkedMethods(Class<?> clazz) {
        List<Method> out = new ArrayList<>();
        for (Method m : clazz.getDeclaredMethods()) {
            if (!Modifier.isPublic(m.getModifiers()) || Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            if (m.getParameterCount() != 0 || m.getReturnType() != String.class) {
                continue;
            }
            if (m.getDeclaringClass() == RuntimeSql.class || m.getDeclaringClass() == Object.class) {
                continue;
            }
            if (m.getAnnotation(Index.class) == null && m.getAnnotation(Indexes.class) == null) {
                continue;
            }
            out.add(m);
        }
        return out;
    }

    private static int invokeMarked(Class<?> clazz, List<Method> methods) {
        Object instance;
        try {
            Constructor<?> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            instance = ctor.newInstance();
        } catch (Exception e) {
            log.warn("QueryIndexDiscovery instantiate failed: class={} err={}", clazz.getName(), e.toString());
            return 0;
        }
        int ok = 0;
        for (Method m : methods) {
            try {
                m.setAccessible(true);
                m.invoke(instance);
                ok++;
            } catch (Exception e) {
                log.warn("QueryIndexDiscovery method failed: class={} method={} err={}",
                        clazz.getName(), m.getName(), e.toString());
            }
        }
        return ok;
    }
}

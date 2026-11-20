package cn.org.autumn.database.runtime.query;

import cn.org.autumn.database.runtime.RuntimeSql;
import cn.org.autumn.table.utils.ClassTools;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 启动时发现 {@link QueryIndexContributor} / {@link QueryIndexScan}，触发索引登记。
 * 与 Entity {@code @Indexes} 并列，无单独开关；包内无 Contributor/Scan 时仅遍历类元数据。
 * <p>
 * 扫描范围与注解建表相同（{@code autumn.table.pack}）；另可注入 Spring 管理的 {@link QueryIndexContributor}。
 */
@Slf4j
public final class QueryIndexDiscovery {

    private static final AtomicBoolean DISCOVERED = new AtomicBoolean(false);

    private QueryIndexDiscovery() {
    }

    public static void discover(Set<String> packs) {
        discover(packs, null);
    }

    /**
     * @param springContributors 可选；Spring 容器中的 Contributor（DaoSql 通常不是 Bean，以包扫描为主）
     */
    public static void discover(Set<String> packs, Collection<? extends QueryIndexContributor> springContributors) {
        if (!DISCOVERED.compareAndSet(false, true)) {
            return;
        }
        int contributors = 0;
        int scanned = 0;

        if (springContributors != null) {
            for (QueryIndexContributor c : springContributors) {
                if (c == null) {
                    continue;
                }
                try {
                    c.contributeQueryIndexes();
                    contributors++;
                } catch (Exception e) {
                    log.warn("QueryIndexContributor(bean) failed: class={} err={}",
                            c.getClass().getName(), e.toString());
                }
            }
        }

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
                    boolean isContributor = QueryIndexContributor.class.isAssignableFrom(clazz);
                    boolean isScan = clazz.isAnnotationPresent(QueryIndexScan.class);
                    if (!isContributor && !isScan) {
                        continue;
                    }
                    if (isContributor) {
                        if (invokeContributor(clazz)) {
                            contributors++;
                        }
                        continue;
                    }
                    if (invokeScanMethods(clazz)) {
                        scanned++;
                    }
                }
            }
        }

        if (contributors > 0 || scanned > 0) {
            if (QueryIndexRegistry.isEmpty()) {
                log.warn("QueryIndexDiscovery: 已扫描 Contributor/Scan 但未登记任何索引；"
                        + "请确认 contributeQueryIndexes / 无参 String 方法调用了 IndexedSelect，且 select(table) 与实体表名一致");
            } else {
                log.info("QueryIndexDiscovery done: contributors={} queryIndexScan={} tables={}",
                        contributors, scanned, QueryIndexRegistry.tableCount());
            }
        } else {
            log.debug("QueryIndexDiscovery done: no Contributor/@QueryIndexScan in packs");
        }
        QueryIndexRegistry.freezeAfterBootstrap();
    }

    /** 测试用：允许再次 discover。 */
    public static void resetForTests() {
        DISCOVERED.set(false);
        QueryIndexRegistry.clear();
    }

    private static boolean invokeContributor(Class<?> clazz) {
        try {
            Object instance = newInstance(clazz);
            if (instance instanceof QueryIndexContributor) {
                ((QueryIndexContributor) instance).contributeQueryIndexes();
                return true;
            }
        } catch (Exception e) {
            log.warn("QueryIndexContributor failed: class={} err={}", clazz.getName(), e.toString());
        }
        return false;
    }

    private static boolean invokeScanMethods(Class<?> clazz) {
        Object instance;
        try {
            instance = newInstance(clazz);
        } catch (Exception e) {
            log.warn("QueryIndexScan instantiate failed: class={} err={}", clazz.getName(), e.toString());
            return false;
        }
        boolean any = false;
        for (Method m : clazz.getMethods()) {
            if (m.getDeclaringClass() == Object.class) {
                continue;
            }
            if (m.getParameterCount() != 0 || m.getReturnType() != String.class) {
                continue;
            }
            if (!Modifier.isPublic(m.getModifiers()) || Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            if (m.getDeclaringClass() == RuntimeSql.class) {
                continue;
            }
            try {
                m.invoke(instance);
                any = true;
            } catch (Exception e) {
                log.warn("QueryIndexScan method failed: class={} method={} err={}",
                        clazz.getName(), m.getName(), e.toString());
            }
        }
        return any;
    }

    private static Object newInstance(Class<?> clazz) throws Exception {
        Constructor<?> ctor = clazz.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }
}

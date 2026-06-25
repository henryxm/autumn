package cn.org.autumn.database;

import cn.org.autumn.config.Config;
import cn.org.autumn.exception.AException;
import cn.org.autumn.install.InstallMode;
import cn.org.autumn.model.Error;
import java.util.concurrent.Callable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 全局 CRUD 写库守卫（运行时开关 + ThreadLocal 作用域）。
 * <p>
 * 标准 API（详见 {@code docs/AI_DATABASE_READ_ONLY.md}）：
 * <ul>
 *   <li>{@link #writable()} / {@link #allow()} — 是否允许写库</li>
 *   <li>{@link #opt(Runnable)} — 可选写，不允许则跳过</li>
 *   <li>{@link #force(Runnable)} — 必要写，绕过只读</li>
 *   <li>{@link #blocked(Throwable)} / {@link #suppress(Throwable, String)} — 异常消化</li>
 * </ul>
 * enforcement：{@link CrudInterceptor}（MyBatis）、{@link #enforce()}（JDBC/DDL）。
 */
@Slf4j
@Component
public class CrudGuard {

    public enum Scope {
        SYSTEM,
        USER
    }

    public static final class Snapshot {
        private final Scope scope;
        private final int depth;

        private Snapshot(Scope scope, int depth) {
            this.scope = scope;
            this.depth = depth;
        }
    }

    private static final ThreadLocal<Scope> SCOPE = new ThreadLocal<>();
    private static final ThreadLocal<Integer> DEPTH = new ThreadLocal<>();

    /** 启动后由 {@link #bindHolder()} 赋值，避免热路径 {@link Config#getBean(Class)}。 */
    private static volatile CrudGuard holder;

    private volatile boolean databaseWrite = true;
    private volatile boolean localWrite = true;
    private volatile String description = "系统升级，功能暂停使用，请稍后重试";

    public boolean global() {
        return databaseWrite;
    }

    public boolean local() {
        return localWrite;
    }

    public String hint() {
        return description == null ? "" : description;
    }

    public void apply(boolean databaseWrite, boolean localWrite, String description) {
        this.databaseWrite = databaseWrite;
        this.localWrite = localWrite;
        if (description != null) {
            this.description = description;
        }
    }

    @PostConstruct
    private void bindHolder() {
        holder = this;
    }

    @PreDestroy
    private void unbindHolder() {
        if (holder == this) {
            holder = null;
        }
    }

    static void bindHolder(CrudGuard guard) {
        holder = guard;
    }

    static void clearHolder() {
        holder = null;
    }

    public Scope scope() {
        Scope s = SCOPE.get();
        return s == null ? Scope.SYSTEM : s;
    }

    public void user() {
        SCOPE.set(Scope.USER);
    }

    public void clear() {
        SCOPE.remove();
    }

    public static Snapshot capture() {
        Scope scope = SCOPE.get();
        Integer depth = DEPTH.get();
        return new Snapshot(scope, depth == null ? 0 : depth);
    }

    public static void with(Snapshot snapshot, Runnable action) {
        if (action == null) {
            return;
        }
        with(snapshot, () -> {
            action.run();
            return null;
        });
    }

    public static <T> T with(Snapshot snapshot, Callable<T> action) {
        if (action == null) {
            return null;
        }
        Scope previousScope = SCOPE.get();
        Integer previousDepth = DEPTH.get();
        try {
            if (snapshot != null) {
                if (snapshot.scope != null) {
                    SCOPE.set(snapshot.scope);
                } else {
                    SCOPE.remove();
                }
                if (snapshot.depth > 0) {
                    DEPTH.set(snapshot.depth);
                } else {
                    DEPTH.remove();
                }
            }
            return action.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            restore(previousScope, previousDepth);
        }
    }

    private static void restore(Scope scope, Integer depth) {
        if (scope != null) {
            SCOPE.set(scope);
        } else {
            SCOPE.remove();
        }
        if (depth != null && depth > 0) {
            DEPTH.set(depth);
        } else {
            DEPTH.remove();
        }
    }

    public static CrudGuard get() {
        CrudGuard cached = holder;
        if (cached != null) {
            return cached;
        }
        Object bean = Config.getBean(CrudGuard.class);
        return bean instanceof CrudGuard ? (CrudGuard) bean : null;
    }

    public static boolean writable() {
        CrudGuard guard = holder;
        if (guard == null) {
            guard = get();
        }
        return guard == null || guard.allow();
    }

    public static void opt(Runnable action) {
        if (action != null && writable()) {
            action.run();
        }
    }

    public static <T> T opt(Callable<T> action) {
        if (action == null || !writable()) {
            return null;
        }
        try {
            return action.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean blocked(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof AException) {
                return ((AException) current).getCode() == Error.DATABASE_READ_ONLY.getCode();
            }
            Throwable next = current.getCause();
            if (next == current) {
                break;
            }
            current = next;
        }
        return false;
    }

    public static boolean suppress(Throwable throwable) {
        return suppress(throwable, null);
    }

    public static boolean suppress(Throwable throwable, String tag) {
        if (!blocked(throwable)) {
            return false;
        }
        if (log.isDebugEnabled()) {
            if (StringUtils.isNotBlank(tag)) {
                log.debug("{} skipped (database read-only)", tag);
            } else {
                log.debug("Write skipped (database read-only)");
            }
        }
        return true;
    }

    public static void enforce() {
        CrudGuard guard = holder;
        if (guard == null) {
            guard = get();
        }
        if (guard != null) {
            guard.enforceWrite();
        }
    }

    /** MyBatis / JDBC 写路径：安装模式跳过，否则 {@link #check()}。 */
    public void enforceWrite() {
        if (InstallMode.isActive()) {
            return;
        }
        check();
    }

    public static void force(Runnable action) {
        Integer depth = DEPTH.get();
        DEPTH.set(depth == null ? 1 : depth + 1);
        try {
            action.run();
        } finally {
            int next = depth == null ? 0 : depth;
            if (next <= 0) {
                DEPTH.remove();
            } else {
                DEPTH.set(next);
            }
        }
    }

    public static <T> T force(Callable<T> action) {
        if (action == null) {
            return null;
        }
        final Object[] holder = new Object[1];
        force(() -> {
            try {
                holder[0] = action.call();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        @SuppressWarnings("unchecked")
        T result = (T) holder[0];
        return result;
    }

    public void check() {
        if (!allow()) {
            throw readonly();
        }
    }

    /** 当前作用域下是否允许写库（实例判断；静态入口 {@link #writable()}）。 */
    public boolean allow() {
        if (InstallMode.isActive()) {
            return true;
        }
        if (bypass()) {
            return true;
        }
        if (!databaseWrite) {
            return false;
        }
        return localWrite || scope() != Scope.USER;
    }

    private AException readonly() {
        String msg = hint();
        if (msg == null || msg.isEmpty()) {
            return new AException(Error.DATABASE_READ_ONLY);
        }
        return new AException(msg, Error.DATABASE_READ_ONLY.getCode());
    }

    private boolean bypass() {
        Integer depth = DEPTH.get();
        return depth != null && depth > 0;
    }
}

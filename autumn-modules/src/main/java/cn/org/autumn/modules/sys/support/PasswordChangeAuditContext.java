package cn.org.autumn.modules.sys.support;

import org.apache.commons.lang3.StringUtils;
import java.util.concurrent.Callable;

/**
 * 口令写库路径的审计上下文（ThreadLocal）：供 {@code SysUserService#save} 等嵌套调用带上 channel/operator。
 */
public final class PasswordChangeAuditContext {

    private static final ThreadLocal<String> CHANNEL = new ThreadLocal<>();
    private static final ThreadLocal<String> OPERATOR = new ThreadLocal<>();

    private PasswordChangeAuditContext() {
    }

    public static void run(String channel, String operatorUserUuid, Runnable action) {
        String prevChannel = CHANNEL.get();
        String prevOperator = OPERATOR.get();
        try {
            if (StringUtils.isNotBlank(channel)) {
                CHANNEL.set(channel);
            }
            if (StringUtils.isNotBlank(operatorUserUuid)) {
                OPERATOR.set(operatorUserUuid);
            }
            action.run();
        } finally {
            restore(CHANNEL, prevChannel);
            restore(OPERATOR, prevOperator);
        }
    }

    public static <T> T call(String channel, String operatorUserUuid, Callable<T> action) {
        String prevChannel = CHANNEL.get();
        String prevOperator = OPERATOR.get();
        try {
            if (StringUtils.isNotBlank(channel)) {
                CHANNEL.set(channel);
            }
            if (StringUtils.isNotBlank(operatorUserUuid)) {
                OPERATOR.set(operatorUserUuid);
            }
            return action.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            restore(CHANNEL, prevChannel);
            restore(OPERATOR, prevOperator);
        }
    }

    private static void restore(ThreadLocal<String> slot, String previous) {
        if (previous != null) {
            slot.set(previous);
        } else {
            slot.remove();
        }
    }

    public static String channelOrDefault(String fallback) {
        String c = CHANNEL.get();
        return StringUtils.isNotBlank(c) ? c : fallback;
    }

    public static String operatorOrNull() {
        return OPERATOR.get();
    }
}

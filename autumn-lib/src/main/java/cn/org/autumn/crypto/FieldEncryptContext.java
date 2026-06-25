package cn.org.autumn.crypto;

import java.util.function.Supplier;

/**
 * 控制 Service 层字段加解密是否跳过（如 {@link FieldEncryptContext#runSkip} 内读取密文原文）。
 */
public final class FieldEncryptContext {

    private static final ThreadLocal<Boolean> SKIP = new ThreadLocal<>();

    private FieldEncryptContext() {
    }

    public static boolean isSkip() {
        return Boolean.TRUE.equals(SKIP.get());
    }

    public static void runSkip(Runnable action) {
        setSkip(true);
        try {
            action.run();
        } finally {
            SKIP.remove();
        }
    }

    public static <T> T runSkip(Supplier<T> action) {
        setSkip(true);
        try {
            return action.get();
        } finally {
            SKIP.remove();
        }
    }

    private static void setSkip(boolean skip) {
        if (skip) {
            SKIP.set(Boolean.TRUE);
        } else {
            SKIP.remove();
        }
    }
}

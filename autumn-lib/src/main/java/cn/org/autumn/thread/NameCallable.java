package cn.org.autumn.thread;

import java.util.concurrent.Callable;

public interface NameCallable<V> extends Callable<V> {
    default String name() {
        return null;
    }
}

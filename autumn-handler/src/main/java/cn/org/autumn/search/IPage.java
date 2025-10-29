package cn.org.autumn.search;

public interface IPage<T> {
    int page();

    int size();

    default int offset() {
        return (page() - 1) * size();
    }

    T data();
}

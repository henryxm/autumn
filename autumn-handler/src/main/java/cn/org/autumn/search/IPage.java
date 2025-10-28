package cn.org.autumn.search;

public interface IPage<T> {
    int page();

    int size();

    T data();
}

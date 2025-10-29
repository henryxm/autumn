package cn.org.autumn.search;

public interface IPage<T> {
    String getName();

    void setName(String name);

    int getPage();

    void setPage(int page);

    int getSize();

    void setSize(int size);

    T getData();

    void setData(T t);

    default int getOffset() {
        return (getPage() - 1) * getSize();
    }
}

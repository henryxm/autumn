package cn.org.autumn.search;

import java.util.List;

public interface IPages<T> extends IResult {
    List<T> getList();

    int getPage();

    int getSize();

    int getTotal();

    void setList(List<T> list);

    void setPage(int page);

    void setSize(int size);

    void setTotal(int total);
}

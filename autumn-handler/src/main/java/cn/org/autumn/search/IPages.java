package cn.org.autumn.search;

import java.util.List;

public interface IPages<T> extends IResult {
    List<T> list();

    int page();

    int size();

    int total();
}

package cn.org.autumn.model;

import cn.org.autumn.annotation.SearchType;
import cn.org.autumn.search.IPages;
import cn.org.autumn.search.Result;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "数据列表")
@SearchType("Pages")
public class Pages<T> extends DefaultEncrypt implements IPages<T>, Serializable {

    private static final long serialVersionUID = 1L;

    Result result = new Result(Pages.class);

    @Schema(name = "列表")
    List<T> list;

    @Schema(name = "页数")
    int page = 0;

    @Schema(name = "个数")
    int size = 0;

    @Schema(name = "总数")
    int total = 0;

    public static <T> Pages<T> single(T t) {
        Pages<T> pages = new Pages<>();
        if (null != t) {
            List<T> list = new ArrayList<>();
            list.add(t);
            pages.setList(list);
            pages.setPage(1);
            pages.setSize(1);
            pages.setTotal(1);
        }
        return pages;
    }

    public static <T> Pages<T> pages(List<T> list) {
        Pages<T> pages = new Pages<>();
        if (null != list) {
            pages.setList(list);
            pages.setPage(1);
            pages.setSize(list.size());
            pages.setTotal(list.size());
        }
        return pages;
    }

    public static <T> Pages<T> pages(List<T> list, int page, int total) {
        Pages<T> pages = new Pages<>();
        if (null != list) {
            pages.setList(list);
            pages.setPage(page);
            pages.setSize(list.size());
            pages.setTotal(total);
        }
        return pages;
    }
}
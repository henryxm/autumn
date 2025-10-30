package cn.org.autumn.search;

import cn.org.autumn.annotation.SearchType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@SearchType(value = "Common", name = "综合", alias = "通用搜素", describe = "通用类型搜索", order = -1)
public class Common implements IResult {
    private static final long serialVersionUID = 1L;
    private Result result = new Result(Common.class);
}

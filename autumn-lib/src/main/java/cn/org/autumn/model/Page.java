package cn.org.autumn.model;

import cn.org.autumn.search.IPage;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Page<T> extends Request<T> implements IPage<T> {

    @Schema(name = "页数")
    private int page = 1;

    @Schema(name = "个数")
    private int size = 20;

    public Page(T data) {
        super(data);
    }

    public int offset() {
        return (page - 1) * size;
    }

    public int page() {
        return page;
    }

    public int size() {
        return size;
    }
}

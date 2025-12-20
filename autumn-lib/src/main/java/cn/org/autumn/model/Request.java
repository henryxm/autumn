package cn.org.autumn.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Request<T> extends Client {

    @Schema(name = "数据实体")
    private T data;

    public T data() {
        return data;
    }
}

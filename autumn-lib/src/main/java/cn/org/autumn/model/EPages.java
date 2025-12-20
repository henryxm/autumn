package cn.org.autumn.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "数据列表扩展")
public class EPages<T, D> extends Pages<T> {

    @Schema(name = "扩展数据")
    private D data;
}
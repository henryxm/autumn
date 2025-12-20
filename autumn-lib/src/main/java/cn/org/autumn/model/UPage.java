package cn.org.autumn.model;

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
@Schema(name = "翻页")
public class UPage<T> extends Page<T> {

    @Schema(name = "用户")
    private String user;

    public UPage(T data) {
        super(data);
    }
}

package cn.org.autumn.model;

import cn.org.autumn.annotation.JsonMap;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@JsonMap
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "链接")
public class Link implements Serializable {

    @Schema(name = "地址")
    private String url = "";

    @Schema(name = "标题")
    private String title = "";

    public Link(String url) {
        this.url = url;
    }
}

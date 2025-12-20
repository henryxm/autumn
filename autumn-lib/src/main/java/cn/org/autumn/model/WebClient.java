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
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "浏览器客户端")
public class WebClient extends Client {
    private String userAgent;
    private String platform;
    private String language;
    private String appVersion;
    private String vendor;
    private Integer screenWidth;
    private Integer screenHeight;
    private String timezone;
    private Boolean cookieEnabled;
    private Double deviceMemory;
    private Integer hardwareConcurrency;
}

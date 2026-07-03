package cn.org.autumn.modules.qrc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SessionExchangeRequest {
    @Schema(description = "PC 一次性交换令牌")
    private String exchange;
    private boolean rememberMe;
}

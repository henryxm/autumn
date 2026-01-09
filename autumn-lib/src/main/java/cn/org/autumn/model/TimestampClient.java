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
public class TimestampClient extends Client {
    @Schema(name = "秘钥过期时间")
    private long expire = 0;

    @Schema(name = "客户端时间戳")
    private long timestamp = 0;
}

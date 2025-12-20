package cn.org.autumn.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 上传客户端公钥响应
 *
 * @author Autumn
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "上传客户端公钥响应", description = "客户端公钥保存结果")
public class ClientPublicKeyResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 客户端标识（Session ID或客户端ID）
     */
    @Schema(name = "uuid", description = "客户端标识")
    private String uuid;

    /**
     * 公钥过期时间戳（毫秒）
     */
    @Schema(name = "expireTime", description = "公钥过期时间戳（毫秒）")
    private Long expireTime;

    /**
     * 公钥创建时间戳（毫秒）
     */
    @Schema(name = "createTime", description = "公钥创建时间戳（毫秒）")
    private Long createTime;

    /**
     * 响应消息
     */
    @Schema(name = "message", description = "响应消息")
    private String message;
}

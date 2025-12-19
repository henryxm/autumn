package cn.org.autumn.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 客户端公钥状态响应
 *
 * @author Autumn
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "客户端公钥状态响应", description = "客户端公钥状态检查结果")
public class ClientPublicKeyStatusResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 是否存在有效的客户端公钥
     */
    @Schema(name = "hasValidKey", description = "是否存在有效的客户端公钥", example = "true")
    private Boolean hasValidKey;

    /**
     * 客户端标识（Session ID或客户端ID）
     */
    @Schema(name = "uuid", description = "客户端标识", example = "session-id-12345")
    private String uuid;

    /**
     * 公钥过期时间戳（毫秒）
     */
    @Schema(name = "expireTime", description = "公钥过期时间戳（毫秒）", example = "1704672000000")
    private Long expireTime;

    /**
     * 公钥创建时间戳（毫秒）
     */
    @Schema(name = "createTime", description = "公钥创建时间戳（毫秒）", example = "1704067200000")
    private Long createTime;

    /**
     * 公钥是否已过期
     */
    @Schema(name = "isExpired", description = "公钥是否已过期", example = "false")
    private Boolean isExpired;
}

package cn.org.autumn.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 上传客户端公钥请求
 *
 * @author Autumn
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "上传客户端公钥请求", description = "客户端上传自己的RSA公钥给服务端保存")
public class ClientPublicKeyRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 客户端UUID标识
     * 客户端生成并存储在本地，用于关联密钥对
     */
    @NotBlank(message = "UUID不能为空")
    @Schema(name = "uuid", description = "客户端UUID标识", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
    private String uuid;

    /**
     * 客户端公钥（Base64编码，支持PEM格式）
     */
    @NotBlank(message = "客户端公钥不能为空")
    @Schema(name = "publicKey", description = "客户端公钥（Base64编码，支持PEM格式）", required = true, example = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC...")
    private String publicKey;

    /**
     * 公钥过期时间戳（毫秒）
     * 客户端可以自定义过期时间，如果不提供则由后端控制
     * 如果提供，必须大于当前时间，且不能超过最大有效期（默认7天）
     */
    @Schema(name = "expireTime", description = "公钥过期时间戳（毫秒），可选，如果不提供则由后端控制", required = false, example = "1704672000000")
    private Long expireTime;
}

package cn.org.autumn.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 初始化加密请求
 * 客户端提交自己的公钥，服务端返回服务端公钥和加密后的AES密钥
 *
 * @author Autumn
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "初始化加密请求", description = "客户端提交公钥，获取服务端公钥和AES密钥")
public class InitRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 客户端公钥（Base64编码，支持PEM格式）
     */
    @NotBlank(message = "客户端公钥不能为空")
    @Schema(name = "publicKey", description = "客户端公钥（Base64编码，支持PEM格式）", required = true, example = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC...")
    private String publicKey;

    /**
     * 客户端公钥过期时间戳（毫秒）
     * 客户端可以自定义过期时间，如果不提供则由后端控制
     * 如果提供，必须大于当前时间，且不能超过最大有效期（默认7天）
     */
    @Schema(name = "expireTime", description = "客户端公钥过期时间戳（毫秒），可选，如果不提供则由后端控制", required = false, example = "1704672000000")
    private Long expireTime;
}

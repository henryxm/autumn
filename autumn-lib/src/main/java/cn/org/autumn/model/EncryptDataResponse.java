package cn.org.autumn.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 加密数据响应
 *
 * @author Autumn
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "加密数据响应", description = "服务端使用客户端公钥加密后的数据")
public class EncryptDataResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 加密后的数据（Base64编码）
     */
    @Schema(name = "encrypted", description = "加密后的数据（Base64编码）", example = "base64-encoded-encrypted-data...")
    private String encrypted;

    /**
     * 响应消息
     */
    @Schema(name = "message", description = "响应消息", example = "数据加密成功")
    private String message;
}

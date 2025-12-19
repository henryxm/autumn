package cn.org.autumn.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 使用客户端公钥加密数据请求（包含UUID）
 *
 * @author Autumn
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "使用客户端公钥加密数据请求", description = "请求服务端使用客户端公钥加密数据，需要提供UUID")
public class EncryptDataWithUuidRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 客户端UUID标识
     */
    @NotBlank(message = "UUID不能为空")
    @Schema(name = "uuid", description = "客户端UUID标识", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
    private String uuid;

    /**
     * 待加密的数据
     */
    @NotBlank(message = "待加密数据不能为空")
    @Schema(name = "data", description = "待加密的数据", required = true, example = "这是需要加密的敏感数据")
    private String data;
}

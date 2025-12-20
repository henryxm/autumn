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
 * 获取服务端公钥请求
 *
 * @author Autumn
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "获取服务端公钥请求", description = "客户端提交UUID获取服务端的RSA公钥")
public class PublicKeyRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 客户端UUID标识
     * 客户端生成并存储在本地，用于关联密钥对
     */
    @NotBlank(message = "UUID不能为空")
    @Schema(name = "uuid", description = "客户端UUID标识", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
    private String uuid;
}

package cn.org.autumn.model;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * AES密钥请求
 *
 * @author Autumn
 */
@Data
public class AesKeyRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 客户端UUID标识
     */
    @NotBlank(message = "UUID不能为空")
    private String uuid;
}

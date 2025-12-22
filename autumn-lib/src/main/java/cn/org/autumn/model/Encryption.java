package cn.org.autumn.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * 初始化加密响应
 * 包含服务端公钥和加密后的AES密钥信息
 *
 * @author Autumn
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "初始化加密响应", description = "包含服务端公钥和加密后的AES密钥")
public class Encryption implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(name = "ras", description = "RSA")
    private RsaKey ras;

    @Schema(name = "aes", description = "AES")
    private AesKey aes;
}

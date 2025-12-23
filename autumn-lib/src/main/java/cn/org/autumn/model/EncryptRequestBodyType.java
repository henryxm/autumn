package cn.org.autumn.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Encrypt请求Body类型信息
 *
 * @author Autumn
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Encrypt请求Body类型", description = "Encrypt请求Body类型信息")
public class EncryptRequestBodyType implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(name = "参数名称", description = "方法参数名称")
    private String parameterName;

    @Schema(name = "类型完整名称", description = "类型的完整类名")
    private String type;

    @Schema(name = "类型简单名称", description = "类型的简单类名")
    private String simpleType;
}

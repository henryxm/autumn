package cn.org.autumn.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Encrypt端点信息
 *
 * @author Autumn
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Encrypt端点信息", description = "包含请求body或返回值类型为Encrypt的接口信息")
public class EndpointInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(name = "请求路径", description = "完整的请求路径")
    private String path;

    @Schema(name = "HTTP方法", description = "HTTP请求方法（GET/POST/PUT/DELETE/PATCH）")
    private String method;

    @Schema(name = "是否为Encrypt Body类型", description = "实现了Encrypt接口的请求Body类型")
    private Boolean encryptBody;

    @Schema(name = "是否为Encrypt返回类型", description = "返回值类型是否实现了Encrypt接口")
    private Boolean encryptReturn;
}

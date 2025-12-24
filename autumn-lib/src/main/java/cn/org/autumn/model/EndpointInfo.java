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

    @Schema(name = "标注参数是否是支持加密", description = "根据接口规范，标注确认参数是否支持加密的body还是支持返回值加密")
    private Supported param;

    @Schema(name = "标注参数是否是强制加密", description = "根据接口规范，如果接口参数支持加密功能，如果标记为强制加密，则必须使用强制加密，另外: 该参数只强制加密，不强制不加密")
    private Supported force;
}

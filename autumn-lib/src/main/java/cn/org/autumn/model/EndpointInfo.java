package cn.org.autumn.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serializable;

/**
 * Encrypt端点信息
 *
 * @author Autumn
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Encrypt端点信息", description = "包含请求body或返回值类型为Encrypt的接口信息")
public class EndpointInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(name = "请求路径", description = "完整的请求路径")
    private String path;

    @Schema(name = "HTTP方法", description = "HTTP请求方法（GET/POST/PUT/DELETE/PATCH）")
    private String method;

    @Schema(name = "标注参数是否是支持加密", description = "根据接口规范，标注请求body或返回值是否支持加密（含兼容加密返回）")
    private Supported param;

    @Schema(name = "标注参数是否是强制加密", description = "根据接口规范，如果接口参数支持加密功能，如果标记为强制加密，则必须使用强制加密，另外: 该参数只强制加密，不强制不加密")
    private Supported force;

    @Schema(name = "表明加密数据是否被包装", description = "如果应用请求使用Request<T>泛型来接收数据，实际数据被包装在Request的data中则表明请求被包装；同理，返回值如果是Response<T>类型，则表明返回值被包装")
    private Wrap wrap;
}

package cn.org.autumn.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Wrap implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(name = "表明加密数据是否被包装", description = "如果应用请求使用Request<T>泛型来接收数据，实际数据被包装在Request的data中则表明请求被包装")
    boolean request;

    @Schema(name = "表明加密数据是否被包装", description = "如果应用返回值使用Response<T>泛型来处理数据，实际数据被包装在Response的data中则表明返回被包装")
    boolean response;
}

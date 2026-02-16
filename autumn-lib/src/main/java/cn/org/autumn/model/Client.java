package cn.org.autumn.model;

import cn.org.autumn.annotation.JsonMap;
import cn.org.autumn.search.IResult;
import cn.org.autumn.search.Result;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

@JsonMap
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "客户端信息")
public class Client extends Phone implements IResult, Encrypt {
    private static final long serialVersionUID = 1L;

    private Result result = new Result(Client.class);

    @Id
    private String uuid = "";

    private String name = "";

    private String udid = "";

    private String mode = "";

    private String install = "";

    private String channel = "";

    private String profile = "";

    private String version = "";

    private String bundleId = "";
}

package cn.org.autumn.model;

import cn.org.autumn.annotation.JsonMap;
import cn.org.autumn.search.IResult;
import cn.org.autumn.search.Result;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

@JsonMap
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "客户端信息")
public class Client extends Phone implements IResult, Encrypt {
    private static final long serialVersionUID = 1L;

    private Result result = new Result(Client.class);

    @Id
    private String uuid = "";

    @Indexed
    private String name = "";

    @Indexed
    private String udid = "";

    @Indexed
    private String mode = "";

    @Indexed
    private String install = "";

    @Indexed
    private String channel = "";

    @Indexed
    private String profile = "";

    @Indexed
    private String version = "";

    @Indexed
    private String bundleId = "";

    @Indexed
    private String encrypt = "";
}

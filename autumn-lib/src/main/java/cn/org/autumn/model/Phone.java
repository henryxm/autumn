package cn.org.autumn.model;

import cn.org.autumn.annotation.JsonMap;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;

import java.io.Serializable;

@JsonMap
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Phone implements Serializable {
    private static final long serialVersionUID = 1L;

    private String deviceName = "";

    @Indexed
    private String systemVersion = "";

    private String systemName = "";

    private String model = "";

    private String localizedModel = "";

    @Indexed
    private String identifierForVendor = "";

    private String product;
}

package cn.org.autumn.model;

import cn.org.autumn.annotation.JsonMap;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@JsonMap
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Platform extends DefaultEncrypt implements Serializable {

    //ios,harmony,android,macos,linux,windows...
    private String platform;
}

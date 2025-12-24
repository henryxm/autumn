package cn.org.autumn.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class Supported implements Serializable {
    /**
     * 表明请求的body是支持否加密，或者是否要求强制加密
     */
    boolean body;

    /**
     * 表明返回值是否支持加密，或者是否被要求强制加密
     */
    boolean ret;
}

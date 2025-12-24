package cn.org.autumn.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class Supported implements Serializable {
    boolean body;

    boolean ret;
}

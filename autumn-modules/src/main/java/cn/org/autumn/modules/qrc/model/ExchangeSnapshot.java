package cn.org.autumn.modules.qrc.model;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExchangeSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    private String exchange;
    private String user;
    private String uuid;
    private long expired;
}

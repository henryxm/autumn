package cn.org.autumn.modules.qrc.dto;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScannerBrief implements Serializable {
    private static final long serialVersionUID = 1L;

    private String displayName;
    private String icon;
}

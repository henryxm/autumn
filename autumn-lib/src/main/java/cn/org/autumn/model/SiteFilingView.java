package cn.org.autumn.model;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SiteFilingView implements Serializable {
    private static final long serialVersionUID = 1L;

    private String type;
    private String number;
    private String prefix;
    private String suffix;
    private String url;
    private boolean showIcon;
    private boolean external;
}

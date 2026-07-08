package cn.org.autumn.model;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ComplianceFilingItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private String type = ComplianceFilingType.icp.name();
    private String number = "";
    private String prefix = "";
    private String suffix = "";
    private String url = "";
    private boolean showIcon = true;
}

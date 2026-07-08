package cn.org.autumn.model;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SiteLegalLinksView implements Serializable {
    private static final long serialVersionUID = 1L;

    private String privacyUrl;
    private String termsUrl;
    private String aboutUrl;
    private String helpUrl;
    private String contactUrl;

    private boolean privacyExternal;
    private boolean termsExternal;
    private boolean aboutExternal;
    private boolean helpExternal;
    private boolean contactExternal;
}

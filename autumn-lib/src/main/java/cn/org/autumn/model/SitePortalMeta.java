package cn.org.autumn.model;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SitePortalMeta implements Serializable {
    private static final long serialVersionUID = 1L;

    private String copyrightHolder = "";
    private String copyrightYearStart = "";
    private String copyrightYearEnd = "";
    private String versionLabel = "";
}

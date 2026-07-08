package cn.org.autumn.model;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SitePortalBranding implements Serializable {
    private static final long serialVersionUID = 1L;

    private String siteName = "";
    private String tagline = "";
    private String logoUrl = "";
    private String logoAlt = "";
}

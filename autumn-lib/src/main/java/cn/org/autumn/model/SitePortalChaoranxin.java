package cn.org.autumn.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 超然信公开落地页（/ops/discover）配置；不含任何运维证书数据。
 */
@Getter
@Setter
public class SitePortalChaoranxin implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_OFFICIAL_URL = "https://chaoran.xin/";

    /** 官网 / 主 CTA */
    private String officialUrl = DEFAULT_OFFICIAL_URL;

    /** 安装二维码内容；空则回退 {@link #officialUrl} */
    private String installQrUrl = "";

    /** 可选应用市场链接；未配置则仅展示「应用市场搜索」文案 */
    private List<SitePortalStoreLink> storeLinks = new ArrayList<>();
}

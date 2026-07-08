package cn.org.autumn.model;

import org.apache.commons.lang.StringUtils;

/** 中国大陆站点常见备案/许可类型（页脚合规展示）。 */
public enum ComplianceFilingType {
    /** ICP 备案（工信部） */
    icp,
    /** 公安联网备案 / 网安备 */
    psb,
    /** App 备案（工信部） */
    app_icp,
    /** 互联网信息服务算法备案（网信办） */
    algorithm,
    /** ICP 经营许可证（B25 等，工信部） */
    icp_license,
    /** 增值电信业务经营许可证（工信部） */
    telecom,
    /** 网络文化经营许可证（文旅部） */
    network_culture,
    /** 信息网络传播视听节目许可证（广电总局） */
    audiovisual,
    /** 广播电视节目制作经营许可证（广电总局） */
    broadcast_tv,
    /** 网络表演/互联网直播服务许可（文旅部） */
    online_performance,
    /** 网络游戏版号 ISBN（版署） */
    game_isbn,
    /** 互联网新闻信息服务许可证（网信办/国家新闻出版署） */
    news_license,
    /** 网络出版服务许可证（版署） */
    publish_license,
    /** 互联网药品信息服务资格证（药监局） */
    drug_info,
    /** 医疗器械经营/备案/网络交易服务相关（药监局） */
    medical_device,
    /** 医疗广告审查证明（卫健部门） */
    medical_ad,
    /** 食品经营许可证（市场监管） */
    food_license,
    /** 食品生产许可证（市场监管） */
    food_production,
    /** 特殊食品（保健食品等）注册/备案（市场监管） */
    special_food,
    /** 互联网宗教信息服务许可证（宗教事务部门） */
    internet_religion,
    /** 地图审图号/测绘资质（自然资源部门） */
    map_approval,
    /** 自定义文案与链接 */
    custom;

    public static final String DEFAULT_ICP_URL = "https://beian.miit.gov.cn/";
    public static final String DEFAULT_PSB_URL_TEMPLATE = "http://www.beian.gov.cn/portal/registerSystemInfo?recordcode={recordcode}";
    public static final String DEFAULT_PSB_FALLBACK_URL = "http://www.beian.gov.cn/";
    public static final String DEFAULT_ICP_LICENSE_URL = "https://www.miit.gov.cn/";
    public static final String DEFAULT_ALGORITHM_URL = "https://beian.cac.gov.cn/";
    public static final String DEFAULT_APP_ICP_URL = "https://beian.miit.gov.cn/";
    public static final String DEFAULT_TELECOM_URL = "https://www.miit.gov.cn/";
    public static final String DEFAULT_NETWORK_CULTURE_URL = "https://www.mct.gov.cn/";
    public static final String DEFAULT_AUDIOVISUAL_URL = "https://www.nrta.gov.cn/";
    public static final String DEFAULT_BROADCAST_TV_URL = "https://www.nrta.gov.cn/";
    public static final String DEFAULT_ONLINE_PERFORMANCE_URL = "https://www.mct.gov.cn/";
    public static final String DEFAULT_GAME_ISBN_URL = "https://www.nppa.gov.cn/";
    public static final String DEFAULT_NEWS_LICENSE_URL = "https://www.nia.gov.cn/";
    public static final String DEFAULT_PUBLISH_LICENSE_URL = "https://www.nppa.gov.cn/";
    public static final String DEFAULT_DRUG_INFO_URL = "https://www.nmpa.gov.cn/";
    public static final String DEFAULT_MEDICAL_DEVICE_URL = "https://www.nmpa.gov.cn/";
    public static final String DEFAULT_MEDICAL_AD_URL = "https://www.nhc.gov.cn/";
    public static final String DEFAULT_FOOD_LICENSE_URL = "https://www.samr.gov.cn/";
    public static final String DEFAULT_FOOD_PRODUCTION_URL = "https://www.samr.gov.cn/";
    public static final String DEFAULT_SPECIAL_FOOD_URL = "https://www.samr.gov.cn/";
    public static final String DEFAULT_INTERNET_RELIGION_URL = "https://www.sara.gov.cn/";
    public static final String DEFAULT_MAP_APPROVAL_URL = "https://www.mnr.gov.cn/";

    public static ComplianceFilingType parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            return custom;
        }
        try {
            return ComplianceFilingType.valueOf(value.trim().toLowerCase());
        } catch (IllegalArgumentException e) {
            return custom;
        }
    }

    /** 类型默认跳转地址；公安备案含 {@code {recordcode}} 占位符。默认指向主管部门门户，可手填更精确链接。 */
    public String defaultUrlTemplate() {
        switch (this) {
            case icp:
                return DEFAULT_ICP_URL;
            case psb:
                return DEFAULT_PSB_URL_TEMPLATE;
            case icp_license:
                return DEFAULT_ICP_LICENSE_URL;
            case algorithm:
                return DEFAULT_ALGORITHM_URL;
            case app_icp:
                return DEFAULT_APP_ICP_URL;
            case telecom:
                return DEFAULT_TELECOM_URL;
            case network_culture:
                return DEFAULT_NETWORK_CULTURE_URL;
            case audiovisual:
                return DEFAULT_AUDIOVISUAL_URL;
            case broadcast_tv:
                return DEFAULT_BROADCAST_TV_URL;
            case online_performance:
                return DEFAULT_ONLINE_PERFORMANCE_URL;
            case game_isbn:
                return DEFAULT_GAME_ISBN_URL;
            case news_license:
                return DEFAULT_NEWS_LICENSE_URL;
            case publish_license:
                return DEFAULT_PUBLISH_LICENSE_URL;
            case drug_info:
                return DEFAULT_DRUG_INFO_URL;
            case medical_device:
                return DEFAULT_MEDICAL_DEVICE_URL;
            case medical_ad:
                return DEFAULT_MEDICAL_AD_URL;
            case food_license:
                return DEFAULT_FOOD_LICENSE_URL;
            case food_production:
                return DEFAULT_FOOD_PRODUCTION_URL;
            case special_food:
                return DEFAULT_SPECIAL_FOOD_URL;
            case internet_religion:
                return DEFAULT_INTERNET_RELIGION_URL;
            case map_approval:
                return DEFAULT_MAP_APPROVAL_URL;
            default:
                return "";
        }
    }

    /** 按配置 URL 或类型默认解析备案跳转地址。 */
    public String resolveUrl(String number, String configuredUrl) {
        if (StringUtils.isNotBlank(configuredUrl)) {
            return configuredUrl.trim();
        }
        if (this == psb) {
            String code = SitePortalConfig.extractPsbRecordCode(number);
            if (StringUtils.isBlank(code)) {
                return DEFAULT_PSB_FALLBACK_URL;
            }
            return DEFAULT_PSB_URL_TEMPLATE.replace("{recordcode}", code);
        }
        return defaultUrlTemplate();
    }
}

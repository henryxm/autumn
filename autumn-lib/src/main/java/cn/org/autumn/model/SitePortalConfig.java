package cn.org.autumn.model;

import cn.org.autumn.annotation.ConfigField;
import cn.org.autumn.annotation.ConfigParam;
import cn.org.autumn.config.InputType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;

@Getter
@Setter
@ConfigParam(paramKey = SitePortalConfig.CONFIG_KEY, category = SitePortalConfig.config, name = "站点门户配置", description = "登录页品牌、版权、备案与法律链接")
public class SitePortalConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String CONFIG_KEY = "SITE_PORTAL_CONFIG";
    public static final String config = "site_portal_config";

    private static final Pattern YEAR_PATTERN = Pattern.compile("^\\d{4}$");
    private static final Pattern PSB_CODE_PATTERN = Pattern.compile("(\\d{10,})");

    @ConfigField(category = InputType.BooleanType, name = "保存时同步加载页主题", description = "开启后将品牌名与 Logo 同步写入 LOADING_THEME")
    private boolean syncLoadingTheme = true;

    private SitePortalBranding branding = new SitePortalBranding();
    private SitePortalMeta meta = new SitePortalMeta();
    private SitePortalLegalLinks legalLinks = new SitePortalLegalLinks();
    private List<ComplianceFilingItem> filings = new ArrayList<>();

    public List<String> validateAndFix() {
        List<String> fixes = new ArrayList<>();
        if (branding == null) {
            branding = new SitePortalBranding();
        }
        if (meta == null) {
            meta = new SitePortalMeta();
        }
        if (legalLinks == null) {
            legalLinks = new SitePortalLegalLinks();
        }
        if (filings == null) {
            filings = new ArrayList<>();
        }
        branding.setSiteName(trimToEmpty(branding.getSiteName()));
        branding.setTagline(trimToEmpty(branding.getTagline()));
        branding.setLogoAlt(trimToEmpty(branding.getLogoAlt()));
        branding.setLogoUrl(normalizeAssetUrl(branding.getLogoUrl(), fixes, "品牌 Logo URL 无效，已清空"));

        meta.setCopyrightHolder(trimToEmpty(meta.getCopyrightHolder()));
        meta.setVersionLabel(trimToEmpty(meta.getVersionLabel()));
        meta.setCopyrightYearStart(normalizeYear(meta.getCopyrightYearStart(), fixes, "版权起始年无效，已清空"));
        meta.setCopyrightYearEnd(normalizeYear(meta.getCopyrightYearEnd(), fixes, "版权结束年无效，已清空"));

        legalLinks.setPrivacyUrl(normalizeLinkUrl(legalLinks.getPrivacyUrl(), SitePortalLegalLinks.DEFAULT_PRIVACY_PATH, fixes, "隐私政策链接无效，已恢复默认"));
        legalLinks.setTermsUrl(normalizeLinkUrl(legalLinks.getTermsUrl(), SitePortalLegalLinks.DEFAULT_TERMS_PATH, fixes, "服务条款链接无效，已恢复默认"));
        legalLinks.setAboutUrl(normalizeOptionalLinkUrl(legalLinks.getAboutUrl(), fixes, "关于我们链接无效，已清空"));
        legalLinks.setHelpUrl(normalizeOptionalLinkUrl(legalLinks.getHelpUrl(), fixes, "帮助中心链接无效，已清空"));
        legalLinks.setContactUrl(normalizeOptionalLinkUrl(legalLinks.getContactUrl(), fixes, "联系我们链接无效，已清空"));

        List<ComplianceFilingItem> cleaned = new ArrayList<>();
        for (ComplianceFilingItem item : filings) {
            if (item == null) {
                continue;
            }
            item.setType(ComplianceFilingType.parse(item.getType()).name());
            item.setNumber(trimToEmpty(item.getNumber()));
            item.setPrefix(trimToEmpty(item.getPrefix()));
            item.setSuffix(trimToEmpty(item.getSuffix()));
            item.setUrl(normalizeOptionalLinkUrl(item.getUrl(), fixes, "备案链接无效，已清空"));
            if (!ComplianceFilingType.psb.name().equals(item.getType())) {
                item.setShowIcon(false);
            }
            if (StringUtils.isBlank(item.getNumber())) {
                continue;
            }
            cleaned.add(item);
        }
        filings = cleaned;
        return fixes;
    }

    public static String extractPsbRecordCode(String number) {
        if (StringUtils.isBlank(number)) {
            return "";
        }
        Matcher matcher = PSB_CODE_PATTERN.matcher(number);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeYear(String value, List<String> fixes, String fixMessage) {
        String trimmed = trimToEmpty(value);
        if (StringUtils.isBlank(trimmed)) {
            return "";
        }
        if (!YEAR_PATTERN.matcher(trimmed).matches()) {
            fixes.add(fixMessage);
            return "";
        }
        return trimmed;
    }

    private static String normalizeAssetUrl(String value, List<String> fixes, String fixMessage) {
        String trimmed = trimToEmpty(value);
        if (StringUtils.isBlank(trimmed)) {
            return "";
        }
        if (isAllowedUrl(trimmed)) {
            return trimmed;
        }
        fixes.add(fixMessage);
        return "";
    }

    private static String normalizeLinkUrl(String value, String defaultPath, List<String> fixes, String fixMessage) {
        String trimmed = trimToEmpty(value);
        if (StringUtils.isBlank(trimmed)) {
            return defaultPath;
        }
        if (isAllowedUrl(trimmed)) {
            return trimmed;
        }
        fixes.add(fixMessage);
        return defaultPath;
    }

    private static String normalizeOptionalLinkUrl(String value, List<String> fixes, String fixMessage) {
        String trimmed = trimToEmpty(value);
        if (StringUtils.isBlank(trimmed)) {
            return "";
        }
        if (isAllowedUrl(trimmed)) {
            return trimmed;
        }
        fixes.add(fixMessage);
        return "";
    }

    private static boolean isAllowedUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://") || url.startsWith("/");
    }

    public static String buildCopyrightLine(SitePortalMeta meta) {
        if (meta == null) {
            return "";
        }
        if (StringUtils.isBlank(meta.getCopyrightHolder()) && StringUtils.isBlank(meta.getVersionLabel())) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotBlank(meta.getCopyrightHolder())) {
            sb.append("© ");
            String start = trimToEmpty(meta.getCopyrightYearStart());
            String end = trimToEmpty(meta.getCopyrightYearEnd());
            if (StringUtils.isNotBlank(start)) {
                sb.append(start);
                if (StringUtils.isNotBlank(end) && !start.equals(end)) {
                    sb.append("-").append(end);
                } else if (StringUtils.isBlank(end)) {
                    sb.append("-").append(Calendar.getInstance().get(Calendar.YEAR));
                }
            } else if (StringUtils.isNotBlank(end)) {
                sb.append(end);
            } else {
                sb.append(Calendar.getInstance().get(Calendar.YEAR));
            }
            sb.append(" ").append(meta.getCopyrightHolder());
        }
        return sb.toString().trim();
    }
}

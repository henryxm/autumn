package cn.org.autumn.modules.safe.site;

import cn.org.autumn.annotation.PageAware;
import cn.org.autumn.site.SiteFactory;
import cn.org.autumn.site.TemplateFactory;

/**
 * 由代码生成器生成，请勿手动编辑。自定义页面请在 SafeSite 中扩展。
 */
public abstract class SafePages implements SiteFactory.Site, TemplateFactory.Template {
    public final static String siteId = "safe";
    public final static String pack = "safe";

    @PageAware(login = true)
    public String paygateattempt = "modules/safe/paygateattempt";

    @PageAware(login = true)
    public String payusertrustedip = "modules/safe/payusertrustedip";

    @PageAware(login = true)
    public String payusertrusteddevice = "modules/safe/payusertrusteddevice";

    @PageAware(login = true)
    public String payusersecuritysetting = "modules/safe/payusersecuritysetting";

    @PageAware(login = true)
    public String payusergesture = "modules/safe/payusergesture";

    @PageAware(login = true)
    public String payuserbiometric = "modules/safe/payuserbiometric";

    @PageAware(login = true)
    public String paycredentiallog = "modules/safe/paycredentiallog";

    @PageAware(login = true)
    public String payuserpin = "modules/safe/payuserpin";

    public String getPayGateAttemptKey() {
        return getKey("paygateattempt");
    }

    public String getPayUserTrustedIpKey() {
        return getKey("payusertrustedip");
    }

    public String getPayUserTrustedDeviceKey() {
        return getKey("payusertrusteddevice");
    }

    public String getPayUserSecuritySettingKey() {
        return getKey("payusersecuritysetting");
    }

    public String getPayUserGestureKey() {
        return getKey("payusergesture");
    }

    public String getPayUserBiometricKey() {
        return getKey("payuserbiometric");
    }

    public String getPayCredentialLogKey() {
        return getKey("paycredentiallog");
    }

    public String getPayUserPinKey() {
        return getKey("payuserpin");
    }

    @Override
    public String getId() {
        return siteId;
    }

    @Override
    public String getPack() {
        return pack;
    }
}
